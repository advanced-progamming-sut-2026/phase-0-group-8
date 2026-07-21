package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

public final class ProjectileSpec {

    private final PlantType ownerType;
    private final int damage;
    private final int startX;
    private final int lane;
    private final int direction;
    private final int maxRange;
    private final int pierceCount;
    private final int splashRadius;
    private final int stunTicks;
    private final int chillTicks;
    private final int poisonDamage;
    private final boolean fire;
    private final boolean ice;
    private final boolean poison;
    private final boolean lobber;
    private final boolean homing;
    private final boolean ricochet;
    private final boolean ignoreArmor;
    private final boolean ignitable;

    private ProjectileSpec(Builder builder) {
        ownerType = builder.ownerType;
        damage = builder.damage;
        startX = builder.startX;
        lane = builder.lane;
        direction = builder.direction;
        maxRange = builder.maxRange;
        pierceCount = builder.pierceCount;
        splashRadius = builder.splashRadius;
        stunTicks = builder.stunTicks;
        chillTicks = builder.chillTicks;
        poisonDamage = builder.poisonDamage;
        fire = builder.fire;
        ice = builder.ice;
        poison = builder.poison;
        lobber = builder.lobber;
        homing = builder.homing;
        ricochet = builder.ricochet;
        ignoreArmor = builder.ignoreArmor;
        ignitable = builder.ignitable;
    }

    public static Builder builder(PlantType ownerType, int damage, int startX, int lane) {
        return new Builder(ownerType, damage, startX, lane);
    }

    public PlantType getOwnerType() {
        return ownerType;
    }

    public int getDamage() {
        return damage;
    }

    public int getStartX() {
        return startX;
    }

    public int getLane() {
        return lane;
    }

    public int getDirection() {
        return direction;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public int getPierceCount() {
        return pierceCount;
    }

    public int getSplashRadius() {
        return splashRadius;
    }

    public int getStunTicks() {
        return stunTicks;
    }

    public int getChillTicks() {
        return chillTicks;
    }

    public int getPoisonDamage() {
        return poisonDamage;
    }

    public boolean isFire() {
        return fire;
    }

    public boolean isIce() {
        return ice;
    }

    public boolean isPoison() {
        return poison;
    }

    public boolean isLobber() {
        return lobber;
    }

    public boolean isHoming() {
        return homing;
    }

    public boolean isRicochet() {
        return ricochet;
    }

    public boolean isIgnoreArmor() {
        return ignoreArmor;
    }

    public boolean isIgnitable() {
        return ignitable;
    }

    public static final class Builder {

        private final PlantType ownerType;
        private final int damage;
        private final int startX;
        private final int lane;
        private int direction = 1;
        private int maxRange = 9;
        private int pierceCount = 1;
        private int splashRadius;
        private int stunTicks;
        private int chillTicks = 30;
        private int poisonDamage = 1;
        private boolean fire;
        private boolean ice;
        private boolean poison;
        private boolean lobber;
        private boolean homing;
        private boolean ricochet;
        private boolean ignoreArmor;
        private boolean ignitable;

        private Builder(PlantType ownerType, int damage, int startX, int lane) {
            this.ownerType = ownerType;
            this.damage = damage;
            this.startX = startX;
            this.lane = lane;
        }

        public Builder direction(int value) {
            direction = value < 0 ? -1 : 1;
            return this;
        }

        public Builder range(int value) {
            maxRange = value;
            return this;
        }

        public Builder pierce(int value) {
            pierceCount = value;
            return this;
        }

        public Builder splash(int value) {
            splashRadius = value;
            return this;
        }

        public Builder stun(int value) {
            stunTicks = value;
            return this;
        }

        public Builder chillTicks(int value) {
            chillTicks = value;
            return this;
        }

        public Builder poisonDamage(int value) {
            poisonDamage = value;
            return this;
        }

        public Builder fire(boolean value) {
            fire = value;
            return this;
        }

        public Builder ice(boolean value) {
            ice = value;
            return this;
        }

        public Builder poison(boolean value) {
            poison = value;
            return this;
        }

        public Builder lobber(boolean value) {
            lobber = value;
            return this;
        }

        public Builder homing(boolean value) {
            homing = value;
            return this;
        }

        public Builder ricochet(boolean value) {
            ricochet = value;
            return this;
        }

        public Builder ignoreArmor(boolean value) {
            ignoreArmor = value;
            return this;
        }

        public Builder ignitable(boolean value) {
            ignitable = value;
            return this;
        }

        public ProjectileSpec build() {
            return new ProjectileSpec(this);
        }
    }
}
