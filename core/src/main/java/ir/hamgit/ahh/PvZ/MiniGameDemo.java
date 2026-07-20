package ir.hamgit.ahh.PvZ;


import ir.hamgit.ahh.PvZ.model.minigame.BeghouledGame;
import ir.hamgit.ahh.PvZ.model.minigame.IZombieGame;
import ir.hamgit.ahh.PvZ.model.minigame.VasebreakerGame;
import ir.hamgit.ahh.PvZ.model.minigame.WallnutBowlingGame;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;


import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runnable demo exercising all 4 minigames end to end. Like {@link Main}, this
 * is a sanity-check/template, not the real entry point - the menu teammate's
 * TravelLogMenu is what actually launches these.
 *
 * Run with: java -cp out MinigameDemo
 */
public final class MiniGameDemo {

    private MiniGameDemo() {
    }

    public static void main(String[] args) {
        runVasebreaker();
        runWallnutBowling();
        runIZombie();
        runBeghouled();
    }

    private static void runVasebreaker() {
        System.out.println("=== Vasebreaker ===");
        List<int[]> plantVases = List.of(new int[] {2, 2});
        List<int[]> gargantuarVases = List.of(new int[] {6, 3});
        VasebreakerGame game = new VasebreakerGame(plantVases, gargantuarVases);
        game.handle("break vase -l (2, 2)");
        game.handle("collect seed packet -l (2, 2)");
        game.handle("plant plant -t WALLNUT -l (2, 2)");
        game.handle("break vase -l (6, 3)");
        game.handle("advance time -t 100 ticks");
        System.out.println("Over? " + game.isOver() + " Won? " + game.isWon());
    }

    private static void runWallnutBowling() {
        System.out.println("\n=== Wallnut Bowling ===");
        List<ZombieType> zombies = List.of(ZombieType.NORMAL, ZombieType.CONEHEAD);
        WallnutBowlingGame game = new WallnutBowlingGame(4, zombies);
        game.handle("plant ball -l (0, 2)");
        game.handle("advance time -t 60 ticks");
        System.out.println("Over? " + game.isOver() + " Won? " + game.isWon());
    }

    private static void runIZombie() {
        System.out.println("\n=== I, Zombie ===");
        List<ZombieType> roster = List.of(ZombieType.NORMAL, ZombieType.CONEHEAD, ZombieType.GARGANTUAR);
        Map<ZombieType, Integer> costs = new EnumMap<>(ZombieType.class);
        costs.put(ZombieType.NORMAL, 50);
        costs.put(ZombieType.CONEHEAD, 75);
        costs.put(ZombieType.GARGANTUAR, 150);
        IZombieGame game = new IZombieGame(roster, costs);
        game.handle("place zombie -t NORMAL -l (8, 0)");
        game.handle("advance time -t 200 ticks");
        System.out.println("Sun: " + game.getSunAmount() + " Over? " + game.isOver() + " Won? " + game.isWon());
    }

    private static void runBeghouled() {
        System.out.println("\n=== Beghouled ===");
        BeghouledGame game = new BeghouledGame(3, 3);
        for (int i = 0; i < 30 && !game.isOver(); i++) {
            game.swapPlants(i % 8, i % 5, (i % 8) + 1, i % 5);
        }
        game.handle("upgrade -f PEASHOOTER -t REPEATER");
        System.out.println("Matches: " + game.getMatchesMade() + " Sun: " + game.getSunAmount());
    }
}
