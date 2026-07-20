package ir.hamgit.ahh.PvZ.controller;

import ir.hamgit.ahh.PvZ.model.minigame.BeghouledGame;
import ir.hamgit.ahh.PvZ.model.minigame.IZombieGame;
import ir.hamgit.ahh.PvZ.model.minigame.MinigameSession;
import ir.hamgit.ahh.PvZ.model.minigame.VasebreakerGame;
import ir.hamgit.ahh.PvZ.model.minigame.WallnutBowlingGame;
import ir.hamgit.ahh.PvZ.model.minigame.ZombotanyGame;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.view.BoardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Owns command parsing for every minigame model. */
final class MinigameCommandRouter {

    private static final Pattern COORDINATES = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)");
    private static final Pattern COORD_PAIR = Pattern.compile("(-?\\d+)\\D+(-?\\d+)");

    void handle(MinigameSession session, String raw) {
        if (session instanceof VasebreakerGame game) {
            handleVasebreaker(game, raw);
        } else if (session instanceof WallnutBowlingGame game) {
            handleBowling(game, raw);
        } else if (session instanceof IZombieGame game) {
            handleIZombie(game, raw);
        } else if (session instanceof BeghouledGame game) {
            handleBeghouled(game, raw);
        } else if (session instanceof ZombotanyGame game) {
            handleZombotany(game, raw);
        }
    }

    private void handleVasebreaker(VasebreakerGame game, String raw) {
        String command = raw.trim();
        if (command.startsWith("break vase")) {
            applyCoordinates(command, game::breakVase);
        } else if (command.startsWith("collect seed packet")) {
            applyCoordinates(command, game::collectSeedPacket);
        } else if (command.startsWith("plant plant")) {
            plantVaseSeed(game, command);
        } else if (command.startsWith("advance time")) {
            game.tick(ticks(command));
        } else if (command.startsWith("show map")) {
            BoardView.showMap(game.getBoard());
        } else {
            System.out.println("Unknown Vasebreaker command: " + raw);
        }
    }

    private void plantVaseSeed(VasebreakerGame game, String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        int[] position = coordinates(flags.get("-l"));
        PlantType type = plantType(flags.get("-t"), false);
        if (position != null && type != null) {
            game.plantHeldSeed(type, position[0], position[1]);
        }
    }

    private void handleBowling(WallnutBowlingGame game, String raw) {
        String command = raw.trim();
        if (command.startsWith("plant ball")) {
            applyCoordinates(command, game::plantBall);
        } else if (command.startsWith("advance time")) {
            game.tick(ticks(command));
        } else if (command.startsWith("show map")) {
            BoardView.showMap(game.getBoard());
        } else {
            System.out.println("Unknown Wallnut Bowling command: " + raw);
        }
    }

    private void handleIZombie(IZombieGame game, String raw) {
        String command = raw.trim();
        if (command.startsWith("place zombie")) {
            placeZombie(game, command);
        } else if (command.startsWith("advance time")) {
            game.tick(ticks(command));
        } else if (command.startsWith("show map")) {
            BoardView.showMap(game.getBoard());
        } else {
            System.out.println("Unknown I-Zombie command: " + raw);
        }
    }

    private void placeZombie(IZombieGame game, String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        ZombieType type = zombieType(flags.get("-t"));
        int[] position = coordinates(raw);
        if (type != null && position != null) {
            game.placeZombie(type, position[0], position[1]);
        }
    }

    private void handleBeghouled(BeghouledGame game, String raw) {
        String command = raw.trim();
        if (command.startsWith("swap")) {
            swap(game, command);
        } else if (command.startsWith("upgrade")) {
            upgrade(game, command);
        } else if (command.startsWith("advance time")) {
            game.tick(ticks(command));
        } else if (command.startsWith("show map")) {
            BoardView.showMap(game.getBoard());
        } else {
            System.out.println("Unknown Beghouled command: " + raw);
        }
    }

    private void swap(BeghouledGame game, String raw) {
        List<Integer> values = new ArrayList<>();
        Matcher matcher = COORD_PAIR.matcher(raw);
        while (matcher.find() && values.size() < 4) {
            values.add(Integer.parseInt(matcher.group(1)));
            values.add(Integer.parseInt(matcher.group(2)));
        }
        if (values.size() == 4) {
            game.swapPlants(values.get(0), values.get(1), values.get(2), values.get(3));
        }
    }

    private void upgrade(BeghouledGame game, String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        PlantType from = plantType(flags.get("-f"), false);
        PlantType to = plantType(flags.get("-t"), false);
        if (from != null && to != null) {
            game.upgrade(from, to);
        }
    }

    private void handleZombotany(ZombotanyGame game, String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        String command = CommandParser.getCommand(flags);
        if (command.equals("advance time")) {
            game.tick(CommandParser.getIntFlag(flags, "t", 1));
        } else if (command.equals("plant plant")) {
            plantZombotany(game, flags);
        } else if (command.equals("collect sun")) {
            collectZombotany(game, flags);
        } else if (command.equals("show map")) {
            BoardView.showMap(game.getBoard());
        } else {
            System.out.println("Unknown Zombotany command.");
        }
    }

    private void plantZombotany(ZombotanyGame game, Map<String, String> flags) {
        PlantType type = plantType(CommandParser.getFlag(flags, "t"), true);
        int[] position = coordinates(CommandParser.getFlag(flags, "l"));
        if (type != null && (position == null || !game.plant(type, position[0], position[1]))) {
            System.out.println("Could not plant there.");
        }
    }

    private void collectZombotany(ZombotanyGame game, Map<String, String> flags) {
        int[] position = coordinates(CommandParser.getFlag(flags, "l"));
        if (position != null) {
            game.collectSun(position[0], position[1]);
        }
    }

    private int ticks(String raw) {
        return CommandParser.getIntFlag(CommandParser.parse(raw), "-t", 1);
    }

    private PlantType plantType(String raw, boolean normalize) {
        if (raw == null) {
            return null;
        }
        try {
            String name = raw.trim().toUpperCase();
            return PlantType.valueOf(normalize ? name.replace('-', '_').replace(' ', '_') : name);
        } catch (IllegalArgumentException e) {
            System.out.println(normalize ? "Unknown plant type." : "Unknown plant type: " + raw);
            return null;
        }
    }

    private ZombieType zombieType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ZombieType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown zombie type: " + raw);
            return null;
        }
    }

    private void applyCoordinates(String raw, CoordinateAction action) {
        int[] position = coordinates(raw);
        if (position != null) {
            action.apply(position[0], position[1]);
        }
    }

    private int[] coordinates(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = COORDINATES.matcher(raw);
        return matcher.find() ? new int[] {Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2))} : null;
    }

    @FunctionalInterface
    private interface CoordinateAction {
        void apply(int x, int lane);
    }
}
