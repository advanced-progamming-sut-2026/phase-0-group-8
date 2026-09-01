package ir.hamgit.ahh.PvZ.view;

import ir.hamgit.ahh.PvZ.model.minigame.BeghouledGame;
import ir.hamgit.ahh.PvZ.model.minigame.BowlingBall;
import ir.hamgit.ahh.PvZ.model.minigame.IZombieGame;
import ir.hamgit.ahh.PvZ.model.minigame.MinigameSession;
import ir.hamgit.ahh.PvZ.model.minigame.VasebreakerGame;
import ir.hamgit.ahh.PvZ.model.minigame.WallnutBowlingGame;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;

 
public final class MinigameView {

    private MinigameView() {
    }

    public static void show(MinigameSession session) {
        if (session instanceof VasebreakerGame game) {
            showVasebreaker(game);
        } else if (session instanceof WallnutBowlingGame game) {
            showBowling(game);
        } else if (session instanceof IZombieGame game) {
            showIZombie(game);
        } else if (session instanceof BeghouledGame game) {
            showBeghouled(game);
        } else {
            BoardView.showMap(session.getBoard());
        }
    }

    private static void showVasebreaker(VasebreakerGame game) {
        System.out.println("Vases: ?=unknown, P=plant vase, G=giant vase, S=seed packet, .=broken");
        for (int lane = 0; lane < game.getBoard().getRows(); lane++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < game.getBoard().getColumns(); x++) {
                row.append('[').append(game.getVaseMarker(x, lane)).append(']');
            }
            System.out.println(row);
        }
        System.out.println("Held seed packets: " + game.getHeldSeeds());
        BoardView.showGrid(game.getBoard());
        BoardView.showLawnMowers(game.getBoard());
    }

    private static void showBowling(WallnutBowlingGame game) {
        System.out.println("Red line: column " + game.getRedLineColumn()
            + " | Belt queue: " + game.getQueue());
        System.out.println("Rolling balls: " + game.getBalls().stream()
            .map(MinigameView::describeBall).toList());
        BoardView.showGrid(game.getBoard());
        BoardView.showLawnMowers(game.getBoard());
    }

    private static String describeBall(BowlingBall ball) {
        return String.format("%s@(%.1f,%d)", ball.getKind(), ball.getX(), ball.getLane());
    }

    private static void showIZombie(IZombieGame game) {
        String available = game.getAvailableZombies().stream()
            .map(type -> type + "=" + game.getCost(type)).toList().toString();
        if (game.isCouchPlay()) {
            String plants = game.getAvailablePlants().stream()
                .map(type -> type + "=" + PlantRegistry.get(type).getSunCost()).toList().toString();
            System.out.println("Couch Play | P1 plant sun: " + game.getPlantSunAmount()
                + " | P2 zombie sun: " + game.getSunAmount()
                + " | Time: " + game.getRemainingSeconds() + "s");
            System.out.println("P1 plants: x=" + game.getFirstPlantColumn() + "-" + game.getLastPlantColumn()
                + " " + plants);
            System.out.println("P2 zombies: x=" + game.getFirstPlacementColumn() + "-"
                + (game.getBoard().getColumns() - 1) + " " + available);
        } else {
            System.out.println("I-Zombie sun: " + game.getSunAmount());
            System.out.println("Place at x=" + game.getFirstPlacementColumn()
                + " | Available: " + available);
        }
        BoardView.showGrid(game.getBoard());
        printBrains(game.getBrainAvailability());
    }

    private static void printBrains(boolean[] brains) {
        StringBuilder line = new StringBuilder("Brains: ");
        for (int lane = 0; lane < brains.length; lane++) {
            line.append("row ").append(lane).append('=')
                .append(brains[lane] ? "available" : "eaten").append("  ");
        }
        System.out.println(line);
    }

    private static void showBeghouled(BeghouledGame game) {
        System.out.printf("Beghouled sun: %d | Matches: %d/%d%n",
            game.getSunAmount(), game.getMatchesMade(), game.getTargetMatches());
        BoardView.showGrid(game.getBoard());
        System.out.println("Craters: " + craterCoordinates(game));
        BoardView.showLawnMowers(game.getBoard());
    }

    private static String craterCoordinates(BeghouledGame game) {
        StringBuilder result = new StringBuilder();
        for (int lane = 0; lane < game.getBoard().getRows(); lane++) {
            for (int x = 0; x < game.getBoard().getColumns(); x++) {
                if (game.isCrater(x, lane)) {
                    if (result.length() > 0) {
                        result.append(", ");
                    }
                    result.append('(').append(x).append(',').append(lane).append(')');
                }
            }
        }
        return result.length() == 0 ? "none" : result.toString();
    }
}
