package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

/** A shot fired by a plant: straight peas, lobbed shells, ice/fire/poison variants. */
public class Projectile {

    private static final double SPEED_TILES_PER_TICK = 0.5;
    private static final int JESTER_SPIN_TICKS = 30;

    private final PlantType ownerType;
    private final int damage;
    private final boolean fireType;
    private final boolean iceType;
    private final boolean poisonType;
    private final boolean lobber;
    private double x;
    private final int lane;
    private boolean alive = true;

    public Projectile(PlantType ownerType, int damage, boolean fireType, boolean iceType,
                      boolean poisonType, boolean lobber, int startX, int lane) {
        this.ownerType = ownerType;
        this.damage = damage;
        this.fireType = fireType;
        this.iceType = iceType;
        this.poisonType = poisonType;
        this.lobber = lobber;
        this.x = startX;
        this.lane = lane;
    }

    public void tick(Board board) {
        if (!alive) {
            return;
        }
        x += SPEED_TILES_PER_TICK;
        if (x >= board.getColumns()) {
            alive = false;
            return;
        }
        if (!lobber && blockedByGrave(board)) {
            return;
        }
        Zombie target = board.findNearestZombieAheadOfProjectile(lane, x);
        if (target != null) {
            hitZombie(target, board);
        }
    }

    private boolean blockedByGrave(Board board) {
        Tile tile = board.getTileAt((int) Math.floor(x), lane);
        if (tile != null && tile.getType() == TileType.GRAVE && tile.getGravestoneHp() > 0) {
            tile.hitGrave(damage);
            alive = false;
            return true;
        }
        return false;
    }

    private void hitZombie(Zombie target, Board board) {
        if (target.getDef().getType() == ZombieType.PARASOL && lobber) {
            alive = false;
            return;
        }
        if (target.getDef().getType() == ZombieType.JESTER) {
            handleJesterHit(target, board);
            alive = false;
            return;
        }
        target.takeDamage(effectiveDamage(target), poisonType, lobber);
        applySideEffects(target);
        alive = false;
    }

    /**
     * Per spec: an incoming shot starts the Jester spinning; while spinning,
     * further straight shots get reflected back onto the nearest plant
     * (with ice shots freezing that plant too) instead of damaging the
     * Jester. Each reflected hit also refreshes the spin duration, matching
     * "spins for as long as shots keep coming in".
     */
    private void handleJesterHit(Zombie jester, Board board) {
        if (jester.isReflecting()) {
            reflectOntoNearestPlant(jester, board);
        }
        jester.triggerSpin(JESTER_SPIN_TICKS);
    }

    private void reflectOntoNearestPlant(Zombie jester, Board board) {
        Plant nearestPlant = board.getPlantInFrontOf(jester);
        if (nearestPlant != null) {
            nearestPlant.takeDamage(effectiveDamage(jester));
            if (iceType) {
                nearestPlant.applyIceLayer();
            }
        }
    }

    private int effectiveDamage(Zombie target) {
        boolean immuneToFire = target.getDef().getType() == ZombieType.DRAGON_IMP;
        return fireType && !immuneToFire ? damage * 2 : damage;
    }

    private void applySideEffects(Zombie target) {
        if (iceType) {
            target.applyChill(0.5, 30);
            handleIceInteractions(target);
        }
        if (fireType && target.getDef().getType() == ZombieType.EXPLORER) {
            target.setTorchLit(true);
        }
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
        return lobber;
    }

    public double getX() {
        return x;
    }

    public int getLane() {
        return lane;
    }

    public PlantType getOwnerType() {
        return ownerType;
    }
}
