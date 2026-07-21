package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PlantFoodSupport {

    private static final int GIANT_MULTIPLIER = 20;
    private static final int BARRAGE_WAVES = 5;
    private static final int METAL_ARMOR_HP = 4000;

    private final PlantDef def;
    private final PlantProjectileLauncher launcher;

    PlantFoodSupport(PlantDef def, PlantProjectileLauncher launcher) {
        this.def = def;
        this.launcher = launcher;
    }

    void apply(Plant plant, Board board) {
        switch (def.getType()) {
            case SUNFLOWER -> board.addSun(150);
            case TWIN_SUNFLOWER -> board.addSun(250);
            case SUN_SHROOM -> growAndProduce(plant, board, 225);
            case PRIMAL_SUNFLOWER -> board.addSun(225);
            case PEASHOOTER, FIRE_PEASHOOTER, GOO_PEASHOOTER,
                 CAT_TAIL -> launcher.fire(plant, board, plant.effectiveDamage(def.getDamage()));
            case SNOW_PEA -> snowBarrage(plant, board);
            case REPEATER -> heavyRepeater(plant, board);
            case THREEPEATER, ROTOBAGA, SPLIT_PEA, STARFRUIT -> barrage(plant, board);
            case PEA_POD -> giantPeas(plant, board, plant.getStackCount());
            case CITRON -> clearLane(plant, board);
            case CAULIPOWER -> hypnotizeRandom(board, 3);
            case ELECTRIC_BLUEBERRY -> destroyRandom(board, 3);
            case BOWLING_BULB -> launcher.launchFoodBulbs(plant, board,
                plant.effectiveDamage(def.getDamage()), 3);
            case CACTUS -> launcher.launchInfinitePiercing(plant, board,
                plant.effectiveDamage(def.getDamage()) * 5);
            case MEGA_GATLING_PEA -> megaBarrage(plant, board);
            case SEA_SHROOM, PUFF_SHROOM -> resetMushroomsAndBarrage(plant, board);
            case FUME_SHROOM -> fumeCloud(plant, board);
            case CABBAGE_PULT, MELON_PULT -> damageRandom(plant, board, 4, 5, false);
            case KERNEL_PULT -> butterAll(board);
            case WINTER_MELON -> damageRandom(plant, board, 4, 5, true);
            case PEPPER_PULT -> damageRandom(plant, board, 3, 5, false);
            case POTATO_MINE, PRIMAL_POTATO_MINE -> armAndClone(plant, board);
            case SQUASH -> destroyRandom(board, 2);
            case TANGLE_KELP -> drownAquatic(board, 4);
            case ICEBERG_LETTUCE -> freezeAll(plant, board);
            case BONK_CHOY, PHAT_BEET, WASABI_WHIP, KIWIBEAST -> areaStrike(plant, board);
            case CHOMPER -> destroyRandom(board, 3);
            case WALL_NUT -> plant.addPermanentArmor(METAL_ARMOR_HP);
            case TALL_NUT -> plant.addPermanentArmor(METAL_ARMOR_HP * 2);
            case ENDURIAN -> reinforceEndurian(plant);
            case EXPLODE_O_NUT, PUMPKIN, SUN_BEAN -> plant.addPermanentArmor(METAL_ARMOR_HP);
            case GARLIC -> redirectLane(plant, board);
            case SWEET_POTATO -> attractAndHeal(plant, board);
            case TORCHWOOD -> plant.enableBlueFlame();
            case MAGNET_SHROOM -> removeMetal(board, plant);
            case HYPNO_SHROOM -> plant.empowerNextEater();
            case LILY_PAD -> copyLilyPads(plant, board);
            default -> applyGenericInstant(plant, board);
        }
    }

    private void growAndProduce(Plant plant, Board board, int sun) {
        plant.growImmediately();
        board.addSun(sun);
    }

    private void heavyRepeater(Plant plant, Board board) {
        launcher.fire(plant, board, plant.effectiveDamage(def.getDamage()));
        giantPeas(plant, board, 1);
    }

    private void snowBarrage(Plant plant, Board board) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive() && zombie.getLane() == plant.getLane()) {
                zombie.freeze(PlantLevelRuntime.freezeTicks(plant, 50));
            }
        }
        launcher.fire(plant, board, plant.effectiveDamage(def.getDamage()));
    }

    private void reinforceEndurian(Plant plant) {
        plant.addPermanentArmor(METAL_ARMOR_HP);
        plant.boostReflection();
    }

    private void barrage(Plant plant, Board board) {
        int damage = plant.effectiveDamage(def.getDamage());
        for (int i = 0; i < BARRAGE_WAVES; i++) {
            launcher.fire(plant, board, damage);
        }
    }

    private void giantPeas(Plant plant, Board board, int count) {
        launcher.launchGiantPeas(plant, board,
            plant.effectiveDamage(def.getDamage()) * GIANT_MULTIPLIER, count);
    }

    private void clearLane(Plant plant, Board board) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive() && zombie.getLane() == plant.getLane()) {
                zombie.forceKill();
            }
        }
    }

    private void megaBarrage(Plant plant, Board board) {
        barrage(plant, board);
        giantPeas(plant, board, 4);
    }

    private void resetMushroomsAndBarrage(Plant source, Board board) {
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                for (Plant plant : board.getTileAt(x, lane).getPlantLayers()) {
                    if (plant.getDef().getType() == def.getType()) {
                        plant.resetLifespan();
                    }
                }
            }
        }
        launcher.fire(source, board, source.effectiveDamage(def.getDamage()));
    }

    private void fumeCloud(Plant plant, Board board) {
        int damage = plant.effectiveDamage(def.getDamage()) * 5;
        for (Zombie zombie : board.getZombies()) {
            boolean inLane = zombie.isAlive() && zombie.getLane() == plant.getLane();
            if (inLane && zombie.getX() >= plant.getX()) {
                zombie.takeDamage(damage, true);
                zombie.knockBack(2);
            }
        }
    }

    private void damageRandom(Plant plant, Board board, int count, int multiplier, boolean freeze) {
        List<Zombie> targets = shuffledZombies(board);
        for (int i = 0; i < Math.min(count, targets.size()); i++) {
            targets.get(i).takeDamage(plant.effectiveDamage(def.getDamage()) * multiplier, false, true);
            if (freeze) {
                targets.get(i).freeze(PlantLevelRuntime.freezeTicks(plant, 50));
            }
        }
    }

    private void butterAll(Board board) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive()) {
                zombie.applyStun(30);
            }
        }
    }

    private void armAndClone(Plant plant, Board board) {
        plant.armImmediately();
        List<int[]> spaces = emptySpaces(board);
        Collections.shuffle(spaces);
        for (int i = 0; i < Math.min(2, spaces.size()); i++) {
            int[] spot = spaces.get(i);
            if (board.plantForFree(def.getType(), spot[0], spot[1])) {
                Plant clone = board.getTileAt(spot[0], spot[1]).getPlant();
                clone.setLevel(plant.getLevel());
                clone.armImmediately();
            }
        }
    }

    private void drownAquatic(Board board, int count) {
        List<Zombie> targets = shuffledZombies(board);
        int drowned = 0;
        for (Zombie zombie : targets) {
            Tile tile = board.getTileAt((int) Math.round(zombie.getX()), zombie.getLane());
            if (tile != null && tile.getType() == TileType.WATER && drowned++ < count) {
                zombie.forceKill();
            }
        }
    }

    private void freezeAll(Plant plant, Board board) {
        for (Zombie zombie : board.getZombies()) {
            zombie.freeze(PlantLevelRuntime.freezeTicks(plant, 50));
        }
    }

    private void areaStrike(Plant plant, Board board) {
        int damage = plant.effectiveDamage(def.getDamage()) * 8;
        board.dealAreaDamageToZombies(plant.getX(), plant.getLane(), 1, damage);
    }

    private void redirectLane(Plant plant, Board board) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive() && zombie.getLane() == plant.getLane()) {
                zombie.redirectToAdjacentLane(board.getRows());
            }
        }
    }

    private void attractAndHeal(Plant plant, Board board) {
        plant.restoreHealth();
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive() && Math.abs(zombie.getX() - plant.getX()) <= 3) {
                zombie.setLane(plant.getLane());
            }
        }
    }

    private void removeMetal(Board board, Plant plant) {
        int removed = 0;
        while (removed < 5 && board.stealMetalArmorNear(plant.getX(), plant.getLane(),
            PlantLevelRuntime.range(plant, def.getAbilityProfile().getRangeTiles()))) {
            removed++;
        }
    }

    private void hypnotizeRandom(Board board, int count) {
        List<Zombie> targets = shuffledZombies(board);
        for (int i = 0; i < Math.min(count, targets.size()); i++) {
            targets.get(i).hypnotize();
        }
    }

    private void destroyRandom(Board board, int count) {
        List<Zombie> targets = shuffledZombies(board);
        for (int i = 0; i < Math.min(count, targets.size()); i++) {
            targets.get(i).forceKill();
        }
    }

    private List<Zombie> shuffledZombies(Board board) {
        List<Zombie> targets = new ArrayList<>();
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive() && !zombie.isHypnotized()) {
                targets.add(zombie);
            }
        }
        Collections.shuffle(targets);
        return targets;
    }

    private List<int[]> emptySpaces(Board board) {
        List<int[]> spaces = new ArrayList<>();
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                Tile tile = board.getTileAt(x, lane);
                if (tile.isEmpty() && tile.isPlantable()) {
                    spaces.add(new int[] {x, lane});
                }
            }
        }
        return spaces;
    }

    private void copyLilyPads(Plant source, Board board) {
        List<int[]> spaces = new ArrayList<>();
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int x = 0; x < board.getColumns(); x++) {
                Tile tile = board.getTileAt(x, lane);
                if (tile.getType() == TileType.WATER && tile.isEmpty()) {
                    spaces.add(new int[] {x, lane});
                }
            }
        }
        Collections.shuffle(spaces);
        for (int i = 0; i < Math.min(3, spaces.size()); i++) {
            int[] spot = spaces.get(i);
            if (board.plantForFree(PlantType.LILY_PAD, spot[0], spot[1])) {
                board.getTileAt(spot[0], spot[1]).getPlant().setLevel(source.getLevel());
            }
        }
    }

    private void applyGenericInstant(Plant plant, Board board) {
        if (def.hasBehavior(BehaviorType.CONTACT_FREEZE)) {
            freezeAll(plant, board);
        }
    }
}
