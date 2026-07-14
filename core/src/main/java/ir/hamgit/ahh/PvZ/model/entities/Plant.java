package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.behavior.plant.PlantBehaviorFactory;
import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.behavior.plant.PlantBehavior;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.enums.PlantCategory;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.util.List;

public class Plant {
    private final PlantDef def;
    private final int row;
    private final int col;
    private double currentHp;
    private int lastActionTick;
    private final List<PlantBehavior> behaviors;

    public Plant(PlantDef def, int row, int col) {
        this.def = def;
        this.row = row;
        this.col = col;
        this.currentHp = def.getMaxHp();
        this.lastActionTick = 0;

        this.behaviors = PlantBehaviorFactory.createBehaviors(def.getBehaviors());
    }

    public void onPlaced(BoardContext ctx) {
        for (PlantBehavior behavior : behaviors) {
            behavior.onPlanted(this, ctx);
        }
    }

    public void step(BoardContext ctx, int currentTick) {
        for (PlantBehavior behavior : behaviors) {
            behavior.onTick(this, ctx, currentTick);
        }
    }

    public void takeDamage(double damage) {
        this.currentHp = Math.max(0.0, currentHp - damage);
    }

    public boolean isDead() {
        return this.currentHp <= 0.0;
    }

    public PlantDef getDef() {
        return def;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public double getCurrentHp() {
        return currentHp;
    }

    public int getLastActionTick() {
        return lastActionTick;
    }

    public void setLastActionTick(int lastActionTick) {
        this.lastActionTick = lastActionTick;
    }

    // GETTERS FROM PLANT DEF
    public PlantType getType() {
        return def.getType();
    }

    public PlantCategory getCategory() {
        return def.getCategory();
    }

    public int getSunCost() {
        return def.getSunCost();
    }

    public double getMaxHp() {
        return def.getMaxHp();
    }

    public int getRechargeTicks() {
        return def.getRechargeTicks();
    }

    public double getDamage() {
        return def.getDamage();
    }

    public int getRange() {
        return def.getRange();
    }
}
