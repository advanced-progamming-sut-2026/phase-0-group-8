package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.entities.Armor;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

/**
 * Formats the various {@code show ...} CLI commands. Split out of
 * {@link Board} purely to keep Board under the project's class-length
 * Checkstyle/PMD guideline (500 lines) - all of it operates on a Board it is
 * given, it holds no state of its own.
 */
public final class BoardPrinter {

    private BoardPrinter() {
    }

    public static void showMap(Board board) {
        System.out.printf("Wave: %d/%d | Sun: %d | Plant food: %d | Tick: %d%n",
            board.getCurrentWave() + 1, board.getTotalWaves(), board.getSunAmount(),
            board.getPlantFoodCount(), board.getTickCount());
        for (int r = 0; r < board.getRows(); r++) {
            printRow(board, r);
        }
        printLawnMowers(board);
    }

    private static void printRow(Board board, int row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < board.getColumns(); c++) {
            sb.append('[').append(describeTile(board, row, c)).append(']');
        }
        System.out.println(sb);
    }

    private static String describeTile(Board board, int row, int col) {
        Tile tile = board.getTileAt(col, row);
        StringBuilder sb = new StringBuilder();
        sb.append(tile.getType().name().charAt(0));
        if (!tile.isEmpty()) {
            sb.append(':').append(tile.getPlant().getDef().getType());
        }
        for (Zombie z : board.getZombies()) {
            if (z.isAlive() && z.getLane() == row && (int) Math.round(z.getX()) == col) {
                sb.append('|').append(z.getDef().getType());
            }
        }
        return sb.toString();
    }

    private static void printLawnMowers(Board board) {
        StringBuilder sb = new StringBuilder("Lawn mowers: ");
        boolean[] mowers = board.getLawnMowerAvailability();
        for (int r = 0; r < mowers.length; r++) {
            sb.append("row ").append(r).append('=').append(mowers[r] ? "ready" : "used").append("  ");
        }
        System.out.println(sb);
    }

    public static void showPlantsStatus(Board board) {
        for (PlantDef def : PlantRegistry.getAll()) {
            System.out.printf("%s: cost=%d recharge=%ds%n", def.getType(), def.getSunCost(), def.getRechargeSeconds());
        }
    }

    public static void showTileStatus(Board board, int x, int lane) {
        Tile tile = board.getTileAt(x, lane);
        if (tile == null) {
            System.out.println("No such tile.");
            return;
        }
        System.out.printf("Tile (%d, %d): type=%s%n", x, lane, tile.getType());
        if (!tile.isEmpty()) {
            Plant p = tile.getPlant();
            System.out.printf("  Plant: %s hp=%d level=%d boosted=%b%n",
                p.getDef().getType(), p.getCurrentHp(), p.getLevel(), p.isBoosted());
        }
        for (Zombie z : board.getZombies()) {
            if (z.isAlive() && z.getLane() == lane && (int) Math.round(z.getX()) == x) {
                System.out.printf("  Zombie: %s hp=%d%n", z.getDef().getType(), z.getCurrentHp());
            }
        }
    }

    public static void showSunAmount(Board board) {
        System.out.println("Sun: " + board.getSunAmount());
    }

    public static void showZombiesInfo(Board board) {
        for (Zombie z : board.getZombies()) {
            if (z.isAlive()) {
                printZombieInfo(z);
            }
        }
    }

    private static void printZombieInfo(Zombie z) {
        System.out.println(z.getDef().getType() + ":");
        System.out.printf("  position: %.1f, %d%n", z.getX(), z.getLane());
        System.out.println("  health: " + z.getCurrentHp());
        System.out.println("  armor:");
        for (Armor a : z.getArmors()) {
            if (!a.isDestroyed()) {
                System.out.printf("    %s: %d%n", a.getType(), a.getCurrentHp());
            }
        }
        System.out.println("  effects:");
    }
}
