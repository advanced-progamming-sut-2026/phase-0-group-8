package ir.hamgit.ahh.PvZ.model.special;


import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;

/**
 * "هر چه رسد بکار" - fixed starting sun, no natural sun fall, no sun
 * producers selectable, unlimited free planting until the player issues
 * "start zombie waves".
 */
public class PlantWhatYouGetLevel extends SpecialLevelHandler {

    private static final int DEFAULT_INITIAL_SUN = 500;

    private boolean wavesStarted;

    @Override
    public boolean blocksNaturalSun() {
        return true;
    }

    @Override
    public boolean isSelectablePlant(PlantType type) {
        PlantDef def = PlantRegistry.get(type);
        return def == null || !def.hasBehavior(BehaviorType.PRODUCE_SUN);
    }

    @Override
    public boolean waitsForManualWaveStart() {
        return !wavesStarted;
    }

    @Override
    public int getInitialSun() {
        return DEFAULT_INITIAL_SUN;
    }

    public void startWaves() {
        wavesStarted = true;
        System.out.println("Zombie waves will now begin.");
    }

    public boolean isWavesStarted() {
        return wavesStarted;
    }
}
