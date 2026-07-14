package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

public class SunProduceBehavior implements PlantBehavior {
    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {
        int interval = self.getDef().getActionIntervalTicks();
        if (interval <= 0 || (currentTick - self.getLastActionTick()) < interval) {
            return;
        }

        ctx.spawnSun(self.getRow(), self.getCol(), self.getDef().getAbilityValue(), "PRODUCED");
        self.setLastActionTick(currentTick);
    }
}
