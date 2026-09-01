package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;

import java.util.HashMap;
import java.util.Map;

 






final class EmojiArt implements Disposable {
    private static final int SIZE = 128;
    private final Map<String, Texture> textures = new HashMap<>();

    Image image(String emoji) {
        Texture texture = textures.computeIfAbsent(emoji, this::createTexture);
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
        image.setScaling(Scaling.fit);
        image.setTouchable(Touchable.disabled);
        return image;
    }

    private Texture createTexture(String emoji) {
        Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        
        
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        switch (emoji) {
            case "😀" -> drawHappy(pixmap);
            case "😱" -> drawShock(pixmap);
            case "🧟" -> drawZombie(pixmap);
            default -> drawUnknown(pixmap);
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private void drawHappy(Pixmap p) {
        face(p, new Color(1f, .76f, .08f, 1f));
        p.setColor(new Color(.16f, .10f, .05f, 1f));
        p.fillCircle(43, 49, 7);
        p.fillCircle(85, 49, 7);
        p.fillCircle(64, 79, 27);
        p.setColor(new Color(1f, .76f, .08f, 1f));
        p.fillRectangle(33, 49, 62, 24);
        p.setColor(Color.WHITE);
        p.fillRectangle(43, 73, 42, 10);
        p.setColor(new Color(.88f, .28f, .24f, 1f));
        p.fillCircle(30, 68, 6);
        p.fillCircle(98, 68, 6);
    }

    private void drawShock(Pixmap p) {
        face(p, new Color(1f, .78f, .18f, 1f));
        p.setColor(new Color(.27f, .67f, .90f, 1f));
        p.fillCircle(64, 31, 43);
        p.setColor(new Color(1f, .78f, .18f, 1f));
        p.fillCircle(64, 53, 43);
        p.setColor(Color.WHITE);
        p.fillCircle(45, 52, 13);
        p.fillCircle(83, 52, 13);
        p.setColor(new Color(.08f, .07f, .08f, 1f));
        p.fillCircle(45, 53, 5);
        p.fillCircle(83, 53, 5);
        p.fillCircle(64, 84, 18);
        p.setColor(new Color(.34f, .72f, .94f, 1f));
        p.fillCircle(18, 83, 13);
        p.fillCircle(110, 83, 13);
        p.fillRectangle(11, 78, 13, 28);
        p.fillRectangle(104, 78, 13, 28);
    }

    private void drawZombie(Pixmap p) {
        face(p, new Color(.43f, .72f, .32f, 1f));
        p.setColor(new Color(.16f, .28f, .12f, 1f));
        p.fillRectangle(28, 16, 18, 16);
        p.fillRectangle(44, 11, 20, 22);
        p.fillRectangle(63, 16, 20, 17);
        p.fillRectangle(82, 10, 17, 25);
        p.setColor(new Color(.92f, .93f, .68f, 1f));
        p.fillCircle(45, 54, 13);
        p.fillCircle(84, 57, 12);
        p.setColor(new Color(.10f, .12f, .08f, 1f));
        p.fillCircle(49, 56, 5);
        p.fillCircle(80, 53, 5);
        p.fillRectangle(41, 80, 47, 22);
        p.fillCircle(42, 91, 11);
        p.fillCircle(87, 91, 11);
        p.setColor(new Color(.93f, .87f, .61f, 1f));
        p.fillRectangle(48, 80, 10, 10);
        p.fillRectangle(70, 92, 11, 10);
        p.setColor(new Color(.18f, .35f, .15f, 1f));
        p.drawLine(29, 67, 42, 74);
        p.drawLine(29, 68, 39, 61);
        p.drawLine(91, 69, 102, 62);
    }

    private void drawUnknown(Pixmap p) {
        face(p, new Color(.65f, .65f, .65f, 1f));
        p.setColor(new Color(.18f, .18f, .18f, 1f));
        p.fillCircle(45, 52, 6);
        p.fillCircle(83, 52, 6);
        p.fillRectangle(47, 82, 34, 7);
    }

    private void face(Pixmap p, Color color) {
        p.setColor(new Color(.20f, .14f, .06f, 1f));
        p.fillCircle(64, 64, 59);
        p.setColor(color);
        p.fillCircle(64, 64, 55);
    }

    @Override public void dispose() {
        for (Texture texture : textures.values()) texture.dispose();
        textures.clear();
    }
}
