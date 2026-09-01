package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.ObjectMap;
import pvz.skin.PvzSkin;

 









public final class PvzSkinSupport {
    private static final float CHECKBOX_SIZE = 22f;

    private PvzSkinSupport() {
    }

    public static Skin create() {
        Skin skin = PvzSkin.get();
        smoothFonts(skin);

        
        if (!skin.has("window", Label.LabelStyle.class)) {
            Label.LabelStyle source = skin.has("big_outline", Label.LabelStyle.class)
                ? skin.get("big_outline", Label.LabelStyle.class)
                : skin.get("big", Label.LabelStyle.class);
            skin.add("window", source, Label.LabelStyle.class);
        }
        if (!skin.has("subtitle", Label.LabelStyle.class)) {
            Label.LabelStyle source = skin.has("medium_outline", Label.LabelStyle.class)
                ? skin.get("medium_outline", Label.LabelStyle.class)
                : skin.get("medium", Label.LabelStyle.class);
            skin.add("subtitle", source, Label.LabelStyle.class);
        }

        
        
        
        if (!skin.has("white", Drawable.class)) {
            skin.add("white", skin.getDrawable("white_pixel"), Drawable.class);
        }

        
        
        
        if (skin.has("default", TextButton.TextButtonStyle.class)
            && skin.has("green", TextButton.TextButtonStyle.class)) {
            TextButton.TextButtonStyle current = skin.get("default", TextButton.TextButtonStyle.class);
            if (current.up == null && current.down == null) {
                skin.add("default",
                    new TextButton.TextButtonStyle(skin.get("green", TextButton.TextButtonStyle.class)),
                    TextButton.TextButtonStyle.class);
            }
        }

        
        
        
        
        ensureCheckBoxStyle(skin);

        
        
        
        
        ensureSelectBoxStyle(skin);

        
        
        
        if (!skin.has("default", Window.WindowStyle.class)) {
            Drawable background = skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
            Window.WindowStyle windowStyle = new Window.WindowStyle(
                skin.getFont("FBUSV8C5EI_2"), Color.WHITE, background);
            skin.add("default", windowStyle, Window.WindowStyle.class);
        }

        return skin;
    }

    private static void ensureCheckBoxStyle(Skin skin) {
        if (skin.has("default", CheckBox.CheckBoxStyle.class)) return;

        Label.LabelStyle labelStyle = skin.get("default", Label.LabelStyle.class);

        Drawable off = sized(skin.newDrawable("white_pixel", new Color(0.28f, 0.25f, 0.18f, 1f)));
        Drawable on = sized(skin.newDrawable("white_pixel", new Color(0.32f, 0.76f, 0.22f, 1f)));
        Drawable over = sized(skin.newDrawable("white_pixel", new Color(0.52f, 0.48f, 0.31f, 1f)));
        Drawable onOver = sized(skin.newDrawable("white_pixel", new Color(0.46f, 0.90f, 0.28f, 1f)));
        Drawable disabled = sized(skin.newDrawable("white_pixel", new Color(0.22f, 0.22f, 0.22f, 0.8f)));

        CheckBox.CheckBoxStyle style = new CheckBox.CheckBoxStyle(off, on, labelStyle.font, Color.WHITE);
        style.checkboxOver = over;
        style.checkboxOnOver = onOver;
        style.checkboxOffDisabled = disabled;
        style.checkboxOnDisabled = disabled;
        style.disabledFontColor = Color.GRAY;
        skin.add("default", style, CheckBox.CheckBoxStyle.class);
    }

    private static void ensureSelectBoxStyle(Skin skin) {
        
        
        
        
        Label.LabelStyle labelStyle = skin.get("default", Label.LabelStyle.class);
        List.ListStyle baseList = skin.get("default", List.ListStyle.class);
        ScrollPane.ScrollPaneStyle baseScroll = skin.get("default", ScrollPane.ScrollPaneStyle.class);

        Drawable fieldBackground = null;
        Drawable fieldFocused = null;
        if (skin.has("default", TextField.TextFieldStyle.class)) {
            TextField.TextFieldStyle textField = skin.get("default", TextField.TextFieldStyle.class);
            fieldBackground = textField.background;
            fieldFocused = textField.focusedBackground;
        }
        if (fieldBackground == null) {
            fieldBackground = skin.newDrawable("white_pixel", new Color(0.93f, 0.88f, 0.70f, 1f));
        }

        List.ListStyle listStyle = new List.ListStyle(baseList);
        listStyle.fontColorUnselected = new Color(0.12f, 0.10f, 0.06f, 1f);
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.background = skin.newDrawable("white_pixel", new Color(0.94f, 0.89f, 0.72f, 1f));
        listStyle.selection = skin.newDrawable("white_pixel", new Color(0.28f, 0.52f, 0.16f, 1f));
        listStyle.over = skin.newDrawable("white_pixel", new Color(0.76f, 0.70f, 0.50f, 1f));

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle(baseScroll);
        scrollStyle.background = skin.newDrawable("white_pixel", new Color(0.94f, 0.89f, 0.72f, 1f));

        SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle(
            labelStyle.font, new Color(0.10f, 0.09f, 0.05f, 1f), fieldBackground, scrollStyle, listStyle);
        style.backgroundOver = fieldFocused != null ? fieldFocused : fieldBackground;
        style.backgroundOpen = fieldFocused != null ? fieldFocused : fieldBackground;
        style.disabledFontColor = Color.GRAY;
        skin.add("default", style, SelectBox.SelectBoxStyle.class);
    }

    private static void smoothFonts(Skin skin) {
        ObjectMap<String, BitmapFont> fonts = skin.getAll(BitmapFont.class);
        if (fonts == null) return;
        for (BitmapFont font : fonts.values()) {
            if (font == null) continue;
            font.setUseIntegerPositions(false);
            for (TextureRegion region : font.getRegions()) {
                if (region != null && region.getTexture() != null) {
                    region.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                }
            }
        }
    }

    private static Drawable sized(Drawable drawable) {
        if (drawable instanceof BaseDrawable baseDrawable) {
            baseDrawable.setMinWidth(CHECKBOX_SIZE);
            baseDrawable.setMinHeight(CHECKBOX_SIZE);
        }
        return drawable;
    }
}
