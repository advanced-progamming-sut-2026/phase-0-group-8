package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class ZombieIceAgeHunterBehavior implements ZombieBehavior {
    private int attackCooldownTicks = 0;

    @Override
    public void onTick(Zombie self, BoardContext ctx, int currentTick) {
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }

        double farRangeTiles = self.getDef().getPropAsDouble("FarAttackRange", 4.0);
        int snowballsCount = self.getDef().getPropAsInt("SnowballsPerBarrage", 3);

        double currentTileCol = self.getX() / 100.0;

        Plant targetPlant = ctx.getPlantBlocking(self.getRow(), self.getX() - (farRangeTiles * 100.0));

        if (targetPlant != null && attackCooldownTicks == 0) {
            for (int i = 0; i < snowballsCount; i++) {
                ctx.spawnProjectile(self.getRow(), self.getX(), 20.0, "SNOWBALL");
            }
            this.attackCooldownTicks = 60;

            self.setX(self.getX() + (self.getSpeed() / 20.0));
        }
    }
}
