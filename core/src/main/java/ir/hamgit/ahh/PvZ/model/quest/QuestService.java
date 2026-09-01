package ir.hamgit.ahh.PvZ.model.quest;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.User;

import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantFamily;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

 
public final class QuestService {

    public enum Category { STORY, EPIC, DAILY, REPEATABLE }
    public enum RewardType { COINS, DIAMONDS, UNLOCK, SEED_PACKETS }

    public record Quest(String id, String title, Category category, int priority,
                        int target, RewardType rewardType, int rewardAmount) {
    }

    private static final int LOW = 20;
    private static final int MEDIUM = 50;
    private static final int HIGH = 80;
    private static final int CRITICAL = 100;
    private static final String VARIABLE_PREFIX = "variable:";
    private static final List<Quest> QUESTS = List.of(
        new Quest("story_begin", "Complete your first Adventure level", Category.STORY, 100, 1,
            RewardType.UNLOCK, 1),
        new Quest("story_world", "Complete four Adventure levels", Category.STORY, 100, 4,
            RewardType.UNLOCK, 1),
        new Quest("epic_veteran", "Complete eight Adventure levels", Category.EPIC, 80, 8,
            RewardType.DIAMONDS, 10),
        new Quest("epic_minigames", "Win three minigame levels", Category.EPIC, 80, 3,
            RewardType.DIAMONDS, 8),
        new Quest("daily_win", "Win one level today", Category.DAILY, 50, 1,
            RewardType.COINS, 600),
        new Quest("daily_minigame", "Win one minigame today", Category.DAILY, 50, 1,
            RewardType.SEED_PACKETS, 5),
        new Quest("repeat_levels", "Complete three more levels", Category.REPEATABLE, 20, 3,
            RewardType.COINS, 1000),
        new Quest("repeat_games", "Play five games", Category.REPEATABLE, 20, 5,
            RewardType.COINS, 750),
        new Quest("daily_sun_catcher", "آفتاب گیر روزانه", Category.DAILY, MEDIUM, 3000,
            RewardType.COINS, 30),
        new Quest("chapter_hunter", "شکارچی chapter", Category.STORY, HIGH, 50,
            RewardType.SEED_PACKETS, 10),
        new Quest("professional_unlocker", "plant باز حرفه‌ای", Category.DAILY, HIGH, 10,
            RewardType.UNLOCK, 1),
        new Quest("only_cactus", "only cactus", Category.DAILY, HIGH, 10,
            RewardType.DIAMONDS, 20),
        new Quest("economic_plant_eater", "گیاه خوار اقتصادی", Category.STORY, HIGH, 1,
            RewardType.SEED_PACKETS, 20),
        new Quest("defense_master", "استاد دفاع", Category.EPIC, CRITICAL, 1,
            RewardType.DIAMONDS, 200),
        new Quest("speed_runner", "سرعت عمل", Category.STORY, MEDIUM, 10,
            RewardType.COINS, 500),
        new Quest("professional_destroyer", "تخریب گر حرفه ای", Category.DAILY, LOW, 3,
            RewardType.COINS, 100),
        new Quest("symmetry", "تقارن", Category.DAILY, HIGH, 1,
            RewardType.COINS, 500),
        new Quest("family_massacre", "کشتار خانوادگی", Category.DAILY, MEDIUM, 1,
            RewardType.COINS, 1000),
        new Quest("limited_bloom", "شکوفایی در محدودیت‌ها", Category.DAILY, HIGH, 1,
            RewardType.DIAMONDS, 100),
        new Quest("night_or_morning", "شب یا صبح", Category.EPIC, HIGH, 1,
            RewardType.DIAMONDS, 20),
        new Quest("win_streak", "برد پشت برد", Category.DAILY, MEDIUM, 5,
            RewardType.COINS, 5000),
        new Quest("almost_won", "تقریبا پیروز", Category.DAILY, MEDIUM, 10,
            RewardType.COINS, 300),
        new Quest("ocd", "OCD نَمَنَ", Category.DAILY, MEDIUM, 1,
            RewardType.COINS, 800),
        new Quest("cloudy_day", "روز ابری", Category.DAILY, HIGH, 1,
            RewardType.DIAMONDS, 10),
        new Quest("one_less_column", "یه ستون کمتر", Category.DAILY, HIGH, 1,
            RewardType.DIAMONDS, 10),
        new Quest("undefended_row", "سطر بی دفاع", Category.DAILY, HIGH, 1,
            RewardType.DIAMONDS, 20),
        new Quest("undefended_cross", "صلیب بی دفاع", Category.DAILY, HIGH, 1,
            RewardType.DIAMONDS, 25),
        new Quest("mowing_time", "وقت چمن‌زنی", Category.EPIC, MEDIUM, 10,
            RewardType.DIAMONDS, 10)
    );

    private QuestService() {
    }

    public static void prepareDaily(User user) {
        ensurePersistentVariables(user);
        String today = LocalDate.now().toString();
        if (!today.equals(user.getDailyQuestLastDate())) {
            user.setDailyQuestLastDate(today);
            for (Quest quest : QUESTS) {
                if (quest.category() == Category.DAILY) {
                    user.updateQuestProgress(quest.id(), 0);
                    user.updateQuestProgress(claimKey(quest.id()), 0);
                    user.updateQuestProgress(variableKey(quest.id()), 0);
                }
            }
        }
        ensureDailyVariables(user, today);
    }

    private static void ensurePersistentVariables(User user) {
        setVariableIfMissing(user, "chapter_hunter", pick(user, "chapter", 4) + 1);
        setVariableIfMissing(user, "economic_plant_eater", pick(user, "economic", 6) + 1);
        int[] mowerTargets = {10, 20, 30, 40, 50};
        setVariableIfMissing(user, "mowing_time", mowerTargets[pick(user, "mower", 5)]);
    }

    private static void ensureDailyVariables(User user, String date) {
        int[] sunTargets = {3000, 4000, 5000};
        setVariableIfMissing(user, "daily_sun_catcher",
            sunTargets[pick(user, date + "sun", sunTargets.length)]);
        setVariableIfMissing(user, "professional_unlocker", chosenKillingPlant(user, date).ordinal() + 1);
        setVariableIfMissing(user, "family_massacre", pick(user, date + "family-kill", 9) + 1);
        setVariableIfMissing(user, "limited_bloom", pick(user, date + "family-ban", 9) + 1);
        setVariableIfMissing(user, "one_less_column", pick(user, date + "column", Board.COLUMNS) + 1);
        setVariableIfMissing(user, "undefended_row", pick(user, date + "row", Board.ROWS) + 1);
        setVariableIfMissing(user, "undefended_cross", pick(user, date + "cross", Board.ROWS) + 1);
    }

    private static PlantType chosenKillingPlant(User user, String date) {
        List<PlantType> candidates = new ArrayList<>();
        for (PlantType type : user.getUnlockedPlants()) {
            if (SpreadsheetQuestRules.isOffensive(type)) {
                candidates.add(type);
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(PlantType.PEASHOOTER);
        }
        return candidates.get(pick(user, date + "professional", candidates.size()));
    }

    private static int pick(User user, String salt, int bound) {
        return Math.floorMod((user.getUsername() + salt).hashCode(), bound);
    }

    private static void setVariableIfMissing(User user, String id, int value) {
        if (user.getQuestProgress(variableKey(id)) == 0) {
            user.updateQuestProgress(variableKey(id), value);
        }
    }

    static int variable(User user, String id) {
        prepareDaily(user);
        String key = variableKey(id);
        int stored = user.getQuestProgress(key);
        int repaired = repairVariable(id, stored);
        if (stored != repaired) {
            user.updateQuestProgress(key, repaired);
        }
        return repaired;
    }

    private static int repairVariable(String id, int value) {
        return switch (id) {
            case "daily_sun_catcher" -> clamp(value, 3000, 5000);
            case "chapter_hunter" -> clamp(value, 1, 4);
            case "professional_unlocker" -> clamp(value, 1, PlantType.values().length);
            case "economic_plant_eater" -> clamp(value, 1, 6);
            case "family_massacre", "limited_bloom" ->
                clamp(value, 1, PlantFamily.values().length);
            case "one_less_column" -> clamp(value, 1, Board.COLUMNS);
            case "undefended_row", "undefended_cross" -> clamp(value, 1, Board.ROWS);
            case "mowing_time" -> clamp(value, 10, 50);
            default -> Math.max(0, value);
        };
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static void recordLevelCompletion(User user) {
        prepareDaily(user);
        increment(user, "story_begin", 1);
        increment(user, "story_world", 1);
        increment(user, "epic_veteran", 1);
        increment(user, "daily_win", 1);
        increment(user, "repeat_levels", 1);
    }

    public static void recordMinigameWin(User user) {
        prepareDaily(user);
        increment(user, "epic_minigames", 1);
        increment(user, "daily_minigame", 1);
    }

    public static void recordGamePlayed(User user) {
        increment(user, "repeat_games", 1);
    }

     
    public static void recordLevelOutcome(User user, Board board, Set<PlantType> selected,
                                          ChapterType chapter, boolean won) {
        prepareDaily(user);
        SpreadsheetQuestRules.record(user, board, selected, chapter, won,
            LevelQuestTelemetry.finish(board));
    }

    public static List<Quest> questsFor(Category category) {
        return QUESTS.stream().filter(quest -> quest.category() == category)
            .sorted(Comparator.comparingInt(Quest::priority).reversed()).toList();
    }

    public static int targetFor(User user, Quest quest) {
        return switch (quest.id()) {
            case "daily_sun_catcher", "mowing_time" -> variable(user, quest.id());
            default -> quest.target();
        };
    }

    public static String conditionFor(User user, Quest quest) {
        int value = variable(user, quest.id());
        return switch (quest.id()) {
            case "daily_sun_catcher" -> "Collect " + value + " sun today";
            case "chapter_hunter" -> "Defeat 50 zombies in " + chapterName(value);
            case "professional_unlocker" -> "Kill 10 zombies using only " + plantName(value);
            case "economic_plant_eater" -> "Win while losing no more than " + (value - 1) + " plants";
            case "family_massacre" -> "Use only " + familyName(value) + " attackers to kill zombies";
            case "limited_bloom" -> "Win without plants from the " + familyName(value) + " family";
            case "one_less_column" -> "Win without planting in column " + value;
            case "undefended_row" -> "Win without planting in row " + value;
            case "undefended_cross" -> "Win with row and column " + value + " empty";
            case "mowing_time" -> "Kill at least " + value + " zombies with lawn mowers";
            default -> quest.title();
        };
    }

    public static String rewardFor(User user, Quest quest) {
        int amount = rewardAmount(user, quest);
        return amount + " " + switch (quest.rewardType()) {
            case COINS -> "coins";
            case DIAMONDS -> "gems";
            case UNLOCK -> "random new plant";
            case SEED_PACKETS -> "seed packets";
        };
    }

    private static String chapterName(int encoded) {
        ChapterType[] values = {ChapterType.ANCIENT_EGYPT, ChapterType.FROSTBITE_CAVES,
            ChapterType.BIG_WAVE_BEACH, ChapterType.DARK_AGES};
        return values[Math.max(0, Math.min(values.length - 1, encoded - 1))].name();
    }

    private static String plantName(int encoded) {
        PlantType[] values = PlantType.values();
        return values[Math.max(0, Math.min(values.length - 1, encoded - 1))].name();
    }

    private static String familyName(int encoded) {
        PlantFamily[] values = PlantFamily.values();
        return values[Math.max(0, Math.min(values.length - 1, encoded - 1))].name();
    }

    public static boolean claim(User user, String questId) {
        Quest quest = find(questId);
        if (quest == null || user.getQuestProgress(questId) < targetFor(user, quest)
            || user.getQuestProgress(claimKey(questId)) > 0) {
            return false;
        }
        grantReward(user, quest);
        String completion = completionKey(quest.id());
        int completed = user.getQuestProgress(completion);
        user.updateQuestProgress(completion,
            completed == Integer.MAX_VALUE ? completed : completed + 1);
        user.updateQuestProgress(claimKey(questId), 1);
        if (quest.category() == Category.REPEATABLE) {
            user.updateQuestProgress(questId, 0);
            user.updateQuestProgress(claimKey(questId), 0);
        }
        return true;
    }

    private static void grantReward(User user, Quest quest) {
        int amount = rewardAmount(user, quest);
        switch (quest.rewardType()) {
            case COINS -> user.addCoins(amount);
            case DIAMONDS -> user.addDiamonds(amount);
            case UNLOCK -> unlockRandomPlant(user);
            case SEED_PACKETS -> grantSeedPackets(user, amount);
        }
    }

    private static int rewardAmount(User user, Quest quest) {
        return switch (quest.id()) {
            case "daily_sun_catcher" -> variable(user, quest.id()) / 100;
            case "economic_plant_eater" -> 21 - variable(user, quest.id());
            case "mowing_time" -> variable(user, quest.id());
            default -> quest.rewardAmount();
        };
    }

    private static void unlockRandomPlant(User user) {
        List<PlantType> locked = new ArrayList<>();
        for (PlantDef def : PlantRegistry.getAll()) {
            if (!user.hasPlant(def.getType())) {
                locked.add(def.getType());
            }
        }
        if (!locked.isEmpty()) {
            user.unlockPlant(locked.get((int) (Math.random() * locked.size())));
        }
    }

    private static void grantSeedPackets(User user, int amount) {
        if (!user.getUnlockedPlants().isEmpty()) {
            int index = (int) (Math.random() * user.getUnlockedPlants().size());
            user.addSeedPackets(user.getUnlockedPlants().get(index), amount);
        }
    }

    static void increment(User user, String id, int amount) {
        Quest quest = find(id);
        if (quest != null && amount > 0) {
            long updated = (long) user.getQuestProgress(id) + amount;
            int progress = (int) Math.min(targetFor(user, quest), updated);
            user.updateQuestProgress(id, progress);
        }
    }

    static void complete(User user, String id, boolean condition) {
        if (condition) {
            increment(user, id, 1);
        }
    }

    static void reset(User user, String id) {
        user.updateQuestProgress(id, 0);
    }

    private static Quest find(String id) {
        return QUESTS.stream().filter(quest -> quest.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public static boolean isClaimed(User user, String id) {
        return user.getQuestProgress(claimKey(id)) > 0;
    }

    public static int completedCount(User user, boolean daily) {
        long result = 0;
        for (Quest quest : QUESTS) {
            if ((quest.category() == Category.DAILY) == daily) {
                int recorded = user.getQuestProgress(completionKey(quest.id()));
                result += recorded > 0 ? recorded : isClaimed(user, quest.id()) ? 1 : 0;
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, result);
    }

    private static String variableKey(String id) {
        return VARIABLE_PREFIX + id;
    }

    private static String claimKey(String id) {
        return "claimed:" + id;
    }

    private static String completionKey(String id) {
        return "completed:" + id;
    }
}
