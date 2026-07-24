package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.enums.Gender;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class User implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String passwordHash;
    private String nickname;
    private String email;
    private Gender gender;
    private String securityQuestion;
    private String securityAnswerHash;
    private int coins;
    private int diamonds;
    private int gamesPlayed;
    private int levelsCompleted;
    private int bestScoreMode;
    private List<PlantType> unlockedPlants;
    private Set<ZombieType> seenZombies;
    private boolean stayLoggedIn;
    private List<String> unreadNews;
    private List<String> allNews;
    private Map<String, Integer> questProgress;
    private String dailyQuestLastDate;
    private String dailyShopLastDate;
    private Map<PlantType, Integer> plantSeedPackets;
    private Map<PlantType, Integer> plantLevels;
    private Map<PlantType, Boolean> plantBoosts;
    private Map<String, Integer> minigamesCompleted;
    private int difficulty;
    private int potCount;
    private Map<ChapterType, Integer> adventureProgress;
    private int storedPlantFood;
    private List<GreenHousePot> greenhousePots;
    private String dailyOfferPlant;
    private boolean dailyOfferPurchased;

    public User(String username, String passwordHash, String nickname,
                String email, Gender gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.coins = 0;
        this.diamonds = 0;
        this.gamesPlayed = 0;
        this.levelsCompleted = 0;
        this.bestScoreMode = 0;
        this.difficulty = 3;
        this.stayLoggedIn = false;
        this.potCount = 5;
        this.unlockedPlants = new ArrayList<>();
        this.seenZombies = new HashSet<>();
        this.unreadNews = new ArrayList<>();
        this.allNews = new ArrayList<>();
        this.questProgress = new HashMap<>();
        this.plantSeedPackets = new HashMap<>();
        this.plantLevels = new HashMap<>();
        this.plantBoosts = new HashMap<>();
        this.minigamesCompleted = new HashMap<>();
        this.adventureProgress = new HashMap<>();
        this.storedPlantFood = 0;
        initializeGreenhouse();
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        repairDeserializedState();
    }

    private void repairDeserializedState() {
        unlockedPlants = SaveStateSanitizer.list(unlockedPlants);
        seenZombies = SaveStateSanitizer.set(seenZombies);
        unreadNews = SaveStateSanitizer.list(unreadNews);
        allNews = SaveStateSanitizer.list(allNews);
        questProgress = SaveStateSanitizer.nonNegativeMap(questProgress);
        plantSeedPackets = SaveStateSanitizer.nonNegativeMap(plantSeedPackets);
        plantLevels = SaveStateSanitizer.boundedMap(plantLevels, 1, 4);
        plantBoosts = SaveStateSanitizer.booleanMap(plantBoosts);
        minigamesCompleted = SaveStateSanitizer.nonNegativeMap(minigamesCompleted);
        adventureProgress = SaveStateSanitizer.boundedMap(adventureProgress, 0, 4);
        difficulty = difficulty < 1 || difficulty > 5 ? 3 : difficulty;
        coins = Math.max(0, coins);
        diamonds = Math.max(0, diamonds);
        gamesPlayed = Math.max(0, gamesPlayed);
        levelsCompleted = Math.max(0, levelsCompleted);
        bestScoreMode = Math.max(0, bestScoreMode);
        storedPlantFood = Math.max(0, Math.min(3, storedPlantFood));
        dailyOfferPlant = dailyOfferPlant == null ? PlantType.PEASHOOTER.name() : dailyOfferPlant;
        repairGreenhouse();
    }

    private void repairGreenhouse() {
        List<GreenHousePot> saved = greenhousePots;
        initializeGreenhouse();
        if (saved != null) {
            for (int i = 0; i < Math.min(saved.size(), greenhousePots.size()); i++) {
                if (saved.get(i) != null) {
                    greenhousePots.set(i, saved.get(i));
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            greenhousePots.get(i).unlock();
        }
        potCount = (int) greenhousePots.stream().filter(pot -> !pot.isLocked()).count();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Gender getGender() {
        return gender;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String q) {
        this.securityQuestion = q;
    }

    public String getSecurityAnswerHash() {
        return securityAnswerHash;
    }

    public void setSecurityAnswerHash(String h) {
        this.securityAnswerHash = h;
    }

    public int getCoins() {
        return coins;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public void addCoins(int amount) {
        coins = addCurrency(coins, amount);
    }

    public void addDiamonds(int amount) {
        diamonds = addCurrency(diamonds, amount);
    }

    private int addCurrency(int balance, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Currency additions cannot be negative.");
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) balance + amount);
    }

    public boolean spendCoins(int amount) {
        if (amount < 0 || coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
    }

    public boolean spendDiamonds(int amount) {
        if (amount < 0 || diamonds < amount) {
            return false;
        }
        diamonds -= amount;
        return true;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void incrementGamesPlayed() {
        gamesPlayed = gamesPlayed == Integer.MAX_VALUE ? gamesPlayed : gamesPlayed + 1;
    }

    public int getLevelsCompleted() {
        return levelsCompleted;
    }

    public void incrementLevelsCompleted() {
        levelsCompleted = levelsCompleted == Integer.MAX_VALUE ? levelsCompleted : levelsCompleted + 1;
    }

    public int getBestScoreMode() {
        return bestScoreMode;
    }

    public void updateBestScore(int score) {
        if (score > bestScoreMode) {
            bestScoreMode = score;
        }
    }

    public List<PlantType> getUnlockedPlants() {
        return List.copyOf(unlockedPlants);
    }

    public void unlockPlant(PlantType type) {
        if (type != null && !unlockedPlants.contains(type)) {
            unlockedPlants.add(type);
            addNews("New plant unlocked: " + type.name());
        }
    }

    public boolean hasPlant(PlantType type) {
        return unlockedPlants.contains(type);
    }

    public Set<ZombieType> getSeenZombies() {
        return Set.copyOf(seenZombies);
    }

    public void seeZombie(ZombieType type) {
        if (type != null && seenZombies.add(type)) {
            addNews("New zombie encountered: " + type.name());
        }
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public void setStayLoggedIn(boolean val) {
        this.stayLoggedIn = val;
    }

    public void addNews(String item) {
        unreadNews.add(item);
        allNews.add(item);
    }

    public List<String> getUnreadNews() {
        return new ArrayList<>(unreadNews);
    }

    public List<String> getAllNews() {
        return new ArrayList<>(allNews);
    }

    public void markAllNewsRead() {
        unreadNews.clear();
    }

    public void updateQuestProgress(String questId, int value) {
        if (questId != null) {
            questProgress.put(questId, Math.max(0, value));
        }
    }

    public int getQuestProgress(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public String getDailyQuestLastDate() {
        return dailyQuestLastDate;
    }

    public void setDailyQuestLastDate(String date) {
        this.dailyQuestLastDate = date;
    }

    public String getDailyShopLastDate() {
        return dailyShopLastDate;
    }

    public void setDailyShopLastDate(String date) {
        this.dailyShopLastDate = date;
    }

    public void addSeedPackets(PlantType type, int amount) {
        if (type == null || amount < 0) {
            throw new IllegalArgumentException("Seed packets require a plant and nonnegative amount.");
        }
        long total = (long) getSeedPackets(type) + amount;
        plantSeedPackets.put(type, (int) Math.min(Integer.MAX_VALUE, total));
    }

    public int getSeedPackets(PlantType type) {
        return plantSeedPackets.getOrDefault(type, 0);
    }

    public int getPlantLevel(PlantType type) {
        return plantLevels.getOrDefault(type, 1);
    }

    public boolean upgradeAllowed(PlantType type) {
        int level = getPlantLevel(type);
        int packetsNeeded = 10 * level;
        int coinsNeeded = 1000 * level;
        return level < 4 && getSeedPackets(type) >= packetsNeeded && coins >= coinsNeeded;
    }

    public void upgradeStats(PlantType type) {
        int level = getPlantLevel(type);
        int packetsNeeded = 10 * level;
        int coinsNeeded = 1000 * level;
        plantSeedPackets.put(type, getSeedPackets(type) - packetsNeeded);
        coins -= coinsNeeded;
        plantLevels.put(type, level + 1);
    }

    public void setPlantBoost(PlantType type, boolean boosted) {
        plantBoosts.put(type, boosted);
    }

    public boolean hasBoost(PlantType type) {
        return plantBoosts.getOrDefault(type, false);
    }

    public void recordMinigameCompletion(String name) {
        int current = minigamesCompleted.getOrDefault(name, 0);
        minigamesCompleted.put(name, current == Integer.MAX_VALUE ? current : current + 1);
    }

    public int getTotalMinigamesCompleted() {
        long total = minigamesCompleted.values().stream().mapToLong(Integer::longValue).sum();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        if (difficulty < 1 || difficulty > 5) {
            throw new IllegalArgumentException("Difficulty must be between 1 and 5.");
        }
        this.difficulty = difficulty;
    }

    public int getPotCount() {
        return potCount;
    }

    private void initializeGreenhouse() {
        greenhousePots = new ArrayList<>();
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                greenhousePots.add(new GreenHousePot(y > 1));
            }
        }
    }

    public GreenHousePot getGreenhousePot(int x, int y) {
        if (x < 1 || x > 5 || y < 1 || y > 4) {
            return null;
        }
        return greenhousePots.get((y - 1) * 5 + x - 1);
    }

    public List<GreenHousePot> getGreenhousePots() {
        return List.copyOf(greenhousePots);
    }

    public boolean unlockNextPot() {
        for (GreenHousePot pot : greenhousePots) {
            if (pot.isLocked()) {
                pot.unlock();
                potCount++;
                return true;
            }
        }
        return false;
    }

    public boolean consumePlantBoost(PlantType type) {
        if (!hasBoost(type)) {
            return false;
        }
        plantBoosts.put(type, false);
        return true;
    }

    public String getDailyOfferPlant() {
        return dailyOfferPlant;
    }

    public void setDailyOfferPlant(String dailyOfferPlant) {
        this.dailyOfferPlant = dailyOfferPlant;
    }

    public boolean isDailyOfferPurchased() {
        return dailyOfferPurchased;
    }

    public void setDailyOfferPurchased(boolean dailyOfferPurchased) {
        this.dailyOfferPurchased = dailyOfferPurchased;
    }

    public int getCompletedLevel(ChapterType chapter) {
        return adventureProgress.getOrDefault(chapter, 0);
    }

    public boolean isChapterUnlocked(ChapterType chapter) {
        if (chapter == ChapterType.ANCIENT_EGYPT) {
            return true;
        }
        ChapterType[] chapters = adventureChapters();
        for (int i = 1; i < chapters.length; i++) {
            if (chapters[i] == chapter) {
                return getCompletedLevel(chapters[i - 1]) >= 4;
            }
        }
        return false;
    }

    public int getNextLevel(ChapterType chapter) {
        return Math.min(4, getCompletedLevel(chapter) + 1);
    }

    public void completeLevel(ChapterType chapter, int level) {
        int previous = getCompletedLevel(chapter);
        if (level > previous) {
            adventureProgress.put(chapter, Math.min(4, level));
            incrementLevelsCompleted();
            addNews("Completed " + chapter.name() + " level " + level);
            if (level == 4) {
                addNews("A new chapter is now available.");
            }
        }
    }

    private ChapterType[] adventureChapters() {
        return new ChapterType[] {ChapterType.ANCIENT_EGYPT, ChapterType.FROSTBITE_CAVES,
            ChapterType.BIG_WAVE_BEACH, ChapterType.DARK_AGES};
    }

    public int getStoredPlantFood() {
        return storedPlantFood;
    }

    public boolean addStoredPlantFood(int amount) {
        long updated = (long) storedPlantFood + amount;
        if (amount <= 0 || updated > 3) {
            return false;
        }
        storedPlantFood = (int) updated;
        return true;
    }

    public int takeStoredPlantFood() {
        int result = storedPlantFood;
        storedPlantFood = 0;
        return result;
    }
}
