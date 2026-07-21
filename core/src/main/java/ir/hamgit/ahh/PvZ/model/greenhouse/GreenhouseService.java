package ir.hamgit.ahh.PvZ.model.greenhouse;

import ir.hamgit.ahh.PvZ.model.GreenHousePot;
import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;

import java.util.List;

/** Applies greenhouse rules without command parsing, rendering, or persistence. */
public final class GreenhouseService {

    private GreenhouseService() {
    }

    public static Result plant(User user, int x, int y) {
        GreenHousePot pot = user.getGreenhousePot(x, y);
        Result validation = validatePlanting(pot);
        if (validation != null) {
            return validation;
        }
        List<PlantType> boostable = user.getUnlockedPlants().stream()
            .filter(GreenhouseService::isBoostablePlant).toList();
        if (!pot.plantRandom(boostable)) {
            return failure("Error: this pot cannot be planted right now.");
        }
        return success("Planted " + pot.getPlantType() + " in pot (" + x + "," + y + ").");
    }

    private static Result validatePlanting(GreenHousePot pot) {
        if (pot == null) {
            return failure("Error: invalid pot position.");
        }
        if (pot.isLocked()) {
            return failure("Error: this pot is locked. Unlock it in the shop for 2000 coins.");
        }
        if (!pot.isEmpty()) {
            return failure("Error: this pot is already occupied.");
        }
        return null;
    }

    public static Result collect(User user, int x, int y) {
        GreenHousePot pot = user.getGreenhousePot(x, y);
        if (pot == null) {
            return failure("Error: invalid pot position.");
        }
        if (pot.isEmpty()) {
            return failure("Error: nothing to collect from this pot.");
        }
        if (!pot.isReadyToCollect()) {
            return failure(String.format("Error: plant is not ready yet. %.1f hours remaining.",
                pot.hoursRemaining()));
        }
        GreenHousePot.Harvest harvest = pot.harvest();
        if (harvest.marigold()) {
            user.addCoins(500);
            return success("Collected Marigold! Gained 500 coins.");
        }
        user.setPlantBoost(harvest.plantType(), true);
        return success("Collected a plant boost! It will apply the next time you use that plant.");
    }

    public static Result speedGrow(User user, int x, int y) {
        GreenHousePot pot = user.getGreenhousePot(x, y);
        if (pot == null) {
            return failure("Error: invalid pot position.");
        }
        if (pot.isEmpty()) {
            return failure("Error: nothing is growing in this pot.");
        }
        if (pot.isReadyToCollect()) {
            return failure("Error: this plant is already ready to collect.");
        }
        int cost = pot.getAccelerationCost();
        if (user.getDiamonds() < cost) {
            return failure("Error: not enough diamonds to speed up growth. Required: " + cost + ".");
        }
        if (!pot.finishGrowth() || !user.spendDiamonds(cost)) {
            return failure("Error: growth could not be accelerated.");
        }
        return success("Growth sped up for " + cost + " diamond(s)! The plant is ready to collect.");
    }

    private static boolean isBoostablePlant(PlantType type) {
        if (type == PlantType.MARIGOLD || PlantRegistry.get(type) == null) {
            return false;
        }
        return !PlantRegistry.get(type).hasBehavior(BehaviorType.DISAPPEAR_AFTER_ACTION)
            && !PlantRegistry.get(type).hasBehavior(BehaviorType.REMOVE_GRAVE);
    }

    private static Result success(String message) {
        return new Result(true, message);
    }

    private static Result failure(String message) {
        return new Result(false, message);
    }
}
