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

 
abstract class PhaseTwoGameplayScreens extends PhaseTwoContentScreens {

    private static final float ADVENTURE_TOP_UI_INSET = 152f;
    private static final float ADVENTURE_BOTTOM_UI_INSET = 88f;
    private static final float ADVENTURE_BACKGROUND_OFFSET_Y = 88f;

    protected void showGame() {
        Board board = gameController.getBoard();
        if (board == null) { showMain(); return; }
        clearScreen();
        backdrop.setMood(PvzBackdropActor.Mood.HIDDEN);
        audio.playGameMusic(board.getChapter());
        adventureScreenActive = true;
        User user = currentUser();
        
        
        selectedPlant = null;
        selectedZombie = null;
        shovelMode = false;
        plantFoodMode = false;
        paused = false;
        tickAccumulator = 0;
        lastWave = board.getCurrentWave();
        lastPlantFood = board.getPlantFoodCount();
        lastCoins = user.getCoins();
        lastDiamonds = user.getDiamonds();
        lastPotCount = user.getPotCount();

        boardActor = new BoardActor(gameController::getBoard, skin.getFont("FBUSV8C6EI_3"), pvzAssets);
        boardActor.setShowGridSupplier(() -> currentUser().isShowGrid());
        boardActor.setAnimationPausedSupplier(() -> paused);
        boardActor.setCellClickListener(this::handleGameCellClick);
        boardActor.setFallingSunCollector(sun -> {
            boolean collected = board.collectFallingSun(sun.getX(), sun.getLane());
            if (collected) {
                audio.playCue("collect");
                refreshGameHud();
            }
            return collected;
        });
        boardActor.setCancelAction(this::cancelCurrentAction);
        boardActor.setGameplayInsets(ADVENTURE_TOP_UI_INSET, ADVENTURE_BOTTOM_UI_INSET);
        boardActor.setBackgroundVerticalOffset(ADVENTURE_BACKGROUND_OFFSET_Y);
        boardActor.setCursorText(null);
        boardActor.setVisualCueListener(audio::playCue);

        
        
        Stack battlefield = new Stack();
        battlefield.add(boardActor);
        Table overlay = new Table();
        overlay.top();

        Table status = new Table();
        status.setBackground(tinted("image_ui_quests_panel_edge_to_edge_ten", new Color(.08f, .14f, .07f, .94f)));
        status.pad(3f, 10f, 3f, 10f);
        gameHudLabel = new Label("", skin, "subtitle");
        status.add(gameHudLabel).left().expandX();
        gameSunLabel = new Label("", skin, "subtitle");
        status.add(gameSunLabel).padRight(14f);
        gameFoodLabel = new Label("", skin, "subtitle");
        status.add(gameFoodLabel).padRight(14f);
        resourceLabel = new Label(resourceText(user), skin);
        status.add(resourceLabel).padRight(14f);
        status.add(imageButton("ingame_2x", this::cycleGameSpeed)).size(43f);
        gameSpeedLabel = new Label(user.getGameSpeed() + "x", skin);
        status.add(gameSpeedLabel).width(32f).padRight(6f);
        status.add(imageButton("ingame_pause", this::showPauseDialog)).size(43f);
        overlay.add(status).growX().height(50f).row();

        Table seedHud = new Table();
        seedHud.setBackground(tinted("image_ui_mainmenu_mm_settings_tab_10", new Color(.13f, .23f, .10f, .93f)));
        seedHud.pad(4f, 8f, 4f, 8f);
        gamePlantBar = new Table();
        gamePlantBar.defaults().width(154f).height(84f).pad(3f);
        ScrollPane plantScroll = new ScrollPane(gamePlantBar, skin);
        plantScroll.setFadeScrollBars(false);
        plantScroll.setScrollingDisabled(false, true);
        seedHud.add(plantScroll).growX().height(92f);

        Table tools = new Table();
        tools.add(imageButton("ingame_shovel", () -> {
            shovelMode = true; plantFoodMode = false; selectedPlant = null; boardActor.setCursorText("SHOVEL");
        })).size(54f).padRight(5f);
        tools.add(imageButton("plantfood", () -> {
            plantFoodMode = true; shovelMode = false; selectedPlant = null; boardActor.setCursorText("FOOD");
        })).size(54f);
        seedHud.add(tools).width(120f).padLeft(8f);
        overlay.add(seedHud).growX().height(102f).row();
        overlay.add().grow().row();

        Table footer = new Table();
        footer.setBackground(tinted("image_ui_quests_panel_edge_to_edge_ten", new Color(.06f, .10f, .055f, .91f)));
        footer.pad(5f, 10f, 5f, 10f);
        footer.add(new Label("Zombie progress", skin)).padRight(7f);
        waveProgress = new ProgressBar(0f, 1f, .01f, false, skin, "ingame_progress");
        Stack waveMeter = new Stack();
        waveMeter.add(waveProgress);
        Table markers = new Table();
        markers.defaults().expandX().right();
        for (int wave = 1; wave <= board.getTotalWaves(); wave++) {
            Label marker = new Label(wave == board.getTotalWaves() ? "FINAL" : "| " + wave, skin);
            marker.setColor(wave == board.getTotalWaves()
                ? new Color(1f, .28f, .16f, 1f) : new Color(1f, .94f, .65f, .92f));
            markers.add(marker).expandX().right();
        }
        waveMeter.add(markers);
        footer.add(waveMeter).width(440f).height(26f).padRight(10f);
        specialHudLabel = new Label("", skin);
        footer.add(specialHudLabel).expandX().left();
        if (board.getSpecialLevelHandler() instanceof PlantWhatYouGetLevel pwyg && !pwyg.isWavesStarted()) {
            footer.add(button("START WAVES", "green", () -> {
                board.startZombieWaves(); toast("Zombie waves started!");
            })).width(155f).height(42f).padRight(8f);
        }
        gameDebugControls = new Table();
        gameDebugControls.defaults().padLeft(4);
        gameDebugControls.add(button("+100 Sun", () -> { board.cheatAddSuns(100); refreshGameHud(); })).width(100);
        gameDebugControls.add(button("+1 Food", () -> { board.cheatAddPlantFood(); refreshGameHud(); })).width(100);
        gameDebugControls.add(button("Nuke", board::cheatReleaseNuke)).width(80);
        gameDebugControls.setVisible(user.isDebugMode());
        footer.add(gameDebugControls);
        overlay.add(footer).growX().height(ADVENTURE_BOTTOM_UI_INSET);

        battlefield.add(overlay);
        root.add(battlefield).grow();
        rebuildGamePlantBar();
        refreshGameHud();
        showMissionDialog(board);
    }

    protected void showMissionDialog(Board board) {
        String mission = missionText(board);
        Dialog dialog = new Dialog("Level mission", skin);
        dialog.text("Crazy Dave: Ready for another lawn defense?\nPenny: " + chapterIntro(board.getChapter()) + "\n\nMission: " + mission);
        dialog.button("Start");
        dialog.show(stage);
    }

    protected String chapterIntro(ChapterType chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> "Sandstorms can drop zombies deeper into the lawn.";
            case FROSTBITE_CAVES -> "Freezing wind and slippery ice change the battlefield.";
            case BIG_WAVE_BEACH -> "Watch the tide and the marked low-beach cells.";
            case DARK_AGES -> "Graves and necromancy cells can raise new threats.";
            default -> "Keep the house safe.";
        };
    }

    protected String missionText(Board board) {
        SpecialLevelHandler h = board.getSpecialLevelHandler();
        if (h instanceof DeadLineLevel d) return "Do not let any zombie cross the deadline at column " + d.getLineColumn() + ".";
        if (h instanceof TimedWarLevel t) return "Complete the timed objective: " + t.getTarget() + (t.isSunVariant() ? " sun" : " zombie kills") + " before time runs out.";
        if (h instanceof SaveOurSeedsLevel) return "Protect all highlighted seed plants. Zombies must not destroy them.";
        if (h instanceof LoveYourPlantsLevel l) return "Lose fewer than " + l.getMaxPlantLosses() + " ordinary plants.";
        if (h instanceof PlantWhatYouGetLevel) return "Plant freely first. Press START WAVES when your setup is ready.";
        if (h instanceof ConveyorBeltLevel) return "Use the plants delivered by the conveyor belt. Zombies must not reach the house.";
        return "Defend the house: zombies must not reach the left side of the lawn.";
    }

    protected void handleGameCellClick(int column, int lane) {
        Board board = gameController.getBoard();
        if (board == null || paused) return;
        boolean collectedFood = board.collectPlantFood(column, lane);
        boolean collected = board.collectSun(column, lane) | board.collectFallingSun(column, lane);
        if (collectedFood) {
            audio.playCue("pickup");
            toast("Plant Food collected!");
            refreshGameHud();
            return;
        }
        if (collected) {
            audio.playCue("collect");
            refreshGameHud();
            return;
        }
        boolean wasPlantFoodMode = plantFoodMode;
        boolean ok;
        if (shovelMode) {
            ok = board.pluckPlant(column, lane);
            if (!ok) toast("Nothing removable in that cell.");
        } else if (plantFoodMode) {
            ok = board.feedPlant(column, lane);
            if (!ok) toast("Plant Food could not be used there.");
        } else if (selectedPlant != null) {
            ok = gameController.plantSelected(selectedPlant, column, lane);
            if (!ok) toast("Cannot plant there: check sun, cooldown, terrain, and level rules.");
        } else {
            return;
        }
        if (ok && wasPlantFoodMode && boardActor != null) {
            boardActor.triggerPlantFoodAnimation(column, lane);
            audio.playCue("special");
        } else if (ok && !shovelMode) {
            audio.playCue("plant");
        }
        shovelMode = false;
        plantFoodMode = false;
        selectedPlant = null;
        boardActor.setCursorText(null);
        rebuildGamePlantBar();
        refreshGameHud();
    }

    protected void cancelCurrentAction() {
        selectedPlant = null;
        selectedZombie = null;
        shovelMode = false;
        plantFoodMode = false;
        if (boardActor != null) boardActor.setCursorText(null);
    }

    protected void cycleGameSpeed() {
        User user = currentUser();
        if (user == null) return;
        int next = user.getGameSpeed() >= 3 ? 1 : user.getGameSpeed() + 1;
        user.setGameSpeed(next);
        UserRepository.updateUser(user);
        if (gameSpeedLabel != null) gameSpeedLabel.setText(next + "x");
        toast("Game speed: " + next + "x");
    }

    protected void rebuildGamePlantBar() {
        if (gamePlantBar == null || gameController.getBoard() == null) return;
        gamePlantBar.clearChildren();
        Board board = gameController.getBoard();
        boolean conveyor = board.getSpecialLevelHandler() instanceof ConveyorBeltLevel;
        List<PlantType> plants;
        if (board.getSpecialLevelHandler() instanceof ConveyorBeltLevel belt) plants = belt.peekQueue();
        else plants = new ArrayList<>(gameController.getSelectedPlants());
        if (plants.isEmpty()) gamePlantBar.add(wrapped(conveyor
            ? "The conveyor is bringing the next packet..." : "No seed packets selected.")).width(220f);
        int cardIndex = 0;
        for (PlantType type : plants) {
            PlantDef def = PlantRegistry.get(type);
            if (def == null) continue;
            int cooldown = gameController.getPlantCooldownTicks(type);
            int shownCost = gameController.getPlantSunCost(type);
            String coolText = cooldown <= 0 ? "READY"
                : (int) Math.ceil(cooldown / (float) Board.TICKS_PER_SECOND) + "s";
            boolean available = cooldown <= 0 && (conveyor || board.getSunAmount() >= shownCost);
            String text = def.getDisplayName() + "\n" + (conveyor ? "FREE" : shownCost + " sun") + "  " + coolText
                + (gameController.getBoostedPlants().contains(type) || currentUser().hasBoost(type) ? " | BOOST" : "");
            Color tint = available ? null : new Color(.52f, .52f, .52f, 1f);
            Actor card = plantActionCard(type, text, () -> {
                if (!conveyor && gameController.getPlantCooldownTicks(type) > 0) {
                    toast("That seed packet is still recharging.");
                    return;
                }
                if (!conveyor && board.getSunAmount() < gameController.getPlantSunCost(type)) {
                    toast("Not enough sun for " + def.getDisplayName() + ".");
                    return;
                }
                selectedPlant = type; shovelMode = false; plantFoodMode = false;
                if (boardActor != null) boardActor.setCursorPlant(type);
            }, tint, 54);
            card.getColor().a = 0f;
            card.addAction(Actions.sequence(Actions.delay(cardIndex * .045f),
                Actions.fadeIn(.18f), Actions.forever(Actions.sequence(
                    Actions.moveBy(conveyor ? 5f : 2f, 0f, .38f),
                    Actions.moveBy(conveyor ? -5f : -2f, 0f, .38f)))));
            Cell<Actor> cardCell = gamePlantBar.add(card);
            if (conveyor && cardIndex > 0) cardCell.padLeft(-16f);
            cardIndex++;
        }
        lastSeedBankSignature = seedBankSignature();
    }

    protected String seedBankSignature() {
        Board board = gameController.getBoard();
        if (board == null) return "none";
        StringBuilder signature = new StringBuilder().append(board.getSunAmount()).append('|');
        List<PlantType> plants = board.getSpecialLevelHandler() instanceof ConveyorBeltLevel belt
            ? belt.peekQueue() : new ArrayList<>(gameController.getSelectedPlants());
        for (PlantType type : plants) {
            int cooldown = gameController.getPlantCooldownTicks(type);
            int seconds = (cooldown + Board.TICKS_PER_SECOND - 1) / Board.TICKS_PER_SECOND;
            signature.append(type).append(':').append(seconds).append(';');
        }
        return signature.toString();
    }

    protected void showPauseDialog() {
        if (gameController.getBoard() == null) return;
        paused = true;
        Dialog dialog = new Dialog("GAME PAUSED", skin) {
            @Override
            protected void result(Object object) {
                if ("resume".equals(object)) {
                    paused = false;
                } else if ("restart".equals(object)) {
                    restartCurrentLevel();
                } else if ("save".equals(object)) {
                    UserRepository.updateUser(currentUser());
                    
                    showMain();
                    toast("Session saved in memory; account progress persisted.");
                }
            }
        };
        dialog.text("Simulation and entity animation are frozen while this menu is open.");
        dialog.button("Save & Exit", "save");
        dialog.button("Restart", "restart");
        dialog.button("Resume", "resume");
        dialog.show(stage);
    }

    protected void restartCurrentLevel() {
        if (scoreModeActive) gameController.startScoreMode();
        else gameController.startGame(activeChapter.name(), activeLevel);
        selectedPlant = null; shovelMode = false; plantFoodMode = false; paused = false;
        if (gameController.shouldSkipPlantSelection()) {
            gameController.startActualGame(gameController.getDefaultTotalWaves()); showGame();
        } else showPlantSelection();
    }

    protected void refreshGameHud() {
        Board board = gameController.getBoard();
        if (board == null || gameHudLabel == null) return;
        gameHudLabel.setText(human(board.getChapter().name()) + "  " + activeLevel
            + "   Wave " + Math.min(board.getTotalWaves(), board.getCurrentWave() + 1) + "/" + board.getTotalWaves());
        if (gameSunLabel != null) gameSunLabel.setText("Sun  " + board.getSunAmount());
        if (gameFoodLabel != null) gameFoodLabel.setText("Plant Food  " + board.getPlantFoodCount());
        if (waveProgress != null) {
            waveProgress.setValue(board.getZombieProgress());
        }
        if (specialHudLabel != null) specialHudLabel.setText(specialStatus(board));
        refreshResources();
    }

    protected String specialStatus(Board board) {
        SpecialLevelHandler h = board.getSpecialLevelHandler();
        if (h instanceof TimedWarLevel t) return "Timed: " + t.getProgress() + "/" + t.getTarget() + " | "
            + String.format(Locale.ROOT, "%.1fs", t.getTimeRemainingTicks() / (float) Board.TICKS_PER_SECOND);
        if (h instanceof LoveYourPlantsLevel l) return "Plants left to lose: " + Math.max(0, l.getMaxPlantLosses() - l.getLossCount()) + " | Lost " + l.getLossCount() + "/" + l.getMaxPlantLosses();
        if (h instanceof PlantWhatYouGetLevel p) return p.isWavesStarted() ? "Waves active" : "Free setup phase";
        if (h instanceof ConveyorBeltLevel belt) return "Belt cards: " + belt.peekQueue().size();
        return "";
    }

    protected void updateLiveState(float delta) {
        Board board = gameController.getBoard();
        if (adventureScreenActive && board != null && boardActor != null && !paused) {
            User user = currentUser();
            tickAccumulator += delta * user.getGameSpeed();
            int ticks = 0;
            while (tickAccumulator >= 1f / Board.TICKS_PER_SECOND && ticks < 20) {
                tickAccumulator -= 1f / Board.TICKS_PER_SECOND;
                gameController.advanceTime(1);
                ticks++;
                if (board.isGameOver()) break;
            }
            if (ticks > 0) {
                refreshGameHud();
                if (!seedBankSignature().equals(lastSeedBankSignature)) rebuildGamePlantBar();
                if (board.getCurrentWave() != lastWave) {
                    lastWave = board.getCurrentWave();
                    toast("Wave " + Math.min(board.getTotalWaves(), lastWave + 1) + " is approaching!");
                    audio.playCue("wave");
                    if (board.getChapter() == ChapterType.DARK_AGES) toast("Necromancy may raise zombies from marked cells!");
                    if (board.getChapter() == ChapterType.BIG_WAVE_BEACH) toast("Tide change: watch low-water entry points!");
                    if (board.getChapter() == ChapterType.FROSTBITE_CAVES) toast("Freezing wind may affect plants!");
                    if (board.getChapter() == ChapterType.ANCIENT_EGYPT && lastWave + 1 >= board.getTotalWaves()) toast("Sandstorm/tornado entry may bring zombies forward!");
                }
                if (board.getPlantFoodCount() > lastPlantFood) toast("Plant Food collected!");
                lastPlantFood = board.getPlantFoodCount();
                if (user.getCoins() > lastCoins) toast("Coins collected: +" + (user.getCoins() - lastCoins));
                if (user.getDiamonds() > lastDiamonds) toast("Diamonds collected: +" + (user.getDiamonds() - lastDiamonds));
                if (user.getPotCount() > lastPotCount) toast("Greenhouse pot collected/unlocked!");
                lastCoins = user.getCoins(); lastDiamonds = user.getDiamonds(); lastPotCount = user.getPotCount();
            }
            if (board.isGameOver()) finishGame(board);
        }
        updateMinigame(delta);
    }

    protected void finishGame(Board board) {
        boolean won = board.isPlayerWon();
        gameController.endGame(won);
        audio.playCue(won ? "win" : "lose");
        paused = true;
        Dialog dialog = new Dialog(won ? "VICTORY" : "DEFEAT", skin) {
            @Override
            protected void result(Object object) {
                if ("retry".equals(object)) restartCurrentLevel();
                else showAdventure();
            }
        };
        dialog.text((won ? "Crazy Dave: Excellent defense!" : "Crazy Dave: The lawn was overrun.")
            + (scoreModeActive ? "\nYour score-mode best is now " + currentUser().getBestScoreMode() + " myopoints." : ""));
        if (!won) dialog.button("Try again", "retry");
        dialog.button("Exit", "exit");
        dialog.show(stage);
    }

    
    
    


}
