package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.controller.services.TileController;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

public class Tile {
    private TileType type;
    private Plant plant;


    public void plantHere(Plant p) {
        if (plant == null){
            plant = p;
        }
        else {
            p.setUnderPlant(plant);
            plant = p;
        }

    }

    public TileType getType() {
        return type;
    }

    public Plant getPlant() {
        return plant;
    }
}
