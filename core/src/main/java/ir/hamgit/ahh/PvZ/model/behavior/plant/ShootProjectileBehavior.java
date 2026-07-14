package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class ShootProjectileBehavior implements PlantBehavior {
    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {
        int interval = self.getDef().getActionIntervalTicks();
        if (interval <= 0 || (currentTick - self.getLastActionTick()) < interval) {
            return;
        }

        Zombie target = ctx.getNearestZombieAhead(self.getRow(), self.getCol());
        if (target != null) {
            ctx.spawnProjectile(self.getRow(), (self.getCol() * 100.0) + 50.0, self.getDamage(), "NORMAL");
            self.setLastActionTick(currentTick);
        }
    }
}
