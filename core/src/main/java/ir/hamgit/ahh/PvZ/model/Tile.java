package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

/**
 * A single cell of the board grid. Holds terrain type plus whatever state
 * (grave HP, slip direction, necromancy flag) that terrain needs.
 */
public class Tile {

    public static final int GRAVE_MAX_HP = 700;

    private TileType type;
    private Plant plant;
    private int gravestoneHp;
    private boolean hasNecromancy;

    public Tile(TileType type) {
        this.type = type;
        this.gravestoneHp = type == TileType.GRAVE ? GRAVE_MAX_HP : 0;
    }

    public boolean plantHere(Plant newPlant) {
        if (plant != null) {
            newPlant.setUnderPlant(plant);
        }
        plant = newPlant;
        return true;
    }

    public Plant removePlant() {
        Plant removed = plant;
        plant = removed != null ? removed.getUnderPlant() : null;
        return removed;
    }

    public Plant getPlant() {
        return plant;
    }

    public boolean isEmpty() {
        return plant == null;
    }

    /** Graves, water and slippery ground cannot be planted on directly. */
    public boolean isPlantable() {
        return type == TileType.NORMAL || type == TileType.ICY_GROUND || type == TileType.NECROMANCY;
    }

    public void hitGrave(int damage) {
        if (type != TileType.GRAVE) {
            return;
        }
        gravestoneHp = Math.max(0, gravestoneHp - damage);
        if (gravestoneHp == 0) {
            type = TileType.NORMAL;
        }
    }

    public int getGravestoneHp() {
        return gravestoneHp;
    }

    public void makeGrave() {
        if (type == TileType.NORMAL || type == TileType.NECROMANCY) {
            this.gravestoneHp = GRAVE_MAX_HP;
            this.type = TileType.GRAVE;
        }
    }

    public boolean hasNecromancy() {
        return hasNecromancy;
    }

    public void setHasNecromancy(boolean hasNecromancy) {
        this.hasNecromancy = hasNecromancy;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
    }
}
