package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import java.util.Random;

public class ZombieIceAgeDodoBehavior implements ZombieBehavior {
    private final Random rand = new Random();
    private double jumpChance = 0.0;
    private double lastKnownX = -1.0;

    @Override
    public void onTick(Zombie self, BoardContext ctx, int currentTick) {
        if (lastKnownX == -1.0) {
            lastKnownX = self.getX();
            return;
        }

        double distanceWalked = lastKnownX - self.getX();
        lastKnownX = self.getX();

        if (distanceWalked > 0) {
            double scale = self.getDef().getPropAsDouble("AddRandomChanceForJumpPerGridWalked", 0.1);
            jumpChance += (distanceWalked / 100.0) * scale;

            if (rand.nextDouble() < jumpChance) {
                self.setX(self.getX() - 150.0);

                jumpChance = self.getDef().getPropAsDouble("LandedResetRandomChanceForJump", 0.0);
            }
        }
    }
}
