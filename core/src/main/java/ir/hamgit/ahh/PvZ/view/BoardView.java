package ir.hamgit.ahh.PvZ.view;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.entities.Armor;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Tile;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;


public final class BoardView {

    private BoardView() {
    }

    public static void showMap(Board board) {
        System.out.printf("Wave: %d/%d | Sun: %d | Plant food: %d | Tick: %d%n",
            board.getCurrentWave() + 1, board.getTotalWaves(), board.getSunAmount(),
            board.getPlantFoodCount(), board.getTickCount());
        showGrid(board);
        showLawnMowers(board);
    }

    public static void showGrid(Board board) {
        for (int r = 0; r < board.getRows(); r++) {
            printRow(board, r);
        }
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

    public static void showLawnMowers(Board board) {
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
        for (Plant plant : tile.getPlantLayers()) {
            PlantDef def = plant.getDef();
            System.out.printf("  Plant: %s hp=%d/%d damage=%d cost=%d level=%d boosted=%b%n",
                def.getType(), plant.getCurrentHp(), plant.getEffectiveMaxHp(),
                plant.effectiveDamage(def.getDamage()), def.getSunCost(), plant.getLevel(),
                plant.isBoosted());
            System.out.println("    tags=" + def.getTags() + " behaviors=" + def.getBehaviors());
        }
        for (Zombie z : board.getZombies()) {
            if (z.isAlive() && z.getLane() == lane && (int) Math.round(z.getX()) == x) {
                System.out.printf("  Zombie: %s hp=%d/%d speed=%.2f damage=%d armor=%s%n",
                    z.getDef().getType(), z.getCurrentHp(), z.getDef().getMaxHp(),
                    z.getDef().getSpeed(), z.getDef().getDamage(), z.getArmors().stream()
                        .map(armor -> armor.getType() + ":" + armor.getCurrentHp()).toList());
                System.out.println("    behaviors=" + z.getDef().getBehaviors());
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
