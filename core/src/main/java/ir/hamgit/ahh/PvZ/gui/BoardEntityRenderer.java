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
import ir.hamgit.ahh.PvZ.model.PlantFoodPickup;
import ir.hamgit.ahh.PvZ.model.Sun;
import ir.hamgit.ahh.PvZ.model.Tile;
import ir.hamgit.ahh.PvZ.model.entities.Armor;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.entities.Zombie;
import ir.hamgit.ahh.PvZ.model.enums.ArmorType;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantFamily;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.SunType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;
import ir.hamgit.ahh.PvZ.model.minigame.BowlingBall;
import ir.hamgit.ahh.PvZ.model.special.DeadLineLevel;
import ir.hamgit.ahh.PvZ.model.special.SaveOurSeedsLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


 
abstract class BoardEntityRenderer extends BoardTerrainRenderer {
    protected BoardEntityRenderer(Supplier<Board> boardSupplier, BitmapFont font, PvzAssetSystem art) {
        super(boardSupplier, font, art);
    }

    protected void drawPlants(Batch batch, Board board, float cellWidth, float cellHeight) {
        for (int lane = 0; lane < board.getRows(); lane++) {
            for (int column = 0; column < board.getColumns(); column++) {
                Tile tile = board.getTileAt(column, lane);
                if (tile == null || tile.isEmpty()) continue;
                List<Plant> layers = tile.getPlantLayers();
                for (int i = 0; i < layers.size(); i++) {
                    Plant plant = layers.get(i);
                    float x = cellX(column, cellWidth);
                    float y = cellY(lane, board, cellHeight);
                    drawPlant(batch, plant, x, y, cellWidth, cellHeight, i, layers.size());
                }
            }
        }
    }

    protected void drawPlant(Batch batch, Plant plant, float x, float y, float w, float h,
                           int layer, int layerCount) {
        float bob = (float) Math.sin(elapsed * 3.2f + plant.getX() * .7f + plant.getLane()) * h * .025f;
        float scale = (1f + (float) Math.sin(elapsed * 2.6f + plant.getDef().getType().ordinal()) * .035f)
            * visuals.plantSpawnScale(plant, elapsed);
        float size = Math.min(w, h) * .54f * scale;
        float cx = x + w * .5f + layer * 3;
        float cy = y + h * .48f + bob + layer * 2;
        Color base = plantColor(plant.getDef().getFamily());
        if (!plant.isAlive()) base = Color.DARK_GRAY;
        boolean plantFoodPlaying = plant.getX() == plantFoodColumn && plant.getLane() == plantFoodLane
            && elapsed <= plantFoodAnimationUntil;
        String trackedAction = visuals.plantAction(plant, elapsed);
        String action = plantFoodPlaying ? "plantFood"
            : "plant".equals(trackedAction) || "hurt".equals(trackedAction) ? "idle" : trackedAction;
        boolean rendered = plant.isAlive() && art != null && art.drawPlant(batch, plant.getDef().getType(), action,
            elapsed + plant.getX() * .11f + plant.getLane() * .07f, cx, cy,
            w * .94f * scale, h * 1.02f * scale);
        if (!rendered) {
            if (art != null && !plant.isCat()) {
                art.drawProceduralPlant(batch, plant.getDef().getType(), action,
                    elapsed + plant.getX() * .11f + plant.getLane() * .07f,
                    cx, cy, w * .88f * scale, h * .94f * scale, base);
            } else {
                drawCircle(batch, cx, cy, size, base);
                text(batch, plant.isCat() ? "CAT" : shortName(plant.getDef().getType().name()),
                    cx - 16, cy + 4, Color.WHITE);
            }
        }
        float hitFlash = visuals.hitFlash(plant, elapsed);
        if (hitFlash > 0f) {
            drawCircle(batch, cx, cy, size * (1.05f + (1f - hitFlash) * .25f),
                new Color(1f, .32f, .20f, hitFlash * .48f));
        }
        if (plant.getDef().hasBehavior(BehaviorType.CHARGED_ATTACK)
            || plant.getDef().hasBehavior(BehaviorType.ARM_DELAY)) {
            float charge = .5f + .5f * (float) Math.sin(elapsed * 4.5f + plant.getX());
            border(batch, cx - size * (.54f + charge * .10f), cy - size * (.54f + charge * .10f),
                size * (1.08f + charge * .20f), size * (1.08f + charge * .20f), 2f,
                new Color(1f, .65f, .15f, .28f + charge * .38f));
        }
        if (plant.getDef().hasBehavior(BehaviorType.FAMILY_BOOST)) {
            float aura = .45f + .25f * (float) Math.sin(elapsed * 5.5f);
            drawCircle(batch, cx, cy, size * 1.62f,
                new Color(base.r, base.g, base.b, .16f + aura * .16f));
            border(batch, cx - size * .76f, cy - size * .76f, size * 1.52f, size * 1.52f,
                2f, new Color(1f, .85f, .25f, aura));
        }
        if (plant.isBoosted()) {
            border(batch, cx - size * .58f, cy - size * .58f, size * 1.16f, size * 1.16f, 3f,
                new Color(1f, .78f, .08f, 1f));
        }
        if (!rendered || showGrid.getAsBoolean()) {
            text(batch, "L" + plant.getLevel(), cx - 11, cy - size * .34f, new Color(1f, 1f, .85f, 1f));
        }
        if (plant.getIceLayers() > 0) {
            Color ice = plant.isOctopusCovered() ? new Color(.70f, .30f, .85f, .80f)
                : new Color(.65f, .92f, 1f, .72f);
            float pad = 3 + plant.getIceLayers() * 2f;
            border(batch, cx - size * .56f - pad, cy - size * .56f - pad,
                size * 1.12f + pad * 2, size * 1.12f + pad * 2, 3f, ice);
            text(batch, plant.isOctopusCovered() ? "OCTO" : "ICE" + plant.getIceLayers(),
                cx - 18, cy + size * .46f, Color.WHITE);
        }
        if (layerCount > 1 && (!rendered || showGrid.getAsBoolean())) {
            text(batch, (layer + 1) + "/" + layerCount, x + 4, y + 16 + layer * 12, Color.WHITE);
        }
    }

    protected Color plantColor(PlantFamily family) {
        if (family == null) return new Color(.30f, .75f, .30f, 1f);
        return switch (family) {
            case ENLIGHTEN -> new Color(.95f, .78f, .15f, 1f);
            case APPEASE -> new Color(.30f, .75f, .25f, 1f);
            case ARMA -> new Color(.75f, .55f, .20f, 1f);
            case BOMBARD -> new Color(.90f, .28f, .18f, 1f);
            case ENFORCE -> new Color(.75f, .30f, .55f, 1f);
            case REINFORCE -> new Color(.45f, .58f, .20f, 1f);
            case ENCHANT -> new Color(.66f, .30f, .78f, 1f);
            case PIERCE -> new Color(.22f, .65f, .62f, 1f);
            case CATTAIL -> new Color(.85f, .45f, .70f, 1f);
        };
    }

    protected void drawProjectiles(Batch batch, Board board, float cellWidth, float cellHeight) {
        for (Projectile projectile : board.getProjectiles()) {
            float x = cellXFloat((float) projectile.getX(), cellWidth) + cellWidth * .52f;
            float y = cellY(projectile.getLane(), board, cellHeight) + cellHeight * .57f;
            if (projectile.isLobber()) {
                float distance = Math.abs((float) projectile.getX() - projectile.getStartX());
                float t = Math.min(1f, distance / Math.max(1f, board.getColumns() * .65f));
                y += (float) Math.sin(t * Math.PI) * cellHeight * .75f;
            }
            Color color = projectile.isFire() ? new Color(1f, .28f, .05f, 1f)
                : projectile.isIce() ? new Color(.45f, .88f, 1f, 1f)
                : projectile.isPoison() ? new Color(.50f, .85f, .22f, 1f)
                : projectile.getSplashRadius() > 0 ? new Color(.95f, .65f, .18f, 1f)
                : new Color(.55f, .92f, .35f, 1f);
            float size = projectile.isLobber() ? 18f : 12f;
            drawCircle(batch, x, y, size, color);
        }
    }

    protected void drawZombies(Batch batch, Board board, float cellWidth, float cellHeight) {
        List<Zombie> sorted = new ArrayList<>(board.getZombies());
        sorted.sort(Comparator.comparingInt(Zombie::getLane)
            .thenComparing(Comparator.comparingDouble(Zombie::getX).reversed()));
        for (Zombie zombie : sorted) {
            if (!zombie.isAlive()) continue;
            float displayColumn = visuals.zombieDisplayColumn(zombie, elapsed);
            float x = cellXFloat(displayColumn, cellWidth) + cellWidth * .5f;
            float y = cellY(zombie.getLane(), board, cellHeight) + cellHeight * .49f
                + visuals.zombieThrowArc(zombie, elapsed) * cellHeight * 1.25f;
            boolean eating = board.getPlantInFrontOf(zombie) != null;
            float walk = (float) Math.sin(elapsed * (eating ? 8f : 6f) + zombie.getX()) * cellHeight * .035f;
            if (!eating) y += walk;
            float spawnScale = visuals.zombieSpawnScale(zombie, elapsed);
            float size = Math.min(cellWidth, cellHeight) * .49f * spawnScale;
            Color body = zombieColor(zombie);
            String action = visuals.zombieAction(zombie, eating, elapsed);
            boolean rendered = art != null && art.drawZombie(batch, zombie.getDef().getType(), action,
                elapsed + (float) zombie.getX() * .09f + zombie.getLane() * .13f, x, y,
                cellWidth * 1.08f * spawnScale, cellHeight * 1.18f * spawnScale);
            if (!rendered) {
                if (art != null) {
                    art.drawProceduralZombie(batch, zombie.getDef().getType(), action,
                        elapsed + (float) zombie.getX() * .09f + zombie.getLane() * .13f,
                        x, y, cellWidth * 1.02f * spawnScale, cellHeight * 1.13f * spawnScale, body);
                } else {
                    drawCircle(batch, x, y, size, body);
                    text(batch, shortName(zombie.getDef().getType().name()), x - 17, y + 5, Color.WHITE);
                }
            }
            drawArmor(batch, zombie, x, y, size);
            drawZombieEffects(batch, zombie, x, y, size, rendered);
            float hitFlash = visuals.hitFlash(zombie, elapsed);
            if (hitFlash > 0f) {
                drawCircle(batch, x, y, size * (1.10f + (1f - hitFlash) * .22f),
                    new Color(1f, .34f, .18f, hitFlash * .42f));
            }
            if (zombie.getX() < 1.45) {
                float warning = .45f + .35f * (float) Math.sin(elapsed * 12f);
                border(batch, x - size * .72f, y - size * .72f, size * 1.44f, size * 1.44f,
                    4f, new Color(1f, .08f, .04f, warning));
                text(batch, "DANGER", x - 28f, y + size * .88f, new Color(1f, .30f, .18f, 1f));
            }
        }
    }

    protected Color zombieColor(Zombie zombie) {
        int n = zombie.getDef().getType().ordinal();
        float r = .35f + (n % 5) * .05f;
        float g = .47f + (n % 3) * .05f;
        float b = .30f + (n % 4) * .035f;
        if (zombie.isHypnotized()) return new Color(.58f, .28f, .70f, 1f);
        if (zombie.isFrozen()) return new Color(.35f, .75f, .95f, 1f);
        if (zombie.isSlowed()) return new Color(.43f, .65f, .78f, 1f);
        return new Color(r, g, b, 1f);
    }

    protected void drawArmor(Batch batch, Zombie zombie, float x, float y, float size) {
        for (Armor armor : zombie.getArmors()) {
            if (armor.isDestroyed() || armor.getType() == ArmorType.NONE) continue;
            Color color = switch (armor.getType()) {
                case CONE -> new Color(1f, .45f, .08f, 1f);
                case BUCKET -> new Color(.65f, .70f, .74f, 1f);
                case HELMET -> new Color(.38f, .40f, .45f, 1f);
                case SHOULDER -> new Color(.55f, .34f, .20f, 1f);
                case BLOCK -> new Color(.55f, .80f, .90f, 1f);
                case NEWSPAPER -> new Color(.90f, .90f, .82f, 1f);
                default -> new Color(.55f, .45f, .32f, 1f);
            };
            float fraction = armor.getCurrentHp() / (float) Math.max(1, Armor.getMaxHp(armor.getType()));
            float dent = (1f - fraction) * size * .16f;
            border(batch, x - size * .60f + dent, y - size * .60f,
                size * 1.2f - dent * 2f, size * (1.02f + fraction * .18f),
                2f + fraction * 3f, color);
            if (fraction < .70f) {
                fill(batch, x - 2f, y + size * .38f, 4f, size * .28f,
                    new Color(.16f, .13f, .10f, .85f));
            }
            if (fraction < .35f) {
                fill(batch, x - size * .28f, y + size * .20f, size * .56f, 3f,
                    new Color(.16f, .13f, .10f, .85f));
            }
            break;
        }
    }

    protected void drawZombieEffects(Batch batch, Zombie zombie, float x, float y, float size, boolean rendered) {
        if (zombie.isFrozen()) {
            border(batch, x - size * .68f, y - size * .68f, size * 1.36f, size * 1.36f, 4f,
                new Color(.55f, .90f, 1f, .9f));
            if (!rendered || showGrid.getAsBoolean()) text(batch, "FROZEN", x - 26, y - size * .63f, Color.WHITE);
        } else if (zombie.isSlowed() && (!rendered || showGrid.getAsBoolean())) {
            text(batch, "SLOW", x - 18, y - size * .62f, new Color(.75f, .9f, 1f, 1f));
        }
        if (!zombie.getActiveEffects().isEmpty() && (!rendered || showGrid.getAsBoolean())) {
            String effect = zombie.getActiveEffects().keySet().iterator().next();
            text(batch, shortName(effect.toUpperCase(Locale.ROOT)), x - 14, y + size * .82f,
                new Color(1f, .9f, .35f, 1f));
        }
    }

    protected void drawSuns(Batch batch, Board board, float cellWidth, float cellHeight) {
        for (Sun sun : board.getSuns()) {
            if (sun.isCollected()) continue;
            float x = cellX(sun.getX(), cellWidth) + cellWidth * .73f;
            float targetY = cellY(sun.getLane(), board, cellHeight) + cellHeight * .72f;
            float y = targetY + (1f - sun.getFallProgress()) * Math.min(180f, getHeight() * .28f);
            Color c = sun.getType() == SunType.RADIOACTIVE ? new Color(.75f, .30f, .95f, 1f)
                : sun.getType() == SunType.SPECIAL ? new Color(1f, .78f, .06f, 1f)
                : new Color(1f, .90f, .15f, 1f);
            float size = sun.getType() == SunType.SPECIAL ? 34f : 28f;
            boolean rendered = art != null && art.drawHud(batch, "SUN", x, y, size * 1.35f, size * 1.35f, c);
            if (!rendered) drawCircle(batch, x, y, size, c);
            if (!rendered || showGrid.getAsBoolean()) {
                text(batch, String.valueOf(sun.getValue()), x - 12, y + 4, Color.BLACK);
            }
        }
    }

    protected void drawPlantFoodPickups(Batch batch, Board board, float cellWidth, float cellHeight) {
        for (PlantFoodPickup pickup : board.getPlantFoodPickups()) {
            float x = cellX(pickup.getX(), cellWidth) + cellWidth * .55f;
            float groundY = cellY(pickup.getLane(), board, cellHeight) + cellHeight * .54f;
            float y = groundY + (1f - pickup.getDropProgress()) * cellHeight * .72f
                + (float) Math.sin(elapsed * 5f + pickup.getX()) * 4f;
            float pulse = 1f + .10f * (float) Math.sin(elapsed * 7f);
            drawCircle(batch, x, y, 45f * pulse, new Color(.26f, 1f, .32f, .28f));
            drawCircle(batch, x - 7f, y + 3f, 27f * pulse, new Color(.20f, .78f, .18f, 1f));
            drawCircle(batch, x + 8f, y + 7f, 24f * pulse, new Color(.42f, .96f, .28f, 1f));
            fill(batch, x - 2f, y - 17f, 4f, 32f, new Color(.10f, .45f, .10f, 1f));
            border(batch, x - 24f, y - 24f, 48f, 48f, 2f, new Color(.75f, 1f, .45f, .72f));
        }
    }

    protected void drawTransientEffects(Batch batch, Board board, float cellWidth, float cellHeight) {
        for (BoardVisualTracker.Effect effect : visuals.effects()) {
            float p = effect.progress(elapsed);
            float x = cellXFloat(effect.column, cellWidth) + cellWidth * .5f;
            float y = cellY(effect.lane, board, cellHeight) + cellHeight * .50f;
            float fade = 1f - p;
            Color c = new Color(effect.color.r, effect.color.g, effect.color.b, effect.color.a * fade);
            switch (effect.kind) {
                case PLANT_SPAWN, ZOMBIE_SPAWN -> {
                    float radius = Math.min(cellWidth, cellHeight) * (.22f + p * .52f);
                    border(batch, x - radius, y - radius, radius * 2f, radius * 2f,
                        Math.max(1f, 4f * fade), c);
                }
                case HIT, ICE_HIT, OCTOPUS_HIT, IMPACT, GRAVE_HIT -> {
                    for (int i = 0; i < 4; i++) {
                        double angle = i * Math.PI / 2.0 + p * 3.5;
                        float distance = p * Math.min(cellWidth, cellHeight) * .42f;
                        drawCircle(batch, x + (float) Math.cos(angle) * distance,
                            y + (float) Math.sin(angle) * distance,
                            Math.max(3f, 13f * fade), c);
                    }
                }
                case EXPLOSION -> {
                    drawCircle(batch, x, y, Math.min(cellWidth, cellHeight) * (1.0f + p * 2.1f),
                        new Color(1f, .18f, .03f, .18f * fade));
                    for (int i = 0; i < 10; i++) {
                        double angle = i * Math.PI / 5.0 + p * 2f;
                        float distance = p * Math.min(cellWidth, cellHeight) * 1.15f;
                        drawCircle(batch, x + (float) Math.cos(angle) * distance,
                            y + (float) Math.sin(angle) * distance,
                            (8f + (i % 3) * 4f) * fade, i % 2 == 0 ? c
                                : new Color(1f, .90f, .18f, fade));
                    }
                }
                case ARMOR_DEBRIS -> {
                    float dx = (p * 1.4f - .2f) * cellWidth;
                    float dy = (float) Math.sin(p * Math.PI) * cellHeight * .85f - p * cellHeight * .30f;
                    fill(batch, x + dx, y + dy, 18f * fade + 5f, 12f * fade + 4f, c);
                }
                case HEAD -> {
                    float dx = p * cellWidth * .72f;
                    float dy = (float) Math.sin(p * Math.PI) * cellHeight * 1.0f - p * cellHeight * .42f;
                    drawCircle(batch, x + dx, y + dy, 27f * (1f - p * .25f), c);
                    drawCircle(batch, x + dx - 5f, y + dy + 3f, 5f, Color.WHITE);
                }
                case ARM -> {
                    float dx = -p * cellWidth * .56f;
                    float dy = (float) Math.sin(p * Math.PI) * cellHeight * .70f - p * cellHeight * .38f;
                    fill(batch, x + dx, y + dy, 9f, 32f * fade + 5f, c);
                }
                case DEATH -> {
                    boolean drawn = art != null && effect.zombieType != null
                        && art.drawZombie(batch, effect.zombieType, "die", p,
                            x, y - p * cellHeight * .12f, cellWidth * 1.08f, cellHeight * 1.18f);
                    if (!drawn && art != null && effect.zombieType != null) {
                        art.drawProceduralZombie(batch, effect.zombieType, "die", p,
                            x, y, cellWidth, cellHeight, c);
                    }
                }
                case DUST -> {
                    for (int i = 0; i < 12; i++) {
                        double angle = i * .91 + effect.start * 3.0;
                        float distance = p * Math.min(cellWidth, cellHeight) * (.25f + (i % 4) * .13f);
                        drawCircle(batch, x + (float) Math.cos(angle) * distance,
                            y - cellHeight * .20f + (float) Math.sin(angle) * distance * .45f,
                            (4f + i % 3 * 2f) * fade, c);
                    }
                }
                case MOWER -> {
                    float mowerX = cellXFloat(-.55f + p * (board.getColumns() + 1.4f), cellWidth) + cellWidth * .5f;
                    boolean drawn = art != null && art.drawMower(batch, board.getChapter(), mowerX, y,
                        cellHeight * .88f, cellHeight * .72f);
                    if (!drawn) {
                        fill(batch, mowerX - 24f, y - 15f, 48f, 30f, new Color(.82f, .08f, .06f, 1f));
                        drawCircle(batch, mowerX - 17f, y - 17f, 14f, Color.DARK_GRAY);
                        drawCircle(batch, mowerX + 17f, y - 17f, 14f, Color.DARK_GRAY);
                    }
                    for (int i = 0; i < 5; i++) {
                        drawCircle(batch, mowerX - 30f - i * 12f, y - 12f + (i % 2) * 5f,
                            11f * fade, new Color(.72f, .62f, .42f, .65f * fade));
                    }
                }
                case THROWN_IMP -> {
                    float sourceX = cellXFloat(effect.sourceColumn, cellWidth) + cellWidth * .5f;
                    float trailX = sourceX + (x - sourceX) * p;
                    float trailY = y + (float) Math.sin(p * Math.PI) * cellHeight * 1.25f;
                    drawCircle(batch, trailX, trailY, 15f * fade, new Color(.85f, .90f, .55f, .55f * fade));
                }
                case MUZZLE -> drawCircle(batch, x + cellWidth * .30f, y + cellHeight * .09f,
                    22f * (1f - p * .55f), c);
                case SUN_PRODUCE -> {
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4.0 + p;
                        float distance = p * cellHeight * .55f;
                        drawCircle(batch, x + (float) Math.cos(angle) * distance,
                            y + (float) Math.sin(angle) * distance, 8f * fade, c);
                    }
                }
                case PLANT_ACTION -> {
                    if (art != null && effect.plantType != null) {
                        boolean drawn = art.drawPlant(batch, effect.plantType, "attack", p,
                            x, y, cellWidth, cellHeight);
                        if (!drawn) art.drawProceduralPlant(batch, effect.plantType, "attack", p,
                            x, y, cellWidth * .9f, cellHeight * .9f, c);
                    }
                }
            }
        }
        batch.setColor(WHITE);
    }


}
