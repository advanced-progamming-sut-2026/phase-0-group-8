package ir.hamgit.ahh.PvZ.model.minigame;

import ir.hamgit.ahh.PvZ.model.Board;

/** Common model contract used by minigame controllers. */
public interface MinigameSession {

    Board getBoard();

    boolean isOver();

    boolean isWon();
}
