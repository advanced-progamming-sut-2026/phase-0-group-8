package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.special.SpecialLevelHandler;
import model.Board;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * "نوار کناری" - no plant selection; a conveyor belt delivers a random plant
 * (from the plants the player has already unlocked) every 12 seconds, the
 * first one immediately on level start. Deliveries queue up (FIFO) if the
 * player doesn't plant them right away, rather than being discarded, so
 * nothing the belt brings up is ever silently lost.
 */
public class ConveyorBeltLevel extends SpecialLevelHandler {

    private static final int DELIVERY_INTERVAL_TICKS = 120;
    private static final int MAX_QUEUE_SIZE = 6;

    private final List<PlantType> availablePlants;
    private final Deque<PlantType> queue = new ArrayDeque<>();
    private int ticksUntilNextDelivery;

    public ConveyorBeltLevel(List<PlantType> availablePlants) {
        this.availablePlants = availablePlants;
    }

    @Override
    public void onLevelStart(Board board) {
        deliverNextPlant();
    }

    @Override
    public void onTick(Board board) {
        ticksUntilNextDelivery--;
        if (ticksUntilNextDelivery <= 0) {
            deliverNextPlant();
        }
    }

    private void deliverNextPlant() {
        ticksUntilNextDelivery = DELIVERY_INTERVAL_TICKS;
        if (availablePlants.isEmpty() || queue.size() >= MAX_QUEUE_SIZE) {
            return;
        }
        PlantType next = availablePlants.get((int) (Math.random() * availablePlants.size()));
        queue.addLast(next);
        System.out.println("Conveyor belt delivered: " + next);
    }

    /** Removes and returns the plant just planted from the front of the queue. */
    public void consumeOffer(PlantType type) {
        queue.remove(type);
    }

    /** Whether {@code type} is one of the plants currently sitting on the belt, ready to plant. */
    public boolean isOffered(PlantType type) {
        return queue.contains(type);
    }

    public List<PlantType> peekQueue() {
        return List.copyOf(queue);
    }
}
