package ir.hamgit.ahh.PvZ.view;

import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.Map;

public class SettingsMenu {

    public void handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            System.out.println("Error: no user logged in.");
            return;
        }
        if (command.equals("menu settings change-difficulty")) {
            changeDifficulty(user, CommandParser.getIntFlag(flags, "l", -1));
        } else {
            System.out.println("Error: unknown settings command.");
        }
    }

    private void changeDifficulty(User user, int level) {
        if (level < 1 || level > 5) {
            System.out.println("Error: difficulty level must be between 1 and 5.");
            return;
        }
        user.setDifficulty(level);
        UserRepository.updateUser(user);
        System.out.println("Difficulty changed to " + level + ".");
    }
}
