package ir.hamgit.ahh.PvZ.server;

import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.InvitationView;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.LobbyState;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchAssignment;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchCommand;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchSnapshot;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordChallenge;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordChallengeRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordResetRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ReactionRequest;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

 
final class ServerState implements AutoCloseable {
    private static final long ONLINE_MS = 15_000;
    private static final long SESSION_EXPIRY_MS = 24 * 60 * 60 * 1000L;
    private static final long INVITE_MS = 25_000;
    private static final long RESET_MS = 5 * 60 * 1000L;
    private static final long DISCONNECT_FORFEIT_MS = 25_000;

    final AccountStore accounts;
    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final Map<String, ResetTicket> resets = new LinkedHashMap<>();
    private final Map<String, Invitation> invitations = new LinkedHashMap<>();
    private final Set<String> randomQueue = new LinkedHashSet<>();
    private final Map<String, AuthoritativeIZombieMatch> matches = new LinkedHashMap<>();
    private final Map<String, MatchAssignment> assignments = new LinkedHashMap<>();
    private final ScheduledExecutorService ticker;
    private final SecureRandom random = new SecureRandom();

    ServerState(Path accountFile) {
        accounts = new AccountStore(accountFile);
        ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "pvz-phase3-match-ticker");
            thread.setDaemon(true);
            return thread;
        });
        ticker.scheduleAtFixedRate(this::safeTick, 100, 100, TimeUnit.MILLISECONDS);
    }

    synchronized String createSession(String username, boolean persistent) {
        String token = randomToken();
        sessions.put(token, new Session(username, System.currentTimeMillis(), persistent));
        return token;
    }

    synchronized String authenticate(String token) {
        Session session = sessions.get(token);
        if (session == null || System.currentTimeMillis() - session.lastSeen > SESSION_EXPIRY_MS) {
            if (token != null) sessions.remove(token);
            throw ApiException.unauthorized("Session expired; please log in again.");
        }
        session.lastSeen = System.currentTimeMillis();
        return session.username;
    }

    synchronized void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    synchronized PasswordChallenge challenge(PasswordChallengeRequest request) {
        if (request == null) throw ApiException.badRequest("Username and email are required.");
        String username = accounts.challengeUsername(request.username(), request.email());
        String token = randomToken();
        resets.put(token, new ResetTicket(username, System.currentTimeMillis() + RESET_MS));
        return new PasswordChallenge(token, accounts.securityQuestion(username));
    }

    synchronized void resetPassword(PasswordResetRequest request) {
        if (request == null || request.resetToken() == null) throw ApiException.badRequest("Reset token is required.");
        ResetTicket ticket = resets.remove(request.resetToken());
        if (ticket == null || ticket.expiresAt < System.currentTimeMillis()) {
            throw ApiException.unauthorized("Password reset session expired.");
        }
        accounts.resetPassword(ticket.username, request.securityAnswerHash(), request.newPasswordHash());
    }

    synchronized User renameAccount(String username, String replacement) {
        if (assignments.containsKey(key(username))) {
            throw ApiException.conflict("Leave the active match before changing username.");
        }
        User renamed = accounts.rename(username, replacement);
        randomQueue.removeIf(name -> name.equalsIgnoreCase(username));
        invitations.values().removeIf(invite -> invite.from.equalsIgnoreCase(username)
            || invite.to.equalsIgnoreCase(username));
        for (Session session : sessions.values()) {
            if (session.username.equalsIgnoreCase(username)) session.username = renamed.getUsername();
        }
        return renamed;
    }

    synchronized LobbyState lobby(String username) {
        purgeTransientState();
        List<InvitationView> incoming = new ArrayList<>();
        for (Invitation invitation : invitations.values()) {
            if (invitation.to.equalsIgnoreCase(username)) {
                incoming.add(new InvitationView(invitation.id, invitation.from, invitation.expiresAt));
            }
        }
        incoming.sort(Comparator.comparingLong(InvitationView::expiresAt));
        List<String> online = onlineUsers();
        online.removeIf(value -> value.equalsIgnoreCase(username));
        return new LobbyState(List.copyOf(incoming), assignments.get(key(username)),
            containsIgnoreCase(randomQueue, username), List.copyOf(online));
    }

    synchronized void invite(String from, String target) {
        if (target == null || target.isBlank() || !accounts.exists(target)) {
            throw ApiException.notFound("That username does not exist.");
        }
        User targetUser = accounts.publicUser(target);
        target = targetUser.getUsername();
        if (from.equalsIgnoreCase(target)) throw ApiException.badRequest("You cannot invite yourself.");
        if (!isOnline(target, ONLINE_MS)) throw ApiException.conflict("That user is offline.");
        if (assignments.containsKey(key(from)) || assignments.containsKey(key(target))) {
            throw ApiException.conflict("One of the players is already in a match.");
        }
        for (Invitation existing : invitations.values()) {
            if (existing.from.equalsIgnoreCase(from) && existing.to.equalsIgnoreCase(target)) {
                throw ApiException.conflict("An invitation is already pending.");
            }
        }
        String id = UUID.randomUUID().toString();
        invitations.put(id, new Invitation(id, from, target, System.currentTimeMillis() + INVITE_MS));
    }

    synchronized MatchAssignment decideInvite(String username, String invitationId, boolean accepted) {
        Invitation invitation = invitations.remove(invitationId);
        if (invitation == null || invitation.expiresAt < System.currentTimeMillis()) {
            throw ApiException.notFound("Invitation expired or no longer exists.");
        }
        if (!invitation.to.equalsIgnoreCase(username)) throw ApiException.forbidden("That invitation is not yours.");
        if (!accepted) return new MatchAssignment(null, null, invitation.from);
        if (!isOnline(invitation.from, ONLINE_MS)) throw ApiException.conflict("The inviting player went offline.");
        if (assignments.containsKey(key(invitation.from)) || assignments.containsKey(key(username))) {
            throw ApiException.conflict("One of the players is already in a match.");
        }
        return createMatch(invitation.from, username).get(key(username));
    }

    synchronized LobbyState joinRandom(String username) {
        purgeTransientState();
        if (assignments.containsKey(key(username))) return lobby(username);
        randomQueue.removeIf(value -> value.equalsIgnoreCase(username));
        String opponent = null;
        for (String candidate : randomQueue) {
            if (!candidate.equalsIgnoreCase(username) && isOnline(candidate, ONLINE_MS)
                && !assignments.containsKey(key(candidate))) {
                opponent = candidate;
                break;
            }
        }
        if (opponent == null) randomQueue.add(username);
        else {
            String selected = opponent;
            randomQueue.removeIf(value -> value.equalsIgnoreCase(selected));
            createMatch(opponent, username);
        }
        return lobby(username);
    }

    synchronized LobbyState leaveRandom(String username) {
        randomQueue.removeIf(value -> value.equalsIgnoreCase(username));
        return lobby(username);
    }

    synchronized MatchSnapshot match(String username, String matchId) {
        AuthoritativeIZombieMatch match = requireMatch(matchId);
        if (!match.hasPlayer(username)) throw ApiException.forbidden("You are not in that match.");
        return match.snapshot();
    }

    synchronized MatchSnapshot command(String username, String matchId, MatchCommand command) {
        return requireMatch(matchId).command(username, command);
    }

    synchronized MatchSnapshot react(String username, String matchId, ReactionRequest reaction) {
        if (reaction == null) throw ApiException.badRequest("Reaction is required.");
        return requireMatch(matchId).react(username, reaction.kind(), reaction.value());
    }

    synchronized void leaveMatch(String username, String matchId) {
        AuthoritativeIZombieMatch match = requireMatch(matchId);
        match.forfeit(username, "A player left the match.");
        assignments.remove(key(username));
    }

    private Map<String, MatchAssignment> createMatch(String first, String second) {
        randomQueue.removeIf(name -> name.equalsIgnoreCase(first) || name.equalsIgnoreCase(second));
        invitations.values().removeIf(invite -> invite.from.equalsIgnoreCase(first)
            || invite.to.equalsIgnoreCase(first) || invite.from.equalsIgnoreCase(second)
            || invite.to.equalsIgnoreCase(second));
        boolean firstPlants = random.nextBoolean();
        String plants = firstPlants ? first : second;
        String zombies = firstPlants ? second : first;
        String id = UUID.randomUUID().toString();
        AuthoritativeIZombieMatch match = new AuthoritativeIZombieMatch(id, plants, zombies);
        matches.put(id, match);
        assignments.put(key(plants), new MatchAssignment(id, "PLANTS", zombies));
        assignments.put(key(zombies), new MatchAssignment(id, "ZOMBIES", plants));
        return assignments;
    }

    private AuthoritativeIZombieMatch requireMatch(String id) {
        AuthoritativeIZombieMatch match = matches.get(id);
        if (match == null) throw ApiException.notFound("Match not found.");
        return match;
    }

    private void safeTick() {
        try { tick(); }
        catch (RuntimeException e) { System.err.println("Match ticker warning: " + e.getMessage()); }
    }

    synchronized void tick() {
        purgeTransientState();
        for (AuthoritativeIZombieMatch match : matches.values()) {
            if (match.isActive()) {
                match.tick();
                if (!isOnline(match.plantUsername(), DISCONNECT_FORFEIT_MS)) {
                    match.finishByRole("ZOMBIES", "Plant player disconnected.");
                } else if (!isOnline(match.zombieUsername(), DISCONNECT_FORFEIT_MS)) {
                    match.finishByRole("PLANTS", "Zombie player disconnected.");
                }
            }
            if (!match.isActive() && !match.resultRecorded()) {
                MatchSnapshot result = match.snapshot();
                accounts.recordMatchResult(match.plantUsername(), match.zombieUsername(), result.winner());
                match.markResultRecorded();
            }
        }
    }

    private void purgeTransientState() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(session -> now - session.lastSeen > SESSION_EXPIRY_MS);
        resets.values().removeIf(reset -> reset.expiresAt < now);
        invitations.values().removeIf(invite -> invite.expiresAt < now);
        randomQueue.removeIf(username -> !isOnline(username, ONLINE_MS) || assignments.containsKey(key(username)));
    }

    private List<String> onlineUsers() {
        Map<String, String> unique = new LinkedHashMap<>();
        for (Session session : sessions.values()) {
            if (System.currentTimeMillis() - session.lastSeen <= ONLINE_MS) {
                unique.putIfAbsent(key(session.username), session.username);
            }
        }
        List<String> result = new ArrayList<>(unique.values());
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private boolean isOnline(String username, long window) {
        long now = System.currentTimeMillis();
        for (Session session : sessions.values()) {
            if (session.username.equalsIgnoreCase(username) && now - session.lastSeen <= window) return true;
        }
        return false;
    }

    @Override public void close() {
        ticker.shutdownNow();
    }

    private static boolean containsIgnoreCase(Set<String> values, String sought) {
        for (String value : values) if (value.equalsIgnoreCase(sought)) return true;
        return false;
    }
    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
    private static String randomToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private static final class Session {
        String username; long lastSeen; final boolean persistent;
        Session(String username, long lastSeen, boolean persistent) {
            this.username = username; this.lastSeen = lastSeen; this.persistent = persistent;
        }
    }
    private record ResetTicket(String username, long expiresAt) { }
    private record Invitation(String id, String from, String to, long expiresAt) { }
}
