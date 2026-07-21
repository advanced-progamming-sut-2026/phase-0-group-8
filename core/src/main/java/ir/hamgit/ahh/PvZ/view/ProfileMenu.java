package ir.hamgit.ahh.PvZ.view;


import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.controller.Validator;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.Map;

public class ProfileMenu {

    public void handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            System.out.println("Error: no user logged in.");
            return;
        }
        switch (command) {
            case "menu profile change-username":
                changeUsername(user, CommandParser.getFlag(flags, "u"));
                break;
            case "menu profile change-nickname":
                changeNickname(user, CommandParser.getFlag(flags, "u"));
                break;
            case "menu profile change-email":
                changeEmail(user, CommandParser.getFlag(flags, "e"));
                break;
            case "menu profile change-password":
                changePassword(user, CommandParser.getFlag(flags, "p"), CommandParser.getFlag(flags, "o"));
                break;
            case "menu profile show-info":
                showInfo(user);
                break;
            default:
                System.out.println("Error: unknown profile command.");
        }
    }

    private void changeUsername(User user, String newUsername) {
        if (newUsername == null) {
            System.out.println("Error: usage is menu profile change-username -u <username>");
            return;
        }
        if (newUsername.equals(user.getUsername())) {
            System.out.println("Error: new username is the same as current username.");
            return;
        }
        if (!Validator.isValidUsername(newUsername)) {
            System.out.println("Error: username may only contain letters, digits, and hyphens.");
            return;
        }
        if (UserRepository.usernameExists(newUsername)) {
            System.out.println("Error: username already taken.");
            return;
        }
        UserRepository.renameUser(user, newUsername);
        System.out.println("Username changed successfully.");
    }

    private void changeNickname(User user, String newNickname) {
        if (newNickname == null) {
            System.out.println("Error: usage is menu profile change-nickname -u <nickname>");
            return;
        }
        if (newNickname.equals(user.getNickname())) {
            System.out.println("Error: new nickname is the same as current nickname.");
            return;
        }
        if (!Validator.isValidNickname(newNickname)) {
            System.out.println("Error: nickname must be between 3 and 30 characters.");
            return;
        }
        user.setNickname(newNickname);
        UserRepository.updateUser(user);
        System.out.println("Nickname changed successfully.");
    }

    private void changeEmail(User user, String newEmail) {
        if (newEmail == null) {
            System.out.println("Error: usage is menu profile change-email -e <email>");
            return;
        }
        if (newEmail.equals(user.getEmail())) {
            System.out.println("Error: new email is the same as current email.");
            return;
        }
        if (!Validator.isValidEmail(newEmail)) {
            System.out.println("Error: invalid email format.");
            return;
        }
        user.setEmail(newEmail);
        UserRepository.updateUser(user);
        System.out.println("Email changed successfully.");
    }

    private void changePassword(User user, String newPassword, String oldPassword) {
        if (newPassword == null || oldPassword == null) {
            System.out.println("Error: usage is menu profile change-password -p <new_password> -o <old_password>");
            return;
        }
        String oldHash = Validator.hashSha256(oldPassword);
        if (!oldHash.equals(user.getPasswordHash())) {
            System.out.println("Error: old password is incorrect.");
            return;
        }
        if (newPassword.equals(oldPassword)) {
            System.out.println("Error: new password must be different from the old password.");
            return;
        }
        String strengthError = Validator.isStrongPassword(newPassword);
        if (strengthError != null) {
            System.out.println("Error: " + strengthError);
            return;
        }
        user.setPasswordHash(Validator.hashSha256(newPassword));
        UserRepository.updateUser(user);
        System.out.println("Password changed successfully.");
    }

    private void showInfo(User user) {
        System.out.println("Username: " + user.getUsername());
        System.out.println("Nickname: " + user.getNickname());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Gender: " + user.getGender());
        System.out.println("Games played: " + user.getGamesPlayed());
        System.out.println("Coins: " + user.getCoins());
        System.out.println("Diamonds: " + user.getDiamonds());
        System.out.println("Levels completed: " + user.getLevelsCompleted());
        System.out.println("Best score (score mode): " + user.getBestScoreMode());
        System.out.println("Difficulty: " + user.getDifficulty());
    }
}
