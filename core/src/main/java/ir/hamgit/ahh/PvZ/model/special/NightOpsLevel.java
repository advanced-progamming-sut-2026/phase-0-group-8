package ir.hamgit.ahh.PvZ.model.special;

/** "شب عملیات" - no sun falls from the sky; only producer plants give sun. */
public class NightOpsLevel extends SpecialLevelHandler {

    @Override
    public boolean blocksNaturalSun() {
        return true;
    }
}
