package ir.hamgit.ahh.PvZ.gui;

import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

 



public final class PhaseTwoScreen extends PhaseThreeMultiplayerScreens {
    public PhaseTwoScreen() {
        super();
        if (UserRepository.getCurrentUser() != null) {
            showMain();
        } else {
            showLogin();
        }
    }
}
