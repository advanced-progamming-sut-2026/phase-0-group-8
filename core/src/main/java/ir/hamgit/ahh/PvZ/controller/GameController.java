package ir.hamgit.ahh.PvZ.controller;


import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.def.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.*;
import ir.hamgit.ahh.PvZ.model.special.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owns a single play-through of a level: plant selection, the {@link Board}
 * simulation, and translating raw CLI command strings into board calls.
 * Menu navigation (which top-level menu the player is in) is the menu
 * teammate's job - see MenuController/GameMenu in the class reference.
 *
 * <p>Which {@link SpecialLevelType} shows up as level 2 / level 3 of each
 * chapter is a design choice, not something the spec pins down beyond "each
 * of the 8 special types appears exactly once across Adventure" - see
 * {@link #specialTypeFor(ChapterType, int)}. Change the mapping freely.</p>
 */
public class GameController {

    private static final Pattern COORD_PATTERN = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)");
    private static final int DEFAULT_PLANT_SLOTS = 8;
    private static final int BOOST_DIAMOND_COST = 2;

    private Board board;
    private User currentUser;
    private ChapterType pendingChapter;
    private int pendingLevelIndex = 1;
    private SpecialLevelHandler pendingSpecialLevelHandler;
    private final Set<PlantType> selectedPlants = new HashSet<>();
    private final Set<PlantType> boostedPlants = new HashSet<>();
    private final Map<PlantType, Integer> plantCooldowns = new EnumMap<>(PlantType.class);
    private int difficulty = 3;

    public GameController(User user) {
        this.currentUser = user;
    }

    // ------------------------------------------------------------------
    // Chapter selection -> plant selection -> actual game
    // ------------------------------------------------------------------

    public void startGame(String chapterName) {
        startGame(chapterName, 1);
    }

    public void startGame(String chapterName, int levelIndex) {
        this.pendingChapter = ChapterType.valueOf(chapterName.trim().toUpperCase());
        this.pendingLevelIndex = levelIndex;
        this.difficulty = currentUser != null ? currentUser.getDifficulty() : 3;
        this.pendingSpecialLevelHandler = buildHandlerFor(pendingChapter, levelIndex);
        selectedPlants.clear();
        boostedPlants.clear();
        plantCooldowns.clear();
        applyForcedStarterKitIfAny();
    }

    /** LockedPlantsLevel's "forced starter kit" variant pre-selects its mandatory plants. */
    private void applyForcedStarterKitIfAny() {
        if (pendingSpecialLevelHandler instanceof LockedPlantsLevel locked) {
            for (PlantType type : locked.getForcedTypes()) {
                selectedPlants.add(type);
            }
        }
    }

    private SpecialLevelHandler buildHandlerFor(ChapterType chapter, int levelIndex) {
        SpecialLevelType type = specialTypeFor(chapter, levelIndex);
        if (type == null) {
            return null;
        }
        return switch (type) {
            case CONVEYOR_BELT -> new ConveyorBeltLevel(new ArrayList<>(unlockedOrAll()));
            case LOCKED_PLANTS -> LockedPlantsLevel.familyLockout(Tag.PEA, PlantType.PEASHOOTER);
            case SAVE_OUR_SEEDS -> new SaveOurSeedsLevel(3);
            case TIMED_WAR -> new TimedWarLevel(false, 12, 5 * Board.TICKS_PER_SECOND * 60);
            case NIGHT_OPS -> new NightOpsLevel();
            case DEAD_LINE -> new DeadLineLevel(4);
            case LOVE_YOUR_PLANTS -> new LoveYourPlantsLevel(5);
            case PLANT_WHAT_YOU_GET -> new PlantWhatYouGetLevel();
        };
    }

    private static SpecialLevelType specialTypeFor(ChapterType chapter, int levelIndex) {
        if (levelIndex == 2) {
            return switch (chapter) {
                case ANCIENT_EGYPT -> SpecialLevelType.CONVEYOR_BELT;
                case FROSTBITE_CAVES -> SpecialLevelType.SAVE_OUR_SEEDS;
                case BIG_WAVE_BEACH -> SpecialLevelType.NIGHT_OPS;
                case DARK_AGES -> SpecialLevelType.LOVE_YOUR_PLANTS;
                default -> null;
            };
        }
        if (levelIndex == 3) {
            return switch (chapter) {
                case ANCIENT_EGYPT -> SpecialLevelType.LOCKED_PLANTS;
                case FROSTBITE_CAVES -> SpecialLevelType.TIMED_WAR;
                case BIG_WAVE_BEACH -> SpecialLevelType.DEAD_LINE;
                case DARK_AGES -> SpecialLevelType.PLANT_WHAT_YOU_GET;
                default -> null;
            };
        }
        return null;
    }

    private List<PlantType> unlockedOrAll() {
        if (currentUser != null && !currentUser.getUnlockedPlants().isEmpty()) {
            return new ArrayList<>(currentUser.getUnlockedPlants());
        }
        List<PlantType> all = new ArrayList<>();
        for (PlantDef def : PlantRegistry.getAll()) {
            all.add(def.getType());
        }
        return all;
    }

    // ------------------------------------------------------------------
    // Plant selection commands
    // ------------------------------------------------------------------

    public boolean selectPlant(PlantType type) {
        if (!isSelectableNow(type) || selectedPlants.contains(type)) {
            System.out.println("Cannot add plant " + type + ".");
            return false;
        }
        if (selectedPlants.size() >= DEFAULT_PLANT_SLOTS) {
            System.out.println("No plant slots left.");
            return false;
        }
        selectedPlants.add(type);
        return true;
    }

    private boolean isSelectableNow(PlantType type) {
        boolean unlocked = currentUser == null || currentUser.getUnlockedPlants().isEmpty()
            || currentUser.getUnlockedPlants().contains(type);
        boolean allowedBySpecialLevel = pendingSpecialLevelHandler == null
            || pendingSpecialLevelHandler.isSelectablePlant(type);
        return unlocked && allowedBySpecialLevel && PlantRegistry.get(type) != null;
    }

    public boolean removePlantSelection(PlantType type) {
        if (!selectedPlants.remove(type)) {
            System.out.println("Plant " + type + " was not selected.");
            return false;
        }
        boostedPlants.remove(type);
        return true;
    }

    public boolean boostPlant(PlantType type) {
        if (!selectedPlants.contains(type)) {
            System.out.println("Select the plant before boosting it.");
            return false;
        }
        if (currentUser == null || !currentUser.spendDiamonds(BOOST_DIAMOND_COST)) {
            System.out.println("Not enough diamonds.");
            return false;
        }
        boostedPlants.add(type);
        return true;
    }

    public void showAllPlants() {
        for (PlantDef def : PlantRegistry.getAll()) {
            System.out.println(def.getType() + " - cost " + def.getSunCost());
        }
    }

    public void showAvailablePlants() {
        for (PlantDef def : PlantRegistry.getAll()) {
            if (isSelectableNow(def.getType())) {
                System.out.println(def.getType() + " - cost " + def.getSunCost());
            }
        }
    }

    // ------------------------------------------------------------------
    // Start the actual game after plant selection
    // ------------------------------------------------------------------

    public void startActualGame(int totalWaves) {
        SpecialLevelType type = pendingSpecialLevelHandler != null
            ? specialTypeFor(pendingChapter, pendingLevelIndex) : null;
        board = new Board(pendingChapter, totalWaves, difficulty, type, pendingSpecialLevelHandler);
        if (pendingSpecialLevelHandler != null && pendingSpecialLevelHandler.getInitialSun() >= 0) {
            board.setSunAmount(pendingSpecialLevelHandler.getInitialSun());
        }
        if (currentUser != null) {
            for (PlantType boosted : boostedPlants) {
                currentUser.storeBoost(boosted);
            }
        }
    }

    public void endGame(boolean won) {
        if (currentUser != null) {
            currentUser.addCoins(board.drainCoinsEarned());
            currentUser.addDiamonds(board.drainDiamondsEarned());
        }
        System.out.println(won ? "Level complete!" : "Level failed.");
        board = null;
    }

    // ------------------------------------------------------------------
    // In-game command routing
    // ------------------------------------------------------------------

    public void advanceTime(int ticks) {
        board.advanceTime(ticks);
        flushCurrencyToUser();
        for (Map.Entry<PlantType, Integer> entry : new EnumMap<>(plantCooldowns).entrySet()) {
            int remaining = entry.getValue() - ticks;
            if (remaining <= 0) {
                plantCooldowns.remove(entry.getKey());
            } else {
                plantCooldowns.put(entry.getKey(), remaining);
            }
        }
    }

    private void flushCurrencyToUser() {
        if (currentUser != null) {
            currentUser.addCoins(board.drainCoinsEarned());
            currentUser.addDiamonds(board.drainDiamondsEarned());
        }
    }

    public void handleCommand(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("advance time")) {
            handleAdvanceTime(trimmed);
        } else if (trimmed.startsWith("plant plant")) {
            handlePlantPlant(trimmed);
        } else if (trimmed.startsWith("pluck plant")) {
            handlePluckPlant(trimmed);
        } else if (trimmed.startsWith("feed plant")) {
            handleFeedPlant(trimmed);
        } else if (trimmed.startsWith("collect sun")) {
            handleCollectSun(trimmed);
        } else if (trimmed.startsWith("cheat")) {
            handleCheat(trimmed);
        } else if (trimmed.startsWith("show map")) {
            board.showMap();
        } else if (trimmed.startsWith("show plants status")) {
            board.showPlantsStatus();
        } else if (trimmed.startsWith("show tile status")) {
            handleShowTileStatus(trimmed);
        } else if (trimmed.startsWith("show sun amount")) {
            board.showSunAmount();
        } else if (trimmed.startsWith("zombies info")) {
            board.showZombiesInfo();
        } else if (trimmed.startsWith("show all plants")) {
            showAllPlants();
        } else if (trimmed.startsWith("show available plants")) {
            showAvailablePlants();
        } else if (trimmed.startsWith("add plant")) {
            selectPlant(parsePlantType(CommandParser.getFlag(CommandParser.parse(trimmed), "-t")));
        } else if (trimmed.startsWith("remove plant")) {
            removePlantSelection(parsePlantType(CommandParser.getFlag(CommandParser.parse(trimmed), "-t")));
        } else if (trimmed.startsWith("boost plant")) {
            boostPlant(parsePlantType(CommandParser.getFlag(CommandParser.parse(trimmed), "-t")));
        } else if (trimmed.equals("start zombie waves") || trimmed.equals("release the nuke")) {
            handleBareCommand(trimmed);
        } else {
            System.out.println("Unknown command: " + raw);
        }
    }

    private void handleBareCommand(String trimmed) {
        if (trimmed.equals("start zombie waves")) {
            board.startZombieWaves();
        } else {
            board.cheatReleaseNuke();
        }
    }

    private void handleAdvanceTime(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        int ticks = CommandParser.getIntFlag(flags, "-t", 1);
        advanceTime(ticks);
    }

    private void handlePlantPlant(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        PlantType type = parsePlantType(flags.get("-t"));
        int[] coords = parseCoordinates(flags.get("-l"));
        if (type == null || coords == null) {
            System.out.println("Invalid plant command.");
            return;
        }
        if (!isPlantableRightNow(type)) {
            System.out.println("You cannot plant " + type + " right now.");
            return;
        }
        boolean success = board.plantPlant(type, coords[0], coords[1]);
        if (success) {
            afterSuccessfulPlant(type);
        } else {
            System.out.println("Could not plant " + type + " at (" + coords[0] + ", " + coords[1] + ").");
        }
    }

    private boolean isPlantableRightNow(PlantType type) {
        if (board.getSpecialLevelHandler() instanceof ConveyorBeltLevel belt) {
            return belt.isOffered(type);
        }
        boolean waitingForFreePlanting = board.getSpecialLevelHandler().waitsForManualWaveStart();
        boolean selected = selectedPlants.contains(type);
        boolean offCooldown = !plantCooldowns.containsKey(type);
        return selected && (waitingForFreePlanting || offCooldown);
    }

    private void afterSuccessfulPlant(PlantType type) {
        if (board.getSpecialLevelHandler() instanceof ConveyorBeltLevel belt) {
            belt.consumeOffer(type);
        } else if (!board.getSpecialLevelHandler().waitsForManualWaveStart()) {
            PlantDef def = PlantRegistry.get(type);
            plantCooldowns.put(type, def.getRechargeSeconds() * Board.TICKS_PER_SECOND);
        }
        if (boostedPlants.remove(type)) {
            System.out.println(type + " was planted boosted by Plant Food!");
        }
    }

    private void handlePluckPlant(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        int[] coords = parseCoordinates(flags.get("-l"));
        if (coords == null || !board.pluckPlant(coords[0], coords[1])) {
            System.out.println("Could not pluck a plant there.");
        }
    }

    private void handleFeedPlant(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        int[] coords = parseCoordinates(flags.get("-l"));
        if (coords == null || !board.feedPlant(coords[0], coords[1])) {
            System.out.println("Could not feed a plant there.");
        }
    }

    private void handleCollectSun(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        int[] coords = parseCoordinates(flags.get("-l"));
        if (coords == null) {
            System.out.println("Invalid coordinates.");
            return;
        }
        boolean got = board.collectSun(coords[0], coords[1]) || board.collectFallingSun(coords[0], coords[1]);
        if (!got) {
            System.out.println("No sun to collect there.");
        }
        flushCurrencyToUser();
    }

    private void handleShowTileStatus(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        int[] coords = parseCoordinates(flags.get("-l"));
        if (coords != null) {
            board.showTileStatus(coords[0], coords[1]);
        }
    }

    private void handleCheat(String raw) {
        Map<String, String> flags = CommandParser.parse(raw);
        if (raw.contains("add-plant-food")) {
            board.cheatAddPlantFood();
        } else if (raw.contains("remove-cooldown")) {
            plantCooldowns.clear();
            board.cheatRemoveCooldownNoop();
        } else if (raw.contains("spawn-zombie")) {
            handleCheatSpawnZombie(flags);
        } else if (raw.contains("release") && raw.contains("nuke")) {
            board.cheatReleaseNuke();
        } else if (raw.contains("add")) {
            handleCheatAdd(raw, flags);
        }
    }

    private void handleCheatSpawnZombie(Map<String, String> flags) {
        ZombieType type = parseZombieType(flags.get("-t"));
        int[] coords = parseCoordinates(flags.get("-l"));
        if (type != null && coords != null) {
            board.cheatSpawnZombie(type, coords[0], coords[1]);
        }
    }

    private void handleCheatAdd(String raw, Map<String, String> flags) {
        int n = CommandParser.getIntFlag(flags, "-n", 0);
        if (raw.contains("sun")) {
            board.cheatAddSuns(n);
        } else if (raw.contains("coin")) {
            currentUser.addCoins(n);
        } else if (raw.contains("diamond")) {
            currentUser.addDiamonds(n);
        }
    }

    private PlantType parsePlantType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return PlantType.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ZombieType parseZombieType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ZombieType.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int[] parseCoordinates(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = COORD_PATTERN.matcher(raw);
        if (!m.find()) {
            return null;
        }
        return new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
    }

    public Board getBoard() {
        return board;
    }

    public Set<PlantType> getSelectedPlants() {
        return selectedPlants;
    }
}
