package ir.hamgit.ahh.PvZ.network;

import ir.hamgit.ahh.PvZ.model.User;

import java.util.List;

 
public final class PhaseThreeDtos {
    private PhaseThreeDtos() {
    }

    public record Message(String message) { }
    public record ErrorResponse(String error) { }
    public record LoginRequest(String username, String passwordHash, boolean stayLoggedIn) { }
    public record RegisterRequest(User user) { }
    public record SessionResponse(String token, User user) { }
    public record ProfileUpdate(User user) { }
    public record RenameRequest(String username) { }
    public record PasswordChangeRequest(String oldPasswordHash, String newPasswordHash) { }
    public record PasswordChallengeRequest(String username, String email) { }
    public record PasswordChallenge(String resetToken, String securityQuestion) { }
    public record PasswordResetRequest(String resetToken, String securityAnswerHash, String newPasswordHash) { }
    public record UsernameAvailability(boolean exists) { }

    public record LeaderboardEntry(
        String username,
        String nickname,
        String lastLevel,
        int levelsCompleted,
        int minigames,
        int dailyQuests,
        int otherQuests,
        Integer myPoint
    ) { }

    public record InviteRequest(String username) { }
    public record InviteDecision(String invitationId, boolean accepted) { }
    public record InvitationView(String id, String fromUsername, long expiresAt) { }
    public record MatchAssignment(String matchId, String role, String opponentUsername) { }
    public record LobbyState(
        List<InvitationView> invitations,
        MatchAssignment match,
        boolean randomQueued,
        List<String> onlineUsers
    ) { }

     
    public record MatchCommand(long revision, String action, String kind, int column, int lane) { }

    public record EntityState(
        long id,
        String kind,
        int column,
        int lane,
        double x,
        double health,
        double maxHealth,
        boolean slowed
    ) { }

    public record ProjectileState(long id, int lane, double x, double damage, boolean frozen) { }
    public record ReactionRequest(String kind, String value) { }
    public record ReactionView(long id, String fromUsername, String kind, String value, long createdAt) { }

    public record MatchSnapshot(
        String matchId,
        long revision,
        String plantUsername,
        String zombieUsername,
        String status,
        String winner,
        String reason,
        int remainingSeconds,
        int plantSun,
        int zombieSun,
        boolean[] brains,
        List<EntityState> plants,
        List<EntityState> zombies,
        List<ProjectileState> projectiles,
        List<ReactionView> reactions
    ) { }
}
