package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

public class PlantDef {
    private PlantType type;

    public boolean canStackOn() {
        return false;
    }
    public boolean canPlantOnWater() {
        return false;
    }
    public PlantType getType() {
        return type;
    }

}
