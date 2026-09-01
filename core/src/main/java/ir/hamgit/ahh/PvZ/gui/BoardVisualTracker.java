package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Projectile;
import ir.hamgit.ahh.PvZ.model.Sun;
import ir.hamgit.ahh.PvZ.model.Tile;
import ir.hamgit.ahh.PvZ.model.entities.Armor;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

 




final class BoardVisualTracker {
    enum Kind {
        PLANT_SPAWN, ZOMBIE_SPAWN, HIT, ICE_HIT, OCTOPUS_HIT, IMPACT,
        EXPLOSION, ARMOR_DEBRIS, HEAD, ARM, DEATH, DUST, MOWER,
        THROWN_IMP, MUZZLE, SUN_PRODUCE, PLANT_ACTION, GRAVE_HIT
    }

    static final class Effect {
        final Kind kind;
        final float column;
        final int lane;
        final float sourceColumn;
        final float start;
        final float duration;
        final Color color;
        final PlantType plantType;
        final ZombieType zombieType;
        final ArmorType armorType;

        Effect(Kind kind, float column, int lane, float sourceColumn, float start,
               float duration, Color color, PlantType plantType, ZombieType zombieType,
               ArmorType armorType) {
            this.kind = kind;
            this.column = column;
            this.lane = lane;
            this.sourceColumn = sourceColumn;
            this.start = start;
            this.duration = duration;
            this.color = color == null ? Color.WHITE : new Color(color);
            this.plantType = plantType;
            this.zombieType = zombieType;
            this.armorType = armorType;
        }

        float progress(float now) {
            return Math.max(0f, Math.min(1f, (now - start) / Math.max(.001f, duration)));
        }

        boolean expired(float now) {
            return now >= start + duration;
        }
    }

    private Board board;
    private final IdentityHashMap<Plant, PlantState> plants = new IdentityHashMap<>();
    private final IdentityHashMap<Zombie, ZombieState> zombies = new IdentityHashMap<>();
    private final IdentityHashMap<Projectile, ProjectileState> projectiles = new IdentityHashMap<>();
    private final IdentityHashMap<Sun, Boolean> suns = new IdentityHashMap<>();
    private final IdentityHashMap<Plant, TimedAction> plantActions = new IdentityHashMap<>();
    private final IdentityHashMap<Zombie, TimedAction> zombieActions = new IdentityHashMap<>();
    private final IdentityHashMap<Plant, Float> plantBirths = new IdentityHashMap<>();
    private final IdentityHashMap<Zombie, Float> zombieBirths = new IdentityHashMap<>();
    private final IdentityHashMap<Zombie, ThrowState> thrownImps = new IdentityHashMap<>();
    private final List<Effect> effects = new ArrayList<>();
    private int[][] graveHp;
    private boolean[] mowers;
    private float shakeUntil;
    private float shakeStrength;
    private Consumer<String> cueListener = cue -> { };

    void setCueListener(Consumer<String> listener) {
        cueListener = listener == null ? cue -> { } : listener;
    }

    void update(Board current, float now) {
        if (current == null) {
            reset(null);
            return;
        }
        if (current != board) {
            reset(current);
        }
        if (now >= shakeUntil) {
            shakeStrength = 0f;
        }
        scanPlants(current, now);
        scanZombies(current, now);
        scanProjectiles(current, now);
        scanSuns(current, now);
        scanTerrain(current, now);
        effects.removeIf(effect -> effect.expired(now));
        plantActions.entrySet().removeIf(entry -> entry.getValue().until < now || !plants.containsKey(entry.getKey()));
        zombieActions.entrySet().removeIf(entry -> entry.getValue().until < now || !zombies.containsKey(entry.getKey()));
        plantBirths.keySet().removeIf(plant -> !plants.containsKey(plant));
        zombieBirths.keySet().removeIf(zombie -> !zombies.containsKey(zombie));
        thrownImps.entrySet().removeIf(entry -> entry.getValue().until < now || !zombies.containsKey(entry.getKey()));
    }

    private void reset(Board next) {
        board = next;
        plants.clear();
        zombies.clear();
        projectiles.clear();
        suns.clear();
        plantActions.clear();
        zombieActions.clear();
        plantBirths.clear();
        zombieBirths.clear();
        thrownImps.clear();
        effects.clear();
        graveHp = next == null ? null : new int[next.getRows()][next.getColumns()];
        if (graveHp != null) {
            for (int lane = 0; lane < next.getRows(); lane++) {
                for (int column = 0; column < next.getColumns(); column++) {
                    graveHp[lane][column] = next.getTileAt(column, lane).getGravestoneHp();
                }
            }
        }
        mowers = next == null ? null : next.getLawnMowerAvailability().clone();
    }

    private void scanPlants(Board current, float now) {
        IdentityHashMap<Plant, Boolean> seen = new IdentityHashMap<>();
        for (int lane = 0; lane < current.getRows(); lane++) {
            for (int column = 0; column < current.getColumns(); column++) {
                Tile tile = current.getTileAt(column, lane);
                for (Plant plant : tile.getPlantLayers()) {
                    seen.put(plant, Boolean.TRUE);
                    PlantState previous = plants.get(plant);
                    if (previous == null) {
                        plantBirths.put(plant, now);
                        setPlantAction(plant, "plant", now + .55f);
                        add(Kind.PLANT_SPAWN, column, lane, column, now, .55f,
                            new Color(.55f, 1f, .35f, .9f), plant.getDef().getType(), null, null);
                        cueListener.accept("plant");
                    } else {
                        if (plant.getCurrentHp() < previous.hp) {
                            Kind kind = plant.isOctopusCovered() ? Kind.OCTOPUS_HIT
                                : plant.isFrozen() ? Kind.ICE_HIT : Kind.HIT;
                            Color color = kind == Kind.OCTOPUS_HIT ? new Color(.75f, .28f, .90f, 1f)
                                : kind == Kind.ICE_HIT ? new Color(.62f, .92f, 1f, 1f)
                                : new Color(1f, .38f, .22f, 1f);
                            add(kind, column, lane, column, now, .32f, color,
                                plant.getDef().getType(), null, null);
                            setPlantAction(plant, "hurt", now + .25f);
                            cueListener.accept("hit");
                        }
                        if (plant.getIceShellHp() < previous.iceHp) {
                            Kind kind = plant.isOctopusCovered() ? Kind.OCTOPUS_HIT : Kind.ICE_HIT;
                            add(kind, column, lane, column, now, .38f,
                                kind == Kind.OCTOPUS_HIT ? new Color(.78f, .32f, .92f, 1f)
                                    : new Color(.66f, .95f, 1f, 1f),
                                plant.getDef().getType(), null, null);
                        }
                    }
                    plants.put(plant, new PlantState(plant));
                }
            }
        }
        for (Map.Entry<Plant, PlantState> entry : new ArrayList<>(plants.entrySet())) {
            if (seen.containsKey(entry.getKey())) {
                continue;
            }
            PlantState state = entry.getValue();
            boolean explosive = state.explosive;
            boolean pounce = state.type == PlantType.SQUASH;
            if (explosive) {
                add(Kind.EXPLOSION, state.x, state.lane, state.x, now, .65f,
                    new Color(1f, .42f, .08f, 1f), state.type, null, null);
                shake(now, .46f, 8f);
                cueListener.accept("explosion");
            } else if (pounce || state.instantAction) {
                add(Kind.PLANT_ACTION, state.x, state.lane, state.x, now, .72f,
                    new Color(.50f, .92f, .25f, 1f), state.type, null, null);
                if (pounce) shake(now, .25f, 4f);
                cueListener.accept("special");
            } else {
                add(Kind.DUST, state.x, state.lane, state.x, now, .52f,
                    new Color(.45f, .68f, .26f, 1f), state.type, null, null);
            }
            plants.remove(entry.getKey());
        }
    }

    private void scanZombies(Board current, float now) {
        IdentityHashMap<Zombie, Boolean> seen = new IdentityHashMap<>();
        for (Zombie zombie : current.getZombies()) {
            seen.put(zombie, Boolean.TRUE);
            ZombieState previous = zombies.get(zombie);
            if (previous == null) {
                zombieBirths.put(zombie, now);
                add(Kind.ZOMBIE_SPAWN, (float) zombie.getX(), zombie.getLane(), (float) zombie.getX(),
                    now, .50f, new Color(.55f, .70f, .40f, 1f), null,
                    zombie.getDef().getType(), null);
                if (zombie.getDef().getType() == ZombieType.IMP) {
                    Zombie gargantuar = findGargantuar(current, zombie.getLane(), zombie.getX());
                    if (gargantuar != null) {
                        thrownImps.put(zombie, new ThrowState((float) gargantuar.getX(), now, now + .75f));
                        setZombieAction(gargantuar, "special", now + .70f);
                        add(Kind.THROWN_IMP, (float) zombie.getX(), zombie.getLane(),
                            (float) gargantuar.getX(), now, .75f, Color.WHITE, null,
                            ZombieType.IMP, null);
                        cueListener.accept("special");
                    }
                }
            } else {
                if (zombie.getCurrentHp() < previous.hp) {
                    add(zombie.isFrozen() ? Kind.ICE_HIT : Kind.HIT, (float) zombie.getX(), zombie.getLane(),
                        (float) zombie.getX(), now, .30f,
                        zombie.isFrozen() ? new Color(.62f, .94f, 1f, 1f)
                            : new Color(1f, .44f, .22f, 1f), null, zombie.getDef().getType(), null);
                    cueListener.accept("impact");
                    markLikelyPlantAttacker(current, zombie, now);
                    if (!previous.armDropped && zombie.getCurrentHp() <= zombie.getDef().getMaxHp() / 2) {
                        add(Kind.ARM, (float) zombie.getX(), zombie.getLane(), (float) zombie.getX(),
                            now, .80f, new Color(.42f, .56f, .32f, 1f), null,
                            zombie.getDef().getType(), null);
                    }
                }
                scanArmor(zombie, previous, now);
                scanSpecialTimers(zombie, previous, now);
            }
            zombies.put(zombie, new ZombieState(zombie));
        }
        for (Map.Entry<Zombie, ZombieState> entry : new ArrayList<>(zombies.entrySet())) {
            if (seen.containsKey(entry.getKey())) {
                continue;
            }
            ZombieState state = entry.getValue();
            add(Kind.DEATH, state.x, state.lane, state.x, now, 1.05f,
                new Color(.48f, .58f, .35f, 1f), null, state.type, null);
            add(Kind.HEAD, state.x, state.lane, state.x, now, .95f,
                new Color(.43f, .57f, .33f, 1f), null, state.type, null);
            if (!state.armDropped) {
                add(Kind.ARM, state.x, state.lane, state.x, now, .90f,
                    new Color(.40f, .53f, .31f, 1f), null, state.type, null);
            }
            add(Kind.DUST, state.x, state.lane, state.x, now, 1.10f,
                new Color(.55f, .50f, .36f, 1f), null, state.type, null);
            if (state.large) {
                shake(now, .38f, 6f);
            }
            cueListener.accept("hit");
            zombies.remove(entry.getKey());
        }
    }

    private Zombie findGargantuar(Board current, int lane, double impX) {
        Zombie best = null;
        for (Zombie candidate : current.getZombies()) {
            boolean garg = candidate.getDef().getType() == ZombieType.GARGANTUAR
                || candidate.getDef().getType().name().contains("GARGANTUAR");
            if (garg && candidate.getLane() == lane && candidate.getX() > impX
                && (best == null || candidate.getX() < best.getX())) {
                best = candidate;
            }
        }
        return best;
    }

    private void markLikelyPlantAttacker(Board current, Zombie target, float now) {
        Plant nearest = null;
        double best = Double.MAX_VALUE;
        for (int lane = 0; lane < current.getRows(); lane++) {
            for (int column = 0; column < current.getColumns(); column++) {
                for (Plant plant : current.getTileAt(column, lane).getPlantLayers()) {
                    double distance = Math.abs(target.getX() - column) + Math.abs(target.getLane() - lane) * 1.5;
                    if (distance < best) {
                        best = distance;
                        nearest = plant;
                    }
                }
            }
        }
        if (nearest != null && best <= 4.5) {
            setPlantAction(nearest, "attack", now + .42f);
        }
    }

    private void scanArmor(Zombie zombie, ZombieState previous, float now) {
        for (Armor armor : zombie.getArmors()) {
            Integer oldHp = previous.armorHp.get(armor);
            if (oldHp == null || armor.getCurrentHp() >= oldHp) {
                continue;
            }
            add(Kind.HIT, (float) zombie.getX(), zombie.getLane(), (float) zombie.getX(), now,
                .24f, new Color(.92f, .82f, .55f, 1f), null, zombie.getDef().getType(), armor.getType());
            if (oldHp > 0 && armor.isDestroyed()) {
                add(Kind.ARMOR_DEBRIS, (float) zombie.getX(), zombie.getLane(),
                    (float) zombie.getX(), now, 1f, armorColor(armor.getType()), null,
                    zombie.getDef().getType(), armor.getType());
                cueListener.accept("impact");
            }
        }
    }

    private void scanSpecialTimers(Zombie zombie, ZombieState previous, float now) {
        for (Map.Entry<String, Integer> timer : zombie.getActiveEffects().entrySet()) {
            Integer old = previous.effects.get(timer.getKey());
            if (old != null && timer.getValue() > old + 1) {
                setZombieAction(zombie, "special", now + .70f);
                cueListener.accept("special");
            }
        }
        if (zombie.getActiveEffects().containsKey("stealing")) {
            setZombieAction(zombie, "special", now + .18f);
        }
    }

    private void scanProjectiles(Board current, float now) {
        IdentityHashMap<Projectile, Boolean> seen = new IdentityHashMap<>();
        for (Projectile projectile : current.getProjectiles()) {
            seen.put(projectile, Boolean.TRUE);
            if (!projectiles.containsKey(projectile)) {
                Plant owner = plantAt(current, projectile.getStartX(), projectile.getLane());
                if (owner != null) {
                    setPlantAction(owner, "attack", now + .38f);
                }
                add(Kind.MUZZLE, projectile.getStartX(), projectile.getLane(), projectile.getStartX(),
                    now, .22f, projectileColor(projectile), projectile.getOwnerType(), null, null);
            }
            projectiles.put(projectile, new ProjectileState(projectile));
        }
        for (Map.Entry<Projectile, ProjectileState> entry : new ArrayList<>(projectiles.entrySet())) {
            if (seen.containsKey(entry.getKey())) {
                continue;
            }
            ProjectileState state = entry.getValue();
            Kind kind = state.splash ? Kind.EXPLOSION : Kind.IMPACT;
            add(kind, state.x, state.lane, state.x, now, state.splash ? .55f : .28f,
                state.color, null, null, null);
            if (state.splash) {
                shake(now, .22f, 3.5f);
                cueListener.accept("explosion");
            } else {
                cueListener.accept("impact");
            }
            projectiles.remove(entry.getKey());
        }
    }

    private Plant plantAt(Board current, int x, int lane) {
        Tile tile = current.getTileAt(x, lane);
        return tile == null ? null : tile.getPlant();
    }

    private void scanSuns(Board current, float now) {
        IdentityHashMap<Sun, Boolean> seen = new IdentityHashMap<>();
        for (Sun sun : current.getSuns()) {
            if (sun.isCollected()) {
                continue;
            }
            seen.put(sun, Boolean.TRUE);
            if (!suns.containsKey(sun)) {
                Plant producer = plantAt(current, sun.getX(), sun.getLane());
                if (producer != null) {
                    setPlantAction(producer, "produce", now + .55f);
                    add(Kind.SUN_PRODUCE, sun.getX(), sun.getLane(), sun.getX(), now, .65f,
                        new Color(1f, .90f, .18f, 1f), producer.getDef().getType(), null, null);
                }
            }
        }
        suns.clear();
        suns.putAll(seen);
    }

    private void scanTerrain(Board current, float now) {
        for (int lane = 0; lane < current.getRows(); lane++) {
            for (int column = 0; column < current.getColumns(); column++) {
                int hp = current.getTileAt(column, lane).getGravestoneHp();
                int old = graveHp[lane][column];
                if (hp > 0 && old > 0 && hp < old) {
                    add(Kind.GRAVE_HIT, column, lane, column, now, .34f,
                        new Color(.90f, .82f, .62f, 1f), null, null, null);
                    cueListener.accept("impact");
                }
                graveHp[lane][column] = hp;
            }
        }
        boolean[] currentMowers = current.getLawnMowerAvailability();
        for (int lane = 0; lane < Math.min(mowers.length, currentMowers.length); lane++) {
            if (mowers[lane] && !currentMowers[lane]) {
                add(Kind.MOWER, -.55f, lane, -.55f, now, 1.45f,
                    new Color(.82f, .12f, .08f, 1f), null, null, null);
                shake(now, .55f, 7f);
                cueListener.accept("mower");
            }
        }
        mowers = currentMowers.clone();
    }

    String plantAction(Plant plant, float now) {
        TimedAction action = plantActions.get(plant);
        return action != null && action.until >= now ? action.name : "idle";
    }

    String zombieAction(Zombie zombie, boolean eating, float now) {
        TimedAction action = zombieActions.get(zombie);
        if (action != null && action.until >= now) {
            return action.name;
        }
        if (eating && (zombie.getDef().getType() == ZombieType.GARGANTUAR
            || zombie.getDef().getType() == ZombieType.ALL_STAR)) {
            return "special";
        }
        if (!eating && zombie.getDef().getType() == ZombieType.SNORKEL) {
            return "special";
        }
        return eating ? "eat" : "walk";
    }

    float plantSpawnScale(Plant plant, float now) {
        Float birth = plantBirths.get(plant);
        if (birth == null) return 1f;
        float p = Math.min(1f, (now - birth) / .45f);
        return .20f + 1.08f * bounceOut(p);
    }

    float zombieSpawnScale(Zombie zombie, float now) {
        Float birth = zombieBirths.get(zombie);
        if (birth == null) return 1f;
        float p = Math.min(1f, (now - birth) / .42f);
        return .30f + .78f * bounceOut(p);
    }

    float zombieDisplayColumn(Zombie zombie, float now) {
        ThrowState thrown = thrownImps.get(zombie);
        if (thrown == null) return (float) zombie.getX();
        float p = Math.max(0f, Math.min(1f, (now - thrown.start) / (thrown.until - thrown.start)));
        return thrown.from + ((float) zombie.getX() - thrown.from) * p;
    }

    float zombieThrowArc(Zombie zombie, float now) {
        ThrowState thrown = thrownImps.get(zombie);
        if (thrown == null) return 0f;
        float p = Math.max(0f, Math.min(1f, (now - thrown.start) / (thrown.until - thrown.start)));
        return (float) Math.sin(Math.PI * p);
    }

    float hitFlash(Plant plant, float now) {
        return recentEffectFor(plant.getX(), plant.getLane(), now, true);
    }

    float hitFlash(Zombie zombie, float now) {
        return recentEffectFor((float) zombie.getX(), zombie.getLane(), now, false);
    }

    private float recentEffectFor(float column, int lane, float now, boolean plant) {
        float result = 0f;
        for (Effect effect : effects) {
            if (effect.lane == lane && Math.abs(effect.column - column) < .72f
                && (effect.kind == Kind.HIT || effect.kind == Kind.ICE_HIT || effect.kind == Kind.OCTOPUS_HIT)) {
                result = Math.max(result, 1f - effect.progress(now));
            }
        }
        return result;
    }

    List<Effect> effects() {
        return effects;
    }

    float shakeX(float now) {
        if (now >= shakeUntil) return 0f;
        float fade = (shakeUntil - now) / Math.max(.001f, shakeUntil - (shakeUntil - .55f));
        return (float) Math.sin(now * 87f) * shakeStrength * Math.min(1f, fade);
    }

    float shakeY(float now) {
        if (now >= shakeUntil) return 0f;
        float fade = Math.min(1f, (shakeUntil - now) / .35f);
        return (float) Math.cos(now * 71f) * shakeStrength * .65f * fade;
    }

    private void setPlantAction(Plant plant, String name, float until) {
        TimedAction current = plantActions.get(plant);
        if (current == null || until >= current.until) plantActions.put(plant, new TimedAction(name, until));
    }

    private void setZombieAction(Zombie zombie, String name, float until) {
        TimedAction current = zombieActions.get(zombie);
        if (current == null || until >= current.until) zombieActions.put(zombie, new TimedAction(name, until));
    }

    private void shake(float now, float duration, float strength) {
        shakeUntil = Math.max(shakeUntil, now + duration);
        shakeStrength = Math.max(shakeStrength, strength);
    }

    private void add(Kind kind, float column, int lane, float sourceColumn, float now,
                     float duration, Color color, PlantType plantType, ZombieType zombieType,
                     ArmorType armorType) {
        effects.add(new Effect(kind, column, lane, sourceColumn, now, duration,
            color, plantType, zombieType, armorType));
    }

    private Color projectileColor(Projectile projectile) {
        if (projectile.isFire()) return new Color(1f, .30f, .06f, 1f);
        if (projectile.isIce()) return new Color(.50f, .90f, 1f, 1f);
        if (projectile.isPoison()) return new Color(.52f, .88f, .22f, 1f);
        return new Color(.58f, .95f, .34f, 1f);
    }

    private Color armorColor(ArmorType type) {
        return switch (type) {
            case CONE -> new Color(1f, .45f, .08f, 1f);
            case BUCKET, HELMET -> new Color(.65f, .70f, .75f, 1f);
            case BLOCK -> new Color(.55f, .85f, .95f, 1f);
            case NEWSPAPER -> new Color(.93f, .91f, .78f, 1f);
            default -> new Color(.56f, .36f, .20f, 1f);
        };
    }

    private float bounceOut(float p) {
        float value = Math.max(0f, Math.min(1f, p));
        if (value < 1f / 2.75f) return 7.5625f * value * value;
        if (value < 2f / 2.75f) { value -= 1.5f / 2.75f; return 7.5625f * value * value + .75f; }
        if (value < 2.5f / 2.75f) { value -= 2.25f / 2.75f; return 7.5625f * value * value + .9375f; }
        value -= 2.625f / 2.75f;
        return 7.5625f * value * value + .984375f;
    }

    private record TimedAction(String name, float until) { }
    private record ThrowState(float from, float start, float until) { }

    private static final class PlantState {
        final int hp;
        final int iceHp;
        final int x;
        final int lane;
        final PlantType type;
        final boolean explosive;
        final boolean instantAction;

        PlantState(Plant plant) {
            hp = plant.getCurrentHp();
            iceHp = plant.getIceShellHp();
            x = plant.getX();
            lane = plant.getLane();
            type = plant.getDef().getType();
            explosive = plant.getDef().hasBehavior(BehaviorType.CONTACT_EXPLOSION)
                || plant.getDef().hasBehavior(BehaviorType.AREA_EXPLOSION)
                || plant.getDef().hasBehavior(BehaviorType.LANE_EXPLOSION)
                || plant.getDef().hasBehavior(BehaviorType.BOARD_EXPLOSION)
                || plant.getDef().hasBehavior(BehaviorType.EXPLODE_ON_DEATH);
            instantAction = plant.getDef().hasBehavior(BehaviorType.DISAPPEAR_AFTER_ACTION);
        }
    }

    private static final class ZombieState {
        final int hp;
        final float x;
        final int lane;
        final ZombieType type;
        final IdentityHashMap<Armor, Integer> armorHp = new IdentityHashMap<>();
        final Map<String, Integer> effects = new HashMap<>();
        final boolean armDropped;
        final boolean large;

        ZombieState(Zombie zombie) {
            hp = zombie.getCurrentHp();
            x = (float) zombie.getX();
            lane = zombie.getLane();
            type = zombie.getDef().getType();
            for (Armor armor : zombie.getArmors()) armorHp.put(armor, armor.getCurrentHp());
            effects.putAll(zombie.getActiveEffects());
            armDropped = hp <= zombie.getDef().getMaxHp() / 2;
            large = type == ZombieType.GARGANTUAR || type.name().contains("GARGANTUAR");
        }
    }

    private static final class ProjectileState {
        final float x;
        final int lane;
        final boolean splash;
        final Color color;

        ProjectileState(Projectile projectile) {
            x = (float) projectile.getX();
            lane = projectile.getLane();
            splash = projectile.getSplashRadius() > 0;
            color = projectile.isFire() ? new Color(1f, .30f, .06f, 1f)
                : projectile.isIce() ? new Color(.50f, .90f, 1f, 1f)
                : projectile.isPoison() ? new Color(.52f, .88f, .22f, 1f)
                : new Color(.58f, .95f, .34f, 1f);
        }
    }
}
