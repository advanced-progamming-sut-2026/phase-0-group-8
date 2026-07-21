package ir.hamgit.ahh.PvZ.view;


import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.List;
import java.util.Map;

public class NewsMenu {

    public void handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            System.out.println("Error: no user logged in.");
            return;
        }
        switch (command) {
            case "menu news show-unread":
                showUnread(user);
                break;
            case "menu news show-all":
                showAll(user);
                break;
            default:
                System.out.println("Error: unknown news command.");
        }
    }

    private void showUnread(User user) {
        List<String> unread = user.getUnreadNews();
        if (unread.isEmpty()) {
            System.out.println("No new news.");
        } else {
            System.out.println("Unread news:");
            for (String item : unread) {
                System.out.println("  - " + item);
            }
        }
        user.markAllNewsRead();
        UserRepository.updateUser(user);
    }

    private void showAll(User user) {
        List<String> all = user.getAllNews();
        if (all.isEmpty()) {
            System.out.println("No news yet.");
            return;
        }
        System.out.println("All news:");
        for (String item : all) {
            System.out.println("  - " + item);
        }
    }
}
