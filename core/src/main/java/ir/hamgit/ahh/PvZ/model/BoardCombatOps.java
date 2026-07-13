package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;

/**
 * Targeting/damage/projectile helpers used by {@link Plant}, {@link Zombie}
 * and {@link Projectile}. Split out of {@link Board} purely to keep Board
 * under the project's class-length Checkstyle/PMD guideline - Board keeps
 * the exact same public methods (so nothing outside `model` even notices
 * this exists), they just delegate here. Reaches board state entirely
 * through Board's own public API, same pattern as {@link ZombieAbilitySupport}.
 */
class BoardCombatOps {

    boolean hasZombieInLaneAhead(Board board, int plantX, int lane, int range) {
        for (Zombie z : board.getZombies()) {
            boolean ahead = z.isAlive() && z.getLane() == lane && z.getX() > plantX
                && z.getX() <= plantX + Math.max(1, range);
            if (ahead) {
                return true;
            }
        }
        return false;
    }

    boolean hasAdjacentZombie(Board board, int plantX, int lane) {
        return hasZombieInLaneAhead(board, plantX, lane, 1);
    }

    Zombie findNearestZombieAheadOfProjectile(Board board, int lane, double projectileX) {
        Zombie best = null;
        for (Zombie z : board.getZombies()) {
            if (z.isAlive() && z.getLane() == lane && z.getX() <= projectileX
                && (best == null || z.getX() < best.getX())) {
                best = z;
            }
        }
        return best;
    }

    Zombie getNearestEnemyZombieInFront(Board board, Zombie hypnotized) {
        Zombie best = null;
        for (Zombie z : board.getZombies()) {
            boolean candidate = z != hypnotized && z.isAlive() && z.getLane() == hypnotized.getLane()
                && z.getX() > hypnotized.getX();
            if (candidate && (best == null || z.getX() < best.getX())) {
                best = z;
            }
        }
        return best;
    }

    Plant getPlantInFrontOf(Board board, Zombie zombie) {
        int col = (int) Math.floor(zombie.getX());
        Tile tile = board.getTileAt(col, zombie.getLane());
        return tile == null || tile.isEmpty() ? null : tile.getPlant();
    }

    void spawnProjectile(Board board, Plant source) {
        PlantDef def = source.getDef();
        boolean lobber = def.hasBehavior(BehaviorType.SHOOT_ARC);
        boolean fire = def.hasBehavior(BehaviorType.SHOOT_FIRE);
        boolean ice = def.hasBehavior(BehaviorType.SHOOT_ICE);
        boolean poison = def.hasBehavior(BehaviorType.SHOOT_POISON);
        board.getProjectiles().add(new Projectile(def.getType(), def.getDamage(), fire, ice, poison, lobber,
            source.getX(), source.getLane()));
    }

    void dealAreaDamageToZombies(Board board, int centerX, int centerLane, int radius, int damage) {
        for (Zombie z : board.getZombies()) {
            if (!z.isAlive()) {
                continue;
            }
            boolean inLane = Math.abs(z.getLane() - centerLane) <= radius;
            boolean inColumn = radius >= 9 || Math.abs(z.getX() - centerX) <= radius;
            if (inLane && inColumn) {
                z.takeDamage(damage, false);
            }
        }
    }

    void handlePlantDestroyed(Board board, Plant target) {
        System.out.printf("Plant %s at (%d, %d) is destroyed.%n",
            target.getDef().getType(), target.getX(), target.getLane());
        Tile tile = board.getTileAt(target.getX(), target.getLane());
        if (tile != null && tile.getPlant() == target) {
            tile.removePlant();
        }
        board.getSpecialLevelHandler().onPlantLost(board, target);
    }

    void destroyPlantInstantly(Board board, Plant target) {
        target.destroy();
        handlePlantDestroyed(board, target);
    }

    void markDeathHandledIfNeeded(Board board, Zombie zombie) {
        if (!zombie.isAlive() && !zombie.isDeathHandled()) {
            zombie.die(board);
        }
    }
}
