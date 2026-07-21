package ir.hamgit.ahh.PvZ.model.shop;

import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShopService {

    private static final List<ShopItem> PERMANENT_ITEMS = List.of(
        item(ShopItem.Kind.POT, "pot", "Pot", 2000, 0, 1, false),
        item(ShopItem.Kind.PLANT_FOOD, "plant_food", "Plant Food", 0, 3, 1, false),
        item(ShopItem.Kind.RANDOM_SEED_PACKET, "random_seed_packet",
            "Random Seed Packet", 1000, 0, 5, false),
        item(ShopItem.Kind.SELECTED_SEED_PACKET, "selected_seed_packet",
            "Selected Seed Packet", 0, 5, 10, false),
        item(ShopItem.Kind.CURRENCY_EXCHANGE, "currency_exchange",
            "Currency Exchange", 0, 5, 500, false)
    );
    private static final ShopItem DAILY_ITEM = item(ShopItem.Kind.DAILY_OFFER,
        "daily_offer", "Daily Seed Packet Bundle (20% off)", 1600, 0, 10, true);

    private ShopService() {
    }

    private static ShopItem item(ShopItem.Kind kind, String id, String name,
                                 int coins, int diamonds, int quantity, boolean daily) {
        return new ShopItem(kind, id, name, coins, diamonds, quantity, daily);
    }

    public static List<ShopItem> getPermanentItems() {
        return PERMANENT_ITEMS;
    }

    public static ShopItem getDailyItem() {
        return DAILY_ITEM;
    }

    public static boolean refreshDailyIfNeeded(User user) {
        return refreshDailyIfNeeded(user, LocalDate.now());
    }

    static boolean refreshDailyIfNeeded(User user, LocalDate date) {
        String today = date.toString();
        if (today.equals(user.getDailyShopLastDate())) {
            return false;
        }
        user.setDailyShopLastDate(today);
        user.setDailyOfferPurchased(false);
        List<PlantType> unlocked = new ArrayList<>(user.getUnlockedPlants());
        PlantType offer = unlocked.isEmpty() ? PlantType.PEASHOOTER
            : unlocked.get((int) (Math.random() * unlocked.size()));
        user.setDailyOfferPlant(offer.name());
        return true;
    }

    public static Result purchase(User user, String itemId, int count, String plantName) {
        if (itemId == null) {
            return failure("Error: usage is shop buy -i <item_id> -n <count> [-t <plant_type>]");
        }
        if (count <= 0) {
            return failure("Error: count must be positive.");
        }
        ShopItem item = findItem(itemId);
        if (item == null) {
            return failure("Error: unknown item id.");
        }
        if (item.isDailyItem() && count != 1) {
            return failure("Error: the daily offer can only be purchased once.");
        }
        if (!safeCount(item, count)) {
            return failure("Error: purchase count is too large.");
        }
        return switch (item.getKind()) {
            case POT -> buyPots(user, item, count);
            case PLANT_FOOD -> buyPlantFood(user, item, count);
            case RANDOM_SEED_PACKET -> buyRandomSeeds(user, item, count);
            case SELECTED_SEED_PACKET -> buySelectedSeeds(user, item, count, plantName);
            case CURRENCY_EXCHANGE -> exchangeCurrency(user, item, count);
            case DAILY_OFFER -> buyDailyOffer(user, item);
        };
    }

    private static ShopItem findItem(String id) {
        if (DAILY_ITEM.getId().equals(id)) {
            return DAILY_ITEM;
        }
        return PERMANENT_ITEMS.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
    }

    private static Result buyPots(User user, ShopItem item, int count) {
        int quantity = item.getQuantity() * count;
        long lockedPots = user.getGreenhousePots().stream().filter(pot -> pot.isLocked()).count();
        if (quantity > lockedPots) {
            return failure("Error: that purchase would exceed the maximum of 20 pots.");
        }
        if (!spend(user, item, count)) {
            return insufficientFunds(item);
        }
        for (int i = 0; i < quantity; i++) {
            user.unlockNextPot();
        }
        return success("Purchased " + quantity + " greenhouse pot(s)! Total pots: " + user.getPotCount());
    }

    private static Result buyPlantFood(User user, ShopItem item, int count) {
        int quantity = item.getQuantity() * count;
        if (user.getStoredPlantFood() + quantity > 3) {
            return failure("Error: at most 3 Plant Foods can be stored.");
        }
        if (!spend(user, item, count)) {
            return insufficientFunds(item);
        }
        user.addStoredPlantFood(quantity);
        return success("Purchased " + quantity + " Plant Food. It will be available in the next level.");
    }

    private static Result buyRandomSeeds(User user, ShopItem item, int count) {
        if (user.getUnlockedPlants().isEmpty()) {
            return failure("Error: you have no unlocked plants yet.");
        }
        if (!spend(user, item, count)) {
            return insufficientFunds(item);
        }
        List<PlantType> unlocked = user.getUnlockedPlants();
        PlantType type = unlocked.get((int) (Math.random() * unlocked.size()));
        int quantity = item.getQuantity() * count;
        user.addSeedPackets(type, quantity);
        return success("Received " + quantity + " seed packets for " + type.name() + "!");
    }

    private static Result buySelectedSeeds(User user, ShopItem item, int count, String plantName) {
        PlantType type = parsePlant(plantName);
        if (plantName == null) {
            return failure("Error: usage requires -t <plant_type> for selected seed packet.");
        }
        if (type == null) {
            return failure("Error: unknown plant type.");
        }
        if (!user.hasPlant(type)) {
            return failure("Error: you have not unlocked this plant yet.");
        }
        if (!spend(user, item, count)) {
            return insufficientFunds(item);
        }
        int quantity = item.getQuantity() * count;
        user.addSeedPackets(type, quantity);
        return success("Received " + quantity + " seed packets for " + type.name() + "!");
    }

    private static Result exchangeCurrency(User user, ShopItem item, int count) {
        if (!spend(user, item, count)) {
            return insufficientFunds(item);
        }
        int quantity = item.getQuantity() * count;
        user.addCoins(quantity);
        return success("Exchanged " + (item.getCostDiamonds() * count)
            + " diamonds for " + quantity + " coins.");
    }

    private static Result buyDailyOffer(User user, ShopItem item) {
        if (user.isDailyOfferPurchased()) {
            return failure("Error: you have already purchased today's daily offer.");
        }
        PlantType type = parsePlant(user.getDailyOfferPlant());
        if (type == null) {
            return failure("Error: invalid daily item state.");
        }
        if (!spend(user, item, 1)) {
            return insufficientFunds(item);
        }
        user.addSeedPackets(type, item.getQuantity());
        user.setDailyOfferPurchased(true);
        return success("Purchased daily offer: " + item.getQuantity()
            + " seed packets for " + type.name() + " at 20% off!");
    }

    private static PlantType parsePlant(String name) {
        if (name == null) {
            return null;
        }
        try {
            return PlantType.valueOf(name.toUpperCase(Locale.ROOT)
                .replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean spend(User user, ShopItem item, int count) {
        int coins = item.getCostCoins() * count;
        int diamonds = item.getCostDiamonds() * count;
        if (user.getCoins() < coins || user.getDiamonds() < diamonds) {
            return false;
        }
        return user.spendCoins(coins) && user.spendDiamonds(diamonds);
    }

    private static boolean safeCount(ShopItem item, int count) {
        return (long) item.getQuantity() * count <= Integer.MAX_VALUE
            && (long) item.getCostCoins() * count <= Integer.MAX_VALUE
            && (long) item.getCostDiamonds() * count <= Integer.MAX_VALUE;
    }

    private static Result insufficientFunds(ShopItem item) {
        return failure(item.getCostCoins() > 0
            ? "Error: not enough coins." : "Error: not enough diamonds.");
    }

    private static Result success(String message) {
        return new Result(true, message);
    }

    private static Result failure(String message) {
        return new Result(false, message);
    }
}
