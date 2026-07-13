package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.ArmorType;

/**
 * A single armor layer worn by a zombie. HP values for CONE (370), BUCKET
 * (1100), HELMET/SHOULDER (1600 each) and BLOCK (2200) come directly from
 * the spec. NEWSPAPER/BARREL/PIANO/ARCADE_MACHINE HP are not given exact
 * numbers in the spec (only their behaviour is described), so reasonable
 * placeholders are used - tune freely.
 */
public class Armor {

    private static final int CONE_HP = 370;
    private static final int BUCKET_HP = 1100;
    private static final int HELMET_HP = 1600;
    private static final int SHOULDER_HP = 1600;
    private static final int BLOCK_HP = 2200;
    private static final int NEWSPAPER_HP = 270;
    private static final int BARREL_HP = 1400;
    private static final int PIANO_HP = 500;
    private static final int ARCADE_MACHINE_HP = 1100;

    private final ArmorType type;
    private int currentHp;

    public Armor(ArmorType type) {
        this.type = type;
        this.currentHp = getMaxHp(type);
    }

    public void takeDamage(int amount) {
        currentHp = Math.max(0, currentHp - amount);
    }

    public boolean isDestroyed() {
        return currentHp <= 0;
    }

    public ArmorType getType() {
        return type;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public static int getMaxHp(ArmorType type) {
        switch (type) {
            case CONE:
                return CONE_HP;
            case BUCKET:
                return BUCKET_HP;
            case HELMET:
                return HELMET_HP;
            case SHOULDER:
                return SHOULDER_HP;
            case BLOCK:
                return BLOCK_HP;
            case NEWSPAPER:
                return NEWSPAPER_HP;
            case BARREL:
                return BARREL_HP;
            case PIANO:
                return PIANO_HP;
            case ARCADE_MACHINE:
                return ARCADE_MACHINE_HP;
            case NONE:
            default:
                return 0;
        }
    }
}
