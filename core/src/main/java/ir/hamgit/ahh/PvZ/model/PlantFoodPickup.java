package ir.hamgit.ahh.PvZ.model;

 






public final class PlantFoodPickup {
    private static final int DROP_TICKS = 8;

    private final int x;
    private final int lane;
    private int ageTicks;
    private boolean collected;

    public PlantFoodPickup(int x, int lane) {
        this.x = x;
        this.lane = lane;
    }

    void tick() {
        if (!collected) {
            ageTicks++;
        }
    }

    boolean collectAt(int column, int row) {
        if (collected || column != x || row != lane) {
            return false;
        }
        collected = true;
        return true;
    }

    public int getX() {
        return x;
    }

    public int getLane() {
        return lane;
    }

    public float getDropProgress() {
        return Math.min(1f, ageTicks / (float) DROP_TICKS);
    }

    public boolean isCollected() {
        return collected;
    }
}
