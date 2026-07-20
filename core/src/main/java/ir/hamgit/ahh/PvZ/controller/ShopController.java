package ir.hamgit.ahh.PvZ.controller;

import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;
import ir.hamgit.ahh.PvZ.model.shop.ShopService;
import ir.hamgit.ahh.PvZ.view.ShopMenu;

import java.util.Map;

public final class ShopController {
    private final ShopMenu view;

    public ShopController() {
        view = new ShopMenu();
    }

    public void handle(String input) {
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            view.showError("Error: no user logged in.");
            return;
        }
        if (ShopService.refreshDailyIfNeeded(user)) {
            UserRepository.updateUser(user);
        }
        Map<String, String> flags = CommandParser.parse(input);
        switch (CommandParser.getCommand(flags)) {
            case "shop list" -> view.showPermanentItems(ShopService.getPermanentItems());
            case "shop daily" -> view.showDailyItem(ShopService.getDailyItem(), user);
            case "shop buy" -> purchase(user, flags);
            case "enter shop" -> view.showWelcome(ShopService.getPermanentItems());
            default -> view.showError("Error: unknown shop command.");
        }
    }

    private void purchase(User user, Map<String, String> flags) {
        Result result = ShopService.purchase(user, CommandParser.getFlag(flags, "i"),
            CommandParser.getIntFlag(flags, "n", 1), CommandParser.getFlag(flags, "t"));
        if (result.isSuccessful()) {
            UserRepository.updateUser(user);
        }
        view.showResult(result);
    }
}
