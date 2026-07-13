package ir.hamgit.ahh.PvZ.model.behavior.plant;

import ir.hamgit.ahh.PvZ.model.behavior.BoardContext;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

public interface PlantBehavior {
    void onTick(Plant self, BoardContext ctx, int currentTick);
    default void onPlanted(Plant self, BoardContext ctx) {}
}
