package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.Map;

/**
 * Per-species "special ability" methods - Gargantuar's imp throw, Turquoise's
 * steal+laser, Prospector's dynamite, Pianist's row shuffle, Barrel Roller's
 * imp spawn, Ra's sun theft, Tombraiser's graves, Hunter's freeze, Fisherman's
 * hook, Octopus's throw, Jester's spin countdown, Wizard's cat spell, King's
 * knight upgrade - dispatched once per tick from {@link Zombie#tick}.
 *
 * <p>Split out of {@link Zombie} purely to keep it under the project's
 * class-length Checkstyle/PMD guideline. This tightly-coupled state (which
 * timer is counting down, how much sun was stolen, ...) stays conceptually
 * part of Zombie - it's reached through a handful of narrow package-private
 * accessors added to Zombie specifically for this split (getActiveEffects,
 * addStolenSun, setHasThrownImp, ...), the same pattern used for Board's
 * splits (ZombieAbilitySupport, BoardCombatOps, PlantingOps, SunEconomy).</p>
 */
class ZombieSpecialBehaviors {

    private static final int WIZARD_CAST_INTERVAL_TICKS = 80;
    private static final int KING_UPGRADE_INTERVAL_TICKS = 100;
    private static final int STEAL_DURATION_TICKS = 50;
    private static final int STEAL_TICK_INTERVAL = 10;
    private static final int STEAL_AMOUNT_PER_SECOND = 25;
    private static final int LASER_RANGE_TILES = 4;
    private static final int DYNAMITE_FUSE_TICKS = 100;
    private static final int PIANO_SHUFFLE_INTERVAL_TICKS = 60;
    private static final int BONE_THROW_INTERVAL_TICKS = 70;
    private static final int ICE_THROW_INTERVAL_TICKS = 60;
    private static final int HOOK_INTERVAL_TICKS = 40;
    private static final int OCTOPUS_THROW_INTERVAL_TICKS = 90;

    void run(Zombie zombie, model.Board board) {
        switch (zombie.getDef().getType()) {
            case GARGANTUAR -> runGargantuarBehavior(zombie, board);
            case ARCADE, TROGLOBITE -> runPushObjectBehavior(zombie, board);
            case TURQUOISE -> runTurquoiseBehavior(zombie, board);
            case PROSPECTOR -> runProspectorBehavior(zombie);
            case PIANIST -> runPianistBehavior(zombie, board);
            case BARREL_ROLLER -> runBarrelRollerBehavior(zombie, board);
            case RA_ZOMBIE -> runRaZombieBehavior(zombie, board);
            case TOMBRAISER -> runTombraiserBehavior(zombie, board);
            case HUNTER -> runHunterBehavior(zombie, board);
            case FISHERMAN -> runFishermanBehavior(zombie, board);
            case OCTOPUS -> runOctopusBehavior(zombie, board);
            case JESTER -> runJesterBehavior(zombie);
            case WIZARD -> runWizardBehavior(zombie, board);
            case KING -> runKingBehavior(zombie, board);
            default -> { }
        }
    }

    private boolean tickTimer(Zombie zombie, String key, int interval) {
        Map<String, Integer> effects = zombie.getActiveEffects();
        int remaining = effects.getOrDefault(key, interval);
        remaining--;
        if (remaining <= 0) {
            effects.put(key, interval);
            return true;
        }
        effects.put(key, remaining);
        return false;
    }

    private void runGargantuarBehavior(Zombie zombie, model.Board board) {
        boolean atHalfHealth = zombie.getCurrentHp() <= zombie.getDef().getMaxHp() / 2;
        if (!zombie.hasThrownImp() && atHalfHealth) {
            board.spawnZombieAt(ZombieType.IMP, zombie.getLane(), 2);
            zombie.setHasThrownImp(true);
        }
    }

    private void runPushObjectBehavior(Zombie zombie, model.Board board) {
        Plant p = board.getPlantInFrontOf(zombie);
        if (p != null) {
            board.destroyPlantInstantly(p);
        }
    }

    private void runTurquoiseBehavior(Zombie zombie, model.Board board) {
        Map<String, Integer> effects = zombie.getActiveEffects();
        if (effects.containsKey("stealing")) {
            continueStealing(zombie, board);
        } else if (board.hasPlantWithinTiles(zombie.getLane(), zombie.getX(), LASER_RANGE_TILES)) {
            effects.put("stealing", STEAL_DURATION_TICKS);
        }
    }

    private void continueStealing(Zombie zombie, model.Board board) {
        Map<String, Integer> effects = zombie.getActiveEffects();
        int ticksLeft = effects.get("stealing") - 1;
        if (ticksLeft % STEAL_TICK_INTERVAL == 0) {
            zombie.addStolenSun(board.stealSun(STEAL_AMOUNT_PER_SECOND));
        }
        if (ticksLeft <= 0) {
            board.laserDestroyPlantsAhead(zombie, LASER_RANGE_TILES);
            effects.remove("stealing");
        } else {
            effects.put("stealing", ticksLeft);
        }
    }

    private void runProspectorBehavior(Zombie zombie) {
        if (zombie.isDynamiteActive() && tickTimer(zombie, "dynamiteFuse", DYNAMITE_FUSE_TICKS)) {
            zombie.setReversed(true);
            zombie.setDynamiteActive(false);
        }
    }

    private void runPianistBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "pianoShuffle", PIANO_SHUFFLE_INTERVAL_TICKS)) {
            board.shuffleRandomZombieRow();
        }
    }

    private void runBarrelRollerBehavior(Zombie zombie, model.Board board) {
        boolean barrelAlive = zombie.getArmors().stream()
            .anyMatch(a -> a.getType() == ArmorType.BARREL && !a.isDestroyed());
        if (!barrelAlive && !zombie.isImpsSpawned() && zombie.isHadBarrel()) {
            board.spawnZombieAt(ZombieType.IMP, zombie.getLane(), (int) zombie.getX());
            board.spawnZombieAt(ZombieType.IMP, zombie.getLane(), (int) zombie.getX());
            zombie.setImpsSpawned(true);
        }
        zombie.setHadBarrel(barrelAlive || zombie.isHadBarrel());
    }

    private void runRaZombieBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "raSteal", STEAL_TICK_INTERVAL)) {
            zombie.addStolenSun(board.stealNearestFallingSun(zombie.getLane(), zombie.getX()));
        }
    }

    private void runTombraiserBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "boneThrow", BONE_THROW_INTERVAL_TICKS)) {
            board.spawnRandomGraves(2);
        }
    }

    private void runHunterBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "iceThrow", ICE_THROW_INTERVAL_TICKS)) {
            board.throwIceAtNearestPlant(zombie);
        }
    }

    private void runFishermanBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "hook", HOOK_INTERVAL_TICKS)) {
            board.hookPlantTowards(zombie);
        }
    }

    private void runOctopusBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "octopusThrow", OCTOPUS_THROW_INTERVAL_TICKS)) {
            board.throwOctopusAtPlant(zombie);
        }
    }

    private void runJesterBehavior(Zombie zombie) {
        Map<String, Integer> effects = zombie.getActiveEffects();
        int cooldown = effects.getOrDefault("spinCooldown", 0);
        if (cooldown > 0) {
            effects.put("spinCooldown", cooldown - 1);
        }
    }

    private void runWizardBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "wizardCast", WIZARD_CAST_INTERVAL_TICKS)) {
            board.turnRandomPlantIntoCat(zombie.getLane(), zombie);
        }
    }

    private void runKingBehavior(Zombie zombie, model.Board board) {
        if (tickTimer(zombie, "kingUpgrade", KING_UPGRADE_INTERVAL_TICKS)) {
            board.upgradeNearbyZombieToKnight(zombie);
        }
    }
}
