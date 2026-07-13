package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Board-adjacent helper for the zombie special abilities that manipulate
 * plants or other zombies: Turquoise's laser, Hunter's ice throw, Octopus's
 * throw, Fisherman's hook, Wizard's cat spell (+ reverting it when the
 * Wizard dies), King's knight upgrade, Pianist's row shuffle, Tombraiser's
 * graves.
 *
 * <p>Split out of {@link Board} purely to keep Board under the project's
 * class-length Checkstyle/PMD guideline (500 lines) - it only holds the one
 * bit of state that doesn't belong on Board itself (which wizard cast which
 * cat spell); everything else it does is through Board's own public API, so
 * it reads exactly like code that lives on Board.</p>
 */
class ZombieAbilitySupport {

    private static final java.util.Set<ArmorType> METAL_ARMORS = java.util.EnumSet.of(
        ArmorType.BUCKET, ArmorType.HELMET, ArmorType.SHOULDER, ArmorType.BLOCK, ArmorType.ARCADE_MACHINE);

    private final Map<Plant, Zombie> catSpellCasters = new HashMap<>();

    /** MagnetShroom: strips the first metal armor layer off the nearest zombie in range. */
    boolean stealMetalArmorNear(Board board, int plantX, int lane, int range) {
        for (Zombie z : board.getZombies()) {
            boolean inRange = z.isAlive() && z.getLane() == lane && Math.abs(z.getX() - plantX) <= range;
            if (inRange && stripFirstMetalArmor(z)) {
                return true;
            }
        }
        return false;
    }

    private boolean stripFirstMetalArmor(Zombie z) {
        for (Armor armor : z.getArmors()) {
            if (METAL_ARMORS.contains(armor.getType()) && !armor.isDestroyed()) {
                z.stripArmor(armor.getType());
                return true;
            }
        }
        return false;
    }

    boolean hasPlantWithinTiles(Board board, int lane, double x, int range) {
        int from = Math.max(0, (int) x - range);
        int to = Math.min(board.getColumns() - 1, (int) x + range);
        for (int c = from; c <= to; c++) {
            Tile tile = board.getTileAt(c, lane);
            if (tile != null && !tile.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    void laserDestroyPlantsAhead(Board board, Zombie source, int range) {
        int lane = source.getLane();
        int from = Math.max(0, (int) source.getX() - range);
        for (int c = from; c < (int) source.getX(); c++) {
            Tile tile = board.getTileAt(c, lane);
            if (tile != null && !tile.isEmpty()) {
                board.destroyPlantInstantly(tile.getPlant());
            }
        }
    }

    void throwIceAtNearestPlant(Board board, Zombie hunter) {
        Plant target = nearestPlantInLane(board, hunter.getLane());
        if (target != null) {
            target.applyIceLayer();
        }
    }

    void throwOctopusAtPlant(Board board, Zombie octopus) {
        Plant target = nearestPlantInLane(board, octopus.getLane());
        if (target != null) {
            target.freezeCompletely();
        }
    }

    private Plant nearestPlantInLane(Board board, int lane) {
        for (int c = board.getColumns() - 1; c >= 0; c--) {
            Tile tile = board.getTileAt(c, lane);
            if (tile != null && !tile.isEmpty()) {
                return tile.getPlant();
            }
        }
        return null;
    }

    void hookPlantTowards(Board board, Zombie fisherman) {
        int lane = fisherman.getLane();
        for (int c = board.getColumns() - 1; c >= 1; c--) {
            Tile current = board.getTileAt(c, lane);
            Tile left = board.getTileAt(c - 1, lane);
            if (isOccupiedAndTargetEmpty(current, left)) {
                left.plantHere(current.removePlant());
                return;
            }
            if (current != null && !current.isEmpty() && c == board.getColumns() - 1) {
                board.destroyPlantInstantly(current.getPlant());
                return;
            }
        }
    }

    private boolean isOccupiedAndTargetEmpty(Tile current, Tile left) {
        return current != null && !current.isEmpty() && left != null && left.isEmpty();
    }

    void turnRandomPlantIntoCat(Board board, int lane, Zombie wizard) {
        List<Plant> candidates = new ArrayList<>();
        for (int c = 0; c < board.getColumns(); c++) {
            Tile tile = board.getTileAt(c, lane);
            if (tile != null && !tile.isEmpty() && !tile.getPlant().isCat()) {
                candidates.add(tile.getPlant());
            }
        }
        if (!candidates.isEmpty()) {
            Plant chosen = candidates.get((int) (Math.random() * candidates.size()));
            chosen.turnIntoCat();
            catSpellCasters.put(chosen, wizard);
        }
    }

    /** Per spec: a cat-transformed plant reverts once the wizard that cast it dies. */
    void revertCatsCastBy(Zombie wizard) {
        catSpellCasters.entrySet().removeIf(entry -> {
            boolean castByThisWizard = entry.getValue() == wizard;
            if (castByThisWizard) {
                entry.getKey().turnBackFromCat();
            }
            return castByThisWizard;
        });
    }

    void upgradeNearbyZombieToKnight(Board board, Zombie king) {
        for (Zombie z : board.getZombies()) {
            boolean eligible = z.isAlive() && z.getLane() == king.getLane()
                && z.getDef().getType() == ZombieType.NORMAL && z.getArmors().isEmpty();
            if (eligible) {
                z.addArmor(ArmorType.HELMET);
                z.addArmor(ArmorType.SHOULDER);
                break;
            }
        }
    }

    void shuffleRandomZombieRow(Board board) {
        List<Zombie> zombies = board.getZombies();
        if (zombies.isEmpty()) {
            return;
        }
        Zombie z = zombies.get((int) (Math.random() * zombies.size()));
        int delta = Math.random() < 0.5 ? -1 : 1;
        int newLane = z.getLane() + delta;
        if (newLane >= 0 && newLane < board.getRows()) {
            z.setLane(newLane);
        }
    }

    void spawnRandomGraves(Board board, int count) {
        for (int i = 0; i < count; i++) {
            int r = (int) (Math.random() * board.getRows());
            int c = (int) (Math.random() * board.getColumns());
            Tile tile = board.getTileAt(c, r);
            if (tile != null && tile.isEmpty()) {
                tile.makeGrave();
            }
        }
    }
}
