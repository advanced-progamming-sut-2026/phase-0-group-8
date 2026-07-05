package ir.hamgit.ahh.PvZ.model.enums;

/**
 * Defines the types of suns that can drop from the sky or be produced by plants.
 */
public enum SunType {
    NORMAL,      // 80% chance, gives 25 sun
    SPECIAL,     // 15% chance, gives 100 sun
    RADIOACTIVE  // 5% chance, explodes if clicked while falling, turns to NORMAL if it hits the ground
}
