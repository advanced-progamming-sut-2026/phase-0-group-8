package ir.hamgit.ahh.PvZ.model;


import ir.hamgit.ahh.PvZ.model.*;
import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.def.ZombieRegistry;
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
    private boolean isGameOver;
    private boolean playerWon;
    private final SpecialLevelType specialLevelType;
    private final SpecialLevelHandler specialLevelHandler;
    private final WaveManager waveManager;
    private final ZombieAbilitySupport zombieAbilities = new ZombieAbilitySupport();
    private final BoardCombatOps combatOps = new BoardCombatOps();
    private final PlantingOps plantingOps = new PlantingOps();

    public Board(ChapterType chapter, int totalWaves, int difficulty, SpecialLevelType specialLevelType,
                 SpecialLevelHandler specialLevelHandler) {
        this.chapter = chapter;
        this.difficulty = difficulty;
        this.specialLevelType = specialLevelType;
        this.specialLevelHandler = specialLevelHandler != null ? specialLevelHandler : new NormalLevelHandler();
        this.tiles = ChapterTerrainFactory.buildTiles(chapter, ROWS, COLUMNS, difficulty);
        this.lawnMowers = new boolean[ROWS];
        java.util.Arrays.fill(lawnMowers, true);
        this.waveManager = new WaveManager(chapter, difficulty, totalWaves);
        this.sunEconomy = new SunEconomy(getDifficultySpeedMultiplier());
        this.specialLevelHandler.onLevelStart(this);
    }

    // ------------------------------------------------------------------
    // Time advancement - "advance time -t <count> ticks"
    // ------------------------------------------------------------------

    public void advanceTime(int ticks) {
        for (int i = 0; i < ticks && !isGameOver; i++) {
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
                    tile.getPlant().tick(this);
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

    // ------------------------------------------------------------------
    // Sun economy
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Planting / plucking / plant food (see PlantingOps)
    // ------------------------------------------------------------------

    public boolean plantPlant(PlantType type, int x, int lane) {
        return plantingOps.plantPlant(this, type, x, lane);
    }

    /** Places a plant ignoring sun cost - used for level setup (e.g. Save Our Seeds pre-placed plants). */
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

    void incrementPlantFoodCount() {
        plantFoodCount = Math.min(3, plantFoodCount + 1);
    }

    boolean consumePlantFoodIfAvailable() {
        if (plantFoodCount <= 0) {
            return false;
        }
        plantFoodCount--;
        return true;
    }

    // ------------------------------------------------------------------
    // Combat helpers used by Plant/Zombie/Projectile (see BoardCombatOps)
    // ------------------------------------------------------------------

    public boolean hasZombieInLaneAhead(int plantX, int lane, int range) {
        return combatOps.hasZombieInLaneAhead(this, plantX, lane, range);
    }

    public boolean hasAdjacentZombie(int plantX, int lane) {
        return combatOps.hasAdjacentZombie(this, plantX, lane);
    }

    public Zombie findNearestZombieAheadOfProjectile(int lane, double projectileX) {
        return combatOps.findNearestZombieAheadOfProjectile(this, lane, projectileX);
    }

    public Zombie getNearestEnemyZombieInFront(Zombie hypnotized) {
        return combatOps.getNearestEnemyZombieInFront(this, hypnotized);
    }

    public Plant getPlantInFrontOf(Zombie zombie) {
        return combatOps.getPlantInFrontOf(this, zombie);
    }

    public void spawnProjectile(Plant source) {
        combatOps.spawnProjectile(this, source);
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

    // ------------------------------------------------------------------
    // Zombie-specific ability hooks (called from Zombie's per-type methods)
    // ------------------------------------------------------------------

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

    /** Per spec: a cat-transformed plant reverts once the wizard that cast it dies. */
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

    // ------------------------------------------------------------------
    // Lawn mower / zombie spawning / waves
    // ------------------------------------------------------------------

    public void triggerLawnMower(int lane, Zombie triggeringZombie) {
        if (!lawnMowers[lane]) {
            loseGame("The zombie ate your brain; LOSER!!!");
            return;
        }
        lawnMowers[lane] = false;
        List<String> killed = new ArrayList<>();
        for (Zombie z : zombies) {
            if (z.getLane() == lane && z.isAlive() && z.getDef().getType() != ZombieType.GARGANTUAR) {
                killed.add(z.getDef().getType().toString());
                z.forceKill();
            }
        }
        System.out.println("The lawn mower in the row " + lane + " is triggered and killed these zombies:");
        killed.forEach(System.out::println);
    }

    public void spawnZombieAt(ZombieType type, int lane, int x) {
        ZombieDef def = ZombieRegistry.get(type);
        if (def != null) {
            Zombie z = new Zombie(def, lane, x);
            z.setWaveNumber(waveManager.getCurrentWave());
            zombies.add(z);
        }
    }

    public void cheatSpawnZombie(ZombieType type, int x, int lane) {
        spawnZombieAt(type, lane, x);
    }

    public void cheatAddSuns(int n) {
        addSun(n);
    }

    public void cheatRemoveCooldownNoop() {
        // Replant cooldowns are tracked in GameController.plantCooldowns (see reference),
        // so the actual "cheat remove-cooldown" command is handled there.
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
        if (isGameOver) {
            return;
        }
        if (specialLevelHandler.checkCustomLoss(this)) {
            loseGame("Level failed.");
            return;
        }
        if (specialLevelHandler.checkCustomWin(this)) {
            winGame();
            return;
        }
        boolean allWavesCleared = waveManager.getCurrentWave() >= waveManager.getTotalWaves()
            && waveManager.hasFinalWaveStarted() && noZombiesRemain();
        if (allWavesCleared) {
            winGame();
        }
    }

    private boolean noZombiesRemain() {
        return zombies.stream().noneMatch(Zombie::isAlive);
    }

    private void winGame() {
        isGameOver = true;
        playerWon = true;
        System.out.println("Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
    }

    private void loseGame(String message) {
        isGameOver = true;
        playerWon = false;
        System.out.println(message);
    }

    // ------------------------------------------------------------------
    // Currency drops (see CurrencyLedger)
    // ------------------------------------------------------------------

    public void maybeDropCurrency() {
        currencyLedger.maybeDropCurrency();
    }

    public void onZombieKilled(Zombie zombie) {
        specialLevelHandler.onZombieKilled(this, zombie);
    }

    public int drainCoinsEarned() {
        return currencyLedger.drainCoinsEarned();
    }

    public int drainDiamondsEarned() {
        return currencyLedger.drainDiamondsEarned();
    }

    // ------------------------------------------------------------------
    // Printing (delegated to BoardPrinter)
    // ------------------------------------------------------------------

    public void showMap() {
        BoardPrinter.showMap(this);
    }

    public void showPlantsStatus() {
        BoardPrinter.showPlantsStatus(this);
    }

    public void showTileStatus(int x, int lane) {
        BoardPrinter.showTileStatus(this, x, lane);
    }

    public void showSunAmount() {
        BoardPrinter.showSunAmount(this);
    }

    public void showZombiesInfo() {
        BoardPrinter.showZombiesInfo(this);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

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
        return isGameOver;
    }

    public boolean isPlayerWon() {
        return playerWon;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public boolean[] getLawnMowerAvailability() {
        return lawnMowers;
    }

    public ChapterType getChapter() {
        return chapter;
    }

    public SpecialLevelType getSpecialLevelType() {
        return specialLevelType;
    }

    public SpecialLevelHandler getSpecialLevelHandler() {
        return specialLevelHandler;
    }
}
