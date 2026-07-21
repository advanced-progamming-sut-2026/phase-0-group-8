package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.Collections;
import java.util.List;

public final class ZombieDef {

    private final ZombieType type;
    private final String displayName;
    private final int maxHp;
    private final double speed;
    private final int damage;
    private final int waveCost;
    private final List<ArmorType> armorLayers;
    private final List<BehaviorType> behaviors;
    private final String chapter;

    public ZombieDef(ZombieType type, String displayName, int maxHp, double speed, int damage, int waveCost,
                     List<ArmorType> armorLayers, List<BehaviorType> behaviors, String chapter) {
        this.type = type;
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.speed = speed;
        this.damage = damage;
        this.waveCost = waveCost;
        this.armorLayers = Collections.unmodifiableList(armorLayers);
        this.behaviors = Collections.unmodifiableList(behaviors);
        this.chapter = chapter;
    }

    public ZombieType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDamage() {
        return damage;
    }

    public int getWaveCost() {
        return waveCost;
    }

    public boolean hasBehavior(BehaviorType behavior) {
        return behaviors.contains(behavior);
    }

    public List<ArmorType> getArmorLayers() {
        return armorLayers;
    }

    public List<BehaviorType> getBehaviors() {
        return behaviors;
    }

    public String getChapter() {
        return chapter;
    }
}
