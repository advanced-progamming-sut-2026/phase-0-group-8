package ir.hamgit.ahh.PvZ.model.special;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public class DeadLineLevel extends SpecialLevelHandler {

    private final int lineColumn;
    private boolean crossed;

    public DeadLineLevel(int lineColumn) {
        this.lineColumn = lineColumn;
    }

    @Override
    public void onTick(Board board) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.isAlive() && zombie.getX() <= lineColumn) {
                crossed = true;
            }
        }
    }

    @Override
    public boolean checkCustomLoss(Board board) {
        return crossed;
    }

    public int getLineColumn() {
        return lineColumn;
    }
}
