package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class GargantuarSmashBehavior implements ZombieBehavior {
    private boolean threwImp = false;
    private int smashCooldownTicks = 0;

    @Override
    public void onTick(Zombie self, BoardContext ctx, int currentTick) {
        if (smashCooldownTicks > 0) {
            smashCooldownTicks--;
            self.setX(self.getX() + (self.getSpeed() / 20.0));
        }

        Plant targetPlant = ctx.getPlantBlocking(self.getRow(), self.getX());
        if (targetPlant != null && smashCooldownTicks == 0) {
            double smashDamage = self.getDef().getPropAsDouble("SmashDamage", 1500.0);
            targetPlant.takeDamage(smashDamage);

            if (targetPlant.isDead()) {
                ctx.removePlant(targetPlant);
            }
            smashCooldownTicks = (int) (self.getDef().getPropAsDouble("SmashDuration", 2.0) * 20);
        }

        if (!threwImp && self.getCurrentHp() < (self.getDef().getHitpoints() * 0.5)) {
            threwImp = true;

            ctx.spawnProjectile(self.getRow(), self.getX(), 0.0, "LAUNCHED_IMP");
        }
    }
}
