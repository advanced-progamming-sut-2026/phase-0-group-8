package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import ir.hamgit.ahh.PvZ.controller.GameController;
import ir.hamgit.ahh.PvZ.controller.MinigameController;
import ir.hamgit.ahh.PvZ.controller.Validator;
import ir.hamgit.ahh.PvZ.model.*;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.def.ZombieDef;
import ir.hamgit.ahh.PvZ.model.enums.*;
import ir.hamgit.ahh.PvZ.model.greenhouse.GreenhouseService;
import ir.hamgit.ahh.PvZ.model.minigame.*;
import ir.hamgit.ahh.PvZ.model.quest.QuestService;
import ir.hamgit.ahh.PvZ.model.quest.QuestService.Category;
import ir.hamgit.ahh.PvZ.model.quest.QuestService.Quest;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.registry.ZombieRegistry;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;
import ir.hamgit.ahh.PvZ.model.shop.ShopItem;
import ir.hamgit.ahh.PvZ.model.shop.ShopService;
import ir.hamgit.ahh.PvZ.model.special.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

 




abstract class PhaseTwoScreenBase extends ScreenAdapter {
    public static final int VIRTUAL_WIDTH = 1280;
    public static final int VIRTUAL_HEIGHT = 720;

    protected static final String[] SECURITY_QUESTIONS = {
        "What is the name of your first pet?",
        "What is your mother's maiden name?",
        "What city were you born in?",
        "What was the name of your elementary school?",
        "What is your favorite childhood movie?"
    };
    protected static final ChapterType[] CHAPTERS = {
        ChapterType.ANCIENT_EGYPT, ChapterType.FROSTBITE_CAVES,
        ChapterType.BIG_WAVE_BEACH, ChapterType.DARK_AGES
    };

    protected final Stage stage;
    protected final Skin skin;
    protected final PvzAssetSystem pvzAssets;
    protected final PvzAudioSystem audio;
    protected final PvzBackdropActor backdrop;
    protected final Table root;
    protected final GameController gameController;
    protected final MinigameController minigameController;

    protected User forgotTarget;
    protected String forgotResetToken;
    protected String forgotSecurityQuestion;
    protected PlantType collectionPlant;
    protected ZombieType collectionZombie;
    protected boolean collectionPlantTab = true;
    protected String collectionFamily = "All";
    protected String collectionOwnership = "All";
    protected boolean collectionUpgradeable;
    protected Category questCategory = Category.STORY;

    protected ChapterType activeChapter;
    protected int activeLevel = 1;
    protected boolean scoreModeActive;
    protected PlantType selectedPlant;
    protected ZombieType selectedZombie;
    protected boolean shovelMode;
    protected boolean plantFoodMode;
    protected boolean paused;
    protected boolean adventureScreenActive;
    protected boolean minigameScreenActive;
    protected float tickAccumulator;
    protected BoardActor boardActor;
    protected Label resourceLabel;
    protected Label gameHudLabel;
    protected Label gameSunLabel;
    protected Label gameFoodLabel;
    protected Label gameSpeedLabel;
    protected Label specialHudLabel;
    protected ProgressBar waveProgress;
    protected Table gamePlantBar;
    protected Table gameDebugControls;
    protected int lastWave = -1;
    protected int lastPlantFood = -1;
    protected int lastCoins = -1;
    protected int lastDiamonds = -1;
    protected int lastPotCount = -1;
    protected String lastSeedBankSignature;

    protected Table minigameControls;
    protected Label minigameHud;
    protected String lastMinigameControlSignature;
    protected int beghouledFirstX = -1;
    protected int beghouledFirstLane = -1;

    protected PhaseTwoScreenBase() {
        skin = PvzSkinSupport.create();
        pvzAssets = new PvzAssetSystem();
        audio = new PvzAudioSystem();
        
        
        stage = new Stage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        backdrop = new PvzBackdropActor(skin, pvzAssets);
        backdrop.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(backdrop);
        root = new Table();
        root.setFillParent(true);
        root.defaults().pad(0);
        stage.addActor(root);
        gameController = new GameController(UserRepository.getCurrentUser());
        minigameController = new MinigameController();
        Gdx.input.setInputProcessor(stage);
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.F1, true);
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.F2, true);
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.F3, true);
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.F4, true);
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.F5, true);
        stage.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override public boolean keyDown(com.badlogic.gdx.scenes.scene2d.InputEvent e, int keycode) {
                return handleDebugKey(keycode);
            }
        });

    }

    
    
    

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        pvzAssets.update();
        updateLiveState(delta);
        stage.act(Math.min(delta, 1f / 15f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        backdrop.setBounds(0f, 0f, stage.getWidth(), stage.getHeight());
    }

    @Override
    public void dispose() {
        disposeDynamicActors();
        audio.close();
        stage.dispose();
        pvzAssets.dispose();
        skin.dispose();
    }

    protected void disposeDynamicActors() {
        if (boardActor != null) {
            boardActor.dispose();
            boardActor = null;
        }
    }

    protected void clearScreen() {
        stage.unfocusAll();
        root.clearChildren();
        root.pad(0f);
        root.setBackground((Drawable) null);
        backdrop.setMood(PvzBackdropActor.Mood.GARDEN);
        audio.playMenuMusic();
        disposeDynamicActors();
        resourceLabel = null;
        gameHudLabel = null;
        gameSunLabel = null;
        gameFoodLabel = null;
        gameSpeedLabel = null;
        specialHudLabel = null;
        waveProgress = null;
        gamePlantBar = null;
        gameDebugControls = null;
        minigameControls = null;
        minigameHud = null;
        lastSeedBankSignature = null;
        lastMinigameControlSignature = null;
        adventureScreenActive = false;
        minigameScreenActive = false;
    }

    protected void beginPublic(String title) {
        beginPublic(title, null);
    }

    protected void beginPublic(String title, Runnable backAction) {
        clearScreen();
        backdrop.setMood(PvzBackdropActor.Mood.PUBLIC);
        root.pad(14f);
        Table top = new Table();
        top.setBackground(tinted("image_ui_mainmenu_mm_settings_tab_10", new Color(.12f, .21f, .12f, .94f)));
        top.pad(8f, 14f, 8f, 14f);
        if (backAction != null) top.add(button("Back", "brown", backAction)).width(110).padRight(12);
        Label heading = title(title);
        top.add(heading).left().expandX();
        root.add(top).colspan(2).growX().padTop(12).padBottom(8).row();
    }

    protected void beginAuthenticated(String title, boolean showNavigation) {
        beginAuthenticated(title, showNavigation, null);
    }

    protected void beginAuthenticated(String title, boolean showNavigation, Runnable backAction) {
        clearScreen();
        backdrop.setMood(PvzBackdropActor.Mood.GARDEN);
        root.pad(14f);
        User user = currentUser();
        Table top = new Table();
        top.setBackground(tinted("image_ui_mainmenu_mm_settings_tab_10", new Color(.10f, .20f, .10f, .95f)));
        top.pad(7f, 14f, 7f, 14f);
        if (backAction != null) top.add(button("Back", "brown", backAction)).width(110).padRight(12);
        Label heading = title(title);
        top.add(heading).left().expandX();
        resourceLabel = new Label(resourceText(user), skin);
        resourceLabel.setAlignment(Align.right);
        top.add(resourceLabel).right().padRight(8);
        if (user != null && user.isDebugMode()) {
            top.add(button("+1000 coins", () -> {
                user.addCoins(1000);
                UserRepository.updateUser(user);
                refreshResources();
            })).width(108);
            top.add(button("+10 gems", () -> {
                user.addDiamonds(10);
                UserRepository.updateUser(user);
                refreshResources();
            })).width(92);
        }
        root.add(top).growX().row();
        
        
        
    }

    protected boolean handleDebugKey(int keycode) {
        if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE && adventureScreenActive) {
            cancelCurrentAction();
            return true;
        }
        User user = UserRepository.getCurrentUser();
        if (keycode == com.badlogic.gdx.Input.Keys.F1) {
            if (user == null) return false;
            user.setDebugMode(!user.isDebugMode());
            UserRepository.updateUser(user);
            toast("Debug mode " + (user.isDebugMode() ? "enabled" : "disabled") + ".");
            if (gameDebugControls != null) gameDebugControls.setVisible(user.isDebugMode());
            refreshResources();
            return true;
        }
        if (user == null || !user.isDebugMode()) return false;
        Board board = gameController.getBoard();
        if (keycode == com.badlogic.gdx.Input.Keys.F2) {
            if (board == null) toast("Sun cheat is available inside an adventure level.");
            else { board.cheatAddSuns(1000); refreshGameHud(); toast("+1000 sun"); }
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.F3) {
            if (board == null) toast("Plant Food cheat is available inside an adventure level.");
            else { board.cheatAddPlantFood(); refreshGameHud(); toast("+1 Plant Food"); }
            return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.F4) {
            user.addCoins(1000); UserRepository.updateUser(user); refreshResources(); toast("+1000 coins"); return true;
        }
        if (keycode == com.badlogic.gdx.Input.Keys.F5) {
            user.addDiamonds(100); UserRepository.updateUser(user); refreshResources(); toast("+100 diamonds"); return true;
        }
        return false;
    }

    protected User currentUser() {
        User user = UserRepository.getCurrentUser();
        gameController.setCurrentUser(user);
        audio.apply(user);
        return user;
    }

    protected String resourceText(User user) {
        return user == null ? "" : "Coins: " + user.getCoins() + "    Diamonds: " + user.getDiamonds();
    }

    protected void refreshResources() {
        if (resourceLabel != null) resourceLabel.setText(resourceText(currentUser()));
    }

    protected Label title(String value) {
        Label label = new Label(value, skin, "window");
        label.setAlignment(Align.left);
        return label;
    }

    protected Label wrapped(String value) {
        Label label = new Label(value, skin);
        label.setWrap(true);
        label.setAlignment(Align.left, Align.top);
        return label;
    }

    protected TextButton button(String text, Runnable action) {
        return button(text, "default", null, action);
    }

    protected TextButton button(String text, String style, Runnable action) {
        return button(text, style, null, action);
    }

    protected TextButton button(String text, String style, Color initialColor, Runnable action) {
        TextButton button = skin.has(style, TextButton.TextButtonStyle.class)
            ? new TextButton(text, skin, style) : new TextButton(text, skin);
        if (initialColor != null) button.setColor(initialColor);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audio.playCue("click");
                action.run();
            }
        });
        addHoverEffect(button);
        return button;
    }

    protected ImageButton imageButton(String style, Runnable action) {
        String resolved = skin.has(style, ImageButton.ImageButtonStyle.class) ? style : "settings";
        ImageButton button = new ImageButton(skin, resolved);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audio.playCue("click");
                action.run();
            }
        });
        addHoverEffect(button);
        return button;
    }

     
    protected void addHoverEffect(Actor actor) {
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
            group.setTransform(true);
        }
        final Color normalColor = new Color(actor.getColor());
        actor.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                Color hoverColor = new Color(normalColor);
                if (hoverColor.r > .92f && hoverColor.g > .92f && hoverColor.b > .92f) {
                    hoverColor.set(1f, 1f, .90f, hoverColor.a);
                } else {
                    hoverColor.r = Math.min(1f, hoverColor.r + .08f);
                    hoverColor.g = Math.min(1f, hoverColor.g + .08f);
                    hoverColor.b = Math.min(1f, hoverColor.b + .08f);
                }
                actor.addAction(Actions.parallel(Actions.scaleTo(1.035f, 1.035f, .09f),
                    Actions.color(hoverColor, .09f)));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                actor.addAction(Actions.parallel(Actions.scaleTo(1f, 1f, .12f),
                    Actions.color(new Color(normalColor), .12f)));
            }
        });
    }

    protected Actor menuTile(String iconStyle, String heading, String subtitle, Runnable action) {
        Table tile = new Table();
        tile.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        tile.setBackground(tinted("image_ui_mainmenu_mm_settings_tab_10",
            new Color(.12f, .25f, .12f, .96f)));
        tile.pad(10f);

        String resolved = skin.has(iconStyle, ImageButton.ImageButtonStyle.class) ? iconStyle : "settings";
        ImageButton icon = new ImageButton(skin, resolved);
        icon.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        Label name = new Label(heading, skin, "subtitle");
        name.setAlignment(Align.left);
        name.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        Label detail = wrapped(subtitle);
        detail.setColor(.90f, .95f, .80f, 1f);
        detail.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

        tile.add(icon).size(68f).padRight(10f);
        Table copy = new Table();
        copy.add(name).growX().left().row();
        copy.add(detail).width(150f).growX().left().padTop(3f);
        tile.add(copy).grow();
        addImmediateCardAction(tile, action);
        return tile;
    }

    protected Drawable tinted(String preferred, Color color) {
        Drawable source;
        try {
            source = skin.getDrawable(preferred);
        } catch (RuntimeException ignored) {
            source = skin.getDrawable("white");
        }
        return skin.newDrawable(source, color == null ? Color.WHITE : color);
    }

    protected void toast(String text) {
        Label label = new Label(text == null ? "" : text, skin);
        label.setAlignment(Align.center);
        label.setColor(1f, .95f, .8f, 1f);
        Table toast = new Table();
        toast.setBackground(skin.newDrawable("white", new Color(.12f, .12f, .12f, .92f)));
        toast.add(label).grow().pad(8);
        float width = Math.min(820, Math.max(300, (text == null ? 0 : text.length()) * 8f));
        toast.setSize(width, 48);
        toast.setPosition((stage.getWidth() - toast.getWidth()) / 2f, stage.getHeight() - 82f);
        stage.addActor(toast);
        toast.addAction(Actions.sequence(Actions.delay(2.0f), Actions.fadeOut(.45f), Actions.removeActor()));
    }

    protected void confirm(String title, String message, Runnable onConfirm) {
        Dialog dialog = new Dialog(title, skin) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) onConfirm.run();
            }
        };
        Label messageLabel = wrapped(message);
        messageLabel.setAlignment(Align.center);
        dialog.getContentTable().add(messageLabel).width(500).pad(20);
        dialog.button("Cancel", false);
        dialog.button("Confirm", true);
        dialog.show(stage);
        dialog.setSize(580, Math.max(220, dialog.getPrefHeight()));
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2f, (stage.getHeight() - dialog.getHeight()) / 2f);
    }

    protected TextField field(String hint) {
        TextField field = new TextField("", skin);
        field.setMessageText(hint);
        return field;
    }

    protected ScrollPane scroll(Actor actor) {
        ScrollPane pane = new ScrollPane(actor, skin);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        return pane;
    }

    protected PvzPanel framed(Actor content) {
        PvzPanel panel = new PvzPanel(skin, new Color(.18f, .29f, .15f, .97f));
        panel.add(content).grow();
        return panel;
    }

    protected void addFormRow(Table form, String name, Actor field) {
        form.add(new Label(name, skin)).right().padRight(8);
        form.add(field).width(320).height(42).left().row();
    }

    
    
    


    protected abstract void showLogin();
    protected abstract void showMain();
    protected abstract void showAdventure();
    protected abstract void showCollection();
    protected abstract void showGreenhouse();
    protected abstract void showShop();
    protected abstract void showQuests();
    protected abstract void showGame();
    protected abstract void showMultiplayerLobby();
    protected abstract void cancelCurrentAction();
    protected abstract void refreshGameHud();
    protected abstract void updateLiveState(float delta);
    protected abstract void updateMinigame(float delta);
    protected abstract Actor plantActionCard(PlantType type, String text, Runnable action, Color tint, int iconSize);
    protected abstract Actor zombieActionCard(ZombieType type, String text, Runnable action, Color tint, int iconSize);
     
    protected void addImmediateCardAction(Actor actor, Runnable action) {
        addHoverEffect(actor);
        actor.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button != com.badlogic.gdx.Input.Buttons.LEFT) return false;
                audio.playCue("click");
                actor.addAction(Actions.sequence(Actions.scaleTo(.96f, .96f, .04f),
                    Actions.scaleTo(1.035f, 1.035f, .07f)));
                action.run();
                return true;
            }
        });
    }
    protected abstract Actor shopIcon(ShopItem item);
    protected String dailyRemaining() {
        LocalDateTime now = LocalDateTime.now();
        long seconds = Math.max(0, Duration.between(now, LocalDate.now().plusDays(1).atStartOfDay()).getSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format(Locale.ROOT, "%02dh %02dm", hours, minutes);
    }

    protected String human(String raw) {
        if (raw == null) return "";
        String[] parts = raw.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
