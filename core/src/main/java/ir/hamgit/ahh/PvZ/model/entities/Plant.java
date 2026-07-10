package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;

public class Plant {
    private final PlantDef def = null;
    private Plant underPlant;

    public PlantDef getDef() { return def; }

    public void setUnderPlant(Plant underPlant) {
        this.underPlant = underPlant;
    }

    public Plant getUnderPlant() {
        return null;
    }
}
