package ir.hamgit.ahh.PvZ.controller;

import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.greenhouse.GreenhouseService;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;
import ir.hamgit.ahh.PvZ.view.GreenHouseMenu;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GreenHouseController {
    private static final Pattern COORDINATES = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)");
    private final GreenHouseMenu view;
    private final ShopController shopController;

    public GreenHouseController(ShopController shopController) {
        this.shopController = shopController;
        view = new GreenHouseMenu();
    }

    public void handle(String input) {
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            view.showError("Error: no user logged in.");
            return;
        }
        Map<String, String> flags = CommandParser.parse(input);
        String command = normalizeCommand(CommandParser.getCommand(flags));
        if (command.startsWith("shop")) {
            shopController.handle(input);
            return;
        }
        switch (command) {
            case "show greenhouse" -> view.showGreenhouse(user);
            case "plant pot", "plant pot at" -> apply(user, GreenhouseAction.PLANT,
                coordinates(input, flags));
            case "collect" -> apply(user, GreenhouseAction.COLLECT, coordinates(input, flags));
            case "grow" -> apply(user, GreenhouseAction.GROW, coordinates(input, flags));
            default -> view.showError("Error: unknown greenhouse command.");
        }
    }

    private void apply(User user, GreenhouseAction action, int[] position) {
        Result result;
        if (position == null) {
            result = new Result(false, "Error: invalid pot position.");
        } else {
            result = action.apply(user, position[0], position[1]);
        }
        if (result.isSuccessful()) {
            UserRepository.updateUser(user);
        }
        view.showResult(result);
    }

    private int[] coordinates(String input, Map<String, String> flags) {
        int x = CommandParser.getIntFlag(flags, "x", Integer.MIN_VALUE);
        int y = CommandParser.getIntFlag(flags, "y", Integer.MIN_VALUE);
        if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
            return new int[] {x, y};
        }
        Matcher matcher = COORDINATES.matcher(input);
        return matcher.find() ? new int[] {
            Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))} : null;
    }

    private String normalizeCommand(String command) {
        if (command.startsWith("plant pot at")) {
            return "plant pot at";
        }
        if (command.startsWith("collect")) {
            return "collect";
        }
        if (command.startsWith("grow")) {
            return "grow";
        }
        return command;
    }

    private enum GreenhouseAction {
        PLANT {
            @Override
            Result apply(User user, int x, int y) {
                return GreenhouseService.plant(user, x, y);
            }
        },
        COLLECT {
            @Override
            Result apply(User user, int x, int y) {
                return GreenhouseService.collect(user, x, y);
            }
        },
        GROW {
            @Override
            Result apply(User user, int x, int y) {
                return GreenhouseService.speedGrow(user, x, y);
            }
        };

        abstract Result apply(User user, int x, int y);
    }
}
