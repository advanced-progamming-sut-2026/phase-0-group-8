package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.special.SpecialLevelHandler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ConveyorBeltLevel extends SpecialLevelHandler {

    private static final int DELIVERY_INTERVAL_TICKS = 120;
    private static final int MAX_QUEUE_SIZE = 6;

    private final List<PlantType> availablePlants;
    private final Deque<PlantType> queue = new ArrayDeque<>();
    private int ticksUntilNextDelivery;

    public ConveyorBeltLevel(List<PlantType> availablePlants) {
        this.availablePlants = availablePlants == null ? List.of()
            : availablePlants.stream().filter(java.util.Objects::nonNull).distinct().toList();
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

     
    public void consumeOffer(PlantType type) {
        queue.remove(type);
    }

     
    public boolean isOffered(PlantType type) {
        return queue.contains(type);
    }

    public List<PlantType> peekQueue() {
        return List.copyOf(queue);
    }
}
