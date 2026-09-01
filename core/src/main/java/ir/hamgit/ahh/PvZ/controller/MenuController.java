package ir.hamgit.ahh.PvZ.controller;


import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.MenuState;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;
import ir.hamgit.ahh.PvZ.view.*;
import java.util.Map;
import java.util.Scanner;

public class MenuController {
    private MenuState currentState;
    private final Scanner scanner;
    private final GameController gameController;
    private final GameMenu gameMenu;
    private final RegisterMenu registerMenu;
    private final LoginMenu loginMenu;
    private final ProfileMenu profileMenu;
    private final CollectionMenu collectionMenu;
    private final ShopController shopController;
    private final GreenHouseController greenHouseController;
    private final NewsMenu newsMenu;
    private final SettingsMenu settingsMenu;
    private final MinigameController minigameController;
    private final TravelLogMenu travelLogMenu;
    private final LeaderboardMenu leaderboardMenu;
    private boolean running;

    public MenuController() {
        this.scanner = new Scanner(System.in);
        this.gameController = new GameController(null);
        this.gameMenu = new GameMenu(gameController);
        this.registerMenu = new RegisterMenu();
        this.loginMenu = new LoginMenu();
        this.profileMenu = new ProfileMenu();
        this.collectionMenu = new CollectionMenu();
        this.shopController = new ShopController();
        this.greenHouseController = new GreenHouseController(shopController);
        this.newsMenu = new NewsMenu();
        this.settingsMenu = new SettingsMenu();
        this.minigameController = new MinigameController();
        this.travelLogMenu = new TravelLogMenu(minigameController, gameController);
        this.leaderboardMenu = new LeaderboardMenu();
        this.running = true;
        User autoLogin = UserRepository.getAutoLoginUser();
        this.currentState = autoLogin != null ? MenuState.MAIN : MenuState.REGISTER;
    }

    public void run() {
        System.out.println("Welcome to Plants vs Zombies 2!");
        while (running) {
            System.out.print("> ");
            System.out.flush();
            if (!scanner.hasNextLine()) {
                break;
            }
            String input = scanner.nextLine();
            if (input.length() > InputLimits.MAX_COMMAND_LENGTH) {
                System.out.println("Error: command is too long.");
                continue;
            }
            handleCommandSafely(input);
        }
    }

    private void handleCommandSafely(String input) {
        try {
            handleCommand(input);
        } catch (RuntimeException exception) {
            System.out.println("Error: the command could not be completed. Check its name, values, and coordinates.");
        }
    }

    private void handleCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        syncGameUser();
        if (handleGlobalCommand(flags, command)) {
            return;
        }
        dispatchToCurrentMenu(input);
    }

    private boolean handleGlobalCommand(Map<String, String> flags, String command) {
        if (command.startsWith("menu enter ") && !command.equals("menu enter chapter")) {
            enterMenu(command.substring("menu enter ".length()).trim());
            return true;
        }
        switch (command) {
            case "menu enter" -> enterMenu(CommandParser.getFlag(flags, "m"));
            case "menu exit" -> exitMenu();
            case "menu show current" -> showCurrentMenu();
            case "menu logout" -> logout();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void dispatchToCurrentMenu(String input) {
        if (currentState == MenuState.REGISTER || currentState == MenuState.LOGIN
            || currentState == MenuState.PROFILE || currentState == MenuState.COLLECTION
            || currentState == MenuState.SHOP || currentState == MenuState.GREENHOUSE
            || currentState == MenuState.NEWS || currentState == MenuState.SETTINGS) {
            dispatchAccountMenu(input);
            return;
        }
        dispatchGameMenu(input);
    }

    private void dispatchAccountMenu(String input) {
        switch (currentState) {
            case REGISTER:
                currentState = registerMenu.handle(input);
                break;
            case LOGIN:
                currentState = loginMenu.handle(input);
                break;
            case PROFILE:
                profileMenu.handle(input);
                break;
            case COLLECTION:
                collectionMenu.handle(input);
                break;
            case SHOP:
                shopController.handle(input);
                break;
            case GREENHOUSE:
                if (input.trim().equalsIgnoreCase("enter shop")) {
                    currentState = MenuState.SHOP;
                    shopController.handle(input);
                } else {
                    greenHouseController.handle(input);
                }
                break;
            case NEWS:
                newsMenu.handle(input);
                break;
            case SETTINGS:
                settingsMenu.handle(input);
                break;
            default:
                System.out.println("Error: unsupported account menu.");
        }
    }

    private void dispatchGameMenu(String input) {
        switch (currentState) {
            case IN_GAME:
                gameController.handleCommand(input);
                if (gameController.finishIfOver()) {
                    currentState = MenuState.GAME;
                }
                break;
            case PLANT_SELECT:
                handlePlantSelection(input);
                break;
            case TRAVEL_LOG:
                currentState = travelLogMenu.handle(input);
                break;
            case LEADERBOARD:
                leaderboardMenu.handle(input);
                break;
            case MINIGAME:
                if (minigameController.handle(input)) {
                    currentState = MenuState.TRAVEL_LOG;
                }
                break;
            case GAME:
                currentState = gameMenu.handle(input);
                break;
            case MAIN:
            default:
                System.out.println("Error: unknown or unsupported command in this menu.");
        }
    }

    private void handlePlantSelection(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        switch (command) {
            case "show all plants":
                gameController.showAllPlants();
                break;
            case "show available plants":
                gameController.showAvailablePlants();
                break;
            case "add plant":
                gameController.selectPlant(gameController.parsePlantName(CommandParser.getFlag(flags, "t")));
                break;
            case "remove plant":
                gameController.removePlantSelection(
                    gameController.parsePlantName(CommandParser.getFlag(flags, "t")));
                break;
            case "boost plant":
                gameController.boostPlant(gameController.parsePlantName(CommandParser.getFlag(flags, "t")));
                break;
            case "start game":
                startSelectedGame();
                break;
            default:
                System.out.println("Error: unknown plant-selection command.");
        }
    }

    private void startSelectedGame() {
        if (!gameController.canStartSelectedGame()) {
            System.out.println("Error: select at least one plant before starting.");
            return;
        }
        gameController.startActualGame(gameController.getDefaultTotalWaves());
        currentState = MenuState.IN_GAME;
        System.out.println("The level has started.");
    }

    private void syncGameUser() {
        gameController.setCurrentUser(UserRepository.getCurrentUser());
    }

    private void enterMenu(String menuName) {
        if (menuName == null) {
            System.out.println("Error: usage is menu enter -m <menu_name>");
            return;
        }
        User current = UserRepository.getCurrentUser();
        String normalizedName = menuName.toLowerCase();
        switch (normalizedName) {
            case "login":
                if (currentState == MenuState.REGISTER) {
                    currentState = MenuState.LOGIN;
                    System.out.println("Entered login menu.");
                } else {
                    System.out.println("Error: cannot enter login menu from here.");
                }
                break;
            case "main":
                if (current != null) {
                    currentState = MenuState.MAIN;
                    System.out.println("Entered main menu.");
                } else {
                    System.out.println("Error: you must be logged in.");
                }
                break;
            default:
                enterAuthenticatedMenu(normalizedName, current);
        }
    }

    private void enterAuthenticatedMenu(String menuName, User current) {
        switch (menuName) {
            case "game" -> requireLogin(current, MenuState.GAME, "game menu");
            case "settings" -> requireLogin(current, MenuState.SETTINGS, "settings menu");
            case "news" -> requireLogin(current, MenuState.NEWS, "news menu");
            case "profile" -> requireLogin(current, MenuState.PROFILE, "profile menu");
            case "collection" -> requireLogin(current, MenuState.COLLECTION, "collection menu");
            case "shop" -> requireLogin(current, MenuState.SHOP, "shop menu");
            case "greenhouse" -> requireLogin(current, MenuState.GREENHOUSE, "greenhouse menu");
            case "travel-log" -> requireLogin(current, MenuState.TRAVEL_LOG, "travel log");
            case "leaderboard" -> requireLogin(current, MenuState.LEADERBOARD, "leaderboard");
            default -> System.out.println("Error: unknown menu name.");
        }
    }

    private void requireLogin(User current, MenuState target, String label) {
        if (current == null) {
            System.out.println("Error: you must be logged in to enter the " + label + ".");
            return;
        }
        currentState = target;
        System.out.println("Entered " + label + ".");
    }

    private void exitMenu() {
        switch (currentState) {
            case REGISTER:
                running = false;
                System.out.println("Goodbye!");
                break;
            case LOGIN:
                currentState = MenuState.REGISTER;
                System.out.println("Returned to register menu.");
                break;
            case GAME:
            case SETTINGS:
            case NEWS:
            case PROFILE:
                currentState = MenuState.MAIN;
                System.out.println("Returned to main menu.");
                break;
            case COLLECTION:
            case GREENHOUSE:
            case TRAVEL_LOG:
            case LEADERBOARD:
                currentState = MenuState.GAME;
                System.out.println("Returned to game menu.");
                break;
            case PLANT_SELECT:
                currentState = MenuState.GAME;
                System.out.println("Returned to game menu.");
                break;
            case IN_GAME:
                gameController.abandonGame();
                currentState = MenuState.GAME;
                System.out.println("Level abandoned; returned to game menu.");
                break;
            case MINIGAME:
                currentState = MenuState.TRAVEL_LOG;
                System.out.println("Returned to travel log.");
                break;
            case SHOP:
                currentState = MenuState.GREENHOUSE;
                System.out.println("Returned to greenhouse menu.");
                break;
            case MAIN:
                System.out.println("Error: use 'menu logout' to leave the main menu.");
                break;
            default:
                System.out.println("Error: cannot exit from this menu.");
        }
    }

    private void showCurrentMenu() {
        System.out.println("Current menu: " + currentState.name().toLowerCase());
    }

    private void logout() {
        if (currentState != MenuState.MAIN) {
            System.out.println("Error: logout is only available from the main menu.");
            return;
        }
        UserRepository.logout();
        gameController.setCurrentUser(null);
        currentState = MenuState.REGISTER;
        System.out.println("Logged out successfully.");
    }
}
