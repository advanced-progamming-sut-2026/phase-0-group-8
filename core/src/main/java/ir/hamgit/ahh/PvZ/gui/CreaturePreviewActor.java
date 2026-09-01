package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import ir.hamgit.ahh.PvZ.model.enums.PlantFamily;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;

 
public final class CreaturePreviewActor extends Actor {
    private final PvzAssetSystem art;
    private final Texture ownedCircle;
    private PlantType plant;
    private ZombieType zombie;
    private float elapsed;

    public CreaturePreviewActor(BitmapFont font) {
        this(font, null);
    }

    public CreaturePreviewActor(BitmapFont font, PvzAssetSystem art) {
        this.art = art;
        
        
        
        ownedCircle = art == null ? makeCircle() : null;
    }

    private Texture makeCircle() {
        Pixmap map = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        map.setColor(0, 0, 0, 0);
        map.fill();
        map.setColor(Color.WHITE);
        map.fillCircle(32, 32, 30);
        Texture result = new Texture(map);
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        map.dispose();
        return result;
    }

    public void showPlant(PlantType type) {
        plant = type;
        zombie = null;
    }

    public void showZombie(ZombieType type) {
        zombie = type;
        plant = null;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        elapsed += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (plant == null && zombie == null) return;
        float bob = (float) Math.sin(elapsed * (plant != null ? 3f : 6f)) * getHeight() * .06f;
        float scale = 1f + (float) Math.sin(elapsed * 2.5f) * .05f;
        float size = Math.min(getWidth(), getHeight()) * .70f * scale;
        float cx = getX() + getWidth() * .5f;
        float cy = getY() + getHeight() * .48f + bob;
        boolean rendered = false;
        if (art != null) {
            if (plant != null) {
                rendered = art.drawPlant(batch, plant, "idle", elapsed, cx, cy, getWidth() * .94f, getHeight() * .94f);
            } else if (zombie != null) {
                rendered = art.drawZombie(batch, zombie, "idle", elapsed, cx, cy, getWidth() * .94f, getHeight() * .94f);
            }
        }
        if (!rendered) {
            Color c = plant != null ? plantColor(PlantRegistry.get(plant).getFamily()) : new Color(.45f, .58f, .35f, 1f);
            if (art != null) {
                if (plant != null) {
                    art.drawProceduralPlant(batch, plant, "idle", elapsed, cx, cy,
                        getWidth() * .90f, getHeight() * .90f, c);
                } else {
                    art.drawProceduralZombie(batch, zombie, "walk", elapsed, cx, cy,
                        getWidth() * .90f, getHeight() * .90f, c);
                }
            } else if (ownedCircle != null) {
                batch.setColor(c);
                batch.draw(ownedCircle, cx - size / 2, cy - size / 2, size, size);
                batch.setColor(Color.WHITE);
            }
        }
    }

    private Color plantColor(PlantFamily family) {
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

    public void dispose() {
        if (ownedCircle != null) ownedCircle.dispose();
    }
}
