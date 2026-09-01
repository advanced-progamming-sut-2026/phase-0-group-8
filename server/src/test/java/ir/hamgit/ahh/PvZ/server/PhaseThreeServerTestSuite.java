package ir.hamgit.ahh.PvZ.server;

import ir.hamgit.ahh.PvZ.controller.Validator;
import ir.hamgit.ahh.PvZ.controller.MinigameController;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.Gender;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.minigame.IZombieGame;
import ir.hamgit.ahh.PvZ.network.PhaseThreeApiClient;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.LobbyState;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchAssignment;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchCommand;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchSnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

 
public final class PhaseThreeServerTestSuite {
    private static int assertions;

    private PhaseThreeServerTestSuite() { }

    public static void main(String[] args) throws Exception {
        accountPersistenceAndPrivacy();
        authoritativeRulesAndWinConditions();
        couchPlayRulesAndIsolation();
        matchmakingAndInvitations();
        if (!Boolean.getBoolean("pvz.tests.skipHttp")) httpRoundTrip();
        System.out.println("Phase 3 tests passed (" + assertions + " assertions).");
    }

    private static void accountPersistenceAndPrivacy() throws Exception {
        Path file = testDirectory("pvz3-accounts").resolve("accounts.dat");
        AccountStore store = new AccountStore(file);
        store.register(user("Alice", "alice@example.com"));
        check(store.exists("alice"), "usernames must be case-insensitively unique");
        expectApi(409, () -> store.register(user("ALICE", "other@example.com")));
        User authenticated = store.authenticate("ALICE", hash("Strong#1"));
        check(authenticated.getPasswordHash() == null, "password hashes must never be sent to clients");
        check(authenticated.getSecurityAnswerHash() == null, "security answers must never be sent to clients");
        authenticated.setNickname("Remote Alice");
        authenticated.updateBestScore(420);
        store.update("Alice", authenticated);
        store.changePassword("Alice", hash("Strong#1"), hash("Changed#2"));
        AccountStore reloaded = new AccountStore(file);
        check("Remote Alice".equals(reloaded.authenticate("Alice", hash("Changed#2")).getNickname()),
            "account/profile changes must survive a server restart");
        check(Integer.valueOf(420).equals(reloaded.leaderboard().get(0).myPoint()),
            "submitted My Point must be sourced from the server");
        reloaded.register(user("NoScore", "noscore@example.com"));
        check(reloaded.leaderboard().stream().filter(row -> row.username().equals("NoScore"))
            .findFirst().orElseThrow().myPoint() == null, "users who never played score mode must show no My Point");
    }

    private static void authoritativeRulesAndWinConditions() {
        AuthoritativeIZombieMatch timer = new AuthoritativeIZombieMatch("timer", "PlantUser", "ZombieUser");
        expectApi(403, () -> timer.command("ZombieUser", new MatchCommand(0, "PLANT", "PEASHOOTER", 2, 0)));
        timer.command("PlantUser", new MatchCommand(0, "PLANT", "PEASHOOTER", 2, 0));
        expectApi(400, () -> timer.react("PlantUser", "TEXT", "custom cheating text"));
        expectApi(400, () -> timer.react("PlantUser", "STICKER", "DANCING_ZOMBIE"));
        timer.react("PlantUser", "EMOJI", "😀");
        for (int i = 0; i < AuthoritativeIZombieMatch.DURATION_TICKS; i++) timer.tick();
        check("PLANTS".equals(timer.snapshot().winner()), "plants must win after surviving about two minutes");

        AuthoritativeIZombieMatch brains = new AuthoritativeIZombieMatch("brains", "P", "Z");
        for (int lane = 0; lane < 5; lane++) {
            MatchSnapshot view = brains.snapshot();
            brains.command("Z", new MatchCommand(view.revision(), "ZOMBIE", "NORMAL", 8, lane));
            for (int tick = 0; tick < 25; tick++) brains.tick();
        }
        for (int tick = 0; tick < 450 && brains.isActive(); tick++) brains.tick();
        check("ZOMBIES".equals(brains.snapshot().winner()), "zombies must win after all brains are eaten");
        check(brains.snapshot().projectiles() != null && brains.snapshot().plants() != null,
            "snapshots must contain all synchronized entity collections");
    }

    private static void couchPlayRulesAndIsolation() {
        MinigameController launcher = new MinigameController();
        check(launcher.start("i_zombie", 1, true)
                && launcher.getActiveGame() instanceof IZombieGame launched && launched.isCouchPlay(),
            "Travel Log's I, Zombie launcher must create the Couch Play variant");
        IZombieGame couch = new IZombieGame(List.of(ZombieType.NORMAL),
            Map.of(ZombieType.NORMAL, 75), true);
        check(couch.isCouchPlay(), "Couch Play must be an offline IZombieGame mode");
        check(couch.getBoard().getPlantsRemaining() == 0,
            "Couch Play must let Player 1 build the defense instead of pre-placing it");
        check(couch.placePlant(PlantType.PEASHOOTER, 1, 0),
            "Player 1 must be able to plant in columns 1-6");
        check(!couch.placePlant(PlantType.SUNFLOWER, 0, 1),
            "Player 1 must not plant outside columns 1-6");
        check(couch.placeZombie(ZombieType.NORMAL, 7, 0),
            "Player 2 must be able to place a zombie in columns 7-8");
        check(!couch.placeZombie(ZombieType.NORMAL, 6, 1),
            "Player 2 must not place a zombie in the plant zone");

        List<ZombieType> attackers = List.of(ZombieType.NORMAL, ZombieType.CONEHEAD,
            ZombieType.IMP, ZombieType.ALL_STAR, ZombieType.RA_ZOMBIE);
        IZombieGame brains = new IZombieGame(attackers, Map.of(
            ZombieType.NORMAL, 25, ZombieType.CONEHEAD, 50, ZombieType.IMP, 75,
            ZombieType.ALL_STAR, 100, ZombieType.RA_ZOMBIE, 125), true);
        for (int lane = 0; lane < attackers.size(); lane++) {
            check(brains.placeZombie(attackers.get(lane), 7, lane),
                "each Player 2 lane attacker must be placeable");
        }
        brains.tick(600);
        check(brains.isOver() && brains.isLost() && "ZOMBIES".equals(brains.getCouchWinnerRole()),
            "Player 2 zombies must win after eating all five local brains");

        IZombieGame timer = new IZombieGame(List.of(ZombieType.NORMAL),
            Map.of(ZombieType.NORMAL, 75), true);
        timer.tick(IZombieGame.COUCH_DURATION_TICKS);
        check(timer.isOver() && timer.isWon() && "PLANTS".equals(timer.getCouchWinnerRole()),
            "Player 1 plants must win after surviving the local two-minute timer");
    }

    private static void matchmakingAndInvitations() throws Exception {
        Path file = testDirectory("pvz3-state").resolve("accounts.dat");
        try (ServerState state = new ServerState(file)) {
            state.accounts.register(user("InviteA", "a@example.com"));
            state.accounts.register(user("InviteB", "b@example.com"));
            state.accounts.register(user("RandomA", "c@example.com"));
            state.accounts.register(user("RandomB", "d@example.com"));
            state.createSession("InviteA", false);
            state.createSession("InviteB", false);
            state.createSession("RandomA", false);
            state.createSession("RandomB", false);
            state.invite("InviteA", "InviteB");
            var invitation = state.lobby("InviteB").invitations().get(0);
            MatchAssignment invited = state.decideInvite("InviteB", invitation.id(), true);
            check(invited.matchId() != null, "accepted invitations must create a match");
            state.joinRandom("RandomA");
            LobbyState randomResult = state.joinRandom("RandomB");
            check(randomResult.match() != null, "the random waiting queue must pair two online users");
            check(!randomResult.match().role().equals(state.lobby("RandomA").match().role()),
                "a match must assign opposite plant and zombie roles");
        }
    }

    private static void httpRoundTrip() throws Exception {
        Path file = testDirectory("pvz3-http").resolve("accounts.dat");
        try (PhaseThreeServer server = new PhaseThreeServer("127.0.0.1", 0, file)) {
            server.start();
            String url = "http://127.0.0.1:" + server.getPort();
            PhaseThreeApiClient first = new PhaseThreeApiClient(url);
            PhaseThreeApiClient second = new PhaseThreeApiClient(url);
            first.register(user("HttpA", "httpa@example.com"));
            second.register(user("HttpB", "httpb@example.com"));
            check(first.health(), "health endpoint must be reachable");
            first.joinRandom();
            LobbyState paired = second.joinRandom();
            MatchAssignment secondAssignment = paired.match();
            MatchAssignment firstAssignment = first.lobby().match();
            check(secondAssignment != null && firstAssignment != null, "HTTP clients must receive match assignments");
            MatchSnapshot initial = first.match(firstAssignment.matchId());
            String action = firstAssignment.role().equals("PLANTS") ? "PLANT" : "ZOMBIE";
            String kind = action.equals("PLANT") ? "SUNFLOWER" : "NORMAL";
            int column = action.equals("PLANT") ? 2 : 8;
            MatchSnapshot changed = first.command(firstAssignment.matchId(),
                new MatchCommand(initial.revision(), action, kind, column, 0));
            check(changed.revision() > initial.revision(), "accepted commands must advance the shared revision");
            first.react(firstAssignment.matchId(), "EMOJI", "😀");
            check(!second.match(secondAssignment.matchId()).reactions().isEmpty(),
                "reactions must be delivered through synchronized snapshots");
            check(first.leaderboard().size() == 2, "leaderboard must be served from the account server");
        }
    }

    private static User user(String username, String email) {
        User user = new User(username, hash("Strong#1"), username + " Nick", email, Gender.MALE);
        user.setSecurityQuestion("What is the name of your first pet?");
        user.setSecurityAnswerHash(hash("pea"));
        user.unlockPlant(PlantType.PEASHOOTER);
        user.unlockPlant(PlantType.SUNFLOWER);
        return user;
    }

    private static String hash(String value) { return Validator.hashSha256(value); }
    private static Path testDirectory(String prefix) throws Exception {
        Path base = Path.of(System.getProperty("pvz.test.dir", "build/tmp/phase3-tests"));
        Files.createDirectories(base);
        return Files.createTempDirectory(base, prefix);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static void expectApi(int status, Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (ApiException e) {
            if (e.status() == status) return;
            throw new AssertionError("Expected HTTP " + status + " but got " + e.status(), e);
        }
        throw new AssertionError("Expected HTTP " + status + " failure.");
    }
}
