package ir.hamgit.ahh.PvZ.model;
import ir.hamgit.ahh.PvZ.model.quest.LevelQuestTelemetry;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.registry.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.SpecialLevelType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.NormalLevelHandler;
import ir.hamgit.ahh.PvZ.model.special.PlantWhatYouGetLevel;
import ir.hamgit.ahh.PvZ.model.special.SpecialLevelHandler;
import ir.hamgit.ahh.PvZ.model.special.TimedWarLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
public class Board {

    public static final int ROWS = 5;
    public static final int COLUMNS = 9;
    public static final int TICKS_PER_SECOND = 10;

    private final Tile[][] tiles;
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final boolean[] lawnMowers;
    private final SunEconomy sunEconomy;
    private final CurrencyLedger currencyLedger = new CurrencyLedger();
    private int plantFoodCount;
    private int tickCount;
    private final ChapterType chapter;
    private final int difficulty;
    private final SpecialLevelHandler specialLevelHandler;
    private final WaveManager waveManager;
    private final ZombieAbilitySupport zombieAbilities = new ZombieAbilitySupport();
    private final BoardCombatOps combatOps = new BoardCombatOps();
    private final PlantingOps plantingOps = new PlantingOps();
    private final ChapterMechanics chapterMechanics;
    private final BoardLevelOps levelOps = new BoardLevelOps();

    public Board(ChapterType chapter, int totalWaves, int difficulty, SpecialLevelType specialLevelType,
                 SpecialLevelHandler specialLevelHandler) {
        this.chapter = chapter;
        this.chapterMechanics = new ChapterMechanics(chapter);
        this.difficulty = difficulty;
        this.specialLevelHandler = specialLevelHandler != null ? specialLevelHandler : new NormalLevelHandler();
        this.tiles = ChapterTerrainFactory.buildTiles(chapter, ROWS, COLUMNS, difficulty);
        this.lawnMowers = new boolean[ROWS];
        java.util.Arrays.fill(lawnMowers, true);
        this.waveManager = new WaveManager(chapter, difficulty, totalWaves);
        this.sunEconomy = new SunEconomy(getDifficultySpeedMultiplier());
        this.specialLevelHandler.onLevelStart(this);
    }

    public void advanceTime(int ticks) {
        for (int i = 0; i < ticks && !levelOps.isGameOver(); i++) {
            advanceOneTick();
        }
    }

    private void advanceOneTick() {
        tickCount++;
        specialLevelHandler.onTick(this);
        tickSunDrop();
        tickZombies();
        tickPlants();
        tickProjectiles();
        tickSuns();
        chapterMechanics.tick(this);
        checkWaveAdvance();
        checkWinLoss();
    }

    private void tickZombies() {
        for (Zombie z : new ArrayList<>(zombies)) {
            z.tick(this);
            if (!z.isAlive() && !z.isDeathHandled()) {
                z.die(this);
            }
        }
        zombies.removeIf(z -> !z.isAlive());
    }

    private void tickPlants() {
        for (Tile[] row : tiles) {
            for (Tile tile : row) {
                if (!tile.isEmpty()) {
                    for (Plant plant : tile.getPlantLayers()) {
                        plant.tick(this);
                    }
                }
            }
        }
    }

    private void tickProjectiles() {
        for (Projectile p : projectiles) {
            p.tick(this);
        }
        projectiles.removeIf(p -> !p.isAlive());
    }

    private void tickSuns() {
        sunEconomy.tickSuns();
    }

    private void tickSunDrop() {
        sunEconomy.tickDrop(this);
    }

    public void spawnProducedSun(Plant source) {
        sunEconomy.spawnProducedSun(source);
    }

    public boolean collectSun(int x, int lane) {
        return sunEconomy.collectSun(this, x, lane);
    }

    public boolean collectFallingSun(int x, int lane) {
        return sunEconomy.collectFallingSun(this, x, lane);
    }

    void notifySunProduced(int amount) {
        if (specialLevelHandler instanceof TimedWarLevel timedWar) {
            timedWar.registerSunProduced(amount);
        }
    }

    public void addSun(int amount) {
        sunEconomy.addSun(amount);
    }

    /** Direct override, used for special levels with a fixed starting pool (Plant What You Get). */
    public void setSunAmount(int amount) {
        sunEconomy.setSunAmount(amount);
    }

    public void explodeRadioactiveSun(int x, int lane) {
        sunEconomy.explodeRadioactiveSun(this, x, lane);
    }

    public boolean plantPlant(PlantType type, int x, int lane) {
        return plantingOps.plantPlant(this, type, x, lane);
    }


    public boolean plantPlant(PlantType type, int x, int lane, int adjustedCost, int level) {
        return plantingOps.plantPlant(this, type, x, lane, adjustedCost, level);
    }

    public boolean plantForFree(PlantType type, int x, int lane) {
        return plantingOps.plantForFree(this, type, x, lane);
    }

    public boolean pluckPlant(int x, int lane) {
        return plantingOps.pluckPlant(this, x, lane);
    }

    public boolean feedPlant(int x, int lane) {
        return plantingOps.feedPlant(this, x, lane);
    }

    public void grantPlantFood() {
        plantingOps.grantPlantFood(this);
    }

    public void cheatAddPlantFood() {
        plantingOps.cheatAddPlantFood(this);
    }

    void spendSun(int cost) {
        sunEconomy.addSun(-cost);
    }

    public void incrementPlantFoodCount() {
        plantFoodCount = Math.min(3, plantFoodCount + 1);
    }

    boolean consumePlantFoodIfAvailable() {
        if (plantFoodCount <= 0) {
            return false;
        }
        plantFoodCount--;
        return true;
    }


    public Zombie getNearestEnemyZombieInFront(Zombie hypnotized) {
        return combatOps.getNearestEnemyZombieInFront(this, hypnotized);
    }

    public Plant getPlantInFrontOf(Zombie zombie) {
        return combatOps.getPlantInFrontOf(this, zombie);
    }


    public void dealAreaDamageToZombies(int centerX, int centerLane, int radius, int damage) {
        combatOps.dealAreaDamageToZombies(this, centerX, centerLane, radius, damage);
    }

    public void handlePlantDestroyed(Plant target) {
        combatOps.handlePlantDestroyed(this, target);
    }

    public void destroyPlantInstantly(Plant target) {
        combatOps.destroyPlantInstantly(this, target);
    }

    public void markDeathHandledIfNeeded(Zombie zombie) {
        combatOps.markDeathHandledIfNeeded(this, zombie);
    }


    public boolean hasPlantWithinTiles(int lane, double x, int range) {
        return zombieAbilities.hasPlantWithinTiles(this, lane, x, range);
    }

    public boolean stealMetalArmorNear(int plantX, int lane, int range) {
        return zombieAbilities.stealMetalArmorNear(this, plantX, lane, range);
    }

    public int stealSun(int amount) {
        return sunEconomy.stealSun(amount);
    }

    public int stealNearestFallingSun(int lane, double x) {
        return sunEconomy.stealNearestFallingSun(lane, x);
    }

    public void laserDestroyPlantsAhead(Zombie source, int range) {
        zombieAbilities.laserDestroyPlantsAhead(this, source, range);
    }

    public void throwIceAtNearestPlant(Zombie hunter) {
        zombieAbilities.throwIceAtNearestPlant(this, hunter);
    }

    public void throwOctopusAtPlant(Zombie octopus) {
        zombieAbilities.throwOctopusAtPlant(this, octopus);
    }

    public void hookPlantTowards(Zombie fisherman) {
        zombieAbilities.hookPlantTowards(this, fisherman);
    }

    public void turnRandomPlantIntoCat(int lane, Zombie wizard) {
        zombieAbilities.turnRandomPlantIntoCat(this, lane, wizard);
    }

    public void revertCatsCastBy(Zombie wizard) {
        zombieAbilities.revertCatsCastBy(wizard);
    }

    public void upgradeNearbyZombieToKnight(Zombie king) {
        zombieAbilities.upgradeNearbyZombieToKnight(this, king);
    }

    public void shuffleRandomZombieRow() {
        zombieAbilities.shuffleRandomZombieRow(this);
    }

    public void spawnRandomGraves(int count) {
        zombieAbilities.spawnRandomGraves(this, count);
    }

    public void damageNearestPlantLeft(int lane, double zombieX, int damage) {
        zombieAbilities.damageNearestPlantLeft(this, lane, zombieX, damage);
    }

    public void destroyPlantsInLane(int lane) {
        zombieAbilities.destroyPlantsInLane(this, lane);
    }

    public void triggerLawnMower(int lane, Zombie triggeringZombie) {
        levelOps.triggerMower(this, lane, triggeringZombie);
    }

    public void spawnZombieAt(ZombieType type, int lane, int x) {
        ZombieDef def = ZombieRegistry.get(type);
        levelOps.spawn(this, def, lane, x, waveManager.getCurrentWave(), difficulty / 3.0);
    }

    public void cheatSpawnZombie(ZombieType type, int x, int lane) {
        spawnZombieAt(type, lane, x);
    }

    public void cheatAddSuns(int n) {
        addSun(n);
    }

    public void cheatReleaseNuke() {
        for (Zombie z : zombies) {
            z.takeDamage(Integer.MAX_VALUE / 2, true);
        }
    }

    private void checkWaveAdvance() {
        waveManager.checkAdvance(this);
    }

    /** Public entry for the "start zombie waves" command (Plant What You Get). */
    public void startZombieWaves() {
        if (specialLevelHandler instanceof PlantWhatYouGetLevel pwyg) {
            pwyg.startWaves();
        }
    }

    private void checkWinLoss() {
        levelOps.checkTerminalState(this, waveManager.getCurrentWave(), waveManager.getTotalWaves(),
            waveManager.hasFinalWaveStarted());
    }


    public void maybeDropCurrency() {
        currencyLedger.maybeDropCurrency();
    }

    public void onZombieKilled(Zombie zombie) {
        levelOps.recordKill();
        LevelQuestTelemetry.recordKill(this, zombie);
        specialLevelHandler.onZombieKilled(this, zombie);
    }

    public int drainCoinsEarned() {
        return currencyLedger.drainCoinsEarned();
    }

    public int drainDiamondsEarned() {
        return currencyLedger.drainDiamondsEarned();
    }

    int getZombieSpawnColumn(boolean finalWave) {
        int column = chapterMechanics.zombieSpawnColumn(this, finalWave);
        if (specialLevelHandler instanceof ir.hamgit.ahh.PvZ.model.special.DeadLineLevel deadLine) {
            return Math.max(column, deadLine.getLineColumn() + 1);
        }
        return column;
    }

    void onWaveStart(int waveNumber) {
        chapterMechanics.onWaveStart(this, waveNumber);
    }

    boolean chapterBlocksNaturalSun() {
        return chapterMechanics.blocksNaturalSun();
    }

    void onGraveDestroyed(Tile tile) {
        chapterMechanics.onGraveDestroyed(this, tile);
    }

    public int drainPotsEarned() {
        return currencyLedger.drainPotsEarned();
    }

    boolean isInBounds(int x, int lane) {
        return x >= 0 && x < COLUMNS && lane >= 0 && lane < ROWS;
    }

    public Tile getTileAt(int x, int lane) {
        return isInBounds(x, lane) ? tiles[lane][x] : null;
    }

    public double getDifficultySpeedMultiplier() {
        return difficulty / 3.0;
    }

    public int getRows() {
        return ROWS;
    }

    public int getColumns() {
        return COLUMNS;
    }

    public int getSunAmount() {
        return sunEconomy.getSunAmount();
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public int getTickCount() {
        return tickCount;
    }

    public int getCurrentWave() {
        return waveManager.getCurrentWave();
    }

    public int getTotalWaves() {
        return waveManager.getTotalWaves();
    }

    public boolean isGameOver() {
        return levelOps.isGameOver();
    }

    public boolean isPlayerWon() {
        return levelOps.isPlayerWon();
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public Set<ZombieType> getEncounteredZombies() {
        return levelOps.getEncountered();
    }

    public void enableBrainMode() {
        levelOps.enableBrainMode();
    }

    public int getZombiesKilled() {
        return levelOps.getZombiesKilled();
    }

    public int getPlantsRemaining() {
        return levelOps.plantsRemaining(this);
    }

    public int getMowersRemaining() {
        return levelOps.mowersRemaining(lawnMowers);
    }

    public boolean[] getLawnMowerAvailability() {
        return lawnMowers;
    }

    public ChapterType getChapter() {
        return chapter;
    }


    public SpecialLevelHandler getSpecialLevelHandler() {
        return specialLevelHandler;
    }
}
