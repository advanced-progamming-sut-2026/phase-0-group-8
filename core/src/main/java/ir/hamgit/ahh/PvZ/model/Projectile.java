package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.HashSet;
import java.util.Set;

public class Projectile {

    private static final double SPEED_TILES_PER_TICK = 0.5;
    private static final int JESTER_SPIN_TICKS = 30;
    private static final int POISON_TICKS = 50;

    private final ProjectileSpec spec;
    private final Set<Zombie> hitTargets = new HashSet<>();
    private int damage;
    private double x;
    private int lane;
    private int remainingHits;
    private double distanceTravelled;
    private boolean fire;
    private boolean alive = true;
    private boolean ignited;

    public Projectile(ProjectileSpec spec) {
        this.spec = spec;
        damage = spec.getDamage();
        x = spec.getStartX();
        lane = spec.getLane();
        remainingHits = Math.max(1, spec.getPierceCount());
        fire = spec.isFire();
    }

    public void tick(Board board) {
        if (!alive) {
            return;
        }
        if (spec.isHoming()) {
            tickHoming(board);
            return;
        }
        double previousX = x;
        x += SPEED_TILES_PER_TICK * spec.getDirection();
        distanceTravelled += SPEED_TILES_PER_TICK;
        igniteAtTorchwood(board);
        if (isOutOfBounds(board) || distanceTravelled > spec.getMaxRange()) {
            alive = false;
            return;
        }
        if (blockedByFrozenGround(board) || (!spec.isLobber() && blockedByGrave(board))) {
            return;
        }
        Zombie target = findCrossedZombie(board, previousX);
        if (target != null) {
            hitZombie(target, board);
        }
    }

    private void tickHoming(Board board) {
        Zombie target = findNearestZombie(board);
        if (target == null) {
            alive = false;
            return;
        }
        lane = target.getLane();
        x = target.getX();
        hitZombie(target, board);
    }

    private Zombie findNearestZombie(Board board) {
        Zombie nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Zombie zombie : board.getZombies()) {
            double distance = Math.abs(zombie.getX() - x) + Math.abs(zombie.getLane() - lane);
            if (zombie.isAlive() && distance < bestDistance) {
                nearest = zombie;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private Zombie findCrossedZombie(Board board, double previousX) {
        Zombie nearest = null;
        for (Zombie zombie : board.getZombies()) {
            boolean crossed = crossed(previousX, zombie.getX());
            if (zombie.isAlive() && zombie.getLane() == lane && crossed && !hitTargets.contains(zombie)) {
                if (nearest == null || isNearerInDirection(zombie, nearest)) {
                    nearest = zombie;
                }
            }
        }
        return nearest;
    }

    private boolean crossed(double previousX, double zombieX) {
        if (spec.getDirection() > 0) {
            return zombieX >= previousX && zombieX <= x;
        }
        return zombieX <= previousX && zombieX >= x;
    }

    private boolean isNearerInDirection(Zombie candidate, Zombie current) {
        return spec.getDirection() > 0
            ? candidate.getX() < current.getX() : candidate.getX() > current.getX();
    }

    private void igniteAtTorchwood(Board board) {
        if (!spec.isIgnitable() || ignited) {
            return;
        }
        Tile tile = board.getTileAt((int) Math.floor(x), lane);
        Plant plant = tile == null ? null : tile.getPlant();
        if (plant != null && plant.getDef().hasBehavior(BehaviorType.IGNITE_PROJECTILES)) {
            damage *= plant.hasBlueFlame() ? 3 : 2;
            fire = true;
            ignited = true;
        }
    }

    private boolean isOutOfBounds(Board board) {
        return x < 0 || x >= board.getColumns();
    }

    private boolean blockedByGrave(Board board) {
        Tile tile = board.getTileAt((int) Math.floor(x), lane);
        if (tile != null && tile.getType() == TileType.GRAVE && tile.getGravestoneHp() > 0) {
            tile.hitGrave(damage);
            if (tile.getGravestoneHp() == 0) {
                board.onGraveDestroyed(tile);
            }
            alive = false;
            return true;
        }
        return false;
    }

    private boolean blockedByFrozenGround(Board board) {
        Tile tile = board.getTileAt((int) Math.floor(x), lane);
        if (tile == null || tile.getType() != TileType.ICY_GROUND || tile.getIceHp() <= 0) {
            return false;
        }
        tile.hitIce(fire ? damage * 2 : damage);
        alive = false;
        return true;
    }

    private void hitZombie(Zombie target, Board board) {
        if (target.getDef().getType() == ZombieType.PARASOL && spec.isLobber()) {
            alive = false;
            return;
        }
        if (target.getDef().getType() == ZombieType.JESTER && !spec.isLobber()) {
            handleJesterHit(target, board);
            alive = false;
            return;
        }
        dealDamage(target, board);
        if (spec.getSplashRadius() <= 0) {
            applySideEffects(target, board);
        }
        hitTargets.add(target);
        remainingHits--;
        continueOrFinish(board);
    }

    private void dealDamage(Zombie target, Board board) {
        if (fire && target.getDef().getType() == ZombieType.DRAGON_IMP) {
            return;
        }
        if (spec.getSplashRadius() <= 0) {
            target.takeDamage(damage, spec.isIgnoreArmor() || spec.isPoison(), spec.isLobber());
            return;
        }
        for (Zombie zombie : board.getZombies()) {
            boolean inArea = Math.abs(zombie.getLane() - target.getLane()) <= spec.getSplashRadius()
                && Math.abs(zombie.getX() - target.getX()) <= spec.getSplashRadius();
            if (zombie.isAlive() && inArea) {
                if (!(fire && zombie.getDef().getType() == ZombieType.DRAGON_IMP)) {
                    zombie.takeDamage(damage, spec.isIgnoreArmor(), spec.isLobber());
                }
                applySideEffects(zombie, board);
            }
        }
    }

    private void applySideEffects(Zombie target, Board board) {
        if (fire) {
            target.warm();
        }
        if (spec.isIce() && !fire && board.getChapter() != ir.hamgit.ahh.PvZ.model.enums.ChapterType.FROSTBITE_CAVES) {
            target.applyChill(0.5, spec.getChillTicks());
            handleIceInteractions(target);
        }
        if (spec.isPoison()) {
            target.applyPoison(spec.getPoisonDamage(), POISON_TICKS);
        }
        if (spec.getStunTicks() > 0) {
            target.applyStun(spec.getStunTicks());
        }
        if (fire && target.getDef().getType() == ZombieType.EXPLORER) {
            target.setTorchLit(true);
        }
    }

    private void continueOrFinish(Board board) {
        if (spec.isRicochet() && remainingHits > 0) {
            ricochet(board);
        } else if (remainingHits <= 0) {
            alive = false;
        }
    }

    private void ricochet(Board board) {
        int nextLane = lane + (lane >= board.getRows() - 1 ? -1 : 1);
        lane = Math.max(0, Math.min(board.getRows() - 1, nextLane));
    }

    private void handleJesterHit(Zombie jester, Board board) {
        if (jester.isReflecting()) {
            Plant nearestPlant = findNearestPlantLeft(jester, board);
            if (nearestPlant != null) {
                damageReflectedPlant(nearestPlant, board);
            }
        }
        jester.triggerSpin(JESTER_SPIN_TICKS);
    }

    private void damageReflectedPlant(Plant plant, Board board) {
        plant.takeDamage(damage);
        if (!plant.isAlive()) {
            board.handlePlantDestroyed(plant);
        } else if (spec.isIce()) {
            plant.applyIceLayer();
        }
    }

    private Plant findNearestPlantLeft(Zombie jester, Board board) {
        for (int x = Math.min(board.getColumns() - 1, (int) jester.getX()); x >= 0; x--) {
            Tile tile = board.getTileAt(x, jester.getLane());
            if (tile != null && !tile.isEmpty()) {
                return tile.getPlant();
            }
        }
        return null;
    }

    private void handleIceInteractions(Zombie target) {
        if (target.getDef().getType() == ZombieType.EXPLORER) {
            target.setTorchLit(false);
        }
        if (target.getDef().getType() == ZombieType.PROSPECTOR) {
            target.extinguishDynamite();
        }
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isLobber() {
        return spec.isLobber();
    }

    public double getX() {
        return x;
    }

    public int getLane() {
        return lane;
    }

    public PlantType getOwnerType() {
        return spec.getOwnerType();
    }

    public boolean isFire() {
        return fire;
    }

    public boolean isIce() {
        return spec.isIce() && !fire;
    }

    public boolean isPoison() {
        return spec.isPoison();
    }

    public int getSplashRadius() {
        return spec.getSplashRadius();
    }

    public int getStartX() {
        return spec.getStartX();
    }

    public int getDirection() {
        return spec.getDirection();
    }
}
