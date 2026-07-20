package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable "recipe card" describing one kind of plant. Populated by
 * {@link PlantRegistry}.
 *
 * <p>Three fields ({@link #sunProductionAmount}, {@link #sunProductionIntervalTicks}
 * and {@link #aoeRadius}) are additions on top of the original class-reference
 * table. They exist so the generic, behavior-driven combat code in
 * {@code model.Plant} can handle every sun producer / explosive without
 * hard-coding logic per plant name. Fill them in from the real plants.csv
 * once it is wired in - see {@link PlantRegistry}.</p>
 */
public final class PlantDef {

    private final PlantType type;
    private final String displayName;
    private final int sunCost;
    private final int maxHp;
    private final int rechargeSeconds;
    private final int damage;
    private final int range;
    private final Set<Tag> tags;
    private final List<BehaviorType> behaviors;
    private final int seedPacketsToUpgrade;
    private final int coinsToUpgrade;
    private final boolean canStackOn;
    private final boolean canPlantOnWater;
    private final int sunProductionAmount;
    private final int sunProductionIntervalTicks;
    private final int aoeRadius;

    public PlantDef(PlantType type, String displayName, int sunCost, int maxHp, int rechargeSeconds,
                    int damage, int range, Set<Tag> tags, List<BehaviorType> behaviors,
                    int seedPacketsToUpgrade, int coinsToUpgrade, boolean canStackOn,
                    boolean canPlantOnWater, int sunProductionAmount, int sunProductionIntervalTicks,
                    int aoeRadius) {
        this.type = type;
        this.displayName = displayName;
        this.sunCost = sunCost;
        this.maxHp = maxHp;
        this.rechargeSeconds = rechargeSeconds;
        this.damage = damage;
        this.range = range;
        this.tags = tags.isEmpty() ? EnumSet.noneOf(Tag.class) : EnumSet.copyOf(tags);
        this.behaviors = Collections.unmodifiableList(behaviors);
        this.seedPacketsToUpgrade = seedPacketsToUpgrade;
        this.coinsToUpgrade = coinsToUpgrade;
        this.canStackOn = canStackOn;
        this.canPlantOnWater = canPlantOnWater;
        this.sunProductionAmount = sunProductionAmount;
        this.sunProductionIntervalTicks = sunProductionIntervalTicks;
        this.aoeRadius = aoeRadius;
    }

    public PlantType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSunCost() {
        return sunCost;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getRechargeSeconds() {
        return rechargeSeconds;
    }

    public int getDamage() {
        return damage;
    }

    public int getRange() {
        return range;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public List<BehaviorType> getBehaviors() {
        return behaviors;
    }

    public boolean isCanStackOn() {
        return canStackOn;
    }

    public boolean isCanPlantOnWater() {
        return canPlantOnWater;
    }

    public int getSunProductionAmount() {
        return sunProductionAmount;
    }

    public int getSunProductionIntervalTicks() {
        return sunProductionIntervalTicks;
    }

    public int getAoeRadius() {
        return aoeRadius;
    }

    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    public boolean hasBehavior(BehaviorType behavior) {
        return behaviors.contains(behavior);
    }

    /** Returns {@code [seedPackets, coins]} required to upgrade from {@code level} to {@code level + 1}. */
    public int[] getUpgradeCost(int level) {
        int step = level + 1;
        return new int[] {seedPacketsToUpgrade * step, coinsToUpgrade * step};
    }
}
