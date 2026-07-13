package ir.hamgit.ahh.PvZ.model.special;

/**
 * Shared handler for every class in the {@code minigame} package: they all
 * spawn zombies/plants themselves via Board's public API on their own
 * schedule (vase-breaks, conveyor deliveries, direct zombie placement, ...)
 * rather than Board's normal wave system, but still want to reuse its
 * per-tick combat loop (movement, attacks, armor, projectiles, lawn mowers).
 * So: no sky suns, no automatic wave-spawning.
 */
public class MinigameLevelHandler extends SpecialLevelHandler {

    @Override
    public boolean blocksNaturalSun() {
        return true;
    }

    @Override
    public boolean blocksWaveSpawning() {
        return true;
    }
}
