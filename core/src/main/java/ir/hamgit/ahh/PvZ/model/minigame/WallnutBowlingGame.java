package ir.hamgit.ahh.PvZ.model.minigame;


import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WallnutBowlingGame {

    private static final Pattern COORD_PATTERN = Pattern.compile("(-?\\d+)\\s*,\\s*(-?\\d+)");
    private static final int DELIVERY_INTERVAL_TICKS = 100;
    private static final int MAX_QUEUE_SIZE = 3;

    private final Board board;
    private final int redLineColumn;
    private final List<BowlingBall> balls = new ArrayList<>();
    private final Deque<BowlingBall.Kind> queue = new ArrayDeque<>();
    private int ticksUntilNextDelivery = DELIVERY_INTERVAL_TICKS;
    private boolean won;

    public WallnutBowlingGame(int redLineColumn, List<ZombieType> initialZombies) {
        this.board = new Board(ChapterType.MINIGAME, 1, 3, null, new MinigameLevelHandler());
        this.redLineColumn = redLineColumn;
        spawnInitialZombies(initialZombies);
        deliverNextBall();
    }

    private void spawnInitialZombies(List<ZombieType> initialZombies) {
        for (ZombieType type : initialZombies) {
            int lane = (int) (Math.random() * board.getRows());
            board.spawnZombieAt(type, lane, board.getColumns());
        }
    }

    private void deliverNextBall() {
        ticksUntilNextDelivery = DELIVERY_INTERVAL_TICKS;
        if (queue.size() >= MAX_QUEUE_SIZE) {
            return;
        }
        double[] weights = {0.6, 0.25, 0.15};
        double roll = Math.random();
        BowlingBall.Kind kind = roll < weights[0] ? BowlingBall.Kind.BOWLING
                : roll < weights[0] + weights[1] ? BowlingBall.Kind.EXPLODE_O_NUT : BowlingBall.Kind.GIANT;
        queue.addLast(kind);
        System.out.println("Conveyor belt delivered a " + kind + " ball.");
    }

    public boolean plantBall(int x, int lane) {
        if (x > redLineColumn || queue.isEmpty()) {
            System.out.println("Can't place a ball there.");
            return false;
        }
        BowlingBall.Kind kind = queue.pollFirst();
        balls.add(new BowlingBall(kind, x, lane, 1));
        return true;
    }

    public void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            advanceOneTick();
        }
        checkWin();
    }

    private void advanceOneTick() {
        ticksUntilNextDelivery--;
        if (ticksUntilNextDelivery <= 0) {
            deliverNextBall();
        }
        for (BowlingBall ball : new ArrayList<>(balls)) {
            ball.tick(board);
        }
        balls.removeIf(b -> !b.isAlive());
        board.advanceTime(1);
    }

    private void checkWin() {
        won = board.getZombies().stream().noneMatch(z -> z.isAlive());
    }

    public boolean isOver() {
        return won || board.isGameOver();
    }

    public boolean isWon() {
        return won && !board.isGameOver();
    }

    public void handle(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("plant ball")) {
            handlePlantBall(trimmed);
        } else if (trimmed.startsWith("advance time")) {
            Map<String, String> flags = CommandParser.parse(trimmed);
            tick(CommandParser.getIntFlag(flags, "-t", 1));
        } else if (trimmed.startsWith("show map")) {
            board.showMap();
        } else {
            System.out.println("Unknown Wallnut Bowling command: " + raw);
        }
    }

    private void handlePlantBall(String raw) {
        Matcher m = COORD_PATTERN.matcher(raw);
        if (m.find()) {
            plantBall(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
        }
    }

    public Board getBoard() {
        return board;
    }
}
