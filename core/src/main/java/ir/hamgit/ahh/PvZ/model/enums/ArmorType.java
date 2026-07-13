package ir.hamgit.ahh.PvZ.model.enums;

/** Armor layers a zombie can wear. HP caps live in {@code model.Armor}.
 *  ARCADE_MACHINE was added beyond the original reference table so the
 *  Arcade Zombie's pushed machine (same HP as a bucket, per spec) can be
 *  modeled the same way as the other "destroy the object first" zombies. */
public enum ArmorType {
    NONE, CONE, BUCKET, HELMET, SHOULDER, BLOCK, NEWSPAPER, BARREL, PIANO, ARCADE_MACHINE
}
