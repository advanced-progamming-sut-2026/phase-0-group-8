package ir.hamgit.ahh.PvZ.model.quest;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

import ir.hamgit.ahh.PvZ.model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

 
public final class LevelQuestTelemetry {

    private static final int SPEED_WINDOW_TICKS = 30 * Board.TICKS_PER_SECOND;
    private static final Map<Board, Session> SESSIONS = new WeakHashMap<>();

    private LevelQuestTelemetry() {
    }

    public static synchronized void start(Board board) {
        SESSIONS.put(board, new Session());
    }

    public static synchronized void recordPlant(Board board, PlantType type) {
        Session session = SESSIONS.computeIfAbsent(board, ignored -> new Session());
        session.placements.merge(type, 1, Integer::sum);
    }

    public static synchronized void recordSunCollected(Board board, int amount) {
        SESSIONS.computeIfAbsent(board, ignored -> new Session()).sunCollected += amount;
    }

    public static synchronized void recordWaveStart(Board board, int waveNumber) {
        Session session = SESSIONS.computeIfAbsent(board, ignored -> new Session());
        if (waveNumber == 1 && session.firstWaveTick < 0) {
            session.firstWaveTick = board.getTickCount();
        }
    }

    public static synchronized void recordKill(Board board, Zombie zombie) {
        Session session = SESSIONS.computeIfAbsent(board, ignored -> new Session());
        session.kills++;
        if (session.firstWaveTick >= 0
            && board.getTickCount() - session.firstWaveTick < SPEED_WINDOW_TICKS) {
            session.earlyKills++;
        }
        int lane = zombie.getLane();
        boolean mowerMissing = lane >= 0 && lane < board.getRows()
            && !board.getLawnMowerAvailability()[lane];
        if (zombie.getX() < 1 && mowerMissing) {
            session.firstColumnMowerlessKills++;
        }
    }

    public static synchronized void recordMowerKill(Board board) {
        SESSIONS.computeIfAbsent(board, ignored -> new Session()).mowerKills++;
    }

    public static synchronized Snapshot finish(Board board) {
        Session session = SESSIONS.remove(board);
        if (session == null) {
            session = new Session();
        }
        int lostPlants = Math.max(0, session.totalPlacements() - board.getPlantsRemaining());
        return new Snapshot(Map.copyOf(session.placements), session.kills, session.earlyKills,
            session.firstColumnMowerlessKills, session.mowerKills, session.sunCollected, lostPlants);
    }

    public record Snapshot(Map<PlantType, Integer> placements, int kills, int earlyKills,
                           int firstColumnMowerlessKills, int mowerKills, int sunCollected,
                           int lostPlants) {

        public int countPlants(Predicate<PlantType> predicate) {
            return placements.entrySet().stream().filter(entry -> predicate.test(entry.getKey()))
                .mapToInt(Map.Entry::getValue).sum();
        }

        public int totalPlacements() {
            return placements.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private static final class Session {
        private final Map<PlantType, Integer> placements = new EnumMap<>(PlantType.class);
        private int kills;
        private int earlyKills;
        private int firstColumnMowerlessKills;
        private int mowerKills;
        private int sunCollected;
        private int firstWaveTick = -1;

        private int totalPlacements() {
            return placements.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
}
