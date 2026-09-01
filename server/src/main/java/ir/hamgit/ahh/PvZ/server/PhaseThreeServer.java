package ir.hamgit.ahh.PvZ.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.network.JsonSupport;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ErrorResponse;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.InviteDecision;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.InviteRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.LoginRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchCommand;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.Message;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordChangeRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordChallengeRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordResetRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ProfileUpdate;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.RegisterRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.RenameRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ReactionRequest;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.SessionResponse;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.UsernameAvailability;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

 
public final class PhaseThreeServer implements AutoCloseable {
    private final HttpServer http;
    private final ServerState state;

    public PhaseThreeServer(String host, int port, Path accountFile) throws IOException {
        state = new ServerState(accountFile);
        http = HttpServer.create(new InetSocketAddress(host, port), 64);
        http.createContext("/api", this::handle);
        http.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "pvz-phase3-http");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public void start() { http.start(); }
    public int getPort() { return http.getAddress().getPort(); }

    private void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        try {
            Object response = route(exchange);
            send(exchange, 200, response == null ? new Message("OK") : response);
        } catch (ApiException e) {
            send(exchange, e.status(), new ErrorResponse(e.getMessage()));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            send(exchange, 400, new ErrorResponse("Malformed JSON request."));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            send(exchange, 500, new ErrorResponse("Internal server error."));
        }
    }

    private Object route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();
        if (method.equals("GET") && path.equals("/api/health")) return new Message("Phase 3 server is ready.");

        if (method.equals("POST") && path.equals("/api/auth/register")) {
            RegisterRequest request = read(exchange, RegisterRequest.class);
            User user = state.accounts.register(request == null ? null : request.user());
            String token = state.createSession(user.getUsername(), false);
            return new SessionResponse(token, user);
        }
        if (method.equals("POST") && path.equals("/api/auth/login")) {
            LoginRequest request = read(exchange, LoginRequest.class);
            if (request == null) throw ApiException.badRequest("Credentials are required.");
            User user = state.accounts.authenticate(request.username(), request.passwordHash());
            user.setStayLoggedIn(request.stayLoggedIn());
            String token = state.createSession(user.getUsername(), request.stayLoggedIn());
            return new SessionResponse(token, user);
        }
        if (method.equals("GET") && path.equals("/api/auth/session")) {
            String username = authenticated(exchange);
            return new SessionResponse(bearer(exchange), state.accounts.publicUser(username));
        }
        if (method.equals("POST") && path.equals("/api/auth/logout")) {
            String token = bearer(exchange);
            state.authenticate(token);
            state.logout(token);
            return new Message("Logged out.");
        }
        if (method.equals("POST") && path.equals("/api/auth/challenge")) {
            return state.challenge(read(exchange, PasswordChallengeRequest.class));
        }
        if (method.equals("POST") && path.equals("/api/auth/reset")) {
            state.resetPassword(read(exchange, PasswordResetRequest.class));
            return new Message("Password changed.");
        }
        if (method.equals("GET") && path.equals("/api/users/exists")) {
            String username = query(exchange).get("username");
            return new UsernameAvailability(state.accounts.exists(username));
        }

        if (method.equals("PUT") && path.equals("/api/account")) {
            String username = authenticated(exchange);
            ProfileUpdate request = read(exchange, ProfileUpdate.class);
            User updated = state.accounts.update(username, request == null ? null : request.user());
            return new SessionResponse(bearer(exchange), updated);
        }
        if (method.equals("POST") && path.equals("/api/account/rename")) {
            String username = authenticated(exchange);
            RenameRequest request = read(exchange, RenameRequest.class);
            if (request == null) throw ApiException.badRequest("New username is required.");
            User updated = state.renameAccount(username, request.username());
            return new SessionResponse(bearer(exchange), updated);
        }
        if (method.equals("POST") && path.equals("/api/account/password")) {
            String username = authenticated(exchange);
            PasswordChangeRequest request = read(exchange, PasswordChangeRequest.class);
            if (request == null) throw ApiException.badRequest("Password data is required.");
            state.accounts.changePassword(username, request.oldPasswordHash(), request.newPasswordHash());
            return new Message("Password changed.");
        }
        if (method.equals("GET") && path.equals("/api/leaderboard")) {
            authenticated(exchange);
            return state.accounts.leaderboard();
        }

        if (method.equals("GET") && path.equals("/api/lobby")) {
            return state.lobby(authenticated(exchange));
        }
        if (method.equals("POST") && path.equals("/api/invitations")) {
            String username = authenticated(exchange);
            InviteRequest request = read(exchange, InviteRequest.class);
            state.invite(username, request == null ? null : request.username());
            return new Message("Invitation sent.");
        }
        if (method.equals("POST") && path.equals("/api/invitations/decision")) {
            String username = authenticated(exchange);
            InviteDecision request = read(exchange, InviteDecision.class);
            if (request == null) throw ApiException.badRequest("Invitation decision is required.");
            return state.decideInvite(username, request.invitationId(), request.accepted());
        }
        if (method.equals("POST") && path.equals("/api/matchmaking/random")) {
            return state.joinRandom(authenticated(exchange));
        }
        if (method.equals("DELETE") && path.equals("/api/matchmaking/random")) {
            return state.leaveRandom(authenticated(exchange));
        }

        if (path.startsWith("/api/matches/")) return routeMatch(exchange, method, path);
        throw ApiException.notFound("API endpoint not found.");
    }

    private Object routeMatch(HttpExchange exchange, String method, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4 || parts[3].isBlank()) throw ApiException.notFound("Match not found.");
        String matchId = decode(parts[3]);
        String username = authenticated(exchange);
        if (parts.length == 4 && method.equals("GET")) return state.match(username, matchId);
        if (parts.length == 5 && parts[4].equals("commands") && method.equals("POST")) {
            return state.command(username, matchId, read(exchange, MatchCommand.class));
        }
        if (parts.length == 5 && parts[4].equals("reactions") && method.equals("POST")) {
            return state.react(username, matchId, read(exchange, ReactionRequest.class));
        }
        if (parts.length == 5 && parts[4].equals("leave") && method.equals("POST")) {
            state.leaveMatch(username, matchId);
            return new Message("Match left.");
        }
        throw ApiException.notFound("Match endpoint not found.");
    }

    private String authenticated(HttpExchange exchange) {
        return state.authenticate(bearer(exchange));
    }

    private String bearer(HttpExchange exchange) {
        String value = exchange.getRequestHeaders().getFirst("Authorization");
        if (value == null || !value.startsWith("Bearer ") || value.length() <= 7) {
            throw ApiException.unauthorized("Authorization token is required.");
        }
        return value.substring(7).trim();
    }

    private <T> T read(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] data = exchange.getRequestBody().readNBytes(1_000_001);
        if (data.length > 1_000_000) throw ApiException.badRequest("Request body is too large.");
        if (data.length == 0) return null;
        return JsonSupport.MAPPER.readValue(data, type);
    }

    private void send(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] data = JsonSupport.MAPPER.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private Map<String, String> query(HttpExchange exchange) {
        Map<String, String> values = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return values;
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(decode(parts[0]), parts.length == 2 ? decode(parts[1]) : "");
        }
        return values;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override public void close() {
        http.stop(0);
        state.close();
    }
}
