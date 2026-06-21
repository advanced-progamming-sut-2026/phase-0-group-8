package ir.hamgit.ahh.PvZ.model.def;

import com.fasterxml.jackson.annotation.*;
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
    private final double damage;
    private final Set<Tag> tags;
    private final List<BehaviorType> behaviors;
    private final int range;


    @JsonCreator
    public PlantDef(
        @JsonProperty("category") PlantCategory category,
        @JsonProperty("name") String displayName,
        @JsonProperty("cost") int sunCost,
        @JsonProperty("baseHp") double maxHp,
        @JsonProperty("recharge") double rechargeSeconds,
        @JsonProperty("damage") double damage,
        @JsonProperty("tags") Set<Tag> tags,
        @JsonProperty("abilityType") BehaviorType behavior
    ) {
        this.category = category;
        this.displayName = displayName;

        PlantType resolvedType = null;
        if (displayName != null) {
            String enumKey = displayName.toUpperCase().trim()
                .replace(" ", "_")
                .replace("-", "_");
            resolvedType = PlantType.valueOf(enumKey);
        }

        this.type = resolvedType;
        this.sunCost = sunCost;
        this.maxHp = maxHp;

        this.rechargeTicks = (int) (rechargeSeconds * 20);

        this.damage = damage;
        this.tags = tags != null ? tags : new HashSet<>();

        this.behaviors = new ArrayList<>();
        if (behavior != null) {
            this.behaviors.add(behavior);
        }

        this.range = 0;
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

    public double getDamage() {
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

    public boolean canPlantOnWater() {
        return tags.contains(Tag.WATER);
    }

    public boolean canStackOn() {
        return tags.contains(Tag.STACK);
    }
}
