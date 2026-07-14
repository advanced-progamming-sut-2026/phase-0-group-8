package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

public class InstantExplosiveBehavior implements PlantBehavior {
    @Override
    public void onPlanted(Plant self, BoardContext ctx) {
        int radius = self.getRange() > 0 ? self.getRange() : 1;
        ctx.dealAreaDamage(self.getRow(), self.getCol(), radius, self.getDamage());
        ctx.removePlant(self);
    }

    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {}
}
