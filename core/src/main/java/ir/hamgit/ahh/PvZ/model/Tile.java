package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

import ir.hamgit.ahh.PvZ.model.enums.TileType;

import java.util.ArrayList;
import java.util.List;


public class Tile {

    public static final int GRAVE_MAX_HP = 700;
    public static final int ICE_MAX_HP = 600;
    public static final int GRAVE_REWARD_NONE = 0;
    public static final int GRAVE_REWARD_SUN = 1;
    public static final int GRAVE_REWARD_PLANT_FOOD = 2;

    private TileType type;
    private Plant plant;
    private int gravestoneHp;
    private int iceHp;
    private boolean hasNecromancy;
    private int graveReward;

    public Tile(TileType type) {
        this.type = type;
        this.gravestoneHp = type == TileType.GRAVE ? GRAVE_MAX_HP : 0;
        this.iceHp = type == TileType.ICY_GROUND ? ICE_MAX_HP : 0;
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

    public boolean removePlant(Plant target) {
        if (plant == target) {
            removePlant();
            return true;
        }
        Plant current = plant;
        while (current != null && current.getUnderPlant() != target) {
            current = current.getUnderPlant();
        }
        if (current == null) {
            return false;
        }
        current.setUnderPlant(target.getUnderPlant());
        return true;
    }

    public Plant getPlant() {
        return plant;
    }

    public boolean isEmpty() {
        return plant == null;
    }

    public List<Plant> getPlantLayers() {
        List<Plant> layers = new ArrayList<>();
        Plant current = plant;
        while (current != null) {
            layers.add(current);
            current = current.getUnderPlant();
        }
        return layers;
    }

    public boolean isPlantable() {
        return type == TileType.NORMAL || type == TileType.NECROMANCY;
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

    public void hitIce(int damage) {
        if (type != TileType.ICY_GROUND) {
            return;
        }
        iceHp = Math.max(0, iceHp - Math.max(0, damage));
        if (iceHp == 0) {
            setType(TileType.NORMAL);
        }
    }

    public int getIceHp() {
        return iceHp;
    }

    public void makeGrave() {
        if (type == TileType.NORMAL || type == TileType.NECROMANCY) {
            this.gravestoneHp = GRAVE_MAX_HP;
            this.type = TileType.GRAVE;
        }
    }

    public void rollDarkAgesReward() {
        double roll = Math.random();
        graveReward = roll < 0.2 ? GRAVE_REWARD_SUN
            : roll < 0.3 ? GRAVE_REWARD_PLANT_FOOD : GRAVE_REWARD_NONE;
    }

    public int takeGraveReward() {
        int reward = graveReward;
        graveReward = GRAVE_REWARD_NONE;
        return reward;
    }

     
    public int getGraveRewardType() {
        return graveReward;
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
        gravestoneHp = type == TileType.GRAVE ? GRAVE_MAX_HP : 0;
        iceHp = type == TileType.ICY_GROUND ? ICE_MAX_HP : 0;
    }
}
