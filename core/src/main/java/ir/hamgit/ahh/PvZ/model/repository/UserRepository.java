package ir.hamgit.ahh.PvZ.model.repository;

import ir.hamgit.ahh.PvZ.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UserRepository {

    private static final String SAVE_PATH_PROPERTY = "pvz.save.path";
    private static final Map<String, User> USERS = new LinkedHashMap<>();
    private static User loggedInUser;

    private UserRepository() {
    }

    public static void loadAll() {
        USERS.clear();
        loggedInUser = null;
        Path filePath = filePath();
        if (!Files.exists(filePath)) {
            return;
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(filePath))) {
            Object saved = input.readObject();
            if (saved instanceof Map<?, ?> map) {
                restoreUsers(map);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Warning: could not load users file; starting with an empty repository.");
        }
    }

    private static void restoreUsers(Map<?, ?> saved) {
        for (Map.Entry<?, ?> entry : saved.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof User user) {
                USERS.put(key, user);
                if (loggedInUser == null && user.isStayLoggedIn()) {
                    loggedInUser = user;
                }
            }
        }
    }

    public static void saveAll() {
        Path filePath = filePath();
        Path tempPath = temporaryPath(filePath);
        try {
            createParentDirectory(filePath);
        } catch (IOException e) {
            System.out.println("Warning: could not create the save directory.");
            return;
        }
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(tempPath))) {
            output.writeObject(new LinkedHashMap<>(USERS));
            output.flush();
            moveTemporaryFile(tempPath, filePath);
        } catch (IOException e) {
            System.out.println("Warning: could not save users file.");
        }
    }

    private static void createParentDirectory(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void moveTemporaryFile(Path tempPath, Path filePath) throws IOException {
        try {
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path filePath() {
        return Path.of(System.getProperty(SAVE_PATH_PROPERTY, "users.dat"));
    }

    private static Path temporaryPath(Path filePath) {
        return filePath.resolveSibling(filePath.getFileName() + ".tmp");
    }

    public static boolean register(User user) {
        if (USERS.containsKey(user.getUsername())) {
            return false;
        }
        USERS.put(user.getUsername(), user);
        saveAll();
        return true;
    }

    public static User login(String username, String passwordHash, boolean stayLoggedIn) {
        User user = USERS.get(username);
        if (user == null || !user.getPasswordHash().equals(passwordHash)) {
            return null;
        }
        clearOtherAutoLogins(user);
        user.setStayLoggedIn(stayLoggedIn);
        loggedInUser = user;
        saveAll();
        return user;
    }

    private static void clearOtherAutoLogins(User selected) {
        for (User user : USERS.values()) {
            if (user != selected) {
                user.setStayLoggedIn(false);
            }
        }
    }

    public static void logout() {
        if (loggedInUser != null) {
            loggedInUser.setStayLoggedIn(false);
            saveAll();
        }
        loggedInUser = null;
    }

    public static void updateUser(User user) {
        USERS.entrySet().removeIf(entry -> entry.getValue() == user
            && !entry.getKey().equals(user.getUsername()));
        USERS.put(user.getUsername(), user);
        saveAll();
    }

    public static boolean renameUser(User user, String newUsername) {
        if (USERS.containsKey(newUsername)) {
            return false;
        }
        USERS.remove(user.getUsername());
        user.setUsername(newUsername);
        USERS.put(newUsername, user);
        saveAll();
        return true;
    }

    public static User getCurrentUser() {
        return loggedInUser;
    }

    public static User getUser(String username) {
        return USERS.get(username);
    }

    public static boolean usernameExists(String username) {
        return USERS.containsKey(username);
    }

    public static Collection<User> getAllUsers() {
        return java.util.List.copyOf(USERS.values());
    }

    public static User getAutoLoginUser() {
        return loggedInUser;
    }
}
