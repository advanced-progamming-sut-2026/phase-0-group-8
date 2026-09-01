package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;


public class TimedWarLevel extends SpecialLevelHandler {

    private final boolean sunVariant;
    private final int target;
    private int timeRemainingTicks;
    private int progress;

    public TimedWarLevel(boolean sunVariant, int target, int timeLimitTicks) {
        this.sunVariant = sunVariant;
        this.target = target;
        this.timeRemainingTicks = timeLimitTicks;
    }

    @Override
    public void onTick(Board board) {
        if (timeRemainingTicks > 0) {
            timeRemainingTicks--;
        }
    }

    @Override
    public void onZombieKilled(Board board, Zombie zombie) {
        if (!sunVariant) {
            progress++;
        }
    }

    public void registerSunProduced(int amount) {
        if (sunVariant) {
            progress += amount;
        }
    }

    @Override
    public boolean checkCustomWin(Board board) {
        return progress >= target;
    }

    @Override
    public boolean checkCustomLoss(Board board) {
        return timeRemainingTicks <= 0 && progress < target;
    }

    public boolean isSunVariant() {
        return sunVariant;
    }

    public int getTarget() {
        return target;
    }

    public int getTimeRemainingTicks() {
        return timeRemainingTicks;
    }

    public int getProgress() {
        return progress;
    }
}
