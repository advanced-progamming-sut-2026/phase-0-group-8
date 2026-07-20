package ir.hamgit.ahh.PvZ.model.shop;

public final class ShopItem {

    public enum Kind {
        POT,
        PLANT_FOOD,
        RANDOM_SEED_PACKET,
        SELECTED_SEED_PACKET,
        CURRENCY_EXCHANGE,
        DAILY_OFFER
    }

    private final Kind kind;
    private final String id;
    private final String name;
    private final int costCoins;
    private final int costDiamonds;
    private final int quantity;
    private final boolean dailyItem;

    public ShopItem(Kind kind, String id, String name, int costCoins,
                    int costDiamonds, int quantity, boolean dailyItem) {
        if (kind == null || id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Shop items require a kind, id and name.");
        }
        if (costCoins < 0 || costDiamonds < 0 || quantity <= 0) {
            throw new IllegalArgumentException("Shop prices cannot be negative and quantity must be positive.");
        }
        if ((costCoins == 0) == (costDiamonds == 0)) {
            throw new IllegalArgumentException("A shop item must use exactly one currency.");
        }
        this.kind = kind;
        this.id = id;
        this.name = name;
        this.costCoins = costCoins;
        this.costDiamonds = costDiamonds;
        this.quantity = quantity;
        this.dailyItem = dailyItem;
    }

    public Kind getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public int getCostCoins() {
        return costCoins;
    }

    public int getCostDiamonds() {
        return costDiamonds;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isDailyItem() {
        return dailyItem;
    }

    public String getDescription() {
        String price = costCoins > 0 ? costCoins + " coins" : costDiamonds + " diamonds";
        return name + " - " + price + " (x" + quantity + ")";
    }
}
