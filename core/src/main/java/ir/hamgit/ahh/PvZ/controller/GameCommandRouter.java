package ir.hamgit.ahh.PvZ.controller;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.view.BoardView;

import java.util.Map;

final class GameCommandRouter {

    private final GameController controller;

    GameCommandRouter(GameController controller) {
        this.controller = controller;
    }

    void handle(String raw) {
        if (raw == null || raw.isBlank()) {
            System.out.println("Error: enter a game command.");
            return;
        }
        Board board = controller.getBoard();
        if (board == null) {
            System.out.println("No level is currently running.");
            return;
        }
        String command = raw.trim();
        Map<String, String> flags = CommandParser.parse(command);
        if (command.startsWith("advance time")) {
            advanceTime(flags);
        } else if (command.startsWith("plant plant")) {
            plant(flags);
        } else if (command.startsWith("pluck plant")) {
            applyAt(flags, board::pluckPlant, "Could not pluck a plant there.");
        } else if (command.startsWith("feed plant")) {
            applyAt(flags, board::feedPlant, "Could not feed a plant there.");
        } else if (command.startsWith("collect sun")) {
            collectSun(flags, board);
        } else if (command.startsWith("cheat")) {
            cheat(command, flags, board);
        } else {
            handleDisplayOrBare(command, flags, board);
        }
    }

    private void advanceTime(Map<String, String> flags) {
        String rawTicks = CommandParser.getFlag(flags, "t");
        Integer ticks = 1;
        if (rawTicks != null) {
            ticks = CommandParser.parseInteger(rawTicks);
        }
        if (ticks == null) {
            System.out.println("Error: ticks must be a whole number.");
            return;
        }
        if (!InputLimits.isSafeTickCount(ticks)) {
            System.out.println("Error: ticks must be between 1 and "
                + InputLimits.MAX_TICKS_PER_COMMAND + ".");
            return;
        }
        controller.advanceTime(ticks);
    }

    private void plant(Map<String, String> flags) {
        PlantType type = controller.parsePlantName(CommandParser.getFlag(flags, "t"));
        int[] position = coordinates(CommandParser.getFlag(flags, "l"));
        if (type == null || position == null || !controller.plantSelected(type, position[0], position[1])) {
            System.out.println("Could not plant the requested plant there.");
        }
    }

    private void applyAt(Map<String, String> flags, CoordinateAction action, String error) {
        int[] position = coordinates(CommandParser.getFlag(flags, "l"));
        if (position == null || !action.apply(position[0], position[1])) {
            System.out.println(error);
        }
    }

    private void collectSun(Map<String, String> flags, Board board) {
        int[] position = coordinates(CommandParser.getFlag(flags, "l"));
        boolean collected = position != null && (board.collectSun(position[0], position[1])
            || board.collectFallingSun(position[0], position[1]));
        if (!collected) {
            System.out.println("No sun to collect there.");
        }
        controller.flushCurrencyToUser();
    }

    private void cheat(String command, Map<String, String> flags, Board board) {
        if (command.contains("add-plant-food")) {
            board.cheatAddPlantFood();
        } else if (command.contains("remove-cooldown")) {
            controller.clearCooldowns();
        } else if (command.contains("spawn-zombie")) {
            spawnZombie(flags, board);
        } else if (command.contains("release") && command.contains("nuke")) {
            board.cheatReleaseNuke();
        } else if (command.contains("add")) {
            addResource(command, flags, board);
        } else {
            System.out.println("Error: unknown cheat command.");
        }
    }

    private void spawnZombie(Map<String, String> flags, Board board) {
        ZombieType type = controller.parseZombieName(CommandParser.getFlag(flags, "t"));
        int[] position = coordinates(CommandParser.getFlag(flags, "l"));
        if (type == null) {
            System.out.println("Error: unknown zombie type.");
            return;
        }
        if (position == null) {
            System.out.println("Error: coordinates are required as x,y.");
            return;
        }
        boolean outsideBoard = position[0] < 0 || position[0] > board.getColumns()
            || position[1] < 0 || position[1] >= board.getRows();
        if (outsideBoard) {
            System.out.println("Error: zombie coordinates are outside the board.");
            return;
        }
        board.cheatSpawnZombie(type, position[0], position[1]);
    }

    private void addResource(String command, Map<String, String> flags, Board board) {
        String rawAmount = CommandParser.getFlag(flags, "n");
        Integer amount = CommandParser.parseInteger(rawAmount);
        if (amount == null || amount < 0) {
            System.out.println("Error: amount must be a nonnegative whole number.");
            return;
        }
        if (command.contains("sun")) {
            board.cheatAddSuns(amount);
        } else if (command.contains("coin")) {
            controller.addPersistentCurrency(amount, false);
        } else if (command.contains("diamond")) {
            controller.addPersistentCurrency(amount, true);
        } else {
            System.out.println("Error: resource must be sun, coin, or diamond.");
        }
    }

    private void handleDisplayOrBare(String command, Map<String, String> flags, Board board) {
        if (command.startsWith("show map")) {
            BoardView.showMap(board);
            controller.showWalletStatus();
        } else if (command.startsWith("show plants status")) {
            BoardView.showPlantsStatus(board);
        } else if (command.startsWith("show tile status")) {
            showTile(flags, board);
        } else if (command.startsWith("show sun amount")) {
            BoardView.showSunAmount(board);
        } else if (command.startsWith("zombies info")) {
            BoardView.showZombiesInfo(board);
        } else if (command.equals("start zombie waves")) {
            board.startZombieWaves();
        } else if (command.equals("release the nuke")) {
            board.cheatReleaseNuke();
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    private void showTile(Map<String, String> flags, Board board) {
        int[] position = coordinates(CommandParser.getFlag(flags, "l"));
        if (position != null) {
            BoardView.showTileStatus(board, position[0], position[1]);
        } else {
            System.out.println("Error: coordinates are required as x,y.");
        }
    }

    private int[] coordinates(String raw) {
        return CommandParser.parseCoordinates(raw);
    }

    @FunctionalInterface
    private interface CoordinateAction {
        boolean apply(int x, int lane);
    }
}
