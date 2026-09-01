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
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.LeaderboardEntry;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.PasswordChallenge;
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

 
abstract class PhaseTwoAccountScreens extends PhaseTwoScreenBase {

    protected void showLogin() {
        beginPublic("Plants vs Zombies 2 - Login");
        Table form = new Table();
        TextField username = field("username");
        TextField password = field("password");
        password.setPasswordMode(true);
        password.setPasswordCharacter('*');
        CheckBox stay = new CheckBox(" Stay logged in", skin);
        addFormRow(form, "Username", username);
        addFormRow(form, "Password", password);
        form.add(stay).colspan(2).left().padTop(8).row();
        Label error = new Label("", skin);
        error.setColor(1f, .45f, .35f, 1f);
        form.add(error).colspan(2).height(34).row();
        Table actions = new Table();
        actions.add(button("Login", () -> {
            String hash = Validator.hashSha256(password.getText());
            User logged = UserRepository.login(username.getText().trim(), hash, stay.isChecked());
            if (logged != null) {
                gameController.setCurrentUser(logged);
                showMain();
            } else error.setText(serverError("Login failed."));
        })).width(150);
        actions.add(button("Forgot password", this::showForgotPassword)).width(170).padLeft(8);
        actions.add(button("Create account", this::showRegister)).width(160).padLeft(8);
        form.add(actions).colspan(2).padTop(10).row();
        root.add(framed(form)).colspan(2).center().padTop(34f);
    }

    protected void showRegister() {
        beginPublic("Create account", this::showLogin);
        Table form = new Table();
        TextField username = field("letters, digits, hyphens");
        TextField password = field("strong password");
        password.setPasswordMode(true);
        password.setPasswordCharacter('*');
        TextField confirmPass = field("confirm password");
        confirmPass.setPasswordMode(true);
        confirmPass.setPasswordCharacter('*');
        TextField nickname = field("nickname");
        TextField email = field("email@example.com");
        SelectBox<String> gender = new SelectBox<>(skin);
        gender.setItems("Male", "Female");
        gender.setMaxListCount(3);
        SelectBox<String> question = new SelectBox<>(skin);
        question.setItems(SECURITY_QUESTIONS);
        question.setMaxListCount(5);
        TextField answer = field("security answer");
        TextField confirmAnswer = field("confirm answer");
        addFormRow(form, "Username", username);
        addFormRow(form, "Password", password);
        addFormRow(form, "Confirm", confirmPass);
        addFormRow(form, "Nickname", nickname);
        addFormRow(form, "Email", email);
        addFormRow(form, "Gender", gender);
        addFormRow(form, "Security question", question);
        addFormRow(form, "Answer", answer);
        addFormRow(form, "Confirm answer", confirmAnswer);
        Label error = new Label("", skin);
        error.setColor(1f, .45f, .35f, 1f);
        form.add(error).colspan(2).height(34).row();
        Table actions = new Table();
        actions.add(button("Register", () -> {
            String err = validateRegistration(username.getText(), password.getText(), confirmPass.getText(),
                nickname.getText(), email.getText(), answer.getText(), confirmAnswer.getText());
            if (err != null) {
                error.setText(err);
                return;
            }
            Gender g = gender.getSelected().equals("Male") ? Gender.MALE : Gender.FEMALE;
            User user = new User(username.getText().trim(), Validator.hashSha256(password.getText()),
                nickname.getText().trim(), email.getText().trim(), g);
            user.setSecurityQuestion(question.getSelected());
            user.setSecurityAnswerHash(Validator.hashSha256(answer.getText()));
            user.unlockPlant(PlantType.PEASHOOTER);
            user.unlockPlant(PlantType.SUNFLOWER);
            if (!UserRepository.register(user)) {
                error.setText(serverError("Account could not be created."));
                return;
            }
            toast("Account created. You can log in now.");
            showLogin();
        })).width(150);
        form.add(actions).colspan(2).padTop(8);
        root.add(framed(form)).colspan(2).width(900).center().padTop(4);
    }

    protected String validateRegistration(String username, String password, String confirmPassword,
                                        String nickname, String email, String answer, String confirmAnswer) {
        if (!Validator.isValidUsername(username) || UserRepository.usernameExists(username))
            return "Username is invalid or already used.";
        String passwordError = Validator.isStrongPassword(password);
        if (passwordError != null) return passwordError;
        if (!password.equals(confirmPassword)) return "Passwords do not match.";
        if (!Validator.isValidNickname(nickname)) return "Nickname must be 3-30 characters.";
        if (!Validator.isValidEmail(email)) return "Invalid email address.";
        if (answer == null || answer.isBlank() || !answer.equals(confirmAnswer)) return "Security answers do not match.";
        return null;
    }

    protected void showForgotPassword() {
        beginPublic("Password recovery", this::showLogin);
        forgotTarget = null;
        forgotResetToken = null;
        forgotSecurityQuestion = null;
        Table form = new Table();
        TextField username = field("username");
        TextField email = field("account email");
        Label result = wrapped("Enter your username and email to reveal your security question.");
        addFormRow(form, "Username", username);
        addFormRow(form, "Email", email);
        form.add(button("Find account", () -> {
            PasswordChallenge challenge = UserRepository.passwordChallenge(
                username.getText().trim(), email.getText().trim());
            if (challenge == null) {
                result.setText(serverError("No matching account found."));
                return;
            }
            forgotResetToken = challenge.resetToken();
            forgotSecurityQuestion = challenge.securityQuestion();
            showPasswordResetAnswer();
        })).colspan(2).width(180).padTop(8).row();
        form.add(result).colspan(2).width(620).padTop(12).row();
        root.add(framed(form)).colspan(2).center().padTop(48f);
    }

    protected void showPasswordResetAnswer() {
        beginPublic("Security check", this::showForgotPassword);
        if (forgotResetToken == null || forgotSecurityQuestion == null) {
            showForgotPassword();
            return;
        }
        Table form = new Table();
        form.add(wrapped(forgotSecurityQuestion)).colspan(2).width(620).row();
        TextField answer = field("security answer");
        TextField newPassword = field("new strong password");
        newPassword.setPasswordMode(true);
        newPassword.setPasswordCharacter('*');
        addFormRow(form, "Answer", answer);
        addFormRow(form, "New password", newPassword);
        Label error = new Label("", skin);
        error.setColor(1f, .45f, .35f, 1f);
        form.add(error).colspan(2).height(34).row();
        form.add(button("Reset password", () -> {
            String err = Validator.isStrongPassword(newPassword.getText());
            if (err != null) {
                error.setText(err);
                return;
            }
            if (!UserRepository.resetPassword(forgotResetToken, Validator.hashSha256(answer.getText()),
                Validator.hashSha256(newPassword.getText()))) {
                error.setText(serverError("Password could not be changed."));
                return;
            }
            forgotResetToken = null;
            forgotSecurityQuestion = null;
            showLogin();
            toast("Password changed successfully.");
        })).colspan(2).width(180).padTop(8);
        root.add(framed(form)).colspan(2).center().padTop(48f);
    }

    
    
    

    protected void showMain() {
        User user = currentUser();
        beginAuthenticated("Welcome, " + user.getNickname(), true);
        PvzPanel panel = new PvzPanel(skin, new Color(.12f, .24f, .12f, .94f));
        panel.defaults().pad(7f).width(265f).height(118f);
        panel.add(menuTile("hud_zg", "Adventure", "Travel through four worlds and defend the lawn.", this::showAdventure));
        panel.add(menuTile("almanac", "Almanac", "Plants, zombies, levels, upgrades and discoveries.", this::showCollection));
        panel.add(menuTile("plantfood", "Greenhouse", "Grow plants and collect timed rewards.", this::showGreenhouse)).row();
        panel.add(menuTile("hud_quests", "Shop", "Seed packets, Plant Food, pots and daily offers.", this::showShop));
        panel.add(menuTile("hud_quests", "Travel Log", "Story, daily and minigame quests.", this::showQuests));
        panel.add(menuTile("hud_minigames", "Online I, Zombie", "Invite an online user or enter random matchmaking.", this::showMultiplayerLobby)).row();
        panel.add(menuTile("hud_minigames", "Leaderboard", "Live server rankings and My Point scores.", this::showLeaderboard));
        panel.add(menuTile("almanac", "Profile", "Account details, progress and security.", this::showProfile));
        panel.add(menuTile("settings", "Settings", "Difficulty, speed, grid and asset status.", this::showSettings)).row();
        int unread = user.getUnreadNews().size();
        panel.add(menuTile("hud_quests", unread == 0 ? "News" : "News (" + unread + ")",
            unread == 0 ? "Read messages from Crazy Dave and Penny." : unread + " unread message(s).", this::showNews)).colspan(3).row();

        Table actions = new Table();
        if (gameController.getBoard() != null) {
            actions.add(button("Resume level", "green", () -> {
                paused = false;
                showGame();
            })).width(210f).height(50f).padRight(12f);
        }
        actions.add(button("Logout", "brown", () -> {
            UserRepository.logout();
            gameController.setCurrentUser(null);
            showLogin();
        })).width(160f).height(50f);
        panel.add(actions).colspan(3).padTop(10f).height(58f).row();
        root.add(panel).grow().center().pad(18f);
    }

    protected void showProfile() {
        beginAuthenticated("Profile", true, this::showMain);
        User user = currentUser();
        Table info = new Table();
        info.defaults().pad(4).left();
        info.add(new Label("Username: " + user.getUsername(), skin)).row();
        info.add(new Label("Nickname: " + user.getNickname(), skin)).row();
        info.add(new Label("Email: " + user.getEmail(), skin)).row();
        info.add(new Label("Gender: " + user.getGender(), skin)).row();
        info.add(new Label("Games played: " + user.getGamesPlayed(), skin)).row();
        info.add(new Label("Levels completed: " + user.getLevelsCompleted(), skin)).row();
        info.add(new Label("My Point: " + (user.getNetworkBestScore() == null ? "-" : user.getNetworkBestScore()), skin)).row();
        info.add(new Label("Server: " + UserRepository.serverUrl(), skin)).row();

        Table edit = new Table();
        TextField username = field("new username");
        TextField nickname = field("new nickname");
        TextField email = field("new email");
        TextField oldPass = field("old password");
        TextField newPass = field("new password");
        oldPass.setPasswordMode(true); oldPass.setPasswordCharacter('*');
        newPass.setPasswordMode(true); newPass.setPasswordCharacter('*');
        addFormRow(edit, "Username", username);
        edit.add().row();
        edit.add(button("Change username", () -> {
            String value = username.getText().trim();
            if (!Validator.isValidUsername(value) || UserRepository.usernameExists(value)) {
                toast("Invalid or already-used username."); return;
            }
            if (UserRepository.renameUser(user, value)) { toast("Username changed."); showProfile(); }
            else toast(serverError("Username could not be changed."));
        })).colspan(2).width(170).row();
        addFormRow(edit, "Nickname", nickname);
        edit.add(button("Change nickname", () -> {
            if (!Validator.isValidNickname(nickname.getText())) { toast("Nickname must be 3-30 characters."); return; }
            user.setNickname(nickname.getText().trim()); UserRepository.updateUser(user);
            String problem = UserRepository.consumeLastError();
            if (problem != null) toast(problem); else { toast("Nickname changed."); showProfile(); }
        })).colspan(2).width(170).row();
        addFormRow(edit, "Email", email);
        edit.add(button("Change email", () -> {
            if (!Validator.isValidEmail(email.getText())) { toast("Invalid email."); return; }
            user.setEmail(email.getText().trim()); UserRepository.updateUser(user);
            String problem = UserRepository.consumeLastError();
            if (problem != null) toast(problem); else { toast("Email changed."); showProfile(); }
        })).colspan(2).width(170).row();
        addFormRow(edit, "Old password", oldPass);
        addFormRow(edit, "New password", newPass);
        edit.add(button("Change password", () -> {
            String err = Validator.isStrongPassword(newPass.getText());
            if (err != null) { toast(err); return; }
            if (UserRepository.changePassword(Validator.hashSha256(oldPass.getText()),
                Validator.hashSha256(newPass.getText()))) toast("Password changed.");
            else toast(serverError("Password could not be changed."));
        })).colspan(2).width(170).row();

        Table body = new Table();
        body.add(info).width(420).top().pad(20);
        body.add(edit).width(600).top().pad(20);
        root.add(framed(body)).grow().pad(18f);
    }

    protected void showNews() {
        User user = currentUser();
        List<String> unread = user.getUnreadNews();
        beginAuthenticated("News", true, this::showMain);
        Table list = new Table();
        list.defaults().growX().pad(6);
        List<String> all = user.getAllNews();
        if (all.isEmpty()) {
            list.add(wrapped("No news yet.")).width(800).row();
        } else {
            int index = 1;
            for (String item : all) {
                boolean isUnread = unread.contains(item);
                Label row = wrapped((isUnread ? "[NEW] " : "") + "#" + index++ + "  " + item);
                if (isUnread) row.setColor(1f, .82f, .25f, 1f);
                list.add(row).width(900).row();
            }
        }
        user.markAllNewsRead();
        UserRepository.updateUser(user);
        root.add(scroll(list)).grow().pad(18);
    }

    protected void showSettings() {
        beginAuthenticated("Settings", true, this::showMain);
        User user = currentUser();
        Table form = new Table();
        SelectBox<String> difficulty = new SelectBox<>(skin);
        difficulty.setItems("1", "2", "3", "4", "5");
        difficulty.setSelected(String.valueOf(user.getDifficulty()));
        difficulty.setMaxListCount(5);
        SelectBox<String> speed = new SelectBox<>(skin);
        speed.setItems("1", "2", "3");
        speed.setMaxListCount(3);
        speed.setSelected(String.valueOf(user.getGameSpeed()));
        CheckBox grid = new CheckBox(" Show red board grid", skin);
        grid.setChecked(user.isShowGrid());
        CheckBox debug = new CheckBox(" Debug mode (resource/sun/food cheats)", skin);
        debug.setChecked(user.isDebugMode());
        SelectBox<String> music = new SelectBox<>(skin);
        music.setItems("0%", "25%", "50%", "70%", "75%", "80%", "100%");
        music.setSelected(user.getMusicVolume() + "%");
        if (music.getSelectedIndex() < 0) music.setSelected("70%");
        SelectBox<String> effects = new SelectBox<>(skin);
        effects.setItems("0%", "25%", "50%", "70%", "75%", "80%", "100%");
        effects.setSelected(user.getSoundEffectsVolume() + "%");
        if (effects.getSelectedIndex() < 0) effects.setSelected("80%");
        CheckBox mute = new CheckBox(" Mute all audio", skin);
        mute.setChecked(user.isAudioMuted());
        addFormRow(form, "Difficulty", difficulty);
        addFormRow(form, "Game speed", speed);
        addFormRow(form, "Music volume", music);
        addFormRow(form, "Sound effects", effects);
        form.add(mute).colspan(2).left().padTop(10).row();
        form.add(grid).colspan(2).left().padTop(10).row();
        form.add(debug).colspan(2).left().padTop(10).row();
        form.add(wrapped("Game speed controls simulation ticks. Debug mode enables the on-screen resource buttons and F2-F5 cheats during play. F1 toggles debug mode at any time while logged in. See DEBUG_MODE_GUIDE.md.")).colspan(2).width(700).padTop(18).row();
        form.add(button("Save settings", () -> {
            user.setDifficulty(Integer.parseInt(difficulty.getSelected()));
            user.setGameSpeed(Integer.parseInt(speed.getSelected()));
            user.setShowGrid(grid.isChecked());
            user.setDebugMode(debug.isChecked());
            user.setMusicVolume(Integer.parseInt(music.getSelected().replace("%", "")));
            user.setSoundEffectsVolume(Integer.parseInt(effects.getSelected().replace("%", "")));
            user.setAudioMuted(mute.isChecked());
            audio.apply(user);
            UserRepository.updateUser(user);
            showSettings();
            toast("Settings saved.");
        })).colspan(2).width(170).padTop(16).row();
        form.add(button("PVZ asset status", this::showPvzAssetStatus)).colspan(2).width(190).padTop(10);
        root.add(framed(form)).width(900).center().padTop(20f);
    }

    protected void showPvzAssetStatus() {
        Dialog dialog = new Dialog("PvZ graphics assets", skin);
        Label details = wrapped(pvzAssets.diagnostics());
        details.setWrap(true);
        dialog.getContentTable().add(details).width(760).pad(18);
        dialog.button("Close");
        dialog.show(stage);
    }

    protected void showLeaderboard() {
        beginAuthenticated("Leaderboard", true, this::showMain);
        final String[] sort = {"progress"};
        final boolean[] descending = {true};
        Table controls = new Table();
        SelectBox<String> column = new SelectBox<>(skin);
        column.setItems("progress", "minigames", "daily", "quests", "score");
        SelectBox<String> order = new SelectBox<>(skin);
        order.setItems("descending", "ascending");
        controls.add(new Label("Sort by", skin)).padRight(8);
        controls.add(column).width(160).padRight(12);
        controls.add(order).width(160).padRight(12);
        Table list = new Table();
        Runnable rebuild = () -> buildLeaderboardRows(list, column.getSelected(), order.getSelected().equals("descending"));
        column.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { rebuild.run(); }});
        order.addListener(new ChangeListener() { public void changed(ChangeEvent e, Actor a) { rebuild.run(); }});
        root.add(controls).growX().row();
        root.add(scroll(list)).grow().pad(12);
        rebuild.run();
    }

    protected void buildLeaderboardRows(Table list, String sort, boolean descending) {
        list.clearChildren();
        List<LeaderboardEntry> users = new ArrayList<>(UserRepository.getLeaderboard());
        String loadError = UserRepository.consumeLastError();
        if (users.isEmpty() && loadError != null) {
            list.add(wrapped(loadError)).width(800).pad(20).row();
            return;
        }
        Comparator<LeaderboardEntry> comparator = switch (sort) {
            case "minigames" -> Comparator.comparingInt(LeaderboardEntry::minigames);
            case "daily" -> Comparator.comparingInt(LeaderboardEntry::dailyQuests);
            case "quests" -> Comparator.comparingInt(LeaderboardEntry::otherQuests);
            case "score" -> Comparator.comparingInt(u -> u.myPoint() == null ? -1 : u.myPoint());
            default -> Comparator.comparingInt(LeaderboardEntry::levelsCompleted);
        };
        if (descending) comparator = comparator.reversed();
        users.sort(comparator.thenComparing(LeaderboardEntry::username, String.CASE_INSENSITIVE_ORDER));
        String[] headers = {"User", "Last level", "Minigames", "Daily", "Other quests", "My Point"};
        for (String h : headers) list.add(new Label(h, skin, "subtitle")).width(150).pad(5);
        list.row();
        for (LeaderboardEntry user : users) {
            list.add(new Label(user.username(), skin));
            list.add(new Label(user.lastLevel(), skin));
            list.add(new Label(String.valueOf(user.minigames()), skin));
            list.add(new Label(String.valueOf(user.dailyQuests()), skin));
            list.add(new Label(String.valueOf(user.otherQuests()), skin));
            list.add(new Label(user.myPoint() == null ? "-" : String.valueOf(user.myPoint()), skin));
            list.row();
        }
    }

    protected String serverError(String fallback) {
        String message = UserRepository.consumeLastError();
        return message == null || message.isBlank() ? fallback : message;
    }

    protected String latestLevel(User user) {
        String result = "none";
        for (ChapterType chapter : CHAPTERS) {
            int level = user.getCompletedLevel(chapter);
            if (level > 0) result = human(chapter.name()) + " " + level;
        }
        return result;
    }

    
    
    


}
