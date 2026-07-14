package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class ZombieProspectorBehavior implements ZombieBehavior {
    private enum FlightState { COUNTDOWN, FLYING, STUNNED, ACTIVE }

    private FlightState state = FlightState.COUNTDOWN;
    private int timerTicks;

    public ZombieProspectorBehavior(double countdownSeconds) {
        this.timerTicks = (int) (countdownSeconds * 20);
    }

    @Override
    public void onTick(Zombie self, BoardContext ctx, int currentTick) {
        if (timerTicks > 0) {
            timerTicks--;

            if (state == FlightState.COUNTDOWN || state == FlightState.STUNNED) {
                self.setX(self.getX() + (self.getSpeed() / 20.0));
            }
            return;
        }

        switch (state) {
            case COUNTDOWN -> {
                self.setX(150.0);
                state = FlightState.STUNNED;
                double stunTime = self.getDef().getPropAsDouble("StunTime", 2.5);
                timerTicks = (int) (stunTime * 20);
            }
            case STUNNED -> {
                state = FlightState.ACTIVE;
            }
            case ACTIVE -> {
                double movementThisTick = self.getSpeed() / 20.0;
                self.setX(self.getX() + (movementThisTick * 2.0));
            }
        }
    }
}
