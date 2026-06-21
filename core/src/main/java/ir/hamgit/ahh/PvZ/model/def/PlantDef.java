package ir.hamgit.ahh.PvZ.model.def;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

public class PlantDef {
    private PlantType type;
    private int sunCost;

    public boolean canStackOn() {
        return false;
    }
    public boolean canPlantOnWater() {
        return false;
    }
    public PlantType getType() {
        return type;
    }

    public int getSunCost() { return sunCost; }

}
