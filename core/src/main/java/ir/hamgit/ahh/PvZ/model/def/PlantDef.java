package ir.hamgit.ahh.PvZ.model.def;

import com.fasterxml.jackson.annotation.*;
import ir.hamgit.ahh.PvZ.config.GameConfig;
import ir.hamgit.ahh.PvZ.model.enums.*;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlantDef {
    private final PlantType type;
    private final PlantCategory category;
    private final String displayName;
    private final int sunCost;
    private final double maxHp;
    private final int rechargeTicks;
    private final int actionIntervalTicks;
    private final double damage;
    private final double abilityValue;
    private final Set<Tag> tags;
    private final List<PlantBehaviorType> behaviors;
    private final int range;

    @JsonCreator
    public PlantDef(
        @JsonProperty("name") PlantType type,
        @JsonProperty("category") PlantCategory category,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("cost") int sunCost,
        @JsonProperty("baseHp") double maxHp,
        @JsonProperty("recharge") double rechargeSeconds,
        @JsonProperty("actionInterval") Double actionIntervalSeconds,
        @JsonProperty("damage") double damage,
        @JsonProperty("abilityValue") Double abilityValue,
        @JsonProperty("tags") Set<Tag> tags,
        @JsonProperty("abilityType") PlantBehaviorType behavior,
        @JsonProperty("range") Integer jsonRange
    ) {
        this.type = type;
        this.category = category;
        this.displayName = displayName != null ? displayName : (type != null ? type.name() : "Unknown");
        this.sunCost = sunCost;
        this.maxHp = maxHp;

        this.rechargeTicks = (int) (rechargeSeconds * GameConfig.TICKS_PER_SECOND);

        double actualInterval = actionIntervalSeconds != null ? actionIntervalSeconds : 0.0;
        this.actionIntervalTicks = (int) (actualInterval * GameConfig.TICKS_PER_SECOND);

        this.damage = damage;
        this.abilityValue = abilityValue != null ? abilityValue : 0.0;
        this.range = jsonRange != null ? jsonRange : 0;
        this.tags = tags != null ? tags : new HashSet<>();

        this.behaviors = new ArrayList<>();
        if (behavior != null) {
            this.behaviors.add(behavior);
        }
    }

    public PlantType getType() {
        return type;
    }

    public PlantCategory getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSunCost() {
        return sunCost;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public int getRechargeTicks() {
        return rechargeTicks;
    }

    public int getActionIntervalTicks() {
        return actionIntervalTicks;
    }

    public double getDamage() {
        return damage;
    }

    public double getAbilityValue() {
        return abilityValue;
    }

    public int getRange() {
        return range;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public List<PlantBehaviorType> getBehaviors() {
        return behaviors;
    }

    public boolean canPlantOnWater() {
        return tags.contains(Tag.WATER);
    }

    public boolean canStackOn() {
        return tags.contains(Tag.STACK);
    }
}
