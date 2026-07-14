package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.behavior.zombie.*;
import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.*;

public class Zombie {
    private final ZombieDef def;
    private final int row;
    private double x;
    private double currentHp;
    private int lastActionTick;
    private final List<String> loadedArmorRtids = new ArrayList<>();
    private final List<ZombieBehavior> behaviors = new ArrayList<>();

    public Zombie(ZombieDef def, int row, double startingX) {
        this.def = def;
        this.row = row;
        this.x = startingX;
        this.currentHp = def.getHitpoints();
        this.lastActionTick = 0;

        if (def.getArmorProps() != null) {
            this.loadedArmorRtids.addAll(def.getArmorProps());
        }

        this.behaviors.add(new DefaultZombieBehavior());
        this.behaviors.addAll(ZombieBehaviorFactory.createSpecialBehaviors(def.getObjClass()));
    }

    public void step(BoardContext ctx, int currentTick) {
        for (ZombieBehavior behavior : behaviors) {
            behavior.onTick(this, ctx, currentTick);
        }
    }

    public void takeDamage(double damage) {
        this.currentHp = Math.max(0.0, this.currentHp - damage);
    }

    public boolean isDead() {
        return this.currentHp <= 0.0;
    }

    public ZombieDef getDef() {
        return def;
    }

    public int getRow() {
        return row;
    }

    public double getX() {
        return x;
    }

    public double getCurrentHp() {
        return currentHp;
    }

    public int getLastActionTick() {
        return lastActionTick;
    }

    public List<String> getLoadedArmorRtids() {
        return loadedArmorRtids;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setLastActionTick(int lastActionTick) {
        this.lastActionTick = lastActionTick;
    }

    // GETTERS FROM ZOMBIE DEF
    public ZombieType getType() {
        return def.getType();
    }

    public double getSpeed() {
        return def.getSpeed();
    }

    public double getEatDps() {
        return def.getEatDps();
    }
}
