package ir.hamgit.ahh.PvZ.model.def;

import com.fasterxml.jackson.annotation.*;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArmorDef {
    private final ArmorType armorType;
    private final double baseHealth;
    private final List<String> armorFlags;

    @JsonCreator
    public ArmorDef(
        @JsonProperty("ArmorType") ArmorType armorType,
        @JsonProperty("BaseHealth") double baseHealth,
        @JsonProperty("ArmorFlags") List<String> armorFlags
    ) {
        this.armorType = armorType != null ? armorType : ArmorType.NONE;
        this.baseHealth = baseHealth;
        this.armorFlags = armorFlags != null ? armorFlags : List.of();
    }

    public ArmorType getArmorType() {
        return armorType;
    }

    public double getBaseHealth() {
        return baseHealth;
    }

    public List<String> getArmorFlags() {
        return armorFlags;
    }

    public boolean hasFlag(String flag) {
        return armorFlags.contains(flag);
    }
}
