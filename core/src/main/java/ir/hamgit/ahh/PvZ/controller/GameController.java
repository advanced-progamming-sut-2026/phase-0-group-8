package ir.hamgit.ahh.PvZ.controller;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.quest.LevelQuestTelemetry;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.quest.QuestService;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.def.PlantLevelEffects;
import ir.hamgit.ahh.PvZ.model.def.PlantAbilityProfiles;
import ir.hamgit.ahh.PvZ.model.enums.*;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;
import ir.hamgit.ahh.PvZ.model.special.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GameController {

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
    private boolean scoreMode;
    private final GameCommandRouter commandRouter = new GameCommandRouter(this);

    public GameController(User user) {
        this.currentUser = user;
    }

    public void startGame(String chapterName) {
        startGame(chapterName, 1);
    }

    public void startGame(String chapterName, int levelIndex) {
        scoreMode = false;
        if (levelIndex < 1 || levelIndex > 4) {
            throw new IllegalArgumentException("Adventure level must be between 1 and 4.");
        }
        this.pendingChapter = parseChapter(chapterName);
        if (pendingChapter == null || pendingChapter == ChapterType.MINIGAME) {
            throw new IllegalArgumentException("Unknown adventure chapter: " + chapterName);
        }
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
            case TIMED_WAR -> new TimedWarLevel(false, 12, 5 * Board.TICKS_PER_SECOND);
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
        if (boostedPlants.contains(type)) {
            System.out.println("That plant is already boosted.");
            return false;
        }
        if (currentUser == null || !currentUser.spendDiamonds(BOOST_DIAMOND_COST)) {
            System.out.println("Not enough diamonds.");
            return false;
        }
        boostedPlants.add(type);
        UserRepository.updateUser(currentUser);
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

    public void startActualGame(int totalWaves) {
        if (pendingChapter == null) {
            throw new IllegalStateException("Choose a chapter before starting the game.");
        }
        SpecialLevelType type = pendingSpecialLevelHandler != null
            ? specialTypeFor(pendingChapter, pendingLevelIndex) : null;
        board = new Board(pendingChapter, totalWaves, difficulty, type, pendingSpecialLevelHandler);
        LevelQuestTelemetry.start(board);
        if (pendingSpecialLevelHandler != null && pendingSpecialLevelHandler.getInitialSun() >= 0) {
            board.setSunAmount(pendingSpecialLevelHandler.getInitialSun());
        }
        if (currentUser != null) {
            int storedFood = currentUser.takeStoredPlantFood();
            for (int i = 0; i < storedFood; i++) {
                board.incrementPlantFoodCount();
            }
            UserRepository.updateUser(currentUser);
        }
    }

    public void endGame(boolean won) {
        if (board == null) {
            return;
        }
        if (currentUser != null) {
            currentUser.addCoins(board.drainCoinsEarned());
            currentUser.addDiamonds(board.drainDiamondsEarned());
            grantEarnedPots();
            currentUser.incrementGamesPlayed();
            QuestService.recordGamePlayed(currentUser);
            for (ZombieType type : board.getEncounteredZombies()) {
                currentUser.seeZombie(type);
            }
            if (scoreMode) {
                int score = calculateScore();
                currentUser.updateBestScore(score);
                System.out.println("Score-mode result: " + score + " myopoints.");
            } else if (won) {
                currentUser.completeLevel(pendingChapter, pendingLevelIndex);
                QuestService.recordLevelCompletion(currentUser);
                unlockProgressPlant();
            }
            QuestService.recordLevelOutcome(currentUser, board, selectedPlants, pendingChapter, won);
            UserRepository.updateUser(currentUser);
        }
        System.out.println(won ? "Level complete!" : "Level failed.");
        board = null;
    }

    private void unlockProgressPlant() {
        if (currentUser == null) {
            return;
        }
        for (PlantDef def : PlantRegistry.getAll()) {
            if (!currentUser.hasPlant(def.getType())) {
                currentUser.unlockPlant(def.getType());
                break;
            }
        }
    }

    public boolean finishIfOver() {
        if (board == null || !board.isGameOver()) {
            return false;
        }
        endGame(board.isPlayerWon());
        return true;
    }

    public void abandonGame() {
        if (board != null) {
            endGame(false);
        }
    }

    public boolean shouldSkipPlantSelection() {
        return pendingSpecialLevelHandler instanceof ConveyorBeltLevel;
    }

    public int getDefaultTotalWaves() {
        return 2 + Math.max(1, pendingLevelIndex);
    }

    public boolean canStartSelectedGame() {
        return shouldSkipPlantSelection() || !selectedPlants.isEmpty();
    }

    public void startScoreMode() {
        scoreMode = true;
        pendingChapter = ChapterType.ANCIENT_EGYPT;
        pendingLevelIndex = 1;
        difficulty = currentUser == null ? 3 : currentUser.getDifficulty();
        pendingSpecialLevelHandler = null;
        selectedPlants.clear();
        boostedPlants.clear();
        plantCooldowns.clear();
    }

    private int calculateScore() {
        int killScore = board.getZombiesKilled() * 100;
        int speedScore = Math.max(0, 3000 - board.getTickCount());
        int economyScore = board.getSunAmount() * 2;
        int survivalScore = board.getPlantsRemaining() * 50;
        int defenseScore = board.getMowersRemaining() * 250;
        return killScore + speedScore + economyScore + survivalScore + defenseScore;
    }

    public void advanceTime(int ticks) {
        if (board == null) {
            System.out.println("No level is currently running.");
            return;
        }
        if (ticks <= 0) {
            System.out.println("Time must advance by a positive number of ticks.");
            return;
        }
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

    void flushCurrencyToUser() {
        if (currentUser != null) {
            int coins = board.drainCoinsEarned();
            int diamonds = board.drainDiamondsEarned();
            int pots = board.drainPotsEarned();
            currentUser.addCoins(coins);
            currentUser.addDiamonds(diamonds);
            for (int i = 0; i < pots && currentUser.getPotCount() < 20; i++) {
                currentUser.unlockNextPot();
            }
            if (coins > 0 || diamonds > 0 || pots > 0) {
                UserRepository.updateUser(currentUser);
            }
        }
    }

    private void grantEarnedPots() {
        int pots = board.drainPotsEarned();
        for (int i = 0; i < pots && currentUser.getPotCount() < 20; i++) {
            currentUser.unlockNextPot();
        }
    }

    public void handleCommand(String raw) {
        commandRouter.handle(raw);
    }

    boolean plantSelected(PlantType type, int x, int lane) {
        int level = currentUser == null ? 1 : currentUser.getPlantLevel(type);
        int cost = Math.max(0, PlantRegistry.get(type).getSunCost()
            - PlantLevelEffects.sum(type, level, "Cost -"));
        if (!isPlantableRightNow(type) || !board.plantPlant(type, x, lane, cost, level)) {
            return false;
        }
        LevelQuestTelemetry.recordPlant(board, type);
        afterSuccessfulPlant(type, x, lane);
        return true;
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

    private void afterSuccessfulPlant(PlantType type, int x, int lane) {
        applyPlantLevel(type, x, lane);
        if (board.getSpecialLevelHandler() instanceof ConveyorBeltLevel belt) {
            belt.consumeOffer(type);
        } else if (!board.getSpecialLevelHandler().waitsForManualWaveStart()) {
            PlantDef def = PlantRegistry.get(type);
            int level = currentUser == null ? 1 : currentUser.getPlantLevel(type);
            int reduction = PlantLevelEffects.sum(type, level, "Cooldown -") * Board.TICKS_PER_SECOND;
            int cooldown = def.getRechargeSeconds() * Board.TICKS_PER_SECOND - reduction;
            plantCooldowns.put(type, Math.max(1, cooldown));
        }
        resetFamilyCooldownsIfUpgradedMint(type);
        boolean greenhouseBoost = currentUser != null && currentUser.consumePlantBoost(type);
        Plant placedPlant = board.getTileAt(x, lane).getPlant();
        if (placedPlant != null && (boostedPlants.contains(type) || greenhouseBoost)) {
            placedPlant.applyPlantFood(board);
            System.out.println(type + " was planted boosted by Plant Food!");
            if (greenhouseBoost) {
                UserRepository.updateUser(currentUser);
            }
        }
    }

    private void resetFamilyCooldownsIfUpgradedMint(PlantType type) {
        int level = currentUser == null ? 1 : currentUser.getPlantLevel(type);
        if (!PlantLevelEffects.has(type, level, "reset family cooldowns")) {
            return;
        }
        PlantFamily family = PlantAbilityProfiles.getFamily(type);
        plantCooldowns.keySet().removeIf(candidate -> PlantAbilityProfiles.getFamily(candidate) == family);
    }

    private void applyPlantLevel(PlantType type, int x, int lane) {
        if (currentUser == null) {
            return;
        }
        Plant plant = board.getTileAt(x, lane).getPlant();
        if (plant != null) {
            plant.setLevel(currentUser.getPlantLevel(type));
            if (type == PlantType.IMITATER
                && PlantLevelEffects.has(type, currentUser.getPlantLevel(type),
                "plant food on enterance")) {
                plant.applyPlantFood(board);
            }
        }
    }

    void clearCooldowns() {
        plantCooldowns.clear();
    }

    void addPersistentCurrency(int amount, boolean diamonds) {
        if (currentUser == null || amount < 0) {
            return;
        }
        if (diamonds) {
            currentUser.addDiamonds(amount);
        } else {
            currentUser.addCoins(amount);
        }
        UserRepository.updateUser(currentUser);
    }

    private PlantType parsePlantType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return PlantType.valueOf(normalizeEnumName(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    ZombieType parseZombieName(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ZombieType.valueOf(normalizeEnumName(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public PlantType parsePlantName(String raw) {
        return parsePlantType(raw);
    }

    private ChapterType parseChapter(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ChapterType.valueOf(normalizeEnumName(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizeEnumName(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    public Board getBoard() {
        return board;
    }

    public Set<PlantType> getSelectedPlants() {
        return selectedPlants;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
