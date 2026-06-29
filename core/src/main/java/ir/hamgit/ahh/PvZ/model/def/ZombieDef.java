package ir.hamgit.ahh.PvZ.model.def;

import com.fasterxml.jackson.annotation.*;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZombieDef {
    private final ZombieType type;
    private final double hitpoints;
    private final double eatDps;
    private final double speed;
    private final int wavePointCost;
    private final int weight;
    private final List<String> armorProps;

    @JsonCreator
    public ZombieDef(
        @JsonProperty("aliases") List<ZombieType> aliases,
        @JsonProperty("objdata") Map<String, Object> objData
    ) {
        this.type = aliases.get(0);

        this.hitpoints = ((Number) objData.getOrDefault("Hitpoints", 0.0)).doubleValue();
        this.eatDps = ((Number) objData.getOrDefault("EatDPS", 0.0)).doubleValue();
        this.speed = ((Number) objData.getOrDefault("Speed", 0.0)).doubleValue();
        this.wavePointCost = ((Number) objData.getOrDefault("WavePointCost", 0)).intValue();
        this.weight = ((Number) objData.getOrDefault("Weight", 0)).intValue();
        this.armorProps = (List<String>) objData.get("ZombieArmorProps");
    }

    public ZombieType getType() {
        return type;
    }

    public double getHitpoints() {
        return hitpoints;
    }

    public double getEatDps() {
        return eatDps;
    }

    public double getSpeed() {
        return speed;
    }

    public int getWavePointCost() {
        return wavePointCost;
    }

    public int getWeight() {
        return weight;
    }

    public List<String> getArmorProps() {
        return armorProps;
    }
}
