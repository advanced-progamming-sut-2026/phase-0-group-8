package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class ZombieCrystalSkullBehavior implements ZombieBehavior {
    private enum State { CHARGING, FIRING, COOLDOWN }

    private State currentState = State.CHARGING;
    private int stateTimerTicks = 0;

    @Override
    public void onTick(Zombie self, BoardContext ctx, int currentTick) {
        if (stateTimerTicks > 0) {
            stateTimerTicks--;
            if (currentState == State.CHARGING || currentState == State.COOLDOWN) {
                self.setX(self.getX() + (self.getSpeed() / 20.0));
            }
            return;
        }

        switch (currentState) {
            case CHARGING -> {
                double laserDamage = self.getDef().getPropAsDouble("LaserBeamDamage", 4001.0);
                double laserLength = self.getDef().getPropAsDouble("LaserBeamLength", 220.0);

                int tileRadius = (int) (laserLength / 100.0);
                ctx.dealAreaDamage(self.getRow(), (int) (self.getX() / 100.0), tileRadius, laserDamage);

                currentState = State.FIRING;
                stateTimerTicks = 20;
            }
            case FIRING -> {
                double cooldownSec = self.getDef().getPropAsDouble("LaserCooldownTime", 5.0);
                currentState = State.COOLDOWN;
                stateTimerTicks = (int) (cooldownSec * 20);
            }
            case COOLDOWN -> {
                double chargeSec = self.getDef().getPropAsDouble("ChargingTime", 5.0);
                currentState = State.CHARGING;
                stateTimerTicks = (int) (chargeSec * 20);
            }
        }
    }
}
