package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

import ir.hamgit.ahh.PvZ.model.def.PlantLevelEffects;

final class PlantLevelRuntime {

    private PlantLevelRuntime() {
    }

    static int armDelay(Plant plant, int baseTicks) {
        return reducedTicks(plant, baseTicks, "Arm Time -");
    }

    static int lifespan(Plant plant, int baseTicks) {
        return baseTicks + seconds(plant, "Lifespan +") * Board.TICKS_PER_SECOND;
    }

    static int growthThreshold(Plant plant, int baseTicks) {
        return reducedTicks(plant, baseTicks, "Grow Time -");
    }

    static int range(Plant plant, int baseTiles) {
        return baseTiles + value(plant, "Range +");
    }

    static int attackInterval(Plant plant, int baseTicks) {
        int result = baseTicks;
        result -= seconds(plant, "Charge Time -") * Board.TICKS_PER_SECOND;
        result -= seconds(plant, "Regen -") * Board.TICKS_PER_SECOND;
        result -= seconds(plant, "Digest -") * Board.TICKS_PER_SECOND;
        int speedPercent = value(plant, "Atk Speed +");
        result = (int) Math.round(result * (100 - speedPercent) / 100.0);
        return Math.max(1, result);
    }

    static int productionInterval(Plant plant, int baseTicks) {
        return reducedTicks(plant, baseTicks, "Prod. Time -");
    }

    static int freezeTicks(Plant plant, int baseTicks) {
        int seconds = value(plant, "Freeze Time +") + value(plant, "Chill Time +");
        return baseTicks + seconds * Board.TICKS_PER_SECOND;
    }

    static int warmthRadius(Plant plant) {
        return 1 + value(plant, "Warmth Radius +");
    }

    static int targetCount(Plant plant, int baseCount) {
        return baseCount + value(plant, "Targets +");
    }

    static int pierceCount(Plant plant, int baseCount) {
        return baseCount + value(plant, "Pierce +");
    }

    static int bounceCount(Plant plant, int baseCount) {
        return baseCount + value(plant, "Bounces +");
    }

    static int poisonDamage(Plant plant, int baseDamage) {
        return baseDamage + value(plant, "Dmg/Tick +");
    }

    static int areaDamage(Plant plant, int baseDamage) {
        return baseDamage + value(plant, "AoE Dmg +");
    }

    static int mintDuration(Plant mint, int baseTicks) {
        return baseTicks + seconds(mint, "Duration +") * Board.TICKS_PER_SECOND;
    }

    static int crushTargets(Plant plant) {
        return PlantLevelEffects.has(plant.getDef().getType(), plant.getLevel(), "Can crush 2x") ? 2 : 1;
    }

    static int maxGrowthStage(Plant plant) {
        return 3 + value(plant, "Max Size +");
    }

    static boolean has(Plant plant, String effect) {
        return PlantLevelEffects.has(plant.getDef().getType(), plant.getLevel(), effect);
    }

    static int value(Plant plant, String prefix) {
        return PlantLevelEffects.sum(plant.getDef().getType(), plant.getLevel(), prefix);
    }

    private static int seconds(Plant plant, String prefix) {
        return value(plant, prefix);
    }

    private static int reducedTicks(Plant plant, int baseTicks, String prefix) {
        int reduction = seconds(plant, prefix) * Board.TICKS_PER_SECOND;
        return Math.max(0, baseTicks - reduction);
    }
}
