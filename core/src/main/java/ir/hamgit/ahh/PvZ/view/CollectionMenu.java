package ir.hamgit.ahh.PvZ.view;



import ir.hamgit.ahh.PvZ.controller.CommandParser;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.registry.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.Map;
import java.util.Locale;

public class CollectionMenu {

    public void handle(String input) {
        Map<String, String> flags = CommandParser.parse(input);
        String command = CommandParser.getCommand(flags);
        User user = UserRepository.getCurrentUser();
        if (user == null) {
            System.out.println("Error: no user logged in.");
            return;
        }
        switch (command) {
            case "menu collection show-plants":
                showPlants(user);
                break;
            case "menu collection show-all-plants":
                showAllPlants();
                break;
            case "menu collection show-zombies":
                showZombies(user);
                break;
            case "menu collection show-all-zombies":
                showAllZombies();
                break;
            case "menu collection show-plant":
                showPlant(user, CommandParser.getFlag(flags, "p"));
                break;
            case "menu collection show-zombie":
                showZombie(user, CommandParser.getFlag(flags, "z"));
                break;
            case "menu collection upgrade-plant":
                upgradePlant(user, CommandParser.getFlag(flags, "p"));
                break;
            case "menu collection purchase-plant":
                purchasePlant(user, CommandParser.getFlag(flags, "p"));
                break;
            default:
                System.out.println("Error: unknown collection command.");
        }
    }

    private void showPlants(User user) {
        System.out.println("Your plants:");
        for (PlantType type : user.getUnlockedPlants()) {
            PlantDef def = PlantRegistry.get(type);
            System.out.println("  " + def.getDisplayName() + " (Level " + user.getPlantLevel(type) + ")");
        }
    }

    private void showAllPlants() {
        System.out.println("All plants in the game:");
        for (PlantDef def : PlantRegistry.getAll()) {
            System.out.println("  " + def.getDisplayName());
        }
    }

    private void showZombies(User user) {
        System.out.println("Zombies you have encountered:");
        for (ZombieType type : user.getSeenZombies()) {
            ZombieDef def = ZombieRegistry.get(type);
            System.out.println("  " + def.getDisplayName());
        }
    }

    private void showAllZombies() {
        System.out.println("All zombies in the game:");
        for (ZombieDef def : ZombieRegistry.getAll().values()) {
            System.out.println("  " + def.getDisplayName());
        }
    }

    private void showPlant(User user, String plantName) {
        if (plantName == null) {
            System.out.println("Error: usage is menu collection show-plant -p <plant_name>");
            return;
        }
        PlantType type = parsePlantType(plantName);
        if (type == null) {
            System.out.println("Error: unknown plant type.");
            return;
        }
        if (!user.hasPlant(type)) {
            System.out.println("Error: you have not unlocked this plant yet.");
            return;
        }
        PlantDef def = PlantRegistry.get(type);
        System.out.println("Name: " + def.getDisplayName());
        System.out.println("Sun cost: " + def.getSunCost());
        System.out.println("Max HP: " + def.getMaxHp());
        System.out.println("Damage: " + def.getDamage());
        System.out.println("Recharge: " + def.getRechargeSeconds() + "s");
        System.out.println("Level: " + user.getPlantLevel(type));
        System.out.println("Tags: " + def.getTags());
    }

    private void showZombie(User user, String zombieName) {
        if (zombieName == null) {
            System.out.println("Error: usage is menu collection show-zombie -z <zombie_name>");
            return;
        }
        ZombieType type = parseZombieType(zombieName);
        if (type == null) {
            System.out.println("Error: unknown zombie type.");
            return;
        }
        if (!user.getSeenZombies().contains(type)) {
            System.out.println("Error: you have not encountered this zombie yet.");
            return;
        }
        ZombieDef def = ZombieRegistry.get(type);
        System.out.println("Name: " + def.getDisplayName());
        System.out.println("Max HP: " + def.getMaxHp());
        System.out.println("Speed: " + def.getSpeed());
        System.out.println("Damage: " + def.getDamage());
        System.out.println("Armor layers: " + def.getArmorLayers());
        System.out.println("Behaviors: " + def.getBehaviors());
    }

    private void upgradePlant(User user, String plantName) {
        if (plantName == null) {
            System.out.println("Error: usage is menu collection upgrade-plant -p <plant_name>");
            return;
        }
        PlantType type = parsePlantType(plantName);
        if (type == null || !user.hasPlant(type)) {
            System.out.println("Error: you do not own this plant.");
            return;
        }
        if (!user.upgradeAllowed(type)) {
            System.out.println("Error: not enough seed packets or coins to upgrade.");
            return;
        }
        user.upgradeStats(type);
        UserRepository.updateUser(user);
        System.out.println(PlantRegistry.get(type).getDisplayName() + " upgraded to level "
                + user.getPlantLevel(type) + "!");
    }

    private void purchasePlant(User user, String plantName) {
        if (plantName == null) {
            System.out.println("Error: usage is menu collection purchase-plant -p <plant_name>");
            return;
        }
        PlantType type = parsePlantType(plantName);
        if (type == null) {
            System.out.println("Error: unknown plant type.");
            return;
        }
        if (user.hasPlant(type)) {
            System.out.println("Error: you already own this plant.");
            return;
        }
        if (!user.spendCoins(2000)) {
            System.out.println("Error: not enough coins. Purchasing a new plant costs 2000 coins.");
            return;
        }
        user.unlockPlant(type);
        UserRepository.updateUser(user);
        System.out.println("Purchased " + PlantRegistry.get(type).getDisplayName() + "!");
    }

    private PlantType parsePlantType(String name) {
        try {
            return PlantType.valueOf(name.toUpperCase(Locale.ROOT)
                .replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ZombieType parseZombieType(String name) {
        try {
            return ZombieType.valueOf(name.toUpperCase(Locale.ROOT)
                .replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
