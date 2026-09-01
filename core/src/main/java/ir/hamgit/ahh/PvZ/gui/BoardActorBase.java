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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

 





abstract class BoardActorBase extends Actor {
    
    
    
    
    protected static final float LAWN_LEFT_RATIO = 0.245f;
    protected static final float LAWN_RIGHT_RATIO = 0.965f;
    protected static final float LAWN_BOTTOM_RATIO = 0.075f;
    protected static final float LAWN_TOP_RATIO = 0.765f;
    protected static final float BACKGROUND_ASPECT = 4f / 3f;
    protected static final Color GRID_RED = new Color(0.85f, 0.13f, 0.13f, 0.85f);
    protected static final Color WHITE = new Color(Color.WHITE);

    protected final Supplier<Board> boardSupplier;
    protected final BitmapFont font;
    protected final PvzAssetSystem art;
    protected final Texture pixel;
    protected final Texture circle;
    protected final BoardVisualTracker visuals = new BoardVisualTracker();
    protected BooleanSupplier showGrid = () -> false;
    protected BooleanSupplier animationPaused = () -> false;
    protected BoardActor.CellClickListener cellClickListener;
    protected BoardActor.CellHoverListener cellHoverListener;
    protected BoardActor.CellMarkerProvider cellMarkerProvider;
    protected Predicate<Sun> fallingSunCollector;
    protected Runnable cancelAction;
    protected Supplier<List<BowlingBall>> bowlingBallsSupplier;
    protected Supplier<boolean[]> brainSupplier;
    protected int redLineOverride = -1;
    protected int hoverColumn = -1;
    protected int hoverLane = -1;
    protected float mouseX;
    protected float mouseY;
    protected String cursorText;
    protected PlantType cursorPlant;
    protected float elapsed;
    protected int plantFoodColumn = -1;
    protected int plantFoodLane = -1;
    protected float plantFoodAnimationUntil;
    protected float topUiInset;
    protected float bottomUiInset;
    protected float backgroundVerticalOffset;

    protected BoardActorBase(Supplier<Board> boardSupplier, BitmapFont font) {
        this(boardSupplier, font, null);
    }

    protected BoardActorBase(Supplier<Board> boardSupplier, BitmapFont font, PvzAssetSystem art) {
        this.boardSupplier = boardSupplier;
        this.font = font;
        this.art = art;
        this.pixel = makePixel();
        this.circle = makeCircle();
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.LEFT && tryClickFallingSun(x, y)) {
                    return true;
                }
                updateHover(x, y);
                if (button == Input.Buttons.RIGHT) {
                    if (cancelAction != null) cancelAction.run();
                    return true;
                }
                if (button != Input.Buttons.LEFT) return false;
                if (hoverColumn >= 0 && cellClickListener != null) {
                    cellClickListener.clicked(hoverColumn, hoverLane);
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                updateHover(x, y);
                if (hoverColumn >= 0 && cellHoverListener != null) {
                    cellHoverListener.hovered(hoverColumn, hoverLane);
                }
                return true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                hoverColumn = -1;
                hoverLane = -1;
            }
        });
    }

    protected Texture makePixel() {
        Pixmap map = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        map.setColor(Color.WHITE);
        map.fill();
        Texture result = new Texture(map);
        map.dispose();
        return result;
    }

    protected Texture makeCircle() {
        Pixmap map = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        map.setColor(0f, 0f, 0f, 0f);
        map.fill();
        map.setColor(Color.WHITE);
        map.fillCircle(32, 32, 30);
        Texture result = new Texture(map);
        map.dispose();
        return result;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!animationPaused.getAsBoolean()) elapsed += delta;
        visuals.update(boardSupplier.get(), elapsed);
    }

    public void setVisualCueListener(Consumer<String> listener) {
        visuals.setCueListener(listener);
    }

    public void setShowGridSupplier(BooleanSupplier showGrid) {
        this.showGrid = showGrid == null ? () -> false : showGrid;
    }

    public void setAnimationPausedSupplier(BooleanSupplier animationPaused) {
        this.animationPaused = animationPaused == null ? () -> false : animationPaused;
    }

    public void setCellClickListener(BoardActor.CellClickListener listener) {
        this.cellClickListener = listener;
    }

    public void setCellHoverListener(BoardActor.CellHoverListener listener) {
        this.cellHoverListener = listener;
    }

    public void setCellMarkerProvider(BoardActor.CellMarkerProvider provider) {
        this.cellMarkerProvider = provider;
    }

    public void setFallingSunCollector(Predicate<Sun> collector) {
        this.fallingSunCollector = collector;
    }

     
    public void setCancelAction(Runnable cancelAction) {
        this.cancelAction = cancelAction;
    }

     
    public void setGameplayInsets(float top, float bottom) {
        topUiInset = Math.max(0f, top);
        bottomUiInset = Math.max(0f, bottom);
    }

    public void setBackgroundVerticalOffset(float offset) {
        backgroundVerticalOffset = offset;
    }

    public void setBowlingBallsSupplier(Supplier<List<BowlingBall>> supplier) {
        this.bowlingBallsSupplier = supplier;
    }

    public void setBrainSupplier(Supplier<boolean[]> supplier) {
        this.brainSupplier = supplier;
    }

    public void setRedLineOverride(int column) {
        this.redLineOverride = column;
    }

    public void setCursorText(String cursorText) {
        this.cursorText = cursorText;
        this.cursorPlant = null;
    }

    public void setCursorPlant(PlantType cursorPlant) {
        this.cursorPlant = cursorPlant;
        this.cursorText = cursorPlant == null ? null : cursorPlant.name();
    }

     
    public void triggerPlantFoodAnimation(int column, int lane) {
        plantFoodColumn = column;
        plantFoodLane = lane;
        plantFoodAnimationUntil = elapsed + 1.35f;
    }

    public int getHoverColumn() {
        return hoverColumn;
    }

    public int getHoverLane() {
        return hoverLane;
    }

    protected void updateHover(float x, float y) {
        mouseX = x;
        mouseY = y;
        Board board = boardSupplier.get();
        if (board == null) {
            hoverColumn = hoverLane = -1;
            return;
        }
        float left = lawnLeftLocal();
        float bottom = lawnBottomLocal();
        float boardWidth = lawnWidth();
        float boardHeight = lawnHeight();
        float cellWidth = boardWidth / board.getColumns();
        float cellHeight = boardHeight / board.getRows();
        if (x < left || x >= left + boardWidth || y < bottom || y >= bottom + boardHeight) {
            hoverColumn = hoverLane = -1;
            return;
        }
        hoverColumn = Math.min(board.getColumns() - 1, (int) ((x - left) / cellWidth));
        int fromBottom = Math.min(board.getRows() - 1, (int) ((y - bottom) / cellHeight));
        hoverLane = board.getRows() - 1 - fromBottom;
    }

    protected boolean tryClickFallingSun(float x, float y) {
        Board board = boardSupplier.get();
        if (board == null || (cellClickListener == null && fallingSunCollector == null)) return false;
        float width = lawnWidth();
        float height = lawnHeight();
        if (width <= 0f || height <= 0f) return false;
        float cellWidth = width / board.getColumns();
        float cellHeight = height / board.getRows();
        float fallTravel = Math.min(180f, getHeight() * .28f);
        float hitRadius = Math.max(24f, Math.min(cellWidth, cellHeight) * .38f);
        for (Sun sun : board.getSuns()) {
            if (sun.isCollected() || sun.isOnGround() || sun.isProducedByPlant()) continue;
            float sunX = lawnLeftLocal() + sun.getX() * cellWidth + cellWidth * .73f;
            float targetY = lawnBottomLocal()
                + (board.getRows() - 1 - sun.getLane()) * cellHeight + cellHeight * .72f;
            float sunY = targetY + (1f - sun.getFallProgress()) * fallTravel;
            float dx = x - sunX;
            float dy = y - sunY;
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                if (fallingSunCollector != null) {
                    return fallingSunCollector.test(sun);
                }
                cellClickListener.clicked(sun.getX(), sun.getLane());
                return true;
            }
        }
        return false;
    }

    protected float lawnLeftLocal() {
        return backgroundLeftLocal() + backgroundWidth() * LAWN_LEFT_RATIO;
    }

    protected float lawnBottomLocal() {
        float natural = backgroundBottomLocal() + backgroundHeight() * LAWN_BOTTOM_RATIO;
        return Math.max(natural, bottomUiInset);
    }

    protected float lawnWidth() {
        return Math.max(1f, backgroundWidth() * (LAWN_RIGHT_RATIO - LAWN_LEFT_RATIO));
    }

    protected float lawnHeight() {
        float naturalTop = backgroundBottomLocal() + backgroundHeight() * LAWN_TOP_RATIO;
        float usableTop = Math.min(naturalTop, getHeight() - topUiInset);
        return Math.max(1f, usableTop - lawnBottomLocal());
    }

    protected float backgroundWidth() {
        return Math.max(getWidth(), getHeight() * BACKGROUND_ASPECT);
    }

    protected float backgroundHeight() {
        return Math.max(getHeight(), getWidth() / BACKGROUND_ASPECT);
    }

    protected float backgroundLeftLocal() {
        return (getWidth() - backgroundWidth()) * .5f;
    }

    protected float backgroundBottomLocal() {
        return (getHeight() - backgroundHeight()) * .5f;
    }

    protected float lawnLeftDraw() {
        return drawOriginX() + lawnLeftLocal();
    }

    protected float lawnBottomDraw() {
        return drawOriginY() + lawnBottomLocal();
    }

    protected float drawOriginX() {
        return getX() + visuals.shakeX(elapsed);
    }

    protected float drawOriginY() {
        return getY() + visuals.shakeY(elapsed);
    }

    protected float lawnTopDraw() {
        return lawnBottomDraw() + lawnHeight();
    }

    protected float cellX(int column, float cellWidth) {
        return lawnLeftDraw() + column * cellWidth;
    }

    protected float cellXFloat(float column, float cellWidth) {
        return lawnLeftDraw() + column * cellWidth;
    }

    protected float cellY(int lane, Board board, float cellHeight) {
        return lawnBottomDraw() + (board.getRows() - 1 - lane) * cellHeight;
    }

    protected void fill(Batch batch, float x, float y, float w, float h, Color color) {
        batch.setColor(color);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(WHITE);
    }

    protected void drawCircle(Batch batch, float centerX, float centerY, float size, Color color) {
        batch.setColor(color);
        batch.draw(circle, centerX - size * .5f, centerY - size * .5f, size, size);
        batch.setColor(WHITE);
    }

    protected void border(Batch batch, float x, float y, float w, float h, float thickness, Color color) {
        fill(batch, x, y, w, thickness, color);
        fill(batch, x, y + h - thickness, w, thickness, color);
        fill(batch, x, y, thickness, h, color);
        fill(batch, x + w - thickness, y, thickness, h, color);
    }

    protected void text(Batch batch, String value, float x, float y, Color color) {
        font.setColor(color);
        font.draw(batch, value, x, y);
        font.setColor(Color.WHITE);
    }

    protected String shortName(String raw) {
        if (raw == null || raw.isBlank()) return "?";
        String[] parts = raw.split("_");
        if (parts.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) sb.append(p.charAt(0));
                if (sb.length() >= 4) break;
            }
            return sb.toString();
        }
        return raw.substring(0, Math.min(4, raw.length()));
    }

    public void dispose() {
        pixel.dispose();
        circle.dispose();
    }

}
