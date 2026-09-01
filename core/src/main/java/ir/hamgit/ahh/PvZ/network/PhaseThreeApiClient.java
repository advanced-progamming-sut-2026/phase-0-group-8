package ir.hamgit.ahh.PvZ.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

 
public final class PhaseThreeApiClient {
    public static final String URL_PROPERTY = "pvz.server.url";
    private final String baseUrl;
    private final HttpClient http;
    private volatile String token;

    public PhaseThreeApiClient() {
        this(System.getProperty(URL_PROPERTY, "http://127.0.0.1:8080"));
    }

    public PhaseThreeApiClient(String baseUrl) {
        this.baseUrl = trimSlash(baseUrl);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public String getBaseUrl() { return baseUrl; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = blankToNull(token); }

    public boolean health() {
        try { get("/api/health", Message.class, false); return true; }
        catch (NetworkException e) { return false; }
    }

    public SessionResponse register(User user) {
        SessionResponse response = post("/api/auth/register", new RegisterRequest(user), SessionResponse.class, false);
        token = response.token();
        return response;
    }

    public SessionResponse login(String username, String passwordHash, boolean stayLoggedIn) {
        SessionResponse response = post("/api/auth/login",
            new LoginRequest(username, passwordHash, stayLoggedIn), SessionResponse.class, false);
        token = response.token();
        return response;
    }

    public User session() { return get("/api/auth/session", SessionResponse.class, true).user(); }
    public void logout() { post("/api/auth/logout", null, Message.class, true); token = null; }
    public boolean usernameExists(String username) {
        return get("/api/users/exists?username=" + encode(username), UsernameAvailability.class, false).exists();
    }

    public PasswordChallenge passwordChallenge(String username, String email) {
        return post("/api/auth/challenge", new PasswordChallengeRequest(username, email), PasswordChallenge.class, false);
    }

    public void resetPassword(String resetToken, String answerHash, String newHash) {
        post("/api/auth/reset", new PasswordResetRequest(resetToken, answerHash, newHash), Message.class, false);
    }

    public User updateProfile(User user) {
        return put("/api/account", new ProfileUpdate(user), SessionResponse.class, true).user();
    }

    public User rename(String username) {
        return post("/api/account/rename", new RenameRequest(username), SessionResponse.class, true).user();
    }

    public void changePassword(String oldHash, String newHash) {
        post("/api/account/password", new PasswordChangeRequest(oldHash, newHash), Message.class, true);
    }

    public List<LeaderboardEntry> leaderboard() {
        LeaderboardEntry[] rows = get("/api/leaderboard", LeaderboardEntry[].class, true);
        return Arrays.asList(rows);
    }

    public LobbyState lobby() { return get("/api/lobby", LobbyState.class, true); }
    public Message invite(String username) {
        return post("/api/invitations", new InviteRequest(username), Message.class, true);
    }
    public MatchAssignment decideInvite(String id, boolean accepted) {
        return post("/api/invitations/decision", new InviteDecision(id, accepted), MatchAssignment.class, true);
    }
    public LobbyState joinRandom() { return post("/api/matchmaking/random", null, LobbyState.class, true); }
    public LobbyState leaveRandom() { return delete("/api/matchmaking/random", LobbyState.class, true); }
    public MatchSnapshot match(String id) { return get("/api/matches/" + encode(id), MatchSnapshot.class, true); }
    public MatchSnapshot command(String id, MatchCommand command) {
        return post("/api/matches/" + encode(id) + "/commands", command, MatchSnapshot.class, true);
    }
    public MatchSnapshot react(String id, String kind, String value) {
        return post("/api/matches/" + encode(id) + "/reactions",
            new ReactionRequest(kind, value), MatchSnapshot.class, true);
    }
    public void leaveMatch(String id) {
        post("/api/matches/" + encode(id) + "/leave", null, Message.class, true);
    }

    private <T> T get(String path, Class<T> type, boolean auth) {
        return send("GET", path, null, type, auth);
    }

    private <T> T post(String path, Object body, Class<T> type, boolean auth) {
        return send("POST", path, body, type, auth);
    }

    private <T> T put(String path, Object body, Class<T> type, boolean auth) {
        return send("PUT", path, body, type, auth);
    }

    private <T> T delete(String path, Class<T> type, boolean auth) {
        return send("DELETE", path, null, type, auth);
    }

    private <T> T send(String method, String path, Object body, Class<T> type, boolean auth) {
        if (auth && token == null) throw new NetworkException(401, "Please log in again.");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5)).header("Accept", "application/json");
            if (auth) builder.header("Authorization", "Bearer " + token);
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(JsonSupport.MAPPER.writeValueAsString(body)));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NetworkException(response.statusCode(), errorMessage(response.body(), response.statusCode()));
            }
            if (type == Void.class || response.body() == null || response.body().isBlank()) return null;
            return JsonSupport.MAPPER.readValue(response.body(), type);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Request was interrupted.");
        } catch (IOException | IllegalArgumentException e) {
            throw new NetworkException("Cannot reach Phase 3 server at " + baseUrl + ".");
        }
    }

    private String errorMessage(String body, int status) {
        try {
            ErrorResponse error = JsonSupport.MAPPER.readValue(body, ErrorResponse.class);
            if (error.error() != null && !error.error().isBlank()) return error.error();
        } catch (JsonProcessingException | RuntimeException ignored) { }
        return "Server returned HTTP " + status + ".";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trimSlash(String value) {
        String result = value == null || value.isBlank() ? "http://127.0.0.1:8080" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
