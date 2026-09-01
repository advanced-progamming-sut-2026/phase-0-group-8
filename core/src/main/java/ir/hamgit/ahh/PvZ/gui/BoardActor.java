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


 



public final class BoardActor extends BoardOverlayRenderer {
    @FunctionalInterface
    public interface CellClickListener {
        void clicked(int column, int lane);
    }

    @FunctionalInterface
    public interface CellHoverListener {
        void hovered(int column, int lane);
    }

    @FunctionalInterface
    public interface CellMarkerProvider {
        String marker(int column, int lane);
    }

    public BoardActor(Supplier<Board> boardSupplier, BitmapFont font) {
        this(boardSupplier, font, null);
    }

    public BoardActor(Supplier<Board> boardSupplier, BitmapFont font, PvzAssetSystem art) {
        super(boardSupplier, font, art);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Board board = boardSupplier.get();
        if (board == null) {
            return;
        }
        float boardWidth = lawnWidth();
        float cellWidth = boardWidth / board.getColumns();
        float cellHeight = lawnHeight() / board.getRows();

        boolean realBackground = drawBackdrop(batch, board);
        drawTiles(batch, board, cellWidth, cellHeight, realBackground);
        drawSpecialLines(batch, board, cellWidth, cellHeight);
        drawProtectedSeeds(batch, board, cellWidth, cellHeight);
        drawMowersAndBrains(batch, board, cellHeight);
        drawPlants(batch, board, cellWidth, cellHeight);
        drawProjectiles(batch, board, cellWidth, cellHeight);
        drawZombies(batch, board, cellWidth, cellHeight);
        drawBowlingBalls(batch, board, cellWidth, cellHeight);
        drawSuns(batch, board, cellWidth, cellHeight);
        drawPlantFoodPickups(batch, board, cellWidth, cellHeight);
        drawTransientEffects(batch, board, cellWidth, cellHeight);
        drawCellMarkers(batch, board, cellWidth, cellHeight);
        drawHover(batch, board, cellWidth, cellHeight);
        drawCursor(batch);
        batch.setColor(WHITE);
    }


}
