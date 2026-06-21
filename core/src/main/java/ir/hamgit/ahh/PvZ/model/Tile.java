package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.controller.services.TileController;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

public class Tile {
    private TileType type;
    private Plant plant;


    public boolean plantHere(Plant p) {
        if (this.plant != null && !p.getDef().canStackOn()){
            return false;
        }

        this.plant = p;
        return true;
    }

    public TileType getType() {
        return type;
    }

    public Plant getPlant() {
        return plant;
    }
}
