package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.EntityState;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchSnapshot;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ProjectileState;

import java.util.List;
import java.util.function.Supplier;

 







final class MultiplayerBoardActor extends BoardOverlayRenderer {
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;

    private static final Color FALLBACK_BACKGROUND = new Color(.12f, .30f, .16f, 1f);
    private static final Color REAL_CELL_A = new Color(.20f, .58f, .24f, .10f);
    private static final Color REAL_CELL_B = new Color(.10f, .40f, .18f, .10f);
    private static final Color FALLBACK_CELL_A = new Color(.25f, .60f, .22f, .78f);
    private static final Color FALLBACK_CELL_B = new Color(.20f, .50f, .18f, .78f);
    private static final Color REAL_GRID = new Color(.88f, 1f, .78f, .22f);
    private static final Color FALLBACK_GRID = new Color(.90f, 1f, .82f, .38f);
    private static final Color BRAIN_COLOR = new Color(1f, .50f, .70f, 1f);
    private static final Color HEALTH_BACKGROUND = new Color(.08f, .08f, .07f, .82f);
    private static final Color HEALTH_GOOD = new Color(.25f, .88f, .28f, .96f);
    private static final Color HEALTH_WARN = new Color(1f, .62f, .16f, .96f);
    private static final Color HEALTH_BAD = new Color(.95f, .18f, .16f, .96f);
    private static final Color SLOW_COLOR = new Color(.45f, .86f, 1f, .95f);
    private static final Color HOVER_FILL = new Color(1f, 1f, 1f, .16f);

    private final Supplier<MatchSnapshot> snapshotSupplier;

    MultiplayerBoardActor(BitmapFont font, Supplier<MatchSnapshot> snapshotSupplier) {
        this(font, snapshotSupplier, null);
    }

    MultiplayerBoardActor(BitmapFont font, Supplier<MatchSnapshot> snapshotSupplier,
                          PvzAssetSystem art) {
        
        
        
        super(() -> (Board) null, font, art);
        this.snapshotSupplier = snapshotSupplier == null ? () -> (MatchSnapshot) null : snapshotSupplier;
    }

    @Override
    protected void updateHover(float x, float y) {
        mouseX = x;
        mouseY = y;
        MatchSnapshot snapshot = snapshotSupplier.get();
        if (snapshot == null || getWidth() <= 0f || getHeight() <= 0f) {
            hoverColumn = hoverLane = -1;
            return;
        }

        float left = lawnLeftLocal();
        float bottom = lawnBottomLocal();
        float width = lawnWidth();
        float height = lawnHeight();
        float cellWidth = width / COLUMNS;
        float cellHeight = height / ROWS;
        if (x < left || x >= left + width || y < bottom || y >= bottom + height) {
            hoverColumn = hoverLane = -1;
            return;
        }
        hoverColumn = clamp((int) ((x - left) / cellWidth), 0, COLUMNS - 1);
        int fromBottom = clamp((int) ((y - bottom) / cellHeight), 0, ROWS - 1);
        hoverLane = ROWS - 1 - fromBottom;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (getWidth() <= 0f || getHeight() <= 0f) return;
        MatchSnapshot snapshot = snapshotSupplier.get();
        boolean realBackground = drawBackdrop(batch);
        float cellWidth = lawnWidth() / COLUMNS;
        float cellHeight = lawnHeight() / ROWS;

        drawLawn(batch, cellWidth, cellHeight, realBackground);
        if (snapshot != null) {
            drawBrains(batch, snapshot, cellWidth, cellHeight);
            drawPlants(batch, snapshot.plants(), cellWidth, cellHeight);
            drawZombies(batch, snapshot.zombies(), snapshot.plants(), cellWidth, cellHeight);
            drawProjectiles(batch, snapshot.projectiles(), cellWidth, cellHeight);
        }
        drawHoverCell(batch, cellWidth, cellHeight);
        
        drawCursor(batch);
        batch.setColor(WHITE);
    }

    private boolean drawBackdrop(Batch batch) {
        fill(batch, getX(), getY(), getWidth(), getHeight(), FALLBACK_BACKGROUND);
        
        
        return art != null && art.drawMenuBackground(batch, getX(), getY(), getWidth(), getHeight());
    }

    private void drawLawn(Batch batch, float cellWidth, float cellHeight, boolean realBackground) {
        Color even = realBackground ? REAL_CELL_A : FALLBACK_CELL_A;
        Color odd = realBackground ? REAL_CELL_B : FALLBACK_CELL_B;
        Color line = realBackground ? REAL_GRID : FALLBACK_GRID;
        for (int lane = 0; lane < ROWS; lane++) {
            for (int column = 0; column < COLUMNS; column++) {
                float x = cellX(column, cellWidth);
                float y = onlineCellY(lane, cellHeight);
                fill(batch, x + 1f, y + 1f, cellWidth - 2f, cellHeight - 2f,
                    ((column + lane) & 1) == 0 ? even : odd);
                border(batch, x, y, cellWidth, cellHeight, realBackground ? 1f : 1.5f, line);
            }
        }
    }

    private void drawBrains(Batch batch, MatchSnapshot snapshot, float cellWidth, float cellHeight) {
        boolean[] brains = snapshot.brains();
        if (brains == null) return;
        for (int lane = 0; lane < Math.min(ROWS, brains.length); lane++) {
            if (!brains[lane]) continue;
            float centerX = lawnLeftDraw() - cellWidth * .24f;
            float centerY = onlineCellY(lane, cellHeight) + cellHeight * .5f;
            boolean rendered = art != null && art.drawHud(batch, "BRAIN", centerX, centerY,
                cellHeight * .82f, cellHeight * .78f, Color.WHITE);
            if (!rendered) {
                drawCircle(batch, centerX, centerY, Math.min(cellWidth, cellHeight) * .34f, BRAIN_COLOR);
                text(batch, "B", centerX - 7f, centerY + 5f, Color.WHITE);
            }
        }
    }

    private void drawPlants(Batch batch, List<EntityState> plants, float cellWidth, float cellHeight) {
        if (plants == null) return;
        for (EntityState plant : plants) {
            if (plant == null) continue;
            int column = clamp(plant.column(), 0, COLUMNS - 1);
            int lane = clamp(plant.lane(), 0, ROWS - 1);
            float x = cellX(column, cellWidth);
            float y = onlineCellY(lane, cellHeight);
            PlantType type = plantType(plant.kind());
            float bob = (float) Math.sin(elapsed * 3.1f + plant.id() * .17f) * cellHeight * .025f;
            float centerX = x + cellWidth * .5f;
            float centerY = y + cellHeight * .48f + bob;
            boolean rendered = type != null && art != null && art.drawPlant(batch, type, "idle",
                elapsed + plant.id() * .03f, centerX, centerY, cellWidth * .94f, cellHeight * 1.02f);
            if (!rendered) {
                float size = Math.min(cellWidth, cellHeight) * .54f;
                drawCircle(batch, centerX, centerY, size, plantColor(type));
                text(batch, shortName(plant.kind()), centerX - 16f, centerY + 4f, Color.WHITE);
            }
            drawHealthBar(batch, plant, centerX, centerY, cellWidth, cellHeight);
        }
    }

    private void drawZombies(Batch batch, List<EntityState> zombies, List<EntityState> plants,
                             float cellWidth, float cellHeight) {
        if (zombies == null) return;
        for (EntityState zombie : zombies) {
            if (zombie == null) continue;
            float worldX = finite(zombie.x()) ? (float) zombie.x() : zombie.column();
            worldX = Math.max(-.25f, Math.min(COLUMNS - .05f, worldX));
            int lane = clamp(zombie.lane(), 0, ROWS - 1);
            float centerX = cellXFloat(worldX, cellWidth) + cellWidth * .5f;
            boolean eating = isEating(zombie, plants);
            float walk = eating ? 0f
                : (float) Math.sin(elapsed * 6f + zombie.id() * .11f) * cellHeight * .035f;
            float centerY = onlineCellY(lane, cellHeight) + cellHeight * .49f + walk;
            ZombieType type = zombieType(zombie.kind());
            boolean rendered = type != null && art != null && art.drawZombie(batch, type,
                eating ? "eat" : "walk", elapsed + zombie.id() * .025f, centerX, centerY,
                cellWidth * 1.08f, cellHeight * 1.18f);
            if (!rendered) {
                float size = Math.min(cellWidth, cellHeight) * .49f;
                drawCircle(batch, centerX, centerY, size, zombieColor(zombie));
                text(batch, shortName(zombie.kind()), centerX - 18f, centerY + 5f, Color.WHITE);
            }
            if (zombie.slowed()) {
                float ring = Math.min(cellWidth, cellHeight) * .62f;
                border(batch, centerX - ring * .5f, centerY - ring * .5f, ring, ring, 3f, SLOW_COLOR);
                if (!rendered || showGrid.getAsBoolean()) {
                    text(batch, "SLOW", centerX - 18f, centerY - ring * .52f, Color.WHITE);
                }
            }
            drawHealthBar(batch, zombie, centerX, centerY, cellWidth, cellHeight);
        }
    }

    private void drawProjectiles(Batch batch, List<ProjectileState> projectiles,
                                 float cellWidth, float cellHeight) {
        if (projectiles == null) return;
        for (ProjectileState shot : projectiles) {
            if (shot == null) continue;
            float worldX = finite(shot.x()) ? (float) shot.x() : 0f;
            float centerX = cellXFloat(worldX, cellWidth) + cellWidth * .52f;
            int lane = clamp(shot.lane(), 0, ROWS - 1);
            float centerY = onlineCellY(lane, cellHeight) + cellHeight * .57f;
            Color color = shot.frozen() ? new Color(.45f, .88f, 1f, 1f)
                : new Color(.55f, .94f, .32f, 1f);
            drawCircle(batch, centerX, centerY, shot.frozen() ? 16f : 12f, color);
        }
    }

    private void drawHealthBar(Batch batch, EntityState entity, float centerX, float centerY,
                               float cellWidth, float cellHeight) {
        float ratio = healthRatio(entity);
        float width = Math.min(cellWidth * .74f, 76f);
        float height = Math.max(4f, cellHeight * .045f);
        float x = centerX - width * .5f;
        float y = centerY + cellHeight * .38f;
        fill(batch, x, y, width, height, HEALTH_BACKGROUND);
        Color healthColor = ratio > .45f ? HEALTH_GOOD : ratio > .20f ? HEALTH_WARN : HEALTH_BAD;
        fill(batch, x, y, width * ratio, height, healthColor);
    }

    private void drawHoverCell(Batch batch, float cellWidth, float cellHeight) {
        if (hoverColumn < 0 || hoverLane < 0) return;
        float x = cellX(hoverColumn, cellWidth);
        float y = onlineCellY(hoverLane, cellHeight);
        fill(batch, x + 2f, y + 2f, cellWidth - 4f, cellHeight - 4f, HOVER_FILL);
        border(batch, x + 2f, y + 2f, cellWidth - 4f, cellHeight - 4f, 3f, Color.WHITE);
    }

    private float onlineCellY(int lane, float cellHeight) {
        return lawnBottomDraw() + (ROWS - 1 - clamp(lane, 0, ROWS - 1)) * cellHeight;
    }

    private boolean isEating(EntityState zombie, List<EntityState> plants) {
        if (plants == null) return false;
        double zombieX = zombie.x();
        for (EntityState plant : plants) {
            if (plant != null && plant.lane() == zombie.lane() && plant.column() <= zombieX
                && zombieX - plant.column() <= .72) return true;
        }
        return false;
    }

    private Color plantColor(PlantType type) {
        if (type == null || PlantRegistry.get(type) == null) return new Color(.30f, .75f, .30f, 1f);
        return super.plantColor(PlantRegistry.get(type).getFamily());
    }

    private Color zombieColor(EntityState zombie) {
        if (zombie.slowed()) return new Color(.35f, .75f, .95f, 1f);
        int hash = zombie.kind() == null ? 0 : zombie.kind().hashCode();
        float red = .34f + Math.floorMod(hash, 5) * .05f;
        float green = .46f + Math.floorMod(hash, 3) * .05f;
        float blue = .30f + Math.floorMod(hash, 4) * .035f;
        return new Color(red, green, blue, 1f);
    }

    private PlantType plantType(String kind) {
        if (kind == null) return null;
        try {
            return PlantType.valueOf(kind.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ZombieType zombieType(String kind) {
        if (kind == null) return null;
        try {
            return ZombieType.valueOf(kind.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private float healthRatio(EntityState entity) {
        if (entity == null || !finite(entity.health()) || !finite(entity.maxHealth())
            || entity.maxHealth() <= 0d) return 0f;
        return Math.max(0f, Math.min(1f, (float) (entity.health() / entity.maxHealth())));
    }

    private boolean finite(double value) {
        return Double.isFinite(value);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
