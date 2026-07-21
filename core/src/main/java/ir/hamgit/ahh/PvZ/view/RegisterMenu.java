package ir.hamgit.ahh.PvZ.view;


import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.controller.Validator;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.Gender;
import ir.hamgit.ahh.PvZ.model.enums.MenuState;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.List;
import java.util.Map;

public class RegisterMenu {
    private static final List<String> SECURITY_QUESTIONS = List.of(
        "1. What is the name of your first pet?",
        "2. What is your mother's maiden name?",
        "3. What city were you born in?",
        "4. What was the name of your elementary school?",
        "5. What is your favorite childhood movie?"
    );

    private String pendingUsername;
    private String pendingPasswordHash;
    private String pendingNickname;
    private String pendingEmail;
    private Gender pendingGender;
    private boolean waitingForQuestion;

    public RegisterMenu() {
        this.waitingForQuestion = false;
    }

    public MenuState handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        if (waitingForQuestion) {
            return handlePickQuestion(flags, command);
        }
        if (command.equals("register")) {
            handleRegister(flags);
        } else {
            System.out.println("Error: unknown command.");
        }
        return MenuState.REGISTER;
    }

    private void handleRegister(Map<String, String> flags) {
        String username = CommandParser.getFlag(flags, "u");
        String passwordCombined = CommandParser.getFlag(flags, "p");
        String nickname = CommandParser.getFlag(flags, "n");
        String email = CommandParser.getFlag(flags, "e");
        String genderStr = CommandParser.getFlag(flags, "g");
        if (hasMissingFields(username, passwordCombined, nickname, email, genderStr)) {
            System.out.println("Error: missing required fields. Usage: register -u <username> "
                + "-p <password> <confirm> -n <nickname> -e <email> -g <gender>");
            return;
        }
        String[] parts = passwordCombined.trim().split("\\s+", 2);
        String pass = parts[0];
        String confirm = parts.length > 1 ? parts[1] : null;
        if (!isValidRegistration(username, pass, confirm, nickname, email)) {
            return;
        }
        Gender gender = parseGender(genderStr);
        if (gender == null) {
            return;
        }
        savePendingRegistration(username, pass, nickname, email, gender);
        showSecurityQuestions();
    }

    private boolean hasMissingFields(String... values) {
        for (String value : values) {
            if (value == null) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidRegistration(String username, String pass, String confirm,
                                        String nickname, String email) {
        String usernameError = validateUsername(username);
        if (usernameError != null) {
            System.out.println("Error: " + usernameError);
            return false;
        }
        String passwordError = Validator.isStrongPassword(pass);
        if (passwordError != null) {
            System.out.println("Error: " + passwordError);
            return false;
        }
        if (confirm == null || !confirm.equals(pass)) {
            System.out.println("Error: passwords do not match. Please try again.");
            return false;
        }
        if (!Validator.isValidNickname(nickname)) {
            System.out.println("Error: nickname must be between 3 and 30 characters.");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            System.out.println("Error: invalid email format.");
            return false;
        }
        return true;
    }

    private Gender parseGender(String genderStr) {
        if ("male".equalsIgnoreCase(genderStr) || "man".equalsIgnoreCase(genderStr)) {
            return Gender.MALE;
        }
        if ("female".equalsIgnoreCase(genderStr) || "woman".equalsIgnoreCase(genderStr)) {
            return Gender.FEMALE;
        }
        System.out.println("Error: gender must be 'male' or 'female'.");
        return null;
    }

    private void savePendingRegistration(String username, String pass, String nickname,
                                         String email, Gender gender) {
        pendingUsername = username;
        pendingPasswordHash = Validator.hashSha256(pass);
        pendingNickname = nickname;
        pendingEmail = email;
        pendingGender = gender;
        waitingForQuestion = true;
    }

    private void showSecurityQuestions() {
        System.out.println("Please choose a security question:");
        for (String q : SECURITY_QUESTIONS) {
            System.out.println(q);
        }
        System.out.println("Use: pick question -q <question_number> -a <answer> -c <answer_confirm>");
    }

    private MenuState handlePickQuestion(Map<String, String> flags, String command) {
        if (!command.equals("pick question")) {
            System.out.println("Please choose a security question first.");
            return MenuState.REGISTER;
        }
        int qNumber = CommandParser.getIntFlag(flags, "q", -1);
        String answer = CommandParser.getFlag(flags, "a");
        String confirm = CommandParser.getFlag(flags, "c");
        if (qNumber < 1 || qNumber > SECURITY_QUESTIONS.size()) {
            System.out.println("Error: question number must be between 1 and " + SECURITY_QUESTIONS.size());
            return MenuState.REGISTER;
        }
        if (answer == null || answer.isBlank() || confirm == null || !answer.equals(confirm)) {
            System.out.println("Error: answers do not match.");
            return MenuState.REGISTER;
        }
        User newUser = new User(pendingUsername, pendingPasswordHash,
            pendingNickname, pendingEmail, pendingGender);
        newUser.setSecurityQuestion(SECURITY_QUESTIONS.get(qNumber - 1));
        newUser.setSecurityAnswerHash(Validator.hashSha256(answer));
        newUser.unlockPlant(PlantType.PEASHOOTER);
        newUser.unlockPlant(PlantType.SUNFLOWER);
        MenuState nextState = MenuState.REGISTER;
        if (UserRepository.register(newUser)) {
            System.out.println("Registration successful! Welcome, " + pendingNickname + ".");
            System.out.println("Redirecting to login menu...");
            nextState = MenuState.LOGIN;
        } else {
            System.out.println("Error: username already taken.");
        }
        waitingForQuestion = false;
        pendingUsername = null;
        pendingPasswordHash = null;
        pendingNickname = null;
        pendingEmail = null;
        pendingGender = null;
        return nextState;
    }

    private String validateUsername(String username) {
        if (!Validator.isValidUsername(username)) {
            return "Username may only contain letters, digits, and hyphens.";
        }
        if (UserRepository.usernameExists(username)) {
            return "Username already taken.";
        }
        return null;
    }

    public boolean isWaitingForQuestion() {
        return waitingForQuestion;
    }
}
