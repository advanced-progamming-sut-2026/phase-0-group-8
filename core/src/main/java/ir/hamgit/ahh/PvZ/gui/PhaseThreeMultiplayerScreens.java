package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.ZombieType;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;
import ir.hamgit.ahh.PvZ.network.NetworkException;
import ir.hamgit.ahh.PvZ.network.PhaseThreeApiClient;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.InvitationView;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.LobbyState;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchAssignment;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchCommand;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.MatchSnapshot;
import ir.hamgit.ahh.PvZ.network.PhaseThreeDtos.ReactionView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

 
abstract class PhaseThreeMultiplayerScreens extends PhaseTwoMinigameScreens {
    private final PhaseThreeApiClient api = UserRepository.network();
    private final EmojiArt emojiArt = new EmojiArt();
    private final ExecutorService networkWorker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "pvz-phase3-client-network");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean lobbyPollBusy = new AtomicBoolean();
    private final AtomicBoolean matchPollBusy = new AtomicBoolean();
    private final AtomicBoolean presencePollBusy = new AtomicBoolean();
    private final Set<String> promptedInvitations = new HashSet<>();
    private boolean multiplayerLobbyActive;
    private boolean multiplayerMatchActive;
    private boolean finishDialogShown;
    private float multiplayerPollTimer;
    private LobbyState lobbyState;
    private MatchAssignment matchAssignment;
    private MatchSnapshot matchSnapshot;
    private MultiplayerBoardActor multiplayerBoard;
    private Label lobbyStatus;
    private Label onlineUsers;
    private Table invitationRows;
    private TextButton randomButton;
    private Label matchHud;
    private String selectedOnlineKind;
    private long lastReactionId;

    @Override protected void showMultiplayerLobby() {
        multiplayerLobbyActive = true;
        multiplayerMatchActive = false;
        matchAssignment = null;
        matchSnapshot = null;
        multiplayerPollTimer = 0f;
        beginAuthenticated("Online I, Zombie", true, this::leaveLobbyToMain);

        Table invite = new Table();
        TextField target = field("online username");
        invite.add(new Label("Invite a player", skin, "subtitle")).colspan(2).left().row();
        invite.add(target).width(310).height(42).padTop(8);
        invite.add(button("Send invitation", () -> sendInvitation(target.getText().trim())))
            .width(170).height(42).padLeft(8).padTop(8).row();
        invite.add(wrapped("The server reports invalid usernames and offline users separately."))
            .colspan(2).width(500).padTop(8).row();

        Table matchmaking = new Table();
        matchmaking.add(new Label("Random matchmaking", skin, "subtitle")).row();
        randomButton = button("Enter waiting queue", this::toggleRandomQueue);
        matchmaking.add(randomButton).width(240).height(48).padTop(10).row();
        lobbyStatus = wrapped("Connecting to " + api.getBaseUrl() + " ...");
        lobbyStatus.setAlignment(Align.center);
        matchmaking.add(lobbyStatus).width(420).padTop(10).row();

        Table top = new Table();
        top.add(framed(invite)).width(565).growY().pad(8);
        top.add(framed(matchmaking)).width(500).growY().pad(8);

        invitationRows = new Table();
        invitationRows.defaults().growX().pad(4);
        onlineUsers = wrapped("Online users: loading...");
        Table activity = new Table();
        activity.add(new Label("Incoming invitations", skin, "subtitle")).left().row();
        activity.add(invitationRows).growX().left().padTop(5).row();
        activity.add(onlineUsers).width(980).left().padTop(12).row();

        Table body = new Table();
        body.add(top).growX().row();
        body.add(framed(activity)).grow().pad(8).row();
        root.add(body).grow().pad(10);
        pollLobby();
    }

    private void leaveLobbyToMain() {
        multiplayerLobbyActive = false;
        if (lobbyState != null && lobbyState.randomQueued()) {
            request(api::leaveRandom, ignored -> showMain(), error -> { toast(error); showMain(); });
        } else showMain();
    }

    private void sendInvitation(String username) {
        if (username.isBlank()) { toast("Enter a username."); return; }
        request(() -> api.invite(username), result -> toast(result.message()), this::toast);
    }

    private void toggleRandomQueue() {
        boolean queued = lobbyState != null && lobbyState.randomQueued();
        request(() -> queued ? api.leaveRandom() : api.joinRandom(), state -> {
            updateLobby(state);
            if (state.match() != null) startOnlineMatch(state.match());
        }, this::toast);
    }

    private void pollLobby() {
        if (!multiplayerLobbyActive || !lobbyPollBusy.compareAndSet(false, true)) return;
        request(api::lobby, state -> {
            lobbyPollBusy.set(false);
            if (!multiplayerLobbyActive) return;
            updateLobby(state);
            if (state.match() != null) startOnlineMatch(state.match());
        }, error -> {
            lobbyPollBusy.set(false);
            if (multiplayerLobbyActive && lobbyStatus != null) lobbyStatus.setText(error);
        });
    }

     
    private void pollPresence() {
        if (UserRepository.getCurrentUser() == null || multiplayerLobbyActive || multiplayerMatchActive
            || !presencePollBusy.compareAndSet(false, true)) return;
        request(api::lobby, state -> {
            presencePollBusy.set(false);
            
            
            
            if (UserRepository.getCurrentUser() == null || multiplayerLobbyActive || multiplayerMatchActive
                || adventureScreenActive) return;
            if (state.match() != null) { startOnlineMatch(state.match()); return; }
            for (InvitationView invitation : state.invitations()) {
                if (promptedInvitations.add(invitation.id())) showInvitationDialog(invitation);
            }
        }, error -> presencePollBusy.set(false));
    }

    private void updateLobby(LobbyState state) {
        lobbyState = state;
        if (randomButton != null) randomButton.setText(state.randomQueued() ? "Leave waiting queue" : "Enter waiting queue");
        if (lobbyStatus != null) lobbyStatus.setText(state.match() != null ? "Match found!"
            : state.randomQueued() ? "Waiting for a random opponent..." : "Ready. Invite a user or join the queue.");
        if (onlineUsers != null) onlineUsers.setText("Online users: "
            + (state.onlineUsers().isEmpty() ? "none" : String.join(", ", state.onlineUsers())));
        if (invitationRows != null) {
            invitationRows.clearChildren();
            if (state.invitations().isEmpty()) invitationRows.add(wrapped("No pending invitations.")).width(700).row();
            for (InvitationView invitation : state.invitations()) {
                invitationRows.add(new Label(invitation.fromUsername() + " invited you.", skin)).width(500).left();
                invitationRows.add(button("Accept", "green", () -> respondToInvitation(invitation, true))).width(110);
                invitationRows.add(button("Reject", "brown", () -> respondToInvitation(invitation, false))).width(110).row();
                if (promptedInvitations.add(invitation.id())) showInvitationDialog(invitation);
            }
        }
    }

    private void showInvitationDialog(InvitationView invitation) {
        Dialog dialog = new Dialog("Online match invitation", skin) {
            @Override protected void result(Object object) {
                if (object instanceof Boolean accepted) respondToInvitation(invitation, accepted);
            }
        };
        dialog.text(invitation.fromUsername() + " wants to play online I, Zombie.");
        dialog.button("Reject", false);
        dialog.button("Accept", true);
        dialog.show(stage);
    }

    private void respondToInvitation(InvitationView invitation, boolean accepted) {
        request(() -> api.decideInvite(invitation.id(), accepted), assignment -> {
            if (assignment.matchId() != null) startOnlineMatch(assignment);
            else { toast("Invitation rejected."); pollLobby(); }
        }, this::toast);
    }

    private void startOnlineMatch(MatchAssignment assignment) {
        if (assignment == null || assignment.matchId() == null) return;
        matchAssignment = assignment;
        multiplayerLobbyActive = false;
        multiplayerMatchActive = true;
        finishDialogShown = false;
        selectedOnlineKind = null;
        lastReactionId = 0;
        multiplayerPollTimer = 0f;
        clearScreen();
        backdrop.setMood(PvzBackdropActor.Mood.HIDDEN);
        audio.playGameMusic(ir.hamgit.ahh.PvZ.model.enums.ChapterType.ANCIENT_EGYPT);

        multiplayerBoard = new MultiplayerBoardActor(skin.getFont("FBUSV8C6EI_3"),
            () -> matchSnapshot, pvzAssets);
        
        
        
        multiplayerBoard.setGameplayInsets(58f, 160f);
        multiplayerBoard.setShowGridSupplier(() -> {
            User user = UserRepository.getCurrentUser();
            return user != null && user.isShowGrid();
        });
        multiplayerBoard.setCancelAction(this::clearOnlineSelection);
        multiplayerBoard.setCellClickListener(this::onlineCellClicked);
        Stack battlefield = new Stack();
        battlefield.add(multiplayerBoard);
        Table overlay = new Table();
        overlay.top();
        Table header = new Table();
        header.setBackground(tinted("image_ui_quests_panel_edge_to_edge_ten", new Color(.08f, .14f, .07f, .96f)));
        header.pad(5, 10, 5, 10);
        header.add(new Label("ONLINE I, ZOMBIE", skin, "window")).left().padRight(14);
        matchHud = new Label("Synchronizing authoritative board...", skin);
        header.add(matchHud).expandX().left();
        header.add(button("Leave", "brown", this::confirmLeaveOnlineMatch)).width(95).height(40);
        overlay.add(header).growX().height(52).row();
        overlay.add().grow().row();
        overlay.add(buildOnlineControls()).growX().height(152).row();
        battlefield.add(overlay);
        root.add(battlefield).grow();
        pollMatch();
        toast("You control " + assignment.role().toLowerCase() + ". Opponent: " + assignment.opponentUsername());
    }

    private Actor buildOnlineControls() {
        Table frame = new Table();
        frame.setBackground(tinted("image_ui_mainmenu_mm_settings_tab_10", new Color(.12f, .22f, .10f, .95f)));
        Table cards = new Table();
        cards.defaults().height(72).pad(3);
        if ("PLANTS".equals(matchAssignment.role())) {
            addPlantCard(cards, PlantType.PEASHOOTER, "Peashooter\n100 sun");
            addPlantCard(cards, PlantType.SUNFLOWER, "Sunflower\n50 sun");
            addPlantCard(cards, PlantType.WALL_NUT, "Wall-nut\n50 sun");
            addPlantCard(cards, PlantType.SNOW_PEA, "Snow Pea\n175 sun");
        } else {
            addZombieCard(cards, ZombieType.NORMAL, "Normal\n75 sun");
            addZombieCard(cards, ZombieType.CONEHEAD, "Conehead\n125 sun");
            addZombieCard(cards, ZombieType.BUCKETHEAD, "Buckethead\n175 sun");
            addZombieCard(cards, ZombieType.GARGANTUAR, "Gargantuar\n300 sun");
        }
        ScrollPane cardScroll = new ScrollPane(cards, skin);
        cardScroll.setScrollingDisabled(false, true);
        cardScroll.setFadeScrollBars(false);
        frame.add(cardScroll).growX().height(82).row();
        frame.add(buildReactionBar()).growX().height(62).row();
        return frame;
    }

    private void addPlantCard(Table cards, PlantType type, String text) {
        cards.add(plantActionCard(type, text, () -> selectOnline(type.name()), null, 40)).width(180);
    }
    private void addZombieCard(Table cards, ZombieType type, String text) {
        cards.add(zombieActionCard(type, text, () -> selectOnline(type.name()), null, 40)).width(190);
    }
    private void selectOnline(String kind) {
        selectedOnlineKind = kind;
        if (multiplayerBoard != null) {
            if ("PLANTS".equals(matchAssignment.role())) {
                try {
                    multiplayerBoard.setCursorPlant(PlantType.valueOf(kind));
                } catch (IllegalArgumentException ignored) {
                    multiplayerBoard.setCursorText(human(kind));
                }
            } else {
                multiplayerBoard.setCursorText(human(kind));
            }
        }
        toast("Selected " + human(kind) + ".");
    }

    private void clearOnlineSelection() {
        selectedOnlineKind = null;
        if (multiplayerBoard != null) multiplayerBoard.setCursorText(null);
    }

    private Table buildReactionBar() {
        Table reactions = new Table();
        reactions.add(new Label("Reactions", skin, "subtitle")).padRight(8);
        for (String text : List.of("Good luck!", "Well played!", "Brains are mine!"))
            reactions.add(button(text, () -> sendReaction("TEXT", text))).width(128).pad(2);
        for (String emoji : List.of("😀", "😱", "🧟")) {
            reactions.add(emojiButton(emoji)).size(58).pad(2);
        }
        return reactions;
    }

     
    private Actor emojiButton(String value) {
        Table tile = new Table();
        tile.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        tile.setBackground(tinted("image_ui_dialog_asset_inner_bkgd_10",
            new Color(.28f, .44f, .20f, .98f)));
        tile.add(emojiArt.image(value)).size(48);
        addImmediateCardAction(tile, () -> sendReaction("EMOJI", value));
        return tile;
    }

    private void onlineCellClicked(int column, int lane) {
        if (selectedOnlineKind == null || matchSnapshot == null) { toast("Select a unit first."); return; }
        String action = "PLANTS".equals(matchAssignment.role()) ? "PLANT" : "ZOMBIE";
        MatchCommand command = new MatchCommand(matchSnapshot.revision(), action, selectedOnlineKind, column, lane);
        request(() -> api.command(matchAssignment.matchId(), command), this::applyMatchSnapshot, this::toast);
    }

    private void sendReaction(String kind, String value) {
        if (matchAssignment == null) return;
        request(() -> api.react(matchAssignment.matchId(), kind, value), this::applyMatchSnapshot, this::toast);
    }

    private void pollMatch() {
        if (!multiplayerMatchActive || matchAssignment == null || !matchPollBusy.compareAndSet(false, true)) return;
        String id = matchAssignment.matchId();
        request(() -> api.match(id), snapshot -> {
            matchPollBusy.set(false);
            if (multiplayerMatchActive && matchAssignment != null && id.equals(matchAssignment.matchId())) {
                applyMatchSnapshot(snapshot);
            }
        }, error -> {
            matchPollBusy.set(false);
            if (multiplayerMatchActive) toast(error);
        });
    }

    private void applyMatchSnapshot(MatchSnapshot snapshot) {
        matchSnapshot = snapshot;
        int brains = 0; for (boolean available : snapshot.brains()) if (available) brains++;
        int sun = "PLANTS".equals(matchAssignment.role()) ? snapshot.plantSun() : snapshot.zombieSun();
        if (matchHud != null) matchHud.setText("Role: " + matchAssignment.role() + "  |  Opponent: "
            + matchAssignment.opponentUsername() + "  |  " + snapshot.remainingSeconds() + "s  |  Sun: " + sun
            + "  |  Brains: " + brains + "/5  |  Rev " + snapshot.revision());
        for (ReactionView reaction : snapshot.reactions()) {
            if (reaction.id() > lastReactionId) {
                lastReactionId = reaction.id();
                if (!reaction.fromUsername().equalsIgnoreCase(currentUser().getUsername())) showReaction(reaction);
            }
        }
        if ("FINISHED".equals(snapshot.status())) showFinishedMatch(snapshot);
    }

    private void showReaction(ReactionView reaction) {
        Table bubble = new Table();
        bubble.setTransform(true);
        bubble.setBackground(tinted("image_ui_dialog_asset_inner_bkgd_10", new Color(.10f, .18f, .08f, .96f)));
        if ("EMOJI".equals(reaction.kind())) {
            Label sender = new Label(reaction.fromUsername() + ":", skin, "subtitle");
            sender.setAlignment(Align.right);
            bubble.add(sender).pad(10).right();
            bubble.add(emojiArt.image(reaction.value())).size(58).pad(6);
        } else {
            Label label = new Label(reaction.fromUsername() + ":  " + reaction.value(), skin, "subtitle");
            label.setAlignment(Align.center);
            bubble.add(label).grow().pad(12);
        }
        bubble.setSize(440, 70);
        bubble.setOrigin(220, 35);
        bubble.setPosition((stage.getWidth() - 440) / 2f, stage.getHeight() - 145);
        stage.addActor(bubble);
        bubble.addAction(Actions.sequence(Actions.scaleTo(1.08f, 1.08f, .15f), Actions.delay(1.5f),
            Actions.fadeOut(.35f), Actions.removeActor()));
    }

    private void showFinishedMatch(MatchSnapshot snapshot) {
        if (finishDialogShown) return;
        finishDialogShown = true;
        multiplayerMatchActive = false;
        boolean won = matchAssignment.role().equals(snapshot.winner());
        Dialog dialog = new Dialog(won ? "Online victory" : "Online defeat", skin) {
            @Override protected void result(Object object) {
                
                
                
                remove();
                leaveOnlineMatch();
            }
        };
        dialog.text((snapshot.reason() == null ? "Match complete." : snapshot.reason())
            + "\nWinner: " + snapshot.winner() + "\nThe server recorded the result.");
        dialog.button("Return to lobby");
        dialog.show(stage);
    }

    private void confirmLeaveOnlineMatch() {
        confirm("Leave online match", "Leaving awards the match to your opponent.", this::leaveOnlineMatch);
    }

    private void leaveOnlineMatch() {
        String id = matchAssignment == null ? null : matchAssignment.matchId();
        multiplayerMatchActive = false;
        if (id == null) { showMultiplayerLobby(); return; }
        request(() -> { api.leaveMatch(id); return Boolean.TRUE; }, ignored -> showMultiplayerLobby(),
            error -> { toast(error); showMultiplayerLobby(); });
    }

    @Override protected void updateLiveState(float delta) {
        super.updateLiveState(delta);
        multiplayerPollTimer += delta;
        if (multiplayerLobbyActive && multiplayerPollTimer >= 1f) {
            multiplayerPollTimer = 0f;
            pollLobby();
        } else if (multiplayerMatchActive && multiplayerPollTimer >= .25f) {
            multiplayerPollTimer = 0f;
            pollMatch();
        } else if (!multiplayerLobbyActive && !multiplayerMatchActive && multiplayerPollTimer >= 4f) {
            multiplayerPollTimer = 0f;
            pollPresence();
        }
    }

    @Override protected void disposeDynamicActors() {
        super.disposeDynamicActors();
        if (multiplayerBoard != null) { multiplayerBoard.dispose(); multiplayerBoard = null; }
    }

    @Override public void dispose() {
        multiplayerLobbyActive = multiplayerMatchActive = false;
        networkWorker.shutdownNow();
        emojiArt.dispose();
        super.dispose();
    }

    private <T> void request(Callable<T> operation, Consumer<T> success, Consumer<String> failure) {
        networkWorker.submit(() -> {
            try {
                T result = operation.call();
                Gdx.app.postRunnable(() -> success.accept(result));
            } catch (NetworkException e) {
                Gdx.app.postRunnable(() -> failure.accept(e.getMessage()));
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Network operation failed." : e.getMessage();
                Gdx.app.postRunnable(() -> failure.accept(message));
            }
        });
    }
}
