package ir.hamgit.ahh.PvZ;

import ir.hamgit.ahh.PvZ.controller.MenuController;
import ir.hamgit.ahh.PvZ.model.repository.UserRepository;

public class Demo {
    public static void main(String[] args) {
        UserRepository.loadAll();
        MenuController menuController = new MenuController();
        menuController.run();
    }
}
