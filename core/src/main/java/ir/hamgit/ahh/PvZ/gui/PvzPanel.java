package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

 




final class PvzPanel extends Table {
    private final Drawable border;

    PvzPanel(Skin skin, Color tint) {
        Drawable background = drawable(skin, "image_ui_dialog_asset_inner_bkgd_10", "white");
        setBackground(skin.newDrawable(background, tint == null ? Color.WHITE : tint));
        border = drawable(skin, "image_ui_dialog_asset_dialogborder_10", "white");
        pad(22f);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        border.draw(batch, getX() - 5f, getY() - 7f, getWidth() + 10f, getHeight() + 12f);
        batch.setColor(Color.WHITE);
    }

    private static Drawable drawable(Skin skin, String preferred, String fallback) {
        try {
            return skin.getDrawable(preferred);
        } catch (RuntimeException ignored) {
            return skin.getDrawable(fallback);
        }
    }
}
