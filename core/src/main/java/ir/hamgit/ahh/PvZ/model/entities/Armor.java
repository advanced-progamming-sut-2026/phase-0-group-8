package ir.hamgit.ahh.PvZ.model.entities;

import ir.hamgit.ahh.PvZ.model.def.ArmorDef;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;

public class Armor {
    private final ArmorDef def;
    private double currentHp;

    public Armor(ArmorDef def) {
        this.def = def;
        this.currentHp = def != null ? def.getBaseHealth() : 0.0;
    }

    public double takeDamage(double damage) {
        this.currentHp -= damage;

        if (this.currentHp <= 0.0) {
            double leftoverDamage = Math.abs(this.currentHp);
            this.currentHp = 0.0;
            return leftoverDamage;
        }

        return 0.0;
    }

    public boolean isBroken() {
        return this.currentHp <= 0.0;
    }

    public ArmorDef getDef() {
        return def;
    }

    public double getCurrentHp() {
        return currentHp;
    }

    public ArmorType getType() {
        return def != null ? def.getArmorType() : ArmorType.NONE;
    }

    public boolean hasFlag(String flag) {
        return def != null && def.hasFlag(flag);
    }
}
