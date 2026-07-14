package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class DefaultZombieBehavior implements ZombieBehavior {
    @Override
    public void onTick(Zombie self, BoardContext ctx, int currentTick) {
        Plant blockingPlant = ctx.getPlantBlocking(self.getRow(), self.getX());

        if (blockingPlant != null) {
            if (self.getEatDps() > 0) {
                double damagePerTick = self.getEatDps() / 20.0;
                blockingPlant.takeDamage(damagePerTick);

                if (blockingPlant.isDead()) {
                    ctx.removePlant(blockingPlant);
                }
            }
        }
        else {
            double distanceMovedThisTick = self.getSpeed() / 20.0;
            self.setX(self.getX() - distanceMovedThisTick);
        }
    }
}
