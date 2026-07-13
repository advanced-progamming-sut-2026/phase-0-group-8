package ir.hamgit.ahh.PvZ.model.behavior;

import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import java.util.List;

public interface BoardContext {

    // --- Spatial Queries ---
    Zombie getNearestZombieAhead(int row, int col);

    Zombie getAdjacentZombie(int row, int col);

    Plant getPlantBlocking(int row, double zombieX);

    List<Plant> getPlantsOfCategory(ir.hamgit.ahh.PvZ.model.enums.PlantCategory category);


    // --- Game Environment Mutations ---
    void spawnSun(int row, int col, double amount, String generationType);

    void spawnProjectile(int row, double startingX, double damage, String projectileEffect);

    void dealAreaDamage(int centerRow, int centerCol, int tileRadius, double explosiveDamage);

    void removePlant(Plant plant);

    void removeZombie(Zombie zombie);
}
