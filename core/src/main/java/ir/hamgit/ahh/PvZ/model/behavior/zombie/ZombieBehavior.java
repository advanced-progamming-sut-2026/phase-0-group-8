package ir.hamgit.ahh.PvZ.model.behavior.zombie;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;

public interface ZombieBehavior {
    void onTick(Zombie self, BoardContext ctx, int currentTick);
}
