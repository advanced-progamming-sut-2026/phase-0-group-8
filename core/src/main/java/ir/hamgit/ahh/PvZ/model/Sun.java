package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.SunType;

public class Sun {
    private SunType type;
    private final int targetX;
    private final int targetY;

    private int fallTicksRemaining;
    private final int maxFallTicks = 50;

    private boolean isCollected;

    public Sun(SunType type, int targetX, int targetY, boolean fallsFromSky) {
        this.type = type;
        this.targetX = targetX;
        this.targetY = targetY;
        this.isCollected = false;

        if (fallsFromSky) {
            this.fallTicksRemaining = maxFallTicks;
        } else {
            this.fallTicksRemaining = 0;
        }
    }

    /**
     * Called every tick by the Game Loop to simulate falling
     */
    public void tick() {
        if (fallTicksRemaining > 0) {
            fallTicksRemaining--;

            if (fallTicksRemaining == 0 && this.type == SunType.RADIOACTIVE) {
                this.type = SunType.NORMAL;
            }
        }
    }

    public boolean isFalling() {
        return fallTicksRemaining > 0;
    }

    public SunType getType() {
        return type;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public boolean isCollected() {
        return isCollected;
    }

    public void setCollected(boolean collected) {
        isCollected = collected;
    }

    /**
     * Determines the monetary value of the sun based on its current state
     */
    public int getSunValue() {
        switch (type) {
            case SPECIAL: return 100;
            case NORMAL: return 25;
            default: return 0;
        }
    }
}
