package ir.hamgit.ahh.PvZ.view;

import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.shop.ShopItem;

import java.util.List;
public final class ShopMenu {

    public void showPermanentItems(List<ShopItem> items) {
        System.out.println("Permanent items:");
        for (ShopItem item : items) {
            System.out.println("  [" + item.getId() + "] " + item.getDescription());
        }
    }

    public void showDailyItem(ShopItem item, User user) {
        System.out.println("Today's daily offer:");
        System.out.println("  [" + item.getId() + "] " + item.getDescription()
            + " for " + user.getDailyOfferPlant()
            + (user.isDailyOfferPurchased() ? " (ALREADY PURCHASED)" : ""));
    }

    public void showWelcome(List<ShopItem> items) {
        System.out.println("Welcome to the shop!");
        showPermanentItems(items);
    }

    public void showResult(Result result) {
        System.out.println(result.getResult());
    }

    public void showError(String message) {
        System.out.println(message);
    }
}
