package ir.hamgit.ahh.PvZ.model.repository;

import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.network.NetworkException;
import ir.hamgit.ahh.PvZ.network.PhaseThreeApiClient;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.LeaderboardEntry;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordChallenge;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.SessionResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

 



public final class UserRepository {
    private static final String SESSION_PATH_PROPERTY = "pvz.session.path";
    private static final PhaseThreeApiClient API = new PhaseThreeApiClient();
    private static User loggedInUser;
    private static String lastError;

    private UserRepository() { }

    public static void loadAll() {
        loggedInUser = null;
        String saved = readToken();
        if (saved == null) return;
        API.setToken(saved);
        try {
            loggedInUser = normalize(API.session());
            
            loggedInUser.setStayLoggedIn(true);
            lastError = null;
        } catch (NetworkException e) {
            API.setToken(null);
            deleteToken();
            remember(e);
        }
    }

     
    public static void saveAll() {
        if (loggedInUser == null || API.getToken() == null) return;
        try {
            loggedInUser = normalize(API.updateProfile(loggedInUser));
            lastError = null;
            persistTokenIfRequested();
        } catch (NetworkException e) {
            remember(e);
        }
    }

    public static boolean register(User user) {
        try {
            SessionResponse response = API.register(user);
            loggedInUser = normalize(response.user());
            try { API.logout(); }
            catch (NetworkException ignored) { API.setToken(null); }
            loggedInUser = null;
            deleteToken();
            lastError = null;
            return true;
        } catch (NetworkException e) {
            remember(e);
            loggedInUser = null;
            return false;
        }
    }

    public static User login(String username, String passwordHash, boolean stayLoggedIn) {
        try {
            SessionResponse response = API.login(username, passwordHash, stayLoggedIn);
            loggedInUser = normalize(response.user());
            loggedInUser.setStayLoggedIn(stayLoggedIn);
            lastError = null;
            persistTokenIfRequested();
            return loggedInUser;
        } catch (NetworkException e) {
            remember(e);
            return null;
        }
    }

    public static void logout() {
        try {
            if (API.getToken() != null) API.logout();
        } catch (NetworkException e) {
            remember(e);
        } finally {
            API.setToken(null);
            loggedInUser = null;
            deleteToken();
        }
    }

    public static void updateUser(User user) {
        if (user == null || API.getToken() == null) return;
        try {
            loggedInUser = normalize(API.updateProfile(user));
            lastError = null;
            persistTokenIfRequested();
        } catch (NetworkException e) {
            remember(e);
        }
    }

    public static boolean renameUser(User user, String newUsername) {
        if (user == null) return false;
        try {
            loggedInUser = normalize(API.rename(newUsername));
            lastError = null;
            return true;
        } catch (NetworkException e) {
            remember(e);
            return false;
        }
    }

    public static boolean changePassword(String oldHash, String newHash) {
        try {
            API.changePassword(oldHash, newHash);
            lastError = null;
            return true;
        } catch (NetworkException e) {
            remember(e);
            return false;
        }
    }

    public static PasswordChallenge passwordChallenge(String username, String email) {
        try { PasswordChallenge result = API.passwordChallenge(username, email); lastError = null; return result; }
        catch (NetworkException e) { remember(e); return null; }
    }

    public static boolean resetPassword(String resetToken, String answerHash, String newHash) {
        try { API.resetPassword(resetToken, answerHash, newHash); lastError = null; return true; }
        catch (NetworkException e) { remember(e); return false; }
    }

    public static List<LeaderboardEntry> getLeaderboard() {
        try { List<LeaderboardEntry> result = API.leaderboard(); lastError = null; return result; }
        catch (NetworkException e) { remember(e); return List.of(); }
    }

    public static User getCurrentUser() { return loggedInUser; }

     
    public static User getUser(String username) {
        return loggedInUser != null && loggedInUser.getUsername().equalsIgnoreCase(username) ? loggedInUser : null;
    }

    public static boolean usernameExists(String username) {
        try { boolean result = API.usernameExists(username); lastError = null; return result; }
        catch (NetworkException e) { remember(e); return false; }
    }

     
    public static Collection<User> getAllUsers() {
        return loggedInUser == null ? List.of() : List.of(loggedInUser);
    }

    public static User getAutoLoginUser() { return loggedInUser; }
    public static PhaseThreeApiClient network() { return API; }
    public static String serverUrl() { return API.getBaseUrl(); }

    public static String consumeLastError() {
        String value = lastError;
        lastError = null;
        return value;
    }

    private static User normalize(User user) {
        if (user != null) user.normalize();
        return user;
    }

    private static void remember(NetworkException e) {
        lastError = e.getMessage();
    }

    private static void persistTokenIfRequested() {
        if (loggedInUser == null || !loggedInUser.isStayLoggedIn() || API.getToken() == null) {
            deleteToken();
            return;
        }
        try {
            Path path = tokenPath();
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, API.getToken(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            lastError = "Could not save the auto-login token.";
        }
    }

    private static String readToken() {
        try {
            Path path = tokenPath();
            if (!Files.exists(path)) return null;
            String value = Files.readString(path, StandardCharsets.UTF_8).trim();
            return value.isBlank() ? null : value;
        } catch (IOException e) {
            return null;
        }
    }

    private static void deleteToken() {
        try { Files.deleteIfExists(tokenPath()); }
        catch (IOException ignored) { }
    }

    private static Path tokenPath() {
        String override = System.getProperty(SESSION_PATH_PROPERTY);
        if (override != null && !override.isBlank()) return Path.of(override);
        return Path.of(System.getProperty("user.home", "."), ".pvz-phase3", "session.token");
    }
}
