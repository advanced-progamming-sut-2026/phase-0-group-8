package ir.hamgit.ahh.PvZ.server;

import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.EntityState;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchCommand;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchSnapshot;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ProjectileState;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ReactionView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

 
final class AuthoritativeIZombieMatch {
    static final int TICKS_PER_SECOND = 10;
    static final int DURATION_TICKS = 120 * TICKS_PER_SECOND;
    static final Set<String> TEXT_REACTIONS = Set.of("Good luck!", "Well played!", "Brains are mine!");
    static final Set<String> EMOJI_REACTIONS = Set.of("😀", "😱", "🧟");

    private final String id;
    private final String plantUsername;
    private final String zombieUsername;
    private final boolean[] brains = {true, true, true, true, true};
    private final List<PlantUnit> plants = new ArrayList<>();
    private final List<ZombieUnit> zombies = new ArrayList<>();
    private final List<Shot> shots = new ArrayList<>();
    private final List<ReactionView> reactions = new ArrayList<>();
    private final Map<String, Integer> plantCooldowns = new LinkedHashMap<>();
    private final Map<String, Integer> zombieCooldowns = new LinkedHashMap<>();
    private long nextId = 1;
    private long revision;
    private int elapsedTicks;
    private int plantSun = 350;
    private int zombieSun = 400;
    private String status = "ACTIVE";
    private String winner;
    private String reason;
    private boolean resultRecorded;

    AuthoritativeIZombieMatch(String id, String plantUsername, String zombieUsername) {
        this.id = id;
        this.plantUsername = plantUsername;
        this.zombieUsername = zombieUsername;
    }

    synchronized MatchSnapshot command(String username, MatchCommand command) {
        requireParticipant(username);
        requireActive();
        if (command == null || command.action() == null || command.kind() == null) {
            throw ApiException.badRequest("A complete match command is required.");
        }
        if (command.revision() + 30 < revision) {
            throw ApiException.conflict("Your board state is too old; wait for synchronization.");
        }
        if (command.lane() < 0 || command.lane() >= 5) throw ApiException.badRequest("Lane must be 0-4.");
        String action = command.action().trim().toUpperCase(Locale.ROOT);
        String kind = command.kind().trim().toUpperCase(Locale.ROOT);
        if ("PLANT".equals(action)) placePlant(username, kind, command.column(), command.lane());
        else if ("ZOMBIE".equals(action)) placeZombie(username, kind, command.column(), command.lane());
        else throw ApiException.badRequest("Unknown match action.");
        revision++;
        return snapshot();
    }

    private void placePlant(String username, String kind, int column, int lane) {
        if (!plantUsername.equalsIgnoreCase(username)) {
            throw ApiException.forbidden("Only the plant player can place plants.");
        }
        PlantStats stats = plantStats(kind);
        if (column < 1 || column > 6) throw ApiException.badRequest("Plants belong in columns 1-6.");
        if (cooldown(plantCooldowns, kind) > 0) throw ApiException.conflict("That seed packet is recharging.");
        if (plantSun < stats.cost) throw ApiException.conflict("Not enough plant sun.");
        for (PlantUnit plant : plants) {
            if (plant.alive() && plant.column == column && plant.lane == lane) {
                throw ApiException.conflict("That tile already contains a plant.");
            }
        }
        plantSun -= stats.cost;
        plantCooldowns.put(kind, stats.recharge);
        plants.add(new PlantUnit(nextId++, kind, column, lane, stats.health, stats.health));
    }

    private void placeZombie(String username, String kind, int column, int lane) {
        if (!zombieUsername.equalsIgnoreCase(username)) {
            throw ApiException.forbidden("Only the zombie player can place zombies.");
        }
        ZombieStats stats = zombieStats(kind);
        if (column < 7 || column > 8) throw ApiException.badRequest("Zombies enter in columns 7-8.");
        if (cooldown(zombieCooldowns, kind) > 0) throw ApiException.conflict("That zombie is recharging.");
        if (zombieSun < stats.cost) throw ApiException.conflict("Not enough zombie sun.");
        zombieSun -= stats.cost;
        zombieCooldowns.put(kind, stats.recharge);
        zombies.add(new ZombieUnit(nextId++, kind, lane, column + .45, stats.health, stats.health,
            stats.speed, stats.damage, 0, 0));
    }

    synchronized MatchSnapshot react(String username, String kind, String value) {
        requireParticipant(username);
        requireActive();
        String normalized = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        boolean allowed = switch (normalized) {
            case "TEXT" -> TEXT_REACTIONS.contains(value);
            case "EMOJI" -> EMOJI_REACTIONS.contains(value);
            default -> false;
        };
        if (!allowed) throw ApiException.badRequest("Reaction is not in the allowed Phase 3 set.");
        reactions.add(new ReactionView(nextId++, username, normalized, value, System.currentTimeMillis()));
        while (reactions.size() > 20) reactions.remove(0);
        revision++;
        return snapshot();
    }

    synchronized void tick() {
        if (!isActive()) return;
        elapsedTicks++;
        revision++;
        tickCooldowns(plantCooldowns);
        tickCooldowns(zombieCooldowns);
        if (elapsedTicks % 40 == 0) plantSun = Math.min(9999, plantSun + 25);
        if (elapsedTicks % 50 == 0) zombieSun = Math.min(9999, zombieSun + 50);
        tickPlants();
        tickShots();
        tickZombies();
        plants.removeIf(p -> !p.alive());
        zombies.removeIf(z -> !z.alive() || z.finished);
        if (allBrainsEaten()) finish("ZOMBIES", "All five brains were eaten.");
        else if (elapsedTicks >= DURATION_TICKS) finish("PLANTS", "The plants survived for two minutes.");
    }

    private void tickPlants() {
        for (PlantUnit plant : plants) {
            if (!plant.alive()) continue;
            plant.actionTicks++;
            if ("SUNFLOWER".equals(plant.kind)) {
                if (plant.actionTicks >= 80) { plant.actionTicks = 0; plantSun = Math.min(9999, plantSun + 50); }
                continue;
            }
            if ("WALL_NUT".equals(plant.kind)) continue;
            int rate = "SNOW_PEA".equals(plant.kind) ? 20 : 15;
            if (plant.actionTicks < rate || !zombieAhead(plant.lane, plant.column)) continue;
            plant.actionTicks = 0;
            boolean frozen = "SNOW_PEA".equals(plant.kind);
            shots.add(new Shot(nextId++, plant.lane, plant.column + .55, frozen ? 18 : 20, frozen));
        }
    }

    private boolean zombieAhead(int lane, double x) {
        for (ZombieUnit zombie : zombies) if (zombie.alive() && zombie.lane == lane && zombie.x > x) return true;
        return false;
    }

    private void tickShots() {
        Iterator<Shot> iterator = shots.iterator();
        while (iterator.hasNext()) {
            Shot shot = iterator.next();
            shot.x += .42;
            ZombieUnit hit = null;
            for (ZombieUnit zombie : zombies) {
                if (zombie.alive() && zombie.lane == shot.lane && zombie.x >= shot.x - .35 && zombie.x <= shot.x + .35) {
                    hit = zombie;
                    break;
                }
            }
            if (hit != null) {
                hit.health -= shot.damage;
                if (shot.frozen) hit.slowTicks = 35;
                iterator.remove();
            } else if (shot.x > 9.5) iterator.remove();
        }
    }

    private void tickZombies() {
        for (ZombieUnit zombie : zombies) {
            if (!zombie.alive()) continue;
            PlantUnit target = targetFor(zombie);
            if (target != null) {
                if (zombie.attackTicks > 0) zombie.attackTicks--;
                else { target.health -= zombie.damage; zombie.attackTicks = 10; }
            } else {
                double modifier = zombie.slowTicks > 0 ? .5 : 1.0;
                zombie.x -= zombie.speed * modifier;
                if (zombie.slowTicks > 0) zombie.slowTicks--;
            }
            if (zombie.x <= .10) {
                if (brains[zombie.lane]) brains[zombie.lane] = false;
                zombie.finished = true;
            }
        }
    }

    private PlantUnit targetFor(ZombieUnit zombie) {
        PlantUnit best = null;
        for (PlantUnit plant : plants) {
            if (!plant.alive() || plant.lane != zombie.lane || plant.column > zombie.x) continue;
            double gap = zombie.x - plant.column;
            if (gap <= .65 && (best == null || plant.column > best.column)) best = plant;
        }
        return best;
    }

    synchronized MatchSnapshot snapshot() {
        List<EntityState> plantViews = new ArrayList<>();
        for (PlantUnit p : plants) if (p.alive()) plantViews.add(new EntityState(p.id, p.kind, p.column,
            p.lane, p.column, p.health, p.maxHealth, false));
        List<EntityState> zombieViews = new ArrayList<>();
        for (ZombieUnit z : zombies) if (z.alive() && !z.finished) zombieViews.add(new EntityState(z.id, z.kind,
            Math.max(0, (int) Math.floor(z.x)), z.lane, z.x, z.health, z.maxHealth, z.slowTicks > 0));
        List<ProjectileState> shotViews = new ArrayList<>();
        for (Shot s : shots) shotViews.add(new ProjectileState(s.id, s.lane, s.x, s.damage, s.frozen));
        int start = Math.max(0, reactions.size() - 10);
        return new MatchSnapshot(id, revision, plantUsername, zombieUsername, status, winner, reason,
            Math.max(0, (DURATION_TICKS - elapsedTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND),
            plantSun, zombieSun, brains.clone(), List.copyOf(plantViews), List.copyOf(zombieViews),
            List.copyOf(shotViews), List.copyOf(reactions.subList(start, reactions.size())));
    }

    synchronized void forfeit(String username, String why) {
        if (!isActive()) return;
        requireParticipant(username);
        finish(plantUsername.equalsIgnoreCase(username) ? "ZOMBIES" : "PLANTS", why);
    }

    synchronized void finishByRole(String winnerRole, String why) {
        if (isActive()) finish(winnerRole, why);
    }

    private void finish(String winnerRole, String why) {
        status = "FINISHED";
        winner = winnerRole;
        reason = why;
        revision++;
    }

    String id() { return id; }
    String plantUsername() { return plantUsername; }
    String zombieUsername() { return zombieUsername; }
    synchronized boolean isActive() { return "ACTIVE".equals(status); }
    synchronized boolean resultRecorded() { return resultRecorded; }
    synchronized void markResultRecorded() { resultRecorded = true; }
    boolean hasPlayer(String username) {
        return plantUsername.equalsIgnoreCase(username) || zombieUsername.equalsIgnoreCase(username);
    }
    String roleOf(String username) {
        if (plantUsername.equalsIgnoreCase(username)) return "PLANTS";
        if (zombieUsername.equalsIgnoreCase(username)) return "ZOMBIES";
        throw ApiException.forbidden("You are not a participant in this match.");
    }
    String opponentOf(String username) {
        return plantUsername.equalsIgnoreCase(username) ? zombieUsername : plantUsername;
    }

    private void requireParticipant(String username) {
        if (!hasPlayer(username)) throw ApiException.forbidden("You are not a participant in this match.");
    }
    private void requireActive() {
        if (!isActive()) throw ApiException.conflict("This match has already finished.");
    }
    private boolean allBrainsEaten() {
        for (boolean available : brains) if (available) return false;
        return true;
    }
    private static int cooldown(Map<String, Integer> map, String kind) { return map.getOrDefault(kind, 0); }
    private static void tickCooldowns(Map<String, Integer> map) {
        map.replaceAll((key, value) -> Math.max(0, value - 1));
    }

    private static PlantStats plantStats(String kind) {
        return switch (kind) {
            case "PEASHOOTER" -> new PlantStats(100, 100, 35);
            case "SUNFLOWER" -> new PlantStats(50, 80, 35);
            case "WALL_NUT" -> new PlantStats(50, 420, 60);
            case "SNOW_PEA" -> new PlantStats(175, 100, 50);
            default -> throw ApiException.badRequest("Unknown online plant type.");
        };
    }

    private static ZombieStats zombieStats(String kind) {
        return switch (kind) {
            case "NORMAL" -> new ZombieStats(75, 190, .026, 16, 25);
            case "CONEHEAD" -> new ZombieStats(125, 330, .024, 18, 35);
            case "BUCKETHEAD" -> new ZombieStats(175, 520, .021, 20, 50);
            case "GARGANTUAR" -> new ZombieStats(300, 900, .014, 55, 80);
            default -> throw ApiException.badRequest("Unknown online zombie type.");
        };
    }

    private record PlantStats(int cost, double health, int recharge) { }
    private record ZombieStats(int cost, double health, double speed, double damage, int recharge) { }
    private static final class PlantUnit {
        final long id; final String kind; final int column; final int lane; final double maxHealth;
        double health; int actionTicks;
        PlantUnit(long id, String kind, int column, int lane, double health, double maxHealth) {
            this.id = id; this.kind = kind; this.column = column; this.lane = lane;
            this.health = health; this.maxHealth = maxHealth;
        }
        boolean alive() { return health > 0; }
    }
    private static final class ZombieUnit {
        final long id; final String kind; final int lane; final double maxHealth; final double speed; final double damage;
        double x; double health; int attackTicks; int slowTicks; boolean finished;
        ZombieUnit(long id, String kind, int lane, double x, double health, double maxHealth,
                   double speed, double damage, int attackTicks, int slowTicks) {
            this.id = id; this.kind = kind; this.lane = lane; this.x = x; this.health = health;
            this.maxHealth = maxHealth; this.speed = speed; this.damage = damage;
            this.attackTicks = attackTicks; this.slowTicks = slowTicks;
        }
        boolean alive() { return health > 0; }
    }
    private static final class Shot {
        final long id; final int lane; final double damage; final boolean frozen; double x;
        Shot(long id, int lane, double x, double damage, boolean frozen) {
            this.id = id; this.lane = lane; this.x = x; this.damage = damage; this.frozen = frozen;
        }
    }
}
