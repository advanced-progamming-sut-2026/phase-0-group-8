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


 
abstract class BoardTerrainRenderer extends BoardActorBase {
    protected BoardTerrainRenderer(Supplier<Board> boardSupplier, BitmapFont font, PvzAssetSystem art) {
        super(boardSupplier, font, art);
    }

    protected boolean drawBackdrop(Batch batch, Board board) {
        Color color = switch (board.getChapter()) {
            case ANCIENT_EGYPT -> new Color(0.55f, 0.42f, 0.20f, 1f);
            case FROSTBITE_CAVES -> new Color(0.20f, 0.43f, 0.55f, 1f);
            case BIG_WAVE_BEACH -> new Color(0.14f, 0.50f, 0.53f, 1f);
            case DARK_AGES -> new Color(0.16f, 0.16f, 0.23f, 1f);
            default -> new Color(0.20f, 0.46f, 0.20f, 1f);
        };
        
        
        float originX = drawOriginX();
        float originY = drawOriginY();
        float backgroundY = originY + backgroundVerticalOffset;
        
        fill(batch, originX - 10f, originY - 10f, getWidth() + 20f, getHeight() + 20f, color);
        boolean rendered = art != null && art.drawBackground(batch, board.getChapter(),
            originX, backgroundY, getWidth(), getHeight());
        if (!rendered) {
            drawProceduralChapterScenery(batch, board.getChapter(), originX, backgroundY);
        }
        return rendered;
    }

    private void drawProceduralChapterScenery(Batch batch, ChapterType chapter, float x, float y) {
        float w = getWidth();
        float h = getHeight();
        switch (chapter) {
            case ANCIENT_EGYPT -> {
                fill(batch, x, y + h * .68f, w, h * .32f, new Color(.44f, .66f, .72f, 1f));
                drawCircle(batch, x + w * .80f, y + h * .86f, h * .14f, new Color(1f, .82f, .24f, .92f));
                for (int i = 0; i < 5; i++) {
                    fill(batch, x + w * (.04f + i * .035f), y + h * (.64f + i * .035f),
                        w * (.19f - i * .07f), h * .035f, new Color(.72f, .55f, .24f, 1f));
                }
                for (int i = 0; i < 7; i++) {
                    drawCircle(batch, x + w * (.08f + i * .16f), y + h * .63f,
                        h * (.14f + (i % 2) * .035f), new Color(.68f, .50f, .22f, .75f));
                }
            }
            case FROSTBITE_CAVES -> {
                fill(batch, x, y + h * .70f, w, h * .30f, new Color(.08f, .22f, .34f, 1f));
                for (int i = 0; i < 7; i++) {
                    float px = x + w * (i / 6f);
                    fill(batch, px, y + h * .77f, w * .20f, h * .035f,
                        new Color(.25f + i * .025f, .68f, .72f, .30f));
                    float icicleHeight = h * (.08f + (i % 3) * .035f);
                    fill(batch, px, y + h * .93f - icicleHeight, w * .035f, icicleHeight,
                        new Color(.62f, .88f, .94f, .82f));
                }
                drawCircle(batch, x + w * .78f, y + h * .86f, h * .12f,
                    new Color(.78f, .94f, 1f, .72f));
            }
            case BIG_WAVE_BEACH -> {
                fill(batch, x, y + h * .66f, w, h * .34f, new Color(.16f, .64f, .83f, 1f));
                drawCircle(batch, x + w * .18f, y + h * .86f, h * .15f,
                    new Color(1f, .86f, .25f, .95f));
                for (int i = 0; i < 12; i++) {
                    drawCircle(batch, x + w * (i / 11f), y + h * (.66f + (i % 2) * .012f),
                        h * .055f, new Color(.72f, .96f, 1f, .62f));
                }
                fill(batch, x + w * .88f, y + h * .68f, w * .018f, h * .21f,
                    new Color(.32f, .18f, .08f, 1f));
                for (int i = 0; i < 5; i++) {
                    drawCircle(batch, x + w * (.89f + (i - 2) * .035f), y + h * .90f,
                        h * .09f, new Color(.13f, .50f, .20f, .95f));
                }
            }
            case DARK_AGES -> {
                fill(batch, x, y + h * .63f, w, h * .37f, new Color(.055f, .06f, .13f, 1f));
                drawCircle(batch, x + w * .78f, y + h * .86f, h * .16f,
                    new Color(.86f, .89f, .68f, .88f));
                for (int i = 0; i < 6; i++) {
                    float towerX = x + w * (.04f + i * .17f);
                    fill(batch, towerX, y + h * .63f, w * .07f, h * (.10f + (i % 3) * .035f),
                        new Color(.08f, .08f, .10f, 1f));
                    fill(batch, towerX - w * .01f, y + h * (.73f + (i % 3) * .035f),
                        w * .09f, h * .025f, new Color(.08f, .08f, .10f, 1f));
                }
            }
            default -> { }
        }
    }

    protected void drawTiles(Batch batch, Board board, float cellWidth, float cellHeight, boolean realBackground) {
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int column = 0; column < board.getColumns(); column++) {
                float x = cellX(column, cellWidth);
                float y = cellY(lane, board, cellHeight);
                Tile tile = board.getTileAt(column, lane);
                Color base = tileColor(board.getChapter(), tile.getType(), column, lane);
                if (realBackground) {
                    float alpha = tile.getType() == TileType.NORMAL
                        ? (showGrid.getAsBoolean() ? .07f : .018f) : .48f;
                    base = new Color(base.r, base.g, base.b, alpha);
                }
                fill(batch, x + 1, y + 1, cellWidth - 2, cellHeight - 2, base);
                drawTileDecoration(batch, board.getChapter(), tile, x, y, cellWidth, cellHeight);
                if (board.getChapter() == ChapterType.BIG_WAVE_BEACH && column >= 6) {
                    fill(batch, x + 2, y + 2, cellWidth - 4, 5f,
                        new Color(.70f, .95f, 1f, .72f));
                    text(batch, "LOW", x + 5, y + 15, new Color(.76f, .98f, 1f, .92f));
                }
                if (showGrid.getAsBoolean()) {
                    border(batch, x, y, cellWidth, cellHeight, 1.5f, GRID_RED);
                }
            }
        }
    }

    protected Color tileColor(ChapterType chapter, TileType type, int column, int lane) {
        if (type == TileType.WATER) return new Color(0.12f, 0.46f, 0.75f, 1f);
        if (type == TileType.ICY_GROUND) return new Color(0.55f, 0.82f, 0.90f, 1f);
        if (type == TileType.SLIPPERY_UP || type == TileType.SLIPPERY_DOWN) return new Color(0.43f, 0.70f, 0.80f, 1f);
        if (type == TileType.CRATER) return new Color(0.22f, 0.18f, 0.16f, 1f);
        if (chapter == ChapterType.ANCIENT_EGYPT) return alternating(column, lane,
            new Color(0.67f, 0.55f, 0.29f, 1f), new Color(0.61f, 0.49f, 0.25f, 1f));
        if (chapter == ChapterType.FROSTBITE_CAVES) return alternating(column, lane,
            new Color(0.44f, 0.70f, 0.75f, 1f), new Color(0.38f, 0.63f, 0.70f, 1f));
        if (chapter == ChapterType.DARK_AGES) return alternating(column, lane,
            new Color(0.23f, 0.32f, 0.22f, 1f), new Color(0.19f, 0.28f, 0.19f, 1f));
        return alternating(column, lane,
            new Color(0.32f, 0.63f, 0.28f, 1f), new Color(0.28f, 0.58f, 0.24f, 1f));
    }

    protected Color alternating(int column, int lane, Color a, Color b) {
        return ((column + lane) & 1) == 0 ? a : b;
    }

    protected void drawTileDecoration(Batch batch, ChapterType chapter, Tile tile, float x, float y, float w, float h) {
        if (tile.getType() == TileType.GRAVE) {
            Color grave = tile.getGraveRewardType() == Tile.GRAVE_REWARD_SUN
                ? new Color(.62f, .50f, .20f, 1f)
                : tile.getGraveRewardType() == Tile.GRAVE_REWARD_PLANT_FOOD
                ? new Color(.34f, .50f, .30f, 1f)
                : new Color(0.42f, 0.42f, 0.45f, 1f);
            float health = Math.max(0f, Math.min(1f,
                tile.getGravestoneHp() / (float) Math.max(1, Tile.GRAVE_MAX_HP)));
            Color damagedGrave = new Color(grave).lerp(new Color(.22f, .18f, .16f, 1f), 1f - health);
            float damageSink = (1f - health) * h * .12f;
            boolean rendered = art != null && art.drawGrave(batch, chapter, x + w * .5f,
                y + h * .48f - damageSink, w * (.48f + .10f * health), h * (.64f + .14f * health), damagedGrave);
            if (!rendered) {
                float graveHeight = h * (.42f + .18f * health);
                float graveY = y + h * .18f - damageSink;
                fill(batch, x + w * .33f, graveY, w * .34f, graveHeight, damagedGrave);
                border(batch, x + w * .33f, graveY, w * .34f, graveHeight, 2f, Color.DARK_GRAY);
                String label = tile.getGraveRewardType() == Tile.GRAVE_REWARD_SUN ? "SUN"
                    : tile.getGraveRewardType() == Tile.GRAVE_REWARD_PLANT_FOOD ? "FOOD" : "GRAVE";
                text(batch, label, x + w * .24f, y + h * .54f, Color.WHITE);
                if (health < .72f) {
                    fill(batch, x + w * .48f, graveY + graveHeight * .45f,
                        w * .035f, graveHeight * .35f, new Color(.12f, .10f, .09f, .90f));
                }
                if (health < .38f) {
                    fill(batch, x + w * .39f, graveY + graveHeight * .62f,
                        w * .24f, h * .025f, new Color(.12f, .10f, .09f, .90f));
                }
            }
        }
        if (tile.hasNecromancy()) {
            border(batch, x + 4, y + 4, w - 8, h - 8, 4f, new Color(0.65f, 0.20f, 0.75f, .9f));
            text(batch, "NEC", x + 8, y + h - 8, new Color(0.9f, 0.7f, 1f, 1f));
        }
        if (tile.getType() == TileType.SLIPPERY_UP) {
            text(batch, "^^", x + w * .43f, y + h * .55f, Color.WHITE);
        } else if (tile.getType() == TileType.SLIPPERY_DOWN) {
            text(batch, "vv", x + w * .43f, y + h * .45f, Color.WHITE);
        } else if (tile.getType() == TileType.CRATER) {
            text(batch, "CRATER", x + w * .20f, y + h * .52f, Color.LIGHT_GRAY);
        }
    }

    protected void drawSpecialLines(Batch batch, Board board, float cellWidth, float cellHeight) {
        int line = redLineOverride;
        if (line < 0 && board.getSpecialLevelHandler() instanceof DeadLineLevel deadLine) {
            line = deadLine.getLineColumn();
        }
        if (line >= 0) {
            float x = cellX(line, cellWidth) + cellWidth * .5f;
            fill(batch, x - 3, lawnBottomDraw(), 6, lawnHeight(), new Color(0.95f, .12f, .12f, .9f));
            text(batch, "DO NOT CROSS", x - 42, lawnTopDraw() - 8, Color.WHITE);
        }
        if (board.getChapter() == ChapterType.BIG_WAVE_BEACH) {
            int firstWater = board.getColumns();
            for (int c = 0; c < board.getColumns(); c++) {
                boolean water = false;
                for (int r = 0; r < board.getRows(); r++) {
                    if (board.getTileAt(c, r).getType() == TileType.WATER) {
                        water = true;
                        break;
                    }
                }
                if (water) {
                    firstWater = c;
                    break;
                }
            }
            if (firstWater < board.getColumns()) {
                float x = cellX(firstWater, cellWidth);
                fill(batch, x - 2, lawnBottomDraw(), 4, lawnHeight(), new Color(0.2f, .9f, 1f, .95f));
                text(batch, "TIDE", x + 4, lawnTopDraw() - 8, Color.WHITE);
            }
            
            float maxTideX = cellX(Math.min(4, board.getColumns() - 1), cellWidth);
            fill(batch, maxTideX - 1, lawnBottomDraw(), 2, lawnHeight(), new Color(.6f, .95f, 1f, .55f));
            text(batch, "MAX TIDE", maxTideX + 3, lawnBottomDraw() + 18, Color.WHITE);
        }
        if (board.getChapter() == ChapterType.ANCIENT_EGYPT && board.getCurrentWave() + 1 >= board.getTotalWaves()) {
            float pulse = .30f + .15f * (float)Math.sin(elapsed * 5f);
            float x = cellX(Math.max(4, board.getColumns() - 4), cellWidth);
            fill(batch, x, lawnBottomDraw(), cellWidth * 3f, lawnHeight(), new Color(.90f, .75f, .35f, pulse));
            float tornadoX = x + cellWidth * (1.15f + .45f * (float) Math.sin(elapsed * .9f));
            float tornadoBottom = lawnBottomDraw() + cellHeight * .35f;
            for (int i = 0; i < 9; i++) {
                float p = i / 8f;
                float swirl = (float) Math.sin(elapsed * 9f + i * 1.35f) * cellWidth * (.13f + p * .10f);
                float size = cellWidth * (.13f + p * .35f);
                drawCircle(batch, tornadoX + swirl, tornadoBottom + p * lawnHeight() * .70f,
                    size, new Color(.94f, .83f, .52f, .70f - p * .20f));
            }
            text(batch, "SANDSTORM", x + 8, lawnTopDraw() - 24, Color.WHITE);
        }
        if (board.getChapter() == ChapterType.FROSTBITE_CAVES) {
            for (int lane = 0; lane < board.getRows(); lane++) {
                float windX = lawnLeftDraw() + (elapsed * 90f + lane * 70f) % Math.max(1f, lawnWidth());
                float y = cellY(lane, board, cellHeight) + cellHeight * .72f;
                fill(batch, windX, y, Math.min(80f, cellWidth), 3f, new Color(.75f, .95f, 1f, .65f));
            }
        }
    }

    protected void drawProtectedSeeds(Batch batch, Board board, float cellWidth, float cellHeight) {
        if (!(board.getSpecialLevelHandler() instanceof SaveOurSeedsLevel save)) {
            return;
        }
        for (Plant plant : save.getProtectedPlants()) {
            float x = cellX(plant.getX(), cellWidth);
            float y = cellY(plant.getLane(), board, cellHeight);
            border(batch, x + 3, y + 3, cellWidth - 6, cellHeight - 6, 5f,
                new Color(1f, .85f, .15f, 1f));
            text(batch, "PROTECT", x + 7, y + cellHeight - 8, new Color(1f, .9f, .3f, 1f));
        }
    }

    protected void drawMowersAndBrains(Batch batch, Board board, float cellHeight) {
        boolean[] mowers = board.getLawnMowerAvailability();
        boolean[] brains = brainSupplier == null ? null : brainSupplier.get();
        for (int lane = 0; lane < board.getRows(); lane++) {
            float y = cellY(lane, board, cellHeight);
            if (brains != null && lane < brains.length) {
                if (brains[lane]) {
                    boolean rendered = art != null && art.drawHud(batch, "BRAIN",
                        lawnLeftDraw() - cellHeight * .48f, y + cellHeight * .5f,
                        cellHeight * .82f, cellHeight * .72f, Color.WHITE);
                    if (!rendered) {
                        fill(batch, lawnLeftDraw() - 52, y + cellHeight * .30f, 38, cellHeight * .40f,
                            new Color(.95f, .45f, .65f, 1f));
                        text(batch, "BRAIN", lawnLeftDraw() - 58, y + cellHeight * .58f, Color.WHITE);
                    }
                }
            } else if (lane < mowers.length && mowers[lane]) {
                boolean rendered = art != null && art.drawMower(batch, board.getChapter(),
                    lawnLeftDraw() - cellHeight * .48f, y + cellHeight * .5f, cellHeight * .88f, cellHeight * .72f);
                if (!rendered) {
                    fill(batch, lawnLeftDraw() - 52, y + cellHeight * .28f, 38, cellHeight * .44f,
                        new Color(.75f, .10f, .10f, 1f));
                    fill(batch, lawnLeftDraw() - 56, y + cellHeight * .22f, 10, 10, Color.DARK_GRAY);
                    fill(batch, lawnLeftDraw() - 22, y + cellHeight * .22f, 10, 10, Color.DARK_GRAY);
                    text(batch, "M", lawnLeftDraw() - 38, y + cellHeight * .58f, Color.WHITE);
                }
            }
        }
    }


}
