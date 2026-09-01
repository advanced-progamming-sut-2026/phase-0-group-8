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

 
abstract class PhaseTwoContentScreens extends PhaseTwoAccountScreens {

    protected void showCollection() {
        beginAuthenticated("Collection", true, this::showMain);
        User user = currentUser();
        Table tabs = new Table();
        tabs.add(button("Plants", () -> { collectionPlantTab = true; showCollection(); })).width(140);
        tabs.add(button("Zombies", () -> { collectionPlantTab = false; showCollection(); })).width(140).padLeft(6);
        root.add(tabs).growX().row();
        if (collectionPlantTab) showPlantCollection(user); else showZombieCollection(user);
    }

    protected void showPlantCollection(User user) {
        Table filters = new Table();
        SelectBox<String> family = new SelectBox<>(skin);
        List<String> families = new ArrayList<>();
        families.add("All");
        for (PlantFamily f : PlantFamily.values()) families.add(f.name());
        family.setItems(families.toArray(String[]::new));
        family.setSelected(collectionFamily);
        family.setMaxListCount(7);
        SelectBox<String> ownership = new SelectBox<>(skin);
        ownership.setItems("All", "Unlocked", "Locked");
        ownership.setSelected(collectionOwnership);
        ownership.setMaxListCount(3);
        CheckBox upgradeable = new CheckBox(" Upgradeable only", skin);
        upgradeable.setChecked(collectionUpgradeable);
        filters.add(new Label("Family", skin)).padRight(4); filters.add(family).width(150).padRight(12);
        filters.add(new Label("State", skin)).padRight(4); filters.add(ownership).width(140).padRight(12);
        filters.add(upgradeable);
        family.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { collectionFamily = family.getSelected(); showCollection(); }});
        ownership.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { collectionOwnership = ownership.getSelected(); showCollection(); }});
        upgradeable.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { collectionUpgradeable = upgradeable.isChecked(); showCollection(); }});
        root.add(filters).growX().row();

        Table cards = new Table();
        cards.defaults().width(175).height(106).pad(4);
        int count = 0;
        for (PlantDef def : PlantRegistry.getAll()) {
            PlantType type = def.getType();
            boolean unlocked = user.hasPlant(type);
            if (!collectionFamily.equals("All") && def.getFamily() != PlantFamily.valueOf(collectionFamily)) continue;
            if (collectionOwnership.equals("Unlocked") && !unlocked) continue;
            if (collectionOwnership.equals("Locked") && unlocked) continue;
            if (collectionUpgradeable && !user.upgradeAllowed(type)) continue;
            int level = user.getPlantLevel(type);
            int need = level >= 4 ? 0 : 10 * level;
            String packet = level >= 4 ? "MAX" : user.getSeedPackets(type) + "/" + need + " seeds";
            String text = def.getDisplayName() + "\nLv " + level + " | " + def.getFamily() + "\n" + packet
                + (unlocked ? "" : "\nLOCKED");
            Color tint = !unlocked ? new Color(.58f, .58f, .58f, 1f)
                : user.upgradeAllowed(type) ? new Color(1f, .88f, .45f, 1f) : null;
            cards.add(plantActionCard(type, text, () -> { collectionPlant = type; showCollection(); }, tint, 48));
            if (++count % 4 == 0) cards.row();
        }
        Table detail = buildPlantDetail(user, collectionPlant);
        Table body = new Table();
        body.add(scroll(cards)).width(760).growY();
        body.add(detail).width(400).growY().padLeft(16);
        root.add(body).grow().pad(10);
    }

    protected Table buildPlantDetail(User user, PlantType type) {
        Table detail = new Table();
        detail.setBackground(skin.newDrawable("white", new Color(.11f, .16f, .12f, 1f)));
        detail.defaults().pad(5).growX();
        if (type == null) {
            detail.add(wrapped("Click a plant card to see its idle animation and details.")).width(380);
            return detail;
        }
        PlantDef def = PlantRegistry.get(type);
        CreaturePreviewActor preview = new CreaturePreviewActor(skin.getFont("FBUSV8C6EI_3"), pvzAssets);
        preview.showPlant(type);
        detail.add(preview).size(170).row();
        detail.add(new Label(def.getDisplayName(), skin, "subtitle")).row();
        detail.add(wrapped("Health: " + def.getMaxHp() + "   Cost: " + def.getSunCost() + " sun\nFamily: " + def.getFamily()
            + "\nTags: " + def.getTags() + "\nBehaviors: " + def.getBehaviors() + "\nLevel: " + user.getPlantLevel(type)
            + "   Seeds: " + user.getSeedPackets(type))).width(390).row();
        if (!user.hasPlant(type)) {
            detail.add(button("Buy for 2000 coins", () -> confirm("Purchase plant", "Buy " + def.getDisplayName() + " for 2000 coins?", () -> {
                if (!user.spendCoins(2000)) { toast("Not enough coins."); return; }
                user.unlockPlant(type); UserRepository.updateUser(user); showCollection(); toast("Plant unlocked.");
            }))).width(220).row();
        } else {
            detail.add(button("Upgrade", () -> {
                if (!user.upgradeAllowed(type)) { toast("Not enough seed packets/coins, or plant is max level."); return; }
                user.upgradeStats(type); UserRepository.updateUser(user); showCollection(); toast("Plant upgraded.");
            })).width(180).row();
        }
        return detail;
    }

    protected void showZombieCollection(User user) {
        Table cards = new Table();
        cards.defaults().width(175).height(102).pad(4);
        int count = 0;
        for (ZombieDef def : ZombieRegistry.getAll().values()) {
            ZombieType type = def.getType();
            boolean seen = user.getSeenZombies().contains(type);
            if (seen) {
                cards.add(zombieActionCard(type, def.getDisplayName(), () -> {
                    collectionZombie = type; showCollection();
                }, null, 46));
            } else {
                TextButton hidden = button("????\nUndiscovered", "default", new Color(.35f, .35f, .35f, 1f),
                    () -> { collectionZombie = type; showCollection(); });
                cards.add(hidden);
            }
            if (++count % 4 == 0) cards.row();
        }
        Table detail = new Table();
        detail.setBackground(skin.newDrawable("white", new Color(.13f, .13f, .13f, 1f)));
        if (collectionZombie == null || !user.getSeenZombies().contains(collectionZombie)) {
            detail.add(wrapped(collectionZombie == null ? "Click a zombie to view details." : "This zombie has not been discovered yet.")).width(390);
        } else {
            ZombieDef def = ZombieRegistry.get(collectionZombie);
            CreaturePreviewActor preview = new CreaturePreviewActor(skin.getFont("FBUSV8C6EI_3"), pvzAssets);
            preview.showZombie(collectionZombie);
            detail.add(preview).size(170).row();
            detail.add(new Label(def.getDisplayName(), skin, "subtitle")).row();
            detail.add(wrapped("Health: " + def.getMaxHp() + "\nSpeed: " + def.getSpeed() + "\nDamage: " + def.getDamage()
                + "\nArmor: " + def.getArmorLayers() + "\nBehaviors: " + def.getBehaviors())).width(390);
        }
        Table body = new Table();
        body.add(scroll(cards)).width(760).growY();
        body.add(detail).width(400).growY().padLeft(16);
        root.add(body).grow().pad(10);
    }

    
    
    

    protected void showGreenhouse() {
        beginAuthenticated("Greenhouse", true, this::showMain);
        User user = currentUser();
        Table greenhouseHeader = new Table();
        greenhouseHeader.setBackground(skin.newDrawable("white", new Color(.48f, .76f, .72f, .30f)));
        greenhouseHeader.add(new ProceduralIconActor(pvzAssets, "POT")).size(72).padRight(14);
        greenhouseHeader.add(wrapped("Penny's glass greenhouse - animated plants grow in twenty real pots. "
            + "Harvest a mature plant for its reward or use gems to finish growth early.")).width(760).left();
        root.add(greenhouseHeader).growX().height(82).pad(8).row();
        Table pots = new Table();
        pots.setBackground(skin.newDrawable("white", new Color(.20f, .48f, .38f, .82f)));
        pots.defaults().width(205).height(210).pad(6);
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                pots.add(buildPot(user, x, y));
            }
            pots.row();
        }
        root.add(scroll(pots)).grow().pad(10);
    }

    protected Actor buildPot(User user, int x, int y) {
        GreenHousePot pot = user.getGreenhousePot(x, y);
        Table card = new Table();
        card.setBackground(skin.newDrawable("white", new Color(.12f, .18f, .12f, 1f)));
        card.defaults().pad(2);
        card.add(new Label("Pot " + x + "," + y, skin, "subtitle")).row();
        if (pot.isLocked()) {
            ProceduralIconActor potIcon = new ProceduralIconActor(pvzAssets, "POT");
            potIcon.setColor(.38f, .38f, .38f, 1f);
            card.add(potIcon).size(68).row();
            card.add(new Label("LOCKED", skin)).row();
            card.add(button("Buy next pot - 2000", () -> confirm("Buy pot", "Unlock the next greenhouse pot for 2000 coins?", () -> {
                Result r = ShopService.purchase(user, "pot", 1, null);
                if (r.isSuccessful()) UserRepository.updateUser(user);
                toast(r.getResult()); showGreenhouse();
            }))).width(175);
            return card;
        }
        if (pot.isEmpty()) {
            card.add(new ProceduralIconActor(pvzAssets, "POT")).size(74).row();
            card.add(new Label("Empty", skin)).row();
            card.add(button("Plant random", () -> {
                Result r = GreenhouseService.plant(user, x, y);
                if (r.isSuccessful()) UserRepository.updateUser(user);
                toast(r.getResult()); showGreenhouse();
            })).width(135);
            return card;
        }
        CreaturePreviewActor preview = new CreaturePreviewActor(skin.getFont("FBUSV8C6EI_3"), pvzAssets);
        preview.showPlant(pot.getPlantType());
        Stack growingPlant = new Stack();
        growingPlant.add(new ProceduralIconActor(pvzAssets, "POT"));
        growingPlant.add(preview);
        card.add(growingPlant).size(112).padTop(2).row();
        card.add(new Label(human(pot.getPlantType().name()), skin)).row();
        if (pot.isReadyToCollect()) {
            card.add(button("Collect reward", () -> {
                Result r = GreenhouseService.collect(user, x, y);
                if (r.isSuccessful()) UserRepository.updateUser(user);
                toast(r.getResult()); showGreenhouse();
            })).width(140);
        } else {
            card.add(new Label(String.format(Locale.ROOT, "%.1f h remaining", pot.hoursRemaining()), skin)).row();
            card.add(button("Speed: " + pot.getAccelerationCost() + " gems", () -> {
                Result r = GreenhouseService.speedGrow(user, x, y);
                if (r.isSuccessful()) UserRepository.updateUser(user);
                toast(r.getResult()); showGreenhouse();
            })).width(145);
        }
        return card;
    }

    protected void showShop() {
        beginAuthenticated("Shop", true, this::showMain);
        User user = currentUser();
        if (ShopService.refreshDailyIfNeeded(user)) UserRepository.updateUser(user);
        Table options = new Table();
        TextField count = new TextField("1", skin);
        SelectBox<String> plant = new SelectBox<>(skin);
        List<String> unlocked = user.getUnlockedPlants().stream().map(Enum::name).toList();
        plant.setItems(unlocked.isEmpty() ? new String[]{PlantType.PEASHOOTER.name()} : unlocked.toArray(String[]::new));
        options.add(new Label("Quantity", skin)).padRight(4); options.add(count).width(70).padRight(14);
        options.add(new Label("Plant for selected packets", skin)).padRight(4); options.add(plant).width(220);
        root.add(options).growX().row();

        Table list = new Table();
        list.defaults().width(520).height(96).pad(8);
        List<ShopItem> items = new ArrayList<>(ShopService.getPermanentItems());
        items.add(ShopService.getDailyItem());
        int i = 0;
        for (ShopItem item : items) {
            Table card = new Table();
            card.setBackground(skin.newDrawable("white", new Color(.16f, .14f, .09f, 1f)));
            String extra = item.isDailyItem() ? "\nToday's plant: " + user.getDailyOfferPlant()
                + (user.isDailyOfferPurchased() ? " (PURCHASED)" : "") + "\nResets in: " + dailyRemaining() : "";
            card.add(shopIcon(item)).size(64).padRight(8);
            card.add(wrapped((item.isDailyItem() ? "DAILY - " : "") + item.getDescription() + extra)).width(280).left();
            card.add(button("Buy", () -> {
                int n;
                try { n = Integer.parseInt(count.getText().trim()); }
                catch (NumberFormatException e) { toast("Quantity must be a whole number."); return; }
                String chosen = plant.getSelected();
                int purchaseCount = item.isDailyItem() ? 1 : n;
                confirm("Confirm purchase", "Buy " + item.getDescription() + " x" + purchaseCount + "?", () -> {
                    Result r = ShopService.purchase(user, item.getId(), purchaseCount, chosen);
                    if (r.isSuccessful()) UserRepository.updateUser(user);
                    toast(r.getResult()); showShop();
                });
            })).width(90).padLeft(8);
            list.add(card);
            if (++i % 2 == 0) list.row();
        }
        root.add(scroll(list)).grow().pad(10);
    }

    
    
    

    protected void showAdventure() {
        beginAuthenticated("Adventure", true, this::showMain);
        User user = currentUser();
        Table chapters = new Table();
        chapters.defaults().width(250).height(150).pad(12);
        int i = 0;
        for (ChapterType chapter : CHAPTERS) {
            boolean unlocked = user.isChapterUnlocked(chapter);
            String text = human(chapter.name()) + "\nCompleted: " + user.getCompletedLevel(chapter) + "/4\n" + (unlocked ? "OPEN" : "LOCKED");
            TextButton card = button(text, "default", unlocked ? null : new Color(.45f, .45f, .45f, 1f), () -> {
                if (!user.isChapterUnlocked(chapter)) { toast("Complete the previous chapter first."); return; }
                showChapterLevels(chapter);
            });
            chapters.add(card);
            if (++i % 2 == 0) chapters.row();
        }
        root.add(framed(chapters)).grow().center().pad(26f);
    }

    protected void showChapterLevels(ChapterType chapter) {
        beginAuthenticated(human(chapter.name()) + " - Levels", true, this::showAdventure);
        User user = currentUser();
        int completed = user.getCompletedLevel(chapter);
        Table levels = new Table();
        levels.defaults().width(210).height(110).pad(10);
        for (int level = 1; level <= 4; level++) {
            final int chosen = level;
            boolean unlocked = chosen <= completed + 1;
            String special = specialLabel(chapter, chosen);
            String text = "Level " + chosen + (special.isBlank() ? "" : "\n" + special) + "\n" + (chosen <= completed ? "COMPLETED" : unlocked ? "AVAILABLE" : "LOCKED");
            TextButton b = button(text, "default", unlocked ? null : new Color(.45f, .45f, .45f, 1f), () -> {
                if (chosen > user.getCompletedLevel(chapter) + 1) { toast("This level is locked."); return; }
                startAdventure(chapter, chosen);
            });
            levels.add(b);
        }
        root.add(framed(levels)).grow().center().pad(26f);
    }

    protected String specialLabel(ChapterType chapter, int level) {
        if (level == 2) {
            return switch (chapter) {
                case ANCIENT_EGYPT -> "Conveyor Belt";
                case FROSTBITE_CAVES -> "Save Our Seeds";
                case BIG_WAVE_BEACH -> "Night Ops";
                case DARK_AGES -> "Love Your Plants";
                default -> "";
            };
        }
        if (level == 3) {
            return switch (chapter) {
                case ANCIENT_EGYPT -> "Locked Plants";
                case FROSTBITE_CAVES -> "Timed War";
                case BIG_WAVE_BEACH -> "Deadline";
                case DARK_AGES -> "Plant What You Get";
                default -> "";
            };
        }
        return level == 4 ? "Final adventure stage" : "Normal";
    }

    protected void startAdventure(ChapterType chapter, int level) {
        activeChapter = chapter;
        activeLevel = level;
        scoreModeActive = false;
        try {
            gameController.startGame(chapter.name(), level);
        } catch (RuntimeException e) {
            toast(e.getMessage());
            return;
        }
        if (gameController.shouldSkipPlantSelection()) {
            gameController.startActualGame(gameController.getDefaultTotalWaves());
            showGame();
        } else {
            showPlantSelection();
        }
    }

    protected void startScoreMode() {
        activeChapter = ChapterType.ANCIENT_EGYPT;
        activeLevel = 1;
        scoreModeActive = true;
        gameController.startScoreMode();
        showPlantSelection();
    }

    protected void showPlantSelection() {
        beginAuthenticated(scoreModeActive ? "Select plants - Score Mode" : "Select plants", true,
            scoreModeActive ? this::showQuests : () -> showChapterLevels(activeChapter));
        User user = currentUser();
        Table selected = new Table();
        selected.add(new Label("Selected (max 8):", skin)).padRight(8);
        List<PlantType> chosen = new ArrayList<>(gameController.getSelectedPlants());
        for (int i = 0; i < 8; i++) {
            if (i < chosen.size()) {
                PlantType type = chosen.get(i);
                PlantDef def = PlantRegistry.get(type);
                Table slot = new Table();
                CreaturePreviewActor icon = new CreaturePreviewActor(skin.getFont("FBUSV8C6EI_3"), pvzAssets);
                icon.showPlant(type);
                slot.add(icon).size(34).row();
                slot.add(new Label(human(type.name()) + " | " + def.getSunCost() + (gameController.getBoostedPlants().contains(type) ? " | BOOST" : ""), skin)).width(118);
                selected.add(slot).width(122).pad(2);
            } else {
                selected.add(new Label("[empty]", skin)).width(122).pad(2);
            }
        }
        root.add(selected).growX().row();

        Table cards = new Table();
        cards.defaults().width(180).height(106).pad(4);
        int count = 0;
        for (PlantDef def : PlantRegistry.getAll()) {
            PlantType type = def.getType();
            if (!gameController.isSelectablePlant(type)) continue;
            boolean isSelected = gameController.getSelectedPlants().contains(type);
            String text = def.getDisplayName() + "\n" + def.getSunCost() + " sun | Lv " + user.getPlantLevel(type)
                + "\n" + def.getFamily() + (user.hasBoost(type) ? " | GH boost" : "") + (isSelected ? "\nSELECTED" : "");
            Color tint = isSelected ? new Color(.75f, 1f, .72f, 1f) : null;
            cards.add(plantActionCard(type, text, () -> {
                selectedPlant = type;
                if (gameController.getSelectedPlants().contains(type)) gameController.removePlantSelection(type);
                else if (!gameController.selectPlant(type)) toast("Cannot select this plant (locked, restricted, or slots full).");
                showPlantSelection();
            }, tint, 48));
            if (++count % 4 == 0) cards.row();
        }

        Table side = new Table();
        side.setBackground(skin.newDrawable("white", new Color(.11f, .16f, .12f, 1f)));
        side.defaults().pad(7).growX();
        side.add(wrapped("Click a plant card to toggle it. Up to 8 plants can be selected. Boost and upgrade actions are available below.")).width(340).row();
        if (selectedPlant != null && gameController.isSelectablePlant(selectedPlant)) {
            PlantDef def = PlantRegistry.get(selectedPlant);
            CreaturePreviewActor preview = new CreaturePreviewActor(skin.getFont("FBUSV8C6EI_3"), pvzAssets); preview.showPlant(selectedPlant);
            side.add(preview).size(140).row();
            side.add(new Label(def.getDisplayName(), skin, "subtitle")).row();
            side.add(button("Upgrade", () -> {
                if (!user.upgradeAllowed(selectedPlant)) { toast("Not enough coins/seeds or max level."); return; }
                user.upgradeStats(selectedPlant); UserRepository.updateUser(user); showPlantSelection();
            })).width(160).row();
            side.add(button("Boost (2 diamonds)", () -> {
                if (!gameController.getSelectedPlants().contains(selectedPlant)) { toast("Select the plant first."); return; }
                if (!gameController.boostPlant(selectedPlant)) { toast("Boost failed: check diamonds and selection."); return; }
                showPlantSelection(); toast("Plant boosted for this level.");
            })).width(190).row();
        }
        side.add(button("LET'S ROCK", () -> {
            if (!gameController.canStartSelectedGame()) { toast("Select at least one plant."); return; }
            gameController.startActualGame(gameController.getDefaultTotalWaves());
            showGame();
        })).width(200).height(50).padTop(10);

        Table body = new Table();
        body.add(scroll(cards)).width(780).growY();
        body.add(side).width(390).growY().padLeft(14);
        root.add(body).grow().pad(8);
    }


}
