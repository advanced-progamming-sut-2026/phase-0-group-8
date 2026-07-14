package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.behavior.plant.PlantBehavior;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class DelayedExplosiveBehavior implements PlantBehavior {
    private boolean isArmed = false;
    private int plantingTick = -1;

    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {
        if (plantingTick == -1) {
            plantingTick = currentTick;
        }

        if (!isArmed) {
            int armingDuration = self.getDef().getActionIntervalTicks();
            if ((currentTick - plantingTick) >= armingDuration) {
                isArmed = true;
            }
            return;
        }

        Zombie victim = ctx.getAdjacentZombie(self.getRow(), self.getCol());
        if (victim != null) {
            int radius = self.getRange() > 0 ? self.getRange() : 0;
            ctx.dealAreaDamage(self.getRow(), self.getCol(), radius, self.getDamage());
            ctx.removePlant(self);
        }
    }
}
