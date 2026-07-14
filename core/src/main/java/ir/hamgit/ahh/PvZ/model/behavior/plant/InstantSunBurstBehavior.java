package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

public class InstantSunBurstBehavior implements PlantBehavior {
    @Override
    public void onPlanted(Plant self, BoardContext ctx) {
        ctx.spawnSun(self.getRow(), self.getCol(), self.getDef().getAbilityValue(), "BURST");

        ctx.removePlant(self);
    }

    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {}
}
