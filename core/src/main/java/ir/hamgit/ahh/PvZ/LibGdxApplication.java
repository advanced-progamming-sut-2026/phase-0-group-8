package ir.hamgit.ahh.PvZ;

import com.badlogic.gdx.Game;
import ir.hamgit.ahh.PvZ.gui.PhaseThreeScreen;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

 
public final class LibGdxApplication extends Game {
    @Override
    public void create() {
        UserRepository.loadAll();
        setScreen(new PhaseThreeScreen());
    }

    @Override
    public void dispose() {
        UserRepository.saveAll();
        super.dispose();
    }
}
