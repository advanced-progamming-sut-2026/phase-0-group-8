package ir.hamgit.ahh.PvZ.server;

import ir.hamgit.ahh.PvZ.controller.Validator;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.quest.QuestService;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.LeaderboardEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

 
final class AccountStore {
    private final Path file;
    private final Map<String, User> users = new LinkedHashMap<>();

    AccountStore(Path file) {
        this.file = file;
        load();
    }

    synchronized boolean exists(String username) {
        return username != null && users.containsKey(key(username));
    }

    synchronized User register(User candidate) {
        validateRegistration(candidate);
        String key = key(candidate.getUsername());
        if (users.containsKey(key)) throw ApiException.conflict("Username is already taken.");
        User stored = copy(candidate);
        stored.normalize();
        stored.setStayLoggedIn(false);
        users.put(key, stored);
        save();
        return publicCopy(stored);
    }

    synchronized User authenticate(String username, String passwordHash) {
        User stored = users.get(key(username));
        if (stored == null || passwordHash == null || !passwordHash.equals(stored.getPasswordHash())) {
            throw ApiException.unauthorized("Incorrect username or password.");
        }
        return publicCopy(stored);
    }

    synchronized User publicUser(String username) {
        User stored = require(username);
        return publicCopy(stored);
    }

    synchronized User update(String username, User incoming) {
        if (incoming == null) throw ApiException.badRequest("Profile data is required.");
        User stored = require(username);
        User updated = copy(incoming);
        updated.normalize();
        if (!Validator.isValidNickname(updated.getNickname())) throw ApiException.badRequest("Invalid nickname.");
        if (!Validator.isValidEmail(updated.getEmail())) throw ApiException.badRequest("Invalid email address.");
        if (updated.getGender() == null) throw ApiException.badRequest("Gender is required.");
        updated.setUsername(stored.getUsername());
        updated.setPasswordHash(stored.getPasswordHash());
        updated.setSecurityQuestion(stored.getSecurityQuestion());
        updated.setSecurityAnswerHash(stored.getSecurityAnswerHash());
        Integer oldScore = stored.getNetworkBestScore();
        Integer submitted = updated.getNetworkBestScore();
        if (oldScore != null && (submitted == null || submitted < oldScore)) updated.setNetworkBestScore(oldScore);
        if (updated.getNetworkBestScore() != null && updated.getNetworkBestScore() <= 0) updated.setNetworkBestScore(null);
        users.put(key(username), updated);
        save();
        return publicCopy(updated);
    }

    synchronized User rename(String username, String replacement) {
        if (!Validator.isValidUsername(replacement)) throw ApiException.badRequest("Invalid username.");
        String newKey = key(replacement);
        String oldKey = key(username);
        if (!newKey.equals(oldKey) && users.containsKey(newKey)) {
            throw ApiException.conflict("Username is already taken.");
        }
        User stored = require(username);
        users.remove(oldKey);
        stored.setUsername(replacement.trim());
        users.put(newKey, stored);
        save();
        return publicCopy(stored);
    }

    synchronized void changePassword(String username, String oldHash, String newHash) {
        User stored = require(username);
        if (oldHash == null || !oldHash.equals(stored.getPasswordHash())) {
            throw ApiException.forbidden("Old password is incorrect.");
        }
        requireHash(newHash, "New password");
        stored.setPasswordHash(newHash);
        save();
    }

    synchronized String challengeUsername(String username, String email) {
        User stored = users.get(key(username));
        if (stored == null || email == null || !email.trim().equalsIgnoreCase(stored.getEmail())) {
            throw ApiException.notFound("No matching account found.");
        }
        return stored.getUsername();
    }

    synchronized String securityQuestion(String username) {
        return require(username).getSecurityQuestion();
    }

    synchronized void resetPassword(String username, String answerHash, String newHash) {
        User stored = require(username);
        if (answerHash == null || !answerHash.equals(stored.getSecurityAnswerHash())) {
            throw ApiException.forbidden("Incorrect security answer.");
        }
        requireHash(newHash, "New password");
        stored.setPasswordHash(newHash);
        save();
    }

    synchronized List<LeaderboardEntry> leaderboard() {
        List<LeaderboardEntry> rows = new ArrayList<>();
        for (User user : users.values()) {
            rows.add(new LeaderboardEntry(user.getUsername(), user.getNickname(), latestLevel(user),
                user.getLevelsCompleted(), user.getTotalMinigamesCompleted(),
                QuestService.completedCount(user, true), QuestService.completedCount(user, false),
                user.getNetworkBestScore()));
        }
        rows.sort(Comparator.comparingInt(LeaderboardEntry::levelsCompleted).reversed()
            .thenComparing(LeaderboardEntry::username, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(rows);
    }

    synchronized void recordMatchResult(String plantUsername, String zombieUsername, String winnerRole) {
        User plants = users.get(key(plantUsername));
        User zombies = users.get(key(zombieUsername));
        if (plants != null) plants.incrementGamesPlayed();
        if (zombies != null) zombies.incrementGamesPlayed();
        User winner = "PLANTS".equals(winnerRole) ? plants : "ZOMBIES".equals(winnerRole) ? zombies : null;
        if (winner != null) winner.addCoins(100);
        save();
    }

    private User require(String username) {
        User stored = users.get(key(username));
        if (stored == null) throw ApiException.notFound("Account not found.");
        return stored;
    }

    private void validateRegistration(User user) {
        if (user == null) throw ApiException.badRequest("Account data is required.");
        if (!Validator.isValidUsername(user.getUsername())) throw ApiException.badRequest("Invalid username.");
        requireHash(user.getPasswordHash(), "Password");
        if (!Validator.isValidNickname(user.getNickname())) throw ApiException.badRequest("Invalid nickname.");
        if (!Validator.isValidEmail(user.getEmail())) throw ApiException.badRequest("Invalid email address.");
        if (user.getGender() == null) throw ApiException.badRequest("Gender is required.");
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()
            || user.getSecurityAnswerHash() == null || user.getSecurityAnswerHash().isBlank()) {
            throw ApiException.badRequest("Security question and answer are required.");
        }
    }

    private void requireHash(String value, String label) {
        if (value == null || !value.matches("[0-9a-fA-F]{64}")) {
            throw ApiException.badRequest(label + " hash is invalid.");
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(file)) return;
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            Object value = input.readObject();
            if (value instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof User user) {
                        user.normalize();
                        users.put(key(user.getUsername()), user);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            System.err.println("Warning: account database could not be loaded: " + e.getMessage());
        }
    }

    private void save() {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temp))) {
                output.writeObject(new LinkedHashMap<>(users));
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not persist account database.", e);
        }
    }

    private static User publicCopy(User source) {
        User result = copy(source);
        result.setPasswordHash(null);
        result.setSecurityAnswerHash(null);
        return result;
    }

    private static User copy(User source) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(source); }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (User) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Could not copy account.", e);
        }
    }

    private static String latestLevel(User user) {
        String result = "none";
        for (ChapterType chapter : ChapterType.values()) {
            int level = user.getCompletedLevel(chapter);
            if (level > 0) result = pretty(chapter.name()) + " " + level;
        }
        return result;
    }

    private static String pretty(String raw) {
        String value = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
