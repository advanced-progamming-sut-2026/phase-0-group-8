package ir.hamgit.ahh.PvZ.controller;

import ir.hamgit.ahh.PvZ.model.minigame.BeghouledGame;
import ir.hamgit.ahh.PvZ.model.minigame.IZombieGame;
import ir.hamgit.ahh.PvZ.model.minigame.MinigameSession;
import ir.hamgit.ahh.PvZ.model.minigame.VasebreakerGame;
import ir.hamgit.ahh.PvZ.model.minigame.WallnutBowlingGame;
import ir.hamgit.ahh.PvZ.model.minigame.ZombotanyGame;
import ir.hamgit.ahh.PvZ.model.quest.QuestService;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MinigameController {

    private MinigameSession activeGame;
    private String activeName;
    private int activeLevel;
    private final MinigameCommandRouter commandRouter = new MinigameCommandRouter();

    public boolean start(String name, int level) {
        return start(name, level, false);
    }

     
    public boolean start(String name, int level, boolean couchPlay) {
        if (level < 1 || level > 3) {
            return false;
        }
        String normalizedName = normalize(name);
        MinigameSession newGame = createGame(normalizedName, level, couchPlay);
        if (newGame == null) {
            return false;
        }
        activeName = normalizedName;
        activeLevel = level;
        activeGame = newGame;
        commandRouter.showHelp(activeGame);
        return true;
    }

    private MinigameSession createGame(String name, int level, boolean couchPlay) {
        return switch (name) {
            case "vasebreaker" -> createVasebreaker(level);
            case "wallnut_bowling" -> createBowling(level);
            case "i_zombie" -> createIZombie(level, couchPlay);
            case "beghouled" -> new BeghouledGame(3 + level * 2, Math.min(5, level + 2));
            case "zombotany" -> new ZombotanyGame(level);
            default -> null;
        };
    }

    private VasebreakerGame createVasebreaker(int level) {
        List<int[]> plants = List.of(new int[] {1, 1}, new int[] {3, 3});
        List<int[]> giants = level == 1 ? List.of(new int[] {7, 2})
            : level == 2 ? List.of(new int[] {6, 1}, new int[] {8, 3})
            : List.of(new int[] {5, 0}, new int[] {7, 2}, new int[] {8, 4});
        return new VasebreakerGame(plants, giants);
    }

    private WallnutBowlingGame createBowling(int level) {
        List<ZombieType> pool = level == 1
            ? List.of(ZombieType.NORMAL, ZombieType.CONEHEAD, ZombieType.NORMAL)
            : level == 2
            ? List.of(ZombieType.NORMAL, ZombieType.CONEHEAD, ZombieType.BUCKETHEAD,
            ZombieType.ALL_STAR, ZombieType.NORMAL)
            : List.of(ZombieType.BUCKETHEAD, ZombieType.KNIGHT, ZombieType.ALL_STAR,
            ZombieType.GARGANTUAR, ZombieType.CONEHEAD, ZombieType.BLOCKHEAD);
        return new WallnutBowlingGame(3, pool);
    }

    private IZombieGame createIZombie(int level, boolean couchPlay) {
        List<ZombieType> roster = iZombieRoster(level);
        Map<ZombieType, Integer> costs = new EnumMap<>(ZombieType.class);
        for (int i = 0; i < roster.size(); i++) {
            costs.put(roster.get(i), 25 + i * 25);
        }
        return new IZombieGame(roster, costs, couchPlay);
    }

    private List<ZombieType> iZombieRoster(int level) {
        return switch (level) {
            case 1 -> List.of(ZombieType.NORMAL, ZombieType.CONEHEAD, ZombieType.IMP,
                ZombieType.ALL_STAR, ZombieType.RA_ZOMBIE);
            case 2 -> List.of(ZombieType.BUCKETHEAD, ZombieType.PROSPECTOR, ZombieType.DODO_RIDER,
                ZombieType.SNORKEL, ZombieType.JESTER);
            default -> List.of(ZombieType.KNIGHT, ZombieType.GARGANTUAR, ZombieType.EXPLORER,
                ZombieType.OCTOPUS, ZombieType.WIZARD);
        };
    }

    public boolean handle(String input) {
        if (activeGame == null) {
            return true;
        }
        commandRouter.handle(activeGame, input);
        return finishIfOver();
    }

    public boolean finishIfOver() {
        boolean over = isOver();
        if (!over) {
            return false;
        }
        boolean won = isWon();
        User user = UserRepository.getCurrentUser();
        if (user != null) {
            QuestService.recordGamePlayed(user);
            if (won) {
                user.recordMinigameCompletion(activeName + "_" + activeLevel);
                QuestService.recordMinigameWin(user);
                user.addCoins(500 * activeLevel);
            }
            UserRepository.updateUser(user);
        }
        System.out.println(won ? "Minigame complete!" : "Minigame failed.");
        activeGame = null;
        return true;
    }

    public MinigameSession getActiveGame() {
        return activeGame;
    }

    public String getActiveName() {
        return activeName;
    }

    public int getActiveLevel() {
        return activeLevel;
    }

    public boolean advanceTime(int ticks) {
        if (activeGame == null || ticks <= 0) {
            return activeGame == null;
        }
        if (activeGame instanceof VasebreakerGame game) {
            game.tick(ticks);
        } else if (activeGame instanceof WallnutBowlingGame game) {
            game.tick(ticks);
        } else if (activeGame instanceof IZombieGame game) {
            game.tick(ticks);
        } else if (activeGame instanceof BeghouledGame game) {
            game.tick(ticks);
        } else if (activeGame instanceof ZombotanyGame game) {
            game.tick(ticks);
        }
        return finishIfOver();
    }

    private boolean isOver() {
        return activeGame != null && activeGame.isOver();
    }

    private boolean isWon() {
        return activeGame != null && activeGame.isWon();
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase().replace('-', '_').replace(' ', '_');
    }
}
