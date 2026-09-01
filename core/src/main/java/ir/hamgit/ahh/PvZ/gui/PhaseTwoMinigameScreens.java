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

 
abstract class PhaseTwoMinigameScreens extends PhaseTwoGameplayScreens {

    protected void showQuests() {
        beginAuthenticated("Travel Log / Missions", true, this::showMain);
        User user = currentUser();
        QuestService.prepareDaily(user); UserRepository.updateUser(user);
        Table tabs = new Table();
        for (Category c : Category.values()) tabs.add(button(human(c.name()), () -> { questCategory = c; showQuests(); })).width(118).pad(2);
        tabs.add(button("Minigames", this::showMinigamePicker)).width(125).pad(2);
        tabs.add(button("Score Mode", this::startScoreMode)).width(120).pad(2);
        root.add(tabs).growX().row();
        Table list = new Table();
        list.defaults().growX().pad(6);
        for (Quest quest : QuestService.questsFor(questCategory)) {
            int progress = user.getQuestProgress(quest.id());
            int target = QuestService.targetFor(user, quest);
            boolean claimed = QuestService.isClaimed(user, quest.id());
            Table card = new Table();
            card.setBackground(skin.newDrawable("white", new Color(.12f, .15f, .11f, 1f)));
            card.add(wrapped(quest.title() + "\n" + QuestService.conditionFor(user, quest) + "\nProgress: " + progress + "/" + target
                + "\nReward: " + QuestService.rewardFor(user, quest) + (claimed ? "  [CLAIMED]" : ""))).width(740).left();
            TextButton claim = button(claimed ? "Claimed" : "Claim", "default",
                progress < target && !claimed ? new Color(.45f, .45f, .45f, 1f) : null, () -> {
                if (QuestService.claim(user, quest.id())) { UserRepository.updateUser(user); toast("Quest reward claimed."); showQuests(); }
                else toast("Quest is incomplete, already claimed, or unavailable.");
            });
            claim.setDisabled(claimed || progress < target);
            card.add(claim).width(110).padLeft(12);
            list.add(card).width(920).row();
        }
        root.add(scroll(list)).grow().pad(12);
    }

    protected void showMinigamePicker() {
        beginAuthenticated("Minigames", true, this::showQuests);
        String[] games = {"vasebreaker", "wallnut_bowling", "i_zombie", "beghouled", "zombotany"};
        Table table = new Table();
        table.defaults().pad(10);
        for (String game : games) {
            Table card = new Table();
            card.setBackground(skin.newDrawable("white", new Color(.12f, .14f, .10f, 1f)));
            card.add(new Label(human(game), skin, "subtitle")).colspan(3).row();
            for (int level = 1; level <= 3; level++) {
                final int l = level;
                Runnable launch = "i_zombie".equals(game)
                    ? () -> chooseIZombieMode(l) : () -> startMinigame(game, l);
                card.add(button("Level " + level, launch)).width(100).pad(4);
            }
            table.add(card).width(390).height(115);
            if ((Arrays.asList(games).indexOf(game) + 1) % 2 == 0) table.row();
        }
        root.add(framed(table)).grow().center().pad(22f);
    }

    protected void startMinigame(String name, int level) {
        startMinigame(name, level, false);
    }

     
    protected void chooseIZombieMode(int level) {
        Dialog mode = new Dialog("I, Zombie - Level " + level, skin) {
            @Override protected void result(Object object) {
                if (object instanceof Boolean couchPlay) {
                    startMinigame("i_zombie", level, couchPlay);
                }
            }
        };
        mode.text("Choose Solo or two-player Couch Play on this device.\n"
            + "Couch Play uses one mouse: Player 1 controls Plants and Player 2 controls Zombies.");
        mode.button("Solo", false);
        mode.button("Couch Play", true);
        mode.show(stage);
    }

    protected void startMinigame(String name, int level, boolean couchPlay) {
        if (!minigameController.start(name, level, couchPlay)) { toast("Could not start minigame."); return; }
        selectedPlant = null; selectedZombie = null; beghouledFirstX = beghouledFirstLane = -1;
        showMinigame();
    }

    protected void showMinigame() {
        MinigameSession session = minigameController.getActiveGame();
        if (session == null) { showMinigamePicker(); return; }
        clearScreen();
        backdrop.setMood(PvzBackdropActor.Mood.HIDDEN);
        Board minigameBoard = boardOf(session);
        audio.playGameMusic(minigameBoard == null ? ChapterType.ANCIENT_EGYPT : minigameBoard.getChapter());
        minigameScreenActive = true;
        tickAccumulator = 0f;
        User user = currentUser();

        boardActor = new BoardActor(() -> boardOf(minigameController.getActiveGame()), skin.getFont("FBUSV8C6EI_3"), pvzAssets);
        boardActor.setShowGridSupplier(() -> currentUser().isShowGrid());
        boardActor.setCancelAction(this::cancelCurrentAction);
        boardActor.setGameplayInsets(141f, 0f);
        boardActor.setVisualCueListener(audio::playCue);
        configureMinigameBoard(session);

        Stack battlefield = new Stack();
        battlefield.add(boardActor);
        Table overlay = new Table();
        overlay.top();
        Table header = new Table();
        header.setBackground(tinted("image_ui_quests_panel_edge_to_edge_ten", new Color(.08f, .14f, .07f, .95f)));
        header.pad(4f, 10f, 4f, 10f);
        header.add(new Label(human(minigameController.getActiveName()) + " - Level " + minigameController.getActiveLevel(), skin, "window")).expandX().left();
        minigameHud = new Label("", skin);
        header.add(minigameHud).padRight(14);
        resourceLabel = new Label(resourceText(user), skin);
        header.add(resourceLabel).padRight(10);
        header.add(button("Exit", "brown", this::showMinigamePicker)).width(90f).height(40f);
        overlay.add(header).growX().height(51f).row();

        minigameControls = new Table();
        minigameControls.defaults().height(78f).pad(3f);
        ScrollPane controlScroll = new ScrollPane(minigameControls, skin);
        controlScroll.setFadeScrollBars(false);
        controlScroll.setScrollingDisabled(false, true);
        Table controlFrame = new Table();
        controlFrame.setBackground(tinted("image_ui_mainmenu_mm_settings_tab_10", new Color(.13f, .23f, .10f, .93f)));
        controlFrame.pad(4f, 9f, 4f, 9f);
        controlFrame.add(controlScroll).grow();
        overlay.add(controlFrame).growX().height(90f).row();
        overlay.add().grow();
        battlefield.add(overlay);
        root.add(battlefield).grow();
        refreshMinigameControls();
        refreshMinigameHud();
        toast("Minigame started. Use the mouse to interact with the board.");
    }

    protected void configureMinigameBoard(MinigameSession session) {
        if (session instanceof VasebreakerGame vase) {
            boardActor.setCellMarkerProvider(vase::getVaseMarker);
            boardActor.setCellClickListener((x, lane) -> {
                String marker = vase.getVaseMarker(x, lane);
                if (marker.equals("?") || marker.equals("P") || marker.equals("G")) vase.breakVase(x, lane);
                else if (marker.equals("S")) vase.collectSeedPacket(x, lane);
                else if (selectedPlant != null) vase.plantHeldSeed(selectedPlant, x, lane);
                refreshMinigameControls(); refreshMinigameHud();
            });
        } else if (session instanceof WallnutBowlingGame bowling) {
            boardActor.setRedLineOverride(bowling.getRedLineColumn());
            boardActor.setBowlingBallsSupplier(bowling::getBalls);
            boardActor.setCellClickListener((x, lane) -> {
                if (!bowling.plantBall(x, lane)) toast("Place the next nut at or left of the red line.");
                refreshMinigameControls();
            });
        } else if (session instanceof IZombieGame iz) {
            boardActor.setRedLineOverride(iz.getFirstPlacementColumn() - 1);
            boardActor.setBrainSupplier(iz::getBrainAvailability);
            boardActor.setCellClickListener((x, lane) -> {
                if (iz.isCouchPlay() && iz.collectPlantSun(x, lane)) {
                    refreshMinigameControls(); refreshMinigameHud(); return;
                }
                if (iz.isCouchPlay() && selectedPlant != null) {
                    if (!iz.placePlant(selectedPlant, x, lane)) {
                        toast("P1: plant in columns 1-6; check sun, cooldown, and tile.");
                    }
                } else if (selectedZombie != null) {
                    if (!iz.placeZombie(selectedZombie, x, lane)) {
                        toast(iz.isCouchPlay()
                            ? "P2: place zombies in columns 7-8; check sun and cooldown."
                            : "Cannot place that zombie there or not enough sun.");
                    }
                } else {
                    toast(iz.isCouchPlay()
                        ? "Choose a P1 plant or P2 zombie card first."
                        : "Choose a zombie first.");
                }
                refreshMinigameControls(); refreshMinigameHud();
            });
        } else if (session instanceof BeghouledGame beghouled) {
            boardActor.setCellMarkerProvider((x, lane) -> beghouled.isCrater(x, lane) ? "X" : "");
            boardActor.setCellClickListener((x, lane) -> {
                if (beghouledFirstX < 0) {
                    beghouledFirstX = x; beghouledFirstLane = lane; toast("Choose an adjacent plant to swap.");
                } else {
                    boolean ok = beghouled.swapPlants(beghouledFirstX, beghouledFirstLane, x, lane);
                    if (!ok) toast("Swap was invalid or did not create a match.");
                    beghouledFirstX = beghouledFirstLane = -1;
                    refreshMinigameHud();
                }
            });
        } else if (session instanceof ZombotanyGame zombotany) {
            boardActor.setCellClickListener((x, lane) -> {
                boolean collected = zombotany.getBoard().collectSun(x, lane)
                    | zombotany.getBoard().collectFallingSun(x, lane);
                if (collected) { refreshMinigameHud(); return; }
                if (selectedPlant == null) { toast("Choose a plant."); return; }
                if (!zombotany.plant(selectedPlant, x, lane)) toast("Cannot plant there or not enough sun.");
            });
        }
    }

    protected void refreshMinigameControls() {
        if (minigameControls == null) return;
        MinigameSession session = minigameController.getActiveGame();
        String signature = minigameControlSignature(session);
        if (signature.equals(lastMinigameControlSignature)) return;
        lastMinigameControlSignature = signature;
        minigameControls.clearChildren();
        if (session instanceof VasebreakerGame vase) {
            minigameControls.add(wrapped("Break vases, collect seed packets, then choose and plant them.")).width(225f);
            for (PlantType type : vase.getHeldSeeds()) {
                PlantDef def = PlantRegistry.get(type);
                minigameControls.add(plantActionCard(type,
                    def.getDisplayName() + "\n" + def.getSunCost() + " sun",
                    () -> { selectedPlant = type; boardActor.setCursorPlant(type); }, null, 48)).width(175f);
            }
        } else if (session instanceof WallnutBowlingGame bowling) {
            minigameControls.add(wrapped("Conveyor: click at or left of the red line to roll the first nut.")).width(225f);
            for (BowlingBall.Kind kind : bowling.getQueue()) {
                Table nut = new Table();
                nut.setBackground(tinted("image_ui_cards_almanac_plant_card_10", Color.WHITE));
                nut.add(new Label(human(kind.name()), skin, "subtitle"));
                minigameControls.add(nut).width(145f);
            }
        } else if (session instanceof IZombieGame iz) {
            if (iz.isCouchPlay()) {
                minigameControls.add(wrapped("COUCH PLAY - one mouse. P1 Plants: columns 1-6. "
                    + "P2 Zombies: columns 7-8. Select a card, then a cell; click produced sun to collect it."))
                    .width(280f);
                for (PlantType type : iz.getAvailablePlants()) {
                    int cooldown = iz.getPlantCooldownTicks(type);
                    String recharge = cooldown <= 0 ? "READY" :
                        String.format(Locale.ROOT, "%.1fs", cooldown / (float) Board.TICKS_PER_SECOND);
                    Color tint = cooldown > 0 ? new Color(.65f, .65f, .65f, 1f) : null;
                    PlantDef definition = PlantRegistry.get(type);
                    minigameControls.add(plantActionCard(type,
                        "P1 - " + definition.getDisplayName() + " - " + iz.getPlantCost(type)
                            + " sun - " + recharge, () -> {
                            selectedPlant = type;
                            selectedZombie = null;
                            boardActor.setCursorPlant(type);
                        }, tint, 40)).width(210f);
                }
            } else {
                minigameControls.add(wrapped("Choose a zombie, place it right of the red line, and eat every brain."))
                    .width(225f);
            }
            for (ZombieType type : iz.getAvailableZombies()) {
                int cooldown = iz.getCooldownTicks(type);
                String recharge = cooldown <= 0 ? "READY" :
                    String.format(Locale.ROOT, "%.1fs", cooldown / (float) Board.TICKS_PER_SECOND);
                Color tint = cooldown > 0 ? new Color(.65f, .65f, .65f, 1f) : null;
                minigameControls.add(zombieActionCard(type,
                    (iz.isCouchPlay() ? "P2 - " : "") + human(type.name()) + " - "
                        + iz.getCost(type) + " sun - " + recharge, () -> {
                        selectedZombie = type;
                        selectedPlant = null;
                        boardActor.setCursorText(type.name());
                    }, tint, 40)).width(205f);
            }
        } else if (session instanceof BeghouledGame beghouled) {
            minigameControls.add(wrapped("Swap adjacent plants to make matches. Spend sun on upgrades.")).width(225f);
            addBeghouledUpgradeButtons(beghouled);
        } else if (session instanceof ZombotanyGame) {
            minigameControls.add(wrapped("Bonus Zombotany: defend against plant-zombie hybrids.")).width(225f);
            for (PlantType type : List.of(PlantType.PEASHOOTER, PlantType.SUNFLOWER, PlantType.WALL_NUT, PlantType.POTATO_MINE)) {
                PlantDef def = PlantRegistry.get(type);
                minigameControls.add(plantActionCard(type,
                    def.getDisplayName() + "\n" + def.getSunCost() + " sun",
                    () -> { selectedPlant = type; boardActor.setCursorPlant(type); }, null, 48)).width(175f);
            }
        }
    }

    protected String minigameControlSignature(MinigameSession session) {
        if (session == null) return "none";
        if (session instanceof VasebreakerGame vase) return "vase:" + vase.getHeldSeeds();
        if (session instanceof WallnutBowlingGame bowling) return "bowl:" + bowling.getQueue();
        if (session instanceof IZombieGame iz) {
            StringBuilder value = new StringBuilder(iz.isCouchPlay() ? "iz-couch:" : "iz:")
                .append(iz.getSunAmount());
            if (iz.isCouchPlay()) {
                value.append(':').append(iz.getPlantSunAmount());
                for (PlantType type : iz.getAvailablePlants()) {
                    int cooldown = iz.getPlantCooldownTicks(type);
                    value.append('|').append(type).append(':')
                        .append((cooldown + Board.TICKS_PER_SECOND - 1) / Board.TICKS_PER_SECOND);
                }
            }
            for (ZombieType type : iz.getAvailableZombies()) {
                int cooldown = iz.getCooldownTicks(type);
                value.append('|').append(type).append(':')
                    .append((cooldown + Board.TICKS_PER_SECOND - 1) / Board.TICKS_PER_SECOND);
            }
            return value.toString();
        }
        if (session instanceof BeghouledGame game) {
            return "gem:" + game.getSunAmount() + ':' + game.getMatchesMade();
        }
        if (session instanceof ZombotanyGame game) return "zombotany:" + game.getBoard().getSunAmount();
        return session.getClass().getName();
    }

    protected void addBeghouledUpgradeButtons(BeghouledGame game) {
        
        PlantType[][] pairs = {
            {PlantType.PEASHOOTER, PlantType.REPEATER},
            {PlantType.REPEATER, PlantType.MEGA_GATLING_PEA},
            {PlantType.WALL_NUT, PlantType.TALL_NUT},
            {PlantType.PUFF_SHROOM, PlantType.FUME_SHROOM},
            {PlantType.CABBAGE_PULT, PlantType.MELON_PULT},
            {PlantType.MELON_PULT, PlantType.WINTER_MELON}
        };
        for (PlantType[] pair : pairs) {
            minigameControls.add(button(human(pair[0].name()) + " -> " + human(pair[1].name()), () -> {
                if (!game.upgrade(pair[0], pair[1])) toast("That upgrade is unavailable or costs more sun.");
                refreshMinigameHud();
            })).width(205f);
        }
    }

    protected Board boardOf(MinigameSession session) {
        if (session instanceof VasebreakerGame g) return g.getBoard();
        if (session instanceof WallnutBowlingGame g) return g.getBoard();
        if (session instanceof IZombieGame g) return g.getBoard();
        if (session instanceof BeghouledGame g) return g.getBoard();
        if (session instanceof ZombotanyGame g) return g.getBoard();
        return null;
    }

    protected void refreshMinigameHud() {
        if (minigameHud == null) return;
        MinigameSession session = minigameController.getActiveGame();
        if (session instanceof VasebreakerGame vase) {
            long vases = 0; for (int r = 0; r < vase.getBoard().getRows(); r++) for (int c = 0; c < vase.getBoard().getColumns(); c++) if (!vase.getVaseMarker(c, r).equals(".")) vases++;
            minigameHud.setText("Held seeds: " + vase.getHeldSeeds().size() + " | Active vase/seed cells: " + vases);
        } else if (session instanceof WallnutBowlingGame bowling) {
            minigameHud.setText("Queue: " + bowling.getQueue().size() + " | Zombies: " + bowling.getBoard().getZombies().stream().filter(z -> z.isAlive()).count());
        } else if (session instanceof IZombieGame iz) {
            long brains = 0;
            for (boolean available : iz.getBrainAvailability()) if (available) brains++;
            if (iz.isCouchPlay()) {
                minigameHud.setText("P1 Plants: " + iz.getPlantSunAmount() + " sun | P2 Zombies: "
                    + iz.getSunAmount() + " sun | Brains: " + brains + "/5 | Time: "
                    + iz.getRemainingSeconds() + "s");
            } else {
                minigameHud.setText("Sun: " + iz.getSunAmount() + " | Brains left: " + brains);
            }
        } else if (session instanceof BeghouledGame b) {
            minigameHud.setText("Sun: " + b.getSunAmount() + " | Matches: " + b.getMatchesMade() + "/" + b.getTargetMatches());
        } else if (session instanceof ZombotanyGame z) {
            minigameHud.setText("Sun: " + z.getBoard().getSunAmount() + " | Zombies: " + z.getBoard().getZombies().stream().filter(zz -> zz.isAlive()).count());
        }
        refreshResources();
    }

    protected void updateMinigame(float delta) {
        MinigameSession current = minigameController.getActiveGame();
        if (!minigameScreenActive || current == null || boardActor == null) return;
        tickAccumulator += delta * currentUser().getGameSpeed();
        int ticks = 0;
        while (tickAccumulator >= 1f / Board.TICKS_PER_SECOND && ticks < 20) {
            tickAccumulator -= 1f / Board.TICKS_PER_SECOND;
            ticks++;
            MinigameSession before = minigameController.getActiveGame();
            boolean finished = minigameController.advanceTime(1);
                if (finished) {
                boolean won = before != null && before.isWon();
                boolean couch = before instanceof IZombieGame iz && iz.isCouchPlay();
                String title = couch
                    ? (((IZombieGame) before).isWon() ? "Player 1 wins" : "Player 2 wins")
                    : (won ? "Minigame victory" : "Minigame over");
                audio.playCue(won ? "win" : "lose");
                Dialog d = new Dialog(title, skin) {
                    @Override protected void result(Object object) { showMinigamePicker(); }
                };
                if (couch) {
                    IZombieGame iz = (IZombieGame) before;
                    d.text(iz.getCouchResultText() + (iz.isWon()
                        ? "\nThe signed-in Player 1 receives the win reward."
                        : "\nPlayer 2 is a local guest, so no account reward is recorded."));
                } else {
                    d.text(won ? "Reward and quest progress were applied." : "Try another strategy or level.");
                }
                d.button("Return");
                d.show(stage);
                return;
            }
        }
        if (ticks > 0) { refreshMinigameControls(); refreshMinigameHud(); }
    }

    protected Actor plantActionCard(PlantType type, String text, Runnable action, Color tint, int iconSize) {
        Table card = new Table();
        card.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        Color background = tint != null ? new Color(tint.r, tint.g, tint.b, .96f) : Color.WHITE;
        card.setBackground(tinted("image_ui_cards_almanac_plant_card_10", background));
        card.pad(4);

        CreaturePreviewActor preview = new CreaturePreviewActor(skin.getFont("FBUSV8C6EI_3"), pvzAssets);
        preview.showPlant(type);
        Label details = wrapped(text);
        details.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        preview.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        card.add(preview).size(Math.max(52, iconSize)).padRight(7);
        card.add(details).grow().left();
        addImmediateCardAction(card, action);
        return card;
    }

    protected Actor zombieActionCard(ZombieType type, String text, Runnable action, Color tint, int iconSize) {
        Table card = new Table();
        card.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        Color background = tint != null ? new Color(tint.r, tint.g, tint.b, .96f)
            : new Color(.36f, .42f, .28f, .98f);
        card.setBackground(tinted("image_ui_dialog_asset_inner_bkgd_10", background));
        card.pad(4);

        CreaturePreviewActor preview = new CreaturePreviewActor(skin.getFont("FBUSV8C6EI_3"), pvzAssets);
        preview.showZombie(type);
        Label details = wrapped(text);
        details.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        preview.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        card.add(preview).size(Math.max(52, iconSize)).padRight(7);
        card.add(details).grow().left();
        addImmediateCardAction(card, action);
        return card;
    }

    protected Actor shopIcon(ShopItem item) {
        return new ProceduralIconActor(pvzAssets, item.getKind().name());
    }

    
    
    

}
