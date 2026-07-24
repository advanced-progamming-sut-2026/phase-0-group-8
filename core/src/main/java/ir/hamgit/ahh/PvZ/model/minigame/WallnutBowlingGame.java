package ir.hamgit.ahh.PvZ.model.minigame;


import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.special.MinigameLevelHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class WallnutBowlingGame implements MinigameSession {

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
        if (initialZombies == null) {
            return;
        }
        for (ZombieType type : initialZombies) {
            if (type != null) {
                int lane = (int) (Math.random() * board.getRows());
                board.spawnZombieAt(type, lane, board.getColumns());
            }
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
        boolean outsideBoard = x < 0 || x >= board.getColumns()
            || lane < 0 || lane >= board.getRows();
        if (outsideBoard || x > redLineColumn || queue.isEmpty()) {
            System.out.println("Can't place a ball there.");
            return false;
        }
        BowlingBall.Kind kind = queue.pollFirst();
        balls.add(new BowlingBall(kind, x, lane, 1));
        return true;
    }

    public void tick(int ticks) {
        if (ticks <= 0) {
            return;
        }
        for (int i = 0; i < ticks && !isOver(); i++) {
            advanceOneTick();
            checkWin();
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

    public Board getBoard() {
        return board;
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public List<BowlingBall.Kind> getQueue() {
        return List.copyOf(queue);
    }

    public List<BowlingBall> getBalls() {
        return List.copyOf(balls);
    }
}
