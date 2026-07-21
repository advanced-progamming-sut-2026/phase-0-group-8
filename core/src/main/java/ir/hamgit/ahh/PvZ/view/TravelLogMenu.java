package ir.hamgit.ahh.PvZ.view;

import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.controller.GameController;
import ir.hamgit.ahh.PvZ.controller.MinigameController;
import ir.hamgit.ahh.PvZ.model.quest.QuestService;
import ir.hamgit.ahh.PvZ.model.quest.QuestService.Category;
import ir.hamgit.ahh.PvZ.model.quest.QuestService.Quest;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.MenuState;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.Map;
import java.util.Locale;

public class TravelLogMenu {

    private final MinigameController minigames;
    private final GameController gameController;

    public TravelLogMenu(MinigameController minigames, GameController gameController) {
        this.minigames = minigames;
        this.gameController = gameController;
    }

    public MenuState handle(String input) {
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            return MenuState.REGISTER;
        }
        QuestService.prepareDaily(user);
        UserRepository.updateUser(user);
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        if (command.startsWith("travel log page ")) {
            showPage(user, command.substring("travel log page ".length()));
        } else if (command.equals("travel log claim")) {
            claim(user, CommandParser.getFlag(flags, "q"));
        } else if (command.equals("play minigame")) {
            return playMinigame(flags);
        } else if (command.equals("play score-mode")) {
            gameController.startScoreMode();
            return MenuState.PLANT_SELECT;
        } else {
            System.out.println("Error: unknown Travel Log command.");
        }
        UserRepository.updateUser(user);
        return MenuState.TRAVEL_LOG;
    }

    private void showPage(User user, String page) {
        String normalized = page.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("MINIGAMES")) {
            System.out.println("Minigames: vasebreaker, wallnut-bowling, i-zombie, beghouled, zombotany (levels 1-3)");
            System.out.println("Use: play minigame -n <name> -l <level>");
            return;
        }
        try {
            Category category = Category.valueOf(normalized);
            for (Quest quest : QuestService.questsFor(category)) {
                int progress = user.getQuestProgress(quest.id());
                String claimed = QuestService.isClaimed(user, quest.id()) ? " [CLAIMED]" : "";
                System.out.printf("[%s] %s: %d/%d%s%n  %s | Reward: %s%n", quest.id(),
                    quest.title(), progress, QuestService.targetFor(user, quest), claimed,
                    QuestService.conditionFor(user, quest), QuestService.rewardFor(user, quest));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Pages: story, epic, daily, repeatable, minigames.");
        }
    }

    private void claim(User user, String id) {
        if (id == null || !QuestService.claim(user, id)) {
            System.out.println("That quest is incomplete, unknown, or already claimed.");
            return;
        }
        System.out.println("Quest reward claimed.");
    }

    private MenuState playMinigame(Map<String, String> flags) {
        String name = CommandParser.getFlag(flags, "n");
        int level = CommandParser.getIntFlag(flags, "l", 1);
        if (!minigames.start(name, level)) {
            System.out.println("Unknown minigame or invalid level; choose level 1, 2, or 3.");
            return MenuState.TRAVEL_LOG;
        }
        System.out.println("Started " + name + " level " + level + ".");
        return MenuState.MINIGAME;
    }
}
