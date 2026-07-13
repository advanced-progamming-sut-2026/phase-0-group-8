package ir.hamgit.ahh.PvZ.minigame;


import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Zombie;

/**
 * A rolling ball for the Wallnut Bowling minigame. Unlike {@code model.Projectile}
 * it isn't fired by a plant sitting on a tile - it's placed directly on the lawn
 * and keeps moving (and, for the normal ball, bouncing) until it runs out of board.
 *
 * <p>Simplification: the real game changes the ball's bounce <em>angle</em>
 * (45° then 90°) on each hit/wall-bounce, which needs true 2D movement this
 * project's lane-based board doesn't have. Here a normal ball just reverses
 * direction on a hit or a wall, which preserves the "keeps going until it runs
 * out of board" feel without the angle math.</p>
 */
public class BowlingBall {

    /** BOWLING = plain Wall-nut bowling ball, EXPLODE_O_NUT = explodes in a 3x3 area on
     *  its first hit, GIANT = crushes through zombies without bouncing or dying. */
    public enum Kind { BOWLING, EXPLODE_O_NUT, GIANT }

    private static final double SPEED_TILES_PER_TICK = 0.6;
    private static final int NORMAL_DAMAGE = 1800;
    private static final int EXPLODE_DAMAGE = 1800;
    private static final int EXPLODE_RADIUS = 1;

    private final Kind kind;
    private double x;
    private final int lane;
    private int direction;
    private boolean alive = true;

    public BowlingBall(Kind kind, double x, int lane, int direction) {
        this.kind = kind;
        this.x = x;
        this.lane = lane;
        this.direction = direction;
    }

    public void tick(Board board) {
        if (!alive) {
            return;
        }
        x += SPEED_TILES_PER_TICK * direction;
        if (bounceOffWalls(board)) {
            return;
        }
        checkZombieCollision(board);
    }

    private boolean bounceOffWalls(Board board) {
        if (x <= 0) {
            x = 0;
            bounceOrDie();
            return true;
        }
        if (x >= board.getColumns()) {
            alive = false;
            return true;
        }
        return false;
    }

    private void bounceOrDie() {
        if (kind == Kind.GIANT) {
            alive = false;
        } else {
            direction = -direction;
        }
    }

    private void checkZombieCollision(Board board) {
        for (Zombie z : board.getZombies()) {
            if (z.isAlive() && z.getLane() == lane && Math.abs(z.getX() - x) < 0.5) {
                onHitZombie(z, board);
                return;
            }
        }
    }

    public void onHitZombie(Zombie zombie, Board board) {
        switch (kind) {
            case BOWLING -> hitAndBounce(zombie);
            case EXPLODE_O_NUT -> explode(board);
            case GIANT -> zombie.takeDamage(Integer.MAX_VALUE / 2, true);
            default -> { }
        }
    }

    private void hitAndBounce(Zombie zombie) {
        zombie.takeDamage(NORMAL_DAMAGE, false);
        direction = -direction;
    }

    private void explode(Board board) {
        board.dealAreaDamageToZombies((int) Math.round(x), lane, EXPLODE_RADIUS, EXPLODE_DAMAGE);
        alive = false;
    }

    public boolean isAlive() {
        return alive;
    }

    public double getX() {
        return x;
    }

    public int getLane() {
        return lane;
    }

    public Kind getKind() {
        return kind;
    }
}
