package ir.hamgit.ahh.PvZ.view;


import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.controller.Validator;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.MenuState;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.Map;

public class LoginMenu {
    private boolean waitingForSecurityAnswer;
    private User forgetPasswordTargetUser;
    private boolean waitingForNewPassword;

    public LoginMenu() {
        this.waitingForSecurityAnswer = false;
        this.waitingForNewPassword = false;
    }

    public MenuState handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        if (waitingForSecurityAnswer) {
            return handleAnswer(flags, command);
        }
        if (waitingForNewPassword) {
            return handleNewPassword(flags, command);
        }
        if (command.equals("login")) {
            return handleLogin(flags);
        } else if (command.equals("forget password")) {
            handleForgetPassword(flags);
        } else {
            System.out.println("Error: unknown command.");
        }
        return MenuState.LOGIN;
    }

    private MenuState handleLogin(Map<String, String> flags) {
        String username = CommandParser.getFlag(flags, "u");
        String password = CommandParser.getFlag(flags, "p");
        boolean stayLoggedIn = CommandParser.getFlag(flags, "stay-logged-in") != null;
        if (username == null || password == null) {
            System.out.println("Error: usage is login -u <username> -p <password> [-stay-logged-in]");
            return MenuState.LOGIN;
        }
        User knownUser = UserRepository.getUser(username);
        if (knownUser == null) {
            System.out.println("Error: user not found.");
            return MenuState.LOGIN;
        }
        String passwordHash = Validator.hashSha256(password);
        if (!passwordHash.equals(knownUser.getPasswordHash())) {
            System.out.println("Error: incorrect password.");
            return MenuState.LOGIN;
        }
        User user = UserRepository.login(username, passwordHash, stayLoggedIn);
        if (user != null) {
            System.out.println("Login successful. Welcome back, " + user.getNickname() + "!");
            System.out.println("Navigating to main menu...");
            return MenuState.MAIN;
        }
        return MenuState.LOGIN;
    }

    private void handleForgetPassword(Map<String, String> flags) {
        String username = CommandParser.getFlag(flags, "u");
        String email = CommandParser.getFlag(flags, "e");
        if (username == null || email == null) {
            System.out.println("Error: usage is forget password -u <username> -e <email>");
            return;
        }
        User user = UserRepository.getUser(username);
        if (user == null || !email.equals(user.getEmail())) {
            System.out.println("Error: no matching account found.");
            return;
        }
        forgetPasswordTargetUser = user;
        waitingForSecurityAnswer = true;
        System.out.println("Security question: " + user.getSecurityQuestion());
        System.out.println("Use: answer -a <answer>");
    }

    private MenuState handleAnswer(Map<String, String> flags, String command) {
        if (!command.equals("answer")) {
            System.out.println("Please answer the security question first.");
            return MenuState.LOGIN;
        }
        String answer = CommandParser.getFlag(flags, "a");
        if (answer == null) {
            System.out.println("Error: usage is answer -a <answer>");
            return MenuState.LOGIN;
        }
        String hash = Validator.hashSha256(answer);
        if (hash.equals(forgetPasswordTargetUser.getSecurityAnswerHash())) {
            System.out.println("Correct! Please enter your new password:");
            System.out.println("Use: new password -p <new_password>");
            waitingForSecurityAnswer = false;
            waitingForNewPassword = true;
        } else {
            System.out.println("Incorrect answer. Returning to login menu.");
            waitingForSecurityAnswer = false;
            forgetPasswordTargetUser = null;
        }
        return MenuState.LOGIN;
    }

    private MenuState handleNewPassword(Map<String, String> flags, String command) {
        if (!command.equals("new password")) {
            System.out.println("Please enter your new password first.");
            return MenuState.LOGIN;
        }
        String newPassword = CommandParser.getFlag(flags, "p");
        if (newPassword == null) {
            System.out.println("Error: usage is new password -p <new_password>");
            return MenuState.LOGIN;
        }
        String error = Validator.isStrongPassword(newPassword);
        if (error != null) {
            System.out.println("Error: " + error);
            return MenuState.LOGIN;
        }
        forgetPasswordTargetUser.setPasswordHash(Validator.hashSha256(newPassword));
        UserRepository.updateUser(forgetPasswordTargetUser);
        System.out.println("Password changed successfully. Please log in.");
        waitingForNewPassword = false;
        forgetPasswordTargetUser = null;
        return MenuState.LOGIN;
    }

    public boolean isWaitingForSecurityAnswer() {
        return waitingForSecurityAnswer;
    }

    public boolean isWaitingForNewPassword() {
        return waitingForNewPassword;
    }
}
