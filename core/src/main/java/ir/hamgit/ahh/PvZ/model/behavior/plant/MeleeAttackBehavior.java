package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class MeleeAttackBehavior implements PlantBehavior {
    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {
        int interval = self.getDef().getActionIntervalTicks();
        if (interval <= 0 || (currentTick - self.getLastActionTick()) < interval) {
            return;
        }

        Zombie adjacentZombie = ctx.getAdjacentZombie(self.getRow(), self.getCol());
        if (adjacentZombie != null) {
            adjacentZombie.takeDamage(self.getDamage());
            if (adjacentZombie.isDead()) {
                ctx.removeZombie(adjacentZombie);
            }
            self.setLastActionTick(currentTick);
        }
    }
}
