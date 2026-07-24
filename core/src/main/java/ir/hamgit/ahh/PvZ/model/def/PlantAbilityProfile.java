package ir.hamgit.ahh.PvZ.model.def;

import java.util.Arrays;

public final class PlantAbilityProfile {

    private final int actionIntervalTicks;
    private final int rangeTiles;
    private final int shotCount;
    private final int pierceCount;
    private final int splashRadius;
    private final int armDelayTicks;
    private final int lifespanTicks;
    private final int[] damageCycle;
    private final int[] intervalCycle;

    private PlantAbilityProfile(Builder builder) {
        actionIntervalTicks = builder.actionIntervalTicks;
        rangeTiles = builder.rangeTiles;
        shotCount = builder.shotCount;
        pierceCount = builder.pierceCount;
        splashRadius = builder.splashRadius;
        armDelayTicks = builder.armDelayTicks;
        lifespanTicks = builder.lifespanTicks;
        damageCycle = Arrays.copyOf(builder.damageCycle, builder.damageCycle.length);
        intervalCycle = Arrays.copyOf(builder.intervalCycle, builder.intervalCycle.length);
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getActionIntervalTicks() {
        return actionIntervalTicks;
    }

    public int getRangeTiles() {
        return rangeTiles;
    }

    public int getShotCount() {
        return shotCount;
    }

    public int getPierceCount() {
        return pierceCount;
    }

    public int getSplashRadius() {
        return splashRadius;
    }

    public int getArmDelayTicks() {
        return armDelayTicks;
    }

    public int getLifespanTicks() {
        return lifespanTicks;
    }

    public int getDamageAt(int index, int fallback) {
        return damageCycle.length == 0 ? fallback
            : damageCycle[Math.floorMod(index, damageCycle.length)];
    }

    public int getIntervalAt(int index) {
        if (intervalCycle.length == 0) {
            return actionIntervalTicks;
        }
        return intervalCycle[Math.floorMod(index, intervalCycle.length)];
    }

    public int getCycleLength() {
        return Math.max(damageCycle.length, intervalCycle.length);
    }

    public static final class Builder {

        private int actionIntervalTicks;
        private int rangeTiles = 9;
        private int shotCount = 1;
        private int pierceCount = 1;
        private int splashRadius;
        private int armDelayTicks;
        private int lifespanTicks;
        private int[] damageCycle = new int[0];
        private int[] intervalCycle = new int[0];

        private Builder() {
        }

        public Builder interval(int ticks) {
            actionIntervalTicks = ticks;
            return this;
        }

        public Builder range(int tiles) {
            rangeTiles = tiles;
            return this;
        }

        public Builder shots(int count) {
            shotCount = count;
            return this;
        }

        public Builder pierce(int count) {
            pierceCount = count;
            return this;
        }

        public Builder splash(int radius) {
            splashRadius = radius;
            return this;
        }

        public Builder armDelay(int ticks) {
            armDelayTicks = ticks;
            return this;
        }

        public Builder lifespan(int ticks) {
            lifespanTicks = ticks;
            return this;
        }

        public Builder damageCycle(int... values) {
            damageCycle = Arrays.copyOf(values, values.length);
            return this;
        }

        public Builder intervalCycle(int... values) {
            intervalCycle = Arrays.copyOf(values, values.length);
            return this;
        }

        public PlantAbilityProfile build() {
            return new PlantAbilityProfile(this);
        }
    }
}
