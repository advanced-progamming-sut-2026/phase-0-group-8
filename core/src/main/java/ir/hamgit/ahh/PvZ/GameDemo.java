package ir.hamgit.ahh.PvZ;

import ir.hamgit.ahh.PvZ.controller.GameController;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;

;

/**
 * Small runnable demo that exercises the tick engine, planting, combat,
 * currency and a special level end to end from the CLI commands defined in
 * the spec. This is NOT the real game entry point (that's MenuController,
 * owned by the menu teammate, per the class reference) - it's here so you
 * can see the engine actually working and use it as a template for wiring
 * GameController into the real menu flow.
 *
 * Run with:  java -cp out Main
 */
public final class GameDemo {

    private GameDemo() {
    }

    public static void main(String[] args) {
        User user = new User();
        user.getUnlockedPlants().add(PlantType.PEASHOOTER);
        user.getUnlockedPlants().add(PlantType.SUNFLOWER);
        user.getUnlockedPlants().add(PlantType.WALLNUT);
        user.getUnlockedPlants().add(PlantType.CHERRY_BOMB);
        user.setDifficulty(3);

        GameController game = new GameController(user);
        runNormalLevelDemo(game, user);
        runSpecialLevelDemo(game);
    }

    private static void runNormalLevelDemo(GameController game, User user) {
        System.out.println("=== Starting a normal Ancient Egypt level (level 1) ===");
        game.startGame("ANCIENT_EGYPT", 1);
        game.selectPlant(PlantType.PEASHOOTER);
        game.selectPlant(PlantType.SUNFLOWER);
        game.selectPlant(PlantType.WALLNUT);
        game.selectPlant(PlantType.CHERRY_BOMB);
        game.startActualGame(3);

        game.handleCommand("plant plant -t SUNFLOWER -l (0, 2)");
        game.handleCommand("plant plant -t PEASHOOTER -l (2, 2)");
        game.handleCommand("plant plant -t WALLNUT -l (5, 2)");
        game.handleCommand("cheat spawn-zombie -t NORMAL -l 8, 2");
        game.handleCommand("cheat spawn-zombie -t CONEHEAD -l 8, 1");

        System.out.println("\n--- advancing 30 ticks (3 seconds) ---");
        game.handleCommand("advance time -t 30 ticks");
        game.handleCommand("show map");
        game.handleCommand("show sun amount");

        System.out.println("\n--- advancing another 200 ticks (20 seconds) ---");
        game.handleCommand("advance time -t 200 ticks");
        game.handleCommand("show sun amount");
        game.handleCommand("zombies info");

        System.out.println("\n--- letting the level run to completion (up to 4000 more ticks) ---");
        game.handleCommand("advance time -t 4000 ticks");

        System.out.println("\nGame over? " + game.getBoard().isGameOver()
            + " | Player won? " + game.getBoard().isPlayerWon());
        game.endGame(game.getBoard().isPlayerWon());
        System.out.println("User coins after level: " + user.getCoins() + ", diamonds: " + user.getDiamonds());
    }

    private static void runSpecialLevelDemo(GameController game) {
        System.out.println("\n=== Starting a Plant What You Get special level (Dark Ages level 3) ===");
        game.startGame("DARK_AGES", 3);
        game.selectPlant(PlantType.PEASHOOTER);
        game.selectPlant(PlantType.WALLNUT);
        game.startActualGame(2);
        game.handleCommand("show sun amount");
        game.handleCommand("plant plant -t PEASHOOTER -l (3, 0)");
        game.handleCommand("plant plant -t PEASHOOTER -l (3, 1)");
        game.handleCommand("start zombie waves");
        game.handleCommand("advance time -t 500 ticks");
        System.out.println("Game over? " + game.getBoard().isGameOver()
            + " | Player won? " + game.getBoard().isPlayerWon());
    }
}
