package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.behavior.zombie.*;
import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.*;

public class Zombie {
    private final ZombieDef def;
    private final int row;
    private double x;
    private double currentHp;
    private int lastActionTick;
    private final List<Armor> loadedArmors = new ArrayList<>();
    private final List<ZombieBehavior> behaviors = new ArrayList<>();
    private double speedModifier = 1.0;

    public Zombie(ZombieDef def, int row, double startingX) {
        this.def = def;
        this.row = row;
        this.x = startingX;
        this.currentHp = def.getHitpoints();
        this.lastActionTick = 0;

        if (def.getArmorProps() != null) {
            for (String armorPropName : def.getArmorProps()) {
                try {
                    ArmorType type = ArmorType.valueOf(armorPropName.toUpperCase());
                    ArmorDef armorDef = ArmorRegistry.getInstance().getDefinition(type);
                    if (armorDef != null) {
                        this.loadedArmors.add(new Armor(armorDef));
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Could not map armor property: " + armorPropName + " to an ArmorType enum.");
                }
            }
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
        double remainingDamage = damage;


        Iterator<Armor> iterator = loadedArmors.iterator();
        while (iterator.hasNext() && remainingDamage > 0) {
            Armor armor = iterator.next();
            remainingDamage = armor.takeDamage(remainingDamage);

            if (armor.isBroken()) {
                iterator.remove();
                onArmorBroken(armor);
            }
        }

        if (remainingDamage > 0) {
            this.currentHp = Math.max(0.0, this.currentHp - remainingDamage);
        }
    }


    private void onArmorBroken(Armor armor) {
        if (armor.getType() == ArmorType.NEWSPAPER) {
            this.speedModifier = 2.0;
        }
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

    public List<Armor> getLoadedArmors() {
        return loadedArmors;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setLastActionTick(int lastActionTick) {
        this.lastActionTick = lastActionTick;
    }

    // GETTERS DELEGATED TO ZOMBIE DEF
    public ZombieType getType() {
        return def.getType();
    }

    public double getSpeed() {
        return def.getSpeed() * speedModifier;
    }

    public double getEatDps() {
        return def.getEatDps();
    }
}
