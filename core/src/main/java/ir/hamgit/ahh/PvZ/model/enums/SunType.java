package ir.hamgit.ahh.PvZ.model.enums;

public enum SunType {
    NORMAL(0.8),
    SPECIAL(0.15),
    RADIOACTIVE(0.05);

    private final double dropChance;

    SunType(double dropChance) {
        this.dropChance = dropChance;
    }


}
