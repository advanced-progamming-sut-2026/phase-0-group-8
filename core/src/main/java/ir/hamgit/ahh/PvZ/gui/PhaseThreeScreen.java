package ir.hamgit.ahh.PvZ.gui;

import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

 
public final class PhaseThreeScreen extends PhaseThreeMultiplayerScreens {
    public PhaseThreeScreen() {
        super();
        if (UserRepository.getCurrentUser() != null) showMain();
        else showLogin();
    }
}
