package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;

public class Plant {
    private final PlantDef def;
    private final int row;
    private final int col;
    private double currentHp;
    private int lastActionTick;

    public Plant(PlantDef def, int row, int col) {
        this.def = def;
        this.row = row;
        this.col = col;
        this.currentHp = def.getMaxHp();
        this.lastActionTick = 0;
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
}
