package ir.hamgit.ahh.PvZ.model.minigame;

import ir.hamgit.ahh.PvZ.model.Board;

 
public interface MinigameSession {

    Board getBoard();

    boolean isOver();

    boolean isWon();
}
