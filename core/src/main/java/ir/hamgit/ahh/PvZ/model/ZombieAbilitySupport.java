package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.entities.Armor;

import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.*;

class ZombieAbilitySupport {

    private static final Set<ArmorType> METAL_ARMORS = EnumSet.of(
        ArmorType.BUCKET, ArmorType.HELMET, ArmorType.SHOULDER, ArmorType.BLOCK, ArmorType.ARCADE_MACHINE);

    private final Map<Plant, Zombie> catSpellCasters = new HashMap<>();

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
            target.coverWithOctopus();
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
                if (board.getChapter() == ir.hamgit.ahh.PvZ.model.enums.ChapterType.DARK_AGES) {
                    tile.rollDarkAgesReward();
                }
            }
        }
    }

    void damageNearestPlantLeft(Board board, int lane, double zombieX, int damage) {
        for (int x = Math.min(board.getColumns() - 1, (int) zombieX); x >= 0; x--) {
            Tile tile = board.getTileAt(x, lane);
            if (tile != null && !tile.isEmpty()) {
                Plant plant = tile.getPlant();
                plant.takeDamage(damage);
                if (!plant.isAlive()) {
                    board.handlePlantDestroyed(plant);
                }
                return;
            }
        }
    }

    void destroyPlantsInLane(Board board, int lane) {
        for (int x = 0; x < board.getColumns(); x++) {
            Tile tile = board.getTileAt(x, lane);
            for (Plant plant : tile.getPlantLayers()) {
                board.destroyPlantInstantly(plant);
            }
        }
    }
}
