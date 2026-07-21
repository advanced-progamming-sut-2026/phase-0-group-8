package ir.hamgit.ahh.PvZ;

import ir.hamgit.ahh.PvZ.controller.MenuController;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        UserRepository.loadAll();
        new MenuController().run();
    }
}
