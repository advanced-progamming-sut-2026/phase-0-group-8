package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

 
final class PvzBackdropActor extends Actor {
    enum Mood { PUBLIC, GARDEN, NIGHT, HIDDEN }

    private static final Color WHITE = new Color(Color.WHITE);
    private static final Color PUBLIC_TOP = new Color(.16f, .40f, .27f, 1f);
    private static final Color PUBLIC_BOTTOM = new Color(.035f, .12f, .075f, 1f);
    private static final Color GARDEN_TOP = new Color(.22f, .51f, .22f, 1f);
    private static final Color GARDEN_BOTTOM = new Color(.045f, .15f, .065f, 1f);
    private static final Color NIGHT_TOP = new Color(.12f, .14f, .25f, 1f);
    private static final Color NIGHT_BOTTOM = new Color(.025f, .035f, .075f, 1f);
    private static final Color LAWN_SHADE = new Color(.015f, .06f, .025f, .38f);
    private static final Color NIGHT_SHADE = new Color(.025f, .035f, .09f, .62f);

    private final Drawable pixel;
    private final Drawable leaf;
    private final PvzAssetSystem art;
    private final Color gradientColor = new Color();
    private Mood mood = Mood.GARDEN;

    PvzBackdropActor(Skin skin, PvzAssetSystem art) {
        this.art = art;
        pixel = drawable(skin, "white", "white_pixel");
        leaf = drawableOrNull(skin, "image_ui_generic_leaf_backdrop");
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    }

    void setMood(Mood mood) {
        this.mood = mood == null ? Mood.GARDEN : mood;
        setVisible(this.mood != Mood.HIDDEN);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (mood == Mood.HIDDEN || getWidth() <= 0f || getHeight() <= 0f) return;

        boolean realLawn = art != null && art.drawMenuBackground(batch, getX(), getY(), getWidth(), getHeight());
        if (realLawn) {
            fill(batch, getX(), getY(), getWidth(), getHeight(),
                mood == Mood.NIGHT ? NIGHT_SHADE : LAWN_SHADE);
        } else {
            Color top = mood == Mood.PUBLIC ? PUBLIC_TOP : mood == Mood.NIGHT ? NIGHT_TOP : GARDEN_TOP;
            Color bottom = mood == Mood.PUBLIC ? PUBLIC_BOTTOM : mood == Mood.NIGHT ? NIGHT_BOTTOM : GARDEN_BOTTOM;
            float band = getHeight() / 8f;
            for (int i = 0; i < 8; i++) {
                float t = i / 7f;
                fill(batch, getX(), getY() + i * band, getWidth(), band + 1f,
                    gradientColor.set(bottom).lerp(top, t));
            }
        }

        if (leaf != null) {
            batch.setColor(1f, 1f, 1f, realLawn ? .08f : .16f);
            leaf.draw(batch, getX() - 55f, getY() - 25f, 430f, 280f);
            leaf.draw(batch, getX() + getWidth() - 330f, getY() + getHeight() - 210f, 390f, 250f);
            batch.setColor(WHITE);
        }
    }

    private void fill(Batch batch, float x, float y, float width, float height, Color color) {
        batch.setColor(color);
        pixel.draw(batch, x, y, width, height);
        batch.setColor(WHITE);
    }

    private static Drawable drawable(Skin skin, String preferred, String fallback) {
        Drawable result = drawableOrNull(skin, preferred);
        return result != null ? result : skin.getDrawable(fallback);
    }

    private static Drawable drawableOrNull(Skin skin, String name) {
        try {
            return skin.getDrawable(name);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
