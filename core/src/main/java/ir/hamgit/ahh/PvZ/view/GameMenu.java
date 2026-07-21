package ir.hamgit.ahh.PvZ.view;


import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.controller.GameController;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.MenuState;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.Map;
import java.util.Locale;

public class GameMenu {
    private final GameController gameController;

    public GameMenu(GameController gameController) {
        this.gameController = gameController;
    }

    public MenuState handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            System.out.println("Error: no user logged in.");
            return MenuState.GAME;
        }
        switch (command) {
            case "menu enter chapter":
                return enterChapter(user, CommandParser.getFlag(flags, "c"));
            case "menu greenhouse":
                System.out.println("Opening greenhouse...");
                return MenuState.GREENHOUSE;
            case "menu travel-log":
                System.out.println("Opening travel log...");
                return MenuState.TRAVEL_LOG;
            case "menu leaderboard":
                System.out.println("Opening leaderboard...");
                return MenuState.LEADERBOARD;
            case "menu coin-wallet":
                showCoinWallet(user);
                return MenuState.GAME;
            case "menu gem-wallet":
                showGemWallet(user);
                return MenuState.GAME;
            case "menu cheat add":
                cheatAddCurrency(user, input);
                return MenuState.GAME;
            default:
                System.out.println("Error: unknown game menu command.");
                return MenuState.GAME;
        }
    }

    private MenuState enterChapter(User user, String chapterName) {
        if (chapterName == null) {
            System.out.println("Error: usage is menu enter chapter -c <chaptername>");
            return MenuState.GAME;
        }
        ChapterType chapter = parseChapter(chapterName);
        if (chapter == null || chapter == ChapterType.MINIGAME) {
            System.out.println("Error: unknown chapter.");
            return MenuState.GAME;
        }
        if (!user.isChapterUnlocked(chapter)) {
            System.out.println("Error: finish the previous chapter first.");
            return MenuState.GAME;
        }
        int level = user.getNextLevel(chapter);
        gameController.setCurrentUser(user);
        gameController.startGame(chapter.name(), level);
        System.out.println("Entering " + chapter + " level " + level + ".");
        if (gameController.shouldSkipPlantSelection()) {
            gameController.startActualGame(gameController.getDefaultTotalWaves());
            return MenuState.IN_GAME;
        }
        return MenuState.PLANT_SELECT;
    }

    private ChapterType parseChapter(String raw) {
        try {
            return ChapterType.valueOf(raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void showCoinWallet(User user) {
        System.out.println("Coins: " + user.getCoins());
    }

    private void showGemWallet(User user) {
        System.out.println("Diamonds: " + user.getDiamonds());
    }

    private void cheatAddCurrency(User user, String rawInput) {
        String[] tokens = rawInput.trim().split("\\s+");
        if (tokens.length < 4) {
            System.out.println("Error: usage is menu cheat add <n> <coin/diamond>");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(tokens[tokens.length - 2]);
        } catch (NumberFormatException e) {
            System.out.println("Error: amount must be a number.");
            return;
        }
        if (amount < 0) {
            System.out.println("Error: amount cannot be negative.");
            return;
        }
        String currency = tokens[tokens.length - 1];
        if (currency.equalsIgnoreCase("coin")) {
            user.addCoins(amount);
            System.out.println("Added " + amount + " coins. Total: " + user.getCoins());
        } else if (currency.equalsIgnoreCase("diamond")) {
            user.addDiamonds(amount);
            System.out.println("Added " + amount + " diamonds. Total: " + user.getDiamonds());
        } else {
            System.out.println("Error: currency must be 'coin' or 'diamond'.");
            return;
        }
        UserRepository.updateUser(user);
    }
}
