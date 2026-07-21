package ir.hamgit.ahh.PvZ.view;

import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.model.quest.QuestService;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class LeaderboardMenu {

    public void handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        if (!command.equals("leaderboard show") && !command.equals("leaderboard sort")) {
            System.out.println("Use: leaderboard show or leaderboard sort -c <column> -o <asc/desc>.");
            return;
        }
        String column = CommandParser.getFlag(flags, "c");
        String order = CommandParser.getFlag(flags, "o");
        String selectedColumn = column == null ? "progress" : column.toLowerCase();
        if (!List.of("progress", "minigames", "daily", "quests", "score").contains(selectedColumn)) {
            System.out.println("Error: unknown leaderboard column.");
            return;
        }
        if (order != null && !order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc")) {
            System.out.println("Error: order must be asc or desc.");
            return;
        }
        show(selectedColumn, !"asc".equalsIgnoreCase(order));
    }

    private void show(String column, boolean descending) {
        List<User> users = new ArrayList<>(UserRepository.getAllUsers());
        Comparator<User> comparator = comparatorFor(column);
        if (descending) {
            comparator = comparator.reversed();
        }
        users.sort(comparator.thenComparing(User::getUsername));
        System.out.println("USER | LAST LEVEL | MINIGAMES | DAILY | OTHER QUESTS | BEST SCORE");
        for (User user : users) {
            System.out.printf("%s | %s | %d | %d | %d | %d%n", user.getUsername(), latestLevel(user),
                user.getTotalMinigamesCompleted(), QuestService.completedCount(user, true),
                QuestService.completedCount(user, false), user.getBestScoreMode());
        }
    }

    private Comparator<User> comparatorFor(String column) {
        return switch (column.toLowerCase()) {
            case "minigames" -> Comparator.comparingInt(User::getTotalMinigamesCompleted);
            case "daily" -> Comparator.comparingInt(user -> QuestService.completedCount(user, true));
            case "quests" -> Comparator.comparingInt(user -> QuestService.completedCount(user, false));
            case "score" -> Comparator.comparingInt(User::getBestScoreMode);
            default -> Comparator.comparingInt(User::getLevelsCompleted);
        };
    }

    private String latestLevel(User user) {
        ChapterType[] chapters = {ChapterType.ANCIENT_EGYPT, ChapterType.FROSTBITE_CAVES,
            ChapterType.BIG_WAVE_BEACH, ChapterType.DARK_AGES};
        String result = "none";
        for (ChapterType chapter : chapters) {
            int level = user.getCompletedLevel(chapter);
            if (level > 0) {
                result = chapter + "-" + level;
            }
        }
        return result;
    }
}
