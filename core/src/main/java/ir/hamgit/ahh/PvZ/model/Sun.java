package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.SunType;


/** A sun object, either produced by a plant or falling from the sky. */
public class Sun {

    private static final int FALL_TICKS = 50;
    private static final int NORMAL_VALUE = 25;
    private static final int SPECIAL_VALUE = 100;
    private static final int RADIOACTIVE_VALUE = 150;

    private SunType type;
    private final int x;
    private final int lane;
    private int ticksFalling;
    private boolean onGround;
    private boolean collected;
    private final boolean producedByPlant;
    private final int customValue;

    public Sun(SunType type, int x, int lane) {
        this(type, x, lane, false, -1);
    }

    public Sun(SunType type, int x, int lane, boolean producedByPlant) {
        this(type, x, lane, producedByPlant, -1);
    }

    public Sun(SunType type, int x, int lane, boolean producedByPlant, int customValue) {
        this.type = type;
        this.x = x;
        this.lane = lane;
        this.producedByPlant = producedByPlant;
        this.onGround = producedByPlant;
        this.customValue = customValue;
    }

    public void tick() {
        if (onGround || producedByPlant || collected) {
            return;
        }
        ticksFalling++;
        if (ticksFalling >= FALL_TICKS) {
            land();
        }
    }

    private void land() {
        onGround = true;
        if (type == SunType.RADIOACTIVE) {
            type = SunType.NORMAL;
        }
        System.out.printf("Sun reached the ground at position (%d, %d)%n", x, lane);
    }

    public int getValue() {
        if (customValue >= 0) {
            return customValue;
        }
        switch (type) {
            case SPECIAL:
                return SPECIAL_VALUE;
            case RADIOACTIVE:
                return RADIOACTIVE_VALUE;
            case NORMAL:
            default:
                return NORMAL_VALUE;
        }
    }

    public boolean isCollectable() {
        return !collected;
    }

    public void explodeIfRadioactive(Board board) {
        if (type == SunType.RADIOACTIVE) {
            board.explodeRadioactiveSun(x, lane);
        }
    }

    public SunType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getLane() {
        return lane;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isProducedByPlant() {
        return producedByPlant;
    }

    public boolean isCollected() {
        return collected;
    }

    public void markCollected() {
        collected = true;
    }
}
