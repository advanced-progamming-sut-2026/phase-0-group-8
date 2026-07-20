package ir.hamgit.ahh.PvZ.model;

public class ShopItem {
    private final String id;
    private final String name;
    private final int costCoins;
    private final int costDiamonds;
    private final int quantity;
    private final boolean isDailyItem;

    public ShopItem(String id, String name, int costCoins, int costDiamonds,
                    int quantity, boolean isDailyItem) {
        this.id = id;
        this.name = name;
        this.costCoins = costCoins;
        this.costDiamonds = costDiamonds;
        this.quantity = quantity;
        this.isDailyItem = isDailyItem;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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
        return isDailyItem;
    }

    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" - ");
        if (costCoins > 0) {
            sb.append(costCoins).append(" coins");
        }
        if (costDiamonds > 0) {
            if (costCoins > 0) {
                sb.append(" or ");
            }
            sb.append(costDiamonds).append(" diamonds");
        }
        sb.append(" (x").append(quantity).append(")");
        return sb.toString();
    }
}
