package ir.hamgit.ahh.PvZ.model.special;

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
