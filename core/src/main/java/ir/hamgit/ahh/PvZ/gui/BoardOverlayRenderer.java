package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Projectile;
import ir.hamgit.ahh.PvZ.model.Sun;
import ir.hamgit.ahh.PvZ.model.Tile;
import ir.hamgit.ahh.PvZ.model.entities.Armor;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantFamily;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.SunType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;
import ir.hamgit.ahh.PvZ.model.minigame.BowlingBall;
import ir.hamgit.ahh.PvZ.model.special.DeadLineLevel;
import ir.hamgit.ahh.PvZ.model.special.SaveOurSeedsLevel;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


 
abstract class BoardOverlayRenderer extends BoardEntityRenderer {
    protected BoardOverlayRenderer(Supplier<Board> boardSupplier, BitmapFont font, PvzAssetSystem art) {
        super(boardSupplier, font, art);
    }

    protected void drawBowlingBalls(Batch batch, Board board, float cellWidth, float cellHeight) {
        if (bowlingBallsSupplier == null) return;
        List<BowlingBall> balls = bowlingBallsSupplier.get();
        if (balls == null) return;
        for (BowlingBall ball : balls) {
            float x = cellXFloat((float) ball.getX(), cellWidth) + cellWidth * .5f;
            float y = cellY(ball.getLane(), board, cellHeight) + cellHeight * .5f;
            Color c = switch (ball.getKind()) {
                case BOWLING -> new Color(.55f, .38f, .15f, 1f);
                case EXPLODE_O_NUT -> new Color(.85f, .22f, .12f, 1f);
                case GIANT -> new Color(.35f, .22f, .10f, 1f);
            };
            drawCircle(batch, x, y, ball.getKind() == BowlingBall.Kind.GIANT ? 34f : 26f, c);
            text(batch, shortName(ball.getKind().name()), x - 15, y + 4, Color.WHITE);
        }
    }

    protected void drawCellMarkers(Batch batch, Board board, float cellWidth, float cellHeight) {
        if (cellMarkerProvider == null) return;
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int column = 0; column < board.getColumns(); column++) {
                String marker = cellMarkerProvider.marker(column, lane);
                if (marker == null || marker.isBlank() || marker.equals(".")) continue;
                float x = cellX(column, cellWidth);
                float y = cellY(lane, board, cellHeight);
                Color c = marker.equals("G") ? new Color(.38f, .15f, .10f, .92f)
                    : marker.equals("P") ? new Color(.25f, .55f, .18f, .92f)
                    : marker.equals("S") ? new Color(.95f, .78f, .10f, .92f)
                    : new Color(.56f, .36f, .20f, .92f);
                fill(batch, x + cellWidth * .22f, y + cellHeight * .17f, cellWidth * .56f, cellHeight * .66f, c);
                border(batch, x + cellWidth * .22f, y + cellHeight * .17f, cellWidth * .56f, cellHeight * .66f, 3f,
                    Color.WHITE);
                text(batch, marker.equals("?") ? "VASE" : marker, x + cellWidth * .37f, y + cellHeight * .54f,
                    Color.WHITE);
            }
        }
    }

    protected void drawHover(Batch batch, Board board, float cellWidth, float cellHeight) {
        if (hoverColumn < 0 || hoverLane < 0) return;
        float x = cellX(hoverColumn, cellWidth);
        float y = cellY(hoverLane, board, cellHeight);
        fill(batch, x + 2, y + 2, cellWidth - 4, cellHeight - 4, new Color(1f, 1f, 1f, .18f));
        border(batch, x + 2, y + 2, cellWidth - 4, cellHeight - 4, 3f, Color.WHITE);
    }

    protected void drawCursor(Batch batch) {
        if (cursorText == null || cursorText.isBlank() || hoverColumn < 0) return;
        float x = getX() + mouseX;
        float y = getY() + mouseY;
        float pulse = 34f + (float)Math.sin(elapsed * 4f) * 2.5f;
        Color c = cursorPlant != null && PlantRegistry.get(cursorPlant) != null
            ? plantColor(PlantRegistry.get(cursorPlant).getFamily())
            : new Color(.1f, .1f, .1f, .75f);
        boolean rendered = cursorPlant != null && art != null && art.drawPlant(batch, cursorPlant, "idle", elapsed,
            x + 18, y + 18, pulse * 2.1f, pulse * 2.1f);
        if (!rendered) {
            drawCircle(batch, x + 18, y + 18, pulse, c);
            text(batch, shortName(cursorText), x + 3, y + 22, Color.WHITE);
        }
    }


}

