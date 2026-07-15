package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import java.util.Map;

public class ShootProjectileBehavior implements PlantBehavior {
    @Override
    public void onTick(Plant self, BoardContext ctx, int currentTick) {
        PlantDef def = self.getDef();
        int interval = def.getActionIntervalTicks();
        if (interval <= 0 || (currentTick - self.getLastActionTick()) < interval) {
            return;
        }

        Zombie target = ctx.getNearestZombieAhead(self.getRow(), self.getCol());
        if (target == null) {
            return;
        }

        String mode = def.getFireMode();
        double originX = (self.getCol() * 100.0) + 50.0;
        double dmg = self.getDamage();

        switch (mode) {
            case "SEQUENTIAL_SAME_LANE" -> {
                int count = (int) def.getAbilityValue();
                for (int i = 0; i < count; i++) {
                    ctx.spawnProjectile(self.getRow(), originX + (i * 15.0), dmg, "NORMAL");
                }
            }
            case "SIMULTANEOUS_MULTI_LANE" -> {
                ctx.spawnProjectile(self.getRow(), originX, dmg, "NORMAL");
                if (self.getRow() > 0) {
                    ctx.spawnProjectile(self.getRow() - 1, originX, dmg, "NORMAL");
                }
                ctx.spawnProjectile(self.getRow() + 1, originX, dmg, "NORMAL");
            }
            case "SIMULTANEOUS_BIDIRECTIONAL" -> {
                Map<String, Integer> split = def.getDirectionSplit();
                int forwardCount = split.getOrDefault("forward", 1);
                int backwardCount = split.getOrDefault("backward", 2);

                for (int i = 0; i < forwardCount; i++) {
                    ctx.spawnProjectile(self.getRow(), originX, dmg, "NORMAL");
                }
                for (int i = 0; i < backwardCount; i++) {
                    ctx.spawnProjectile(self.getRow(), originX, dmg, "BACKWARD");
                }
            }
            case "SIMULTANEOUS_DIAGONAL" -> {
                ctx.spawnProjectile(self.getRow(), originX, dmg, "DIAGONAL_UP");
                ctx.spawnProjectile(self.getRow(), originX, dmg, "DIAGONAL_DOWN");
            }
            case "SIMULTANEOUS_STAR" -> {
                ctx.spawnProjectile(self.getRow(), originX, dmg, "STAR_UP");
                ctx.spawnProjectile(self.getRow(), originX, dmg, "STAR_DOWN");
                ctx.spawnProjectile(self.getRow(), originX, dmg, "NORMAL");
            }
            default -> {
                ctx.spawnProjectile(self.getRow(), originX, dmg, "NORMAL");
            }
        }

        self.setLastActionTick(currentTick);
    }
}
