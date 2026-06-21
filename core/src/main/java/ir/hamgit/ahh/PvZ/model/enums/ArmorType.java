package ir.hamgit.ahh.PvZ.model.enums;

public enum ArmorType {
    NONE(0),
    CONE(370),
    BUCKET(110),
    HELMET(1600),
    SHOULDER(1600),
    BLOCK(2200),
    NEWSPAPER(23232), // hp = normal zombie hp
    BARREL(22323); // not given in doc

    private final int armorHp;

    ArmorType(int hp) {
        this.armorHp = hp;
    }


}
