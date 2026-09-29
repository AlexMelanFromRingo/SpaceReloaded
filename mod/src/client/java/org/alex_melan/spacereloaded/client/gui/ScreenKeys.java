package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/** Экраны мода закрываются и клавишей инвентаря (E), как ванильные окна; кроме ввода в текстовое поле. */
final class ScreenKeys {

    private ScreenKeys() {
    }

    static boolean closeOnInventoryKey(Screen screen, KeyEvent event) {
        if (!(screen.getFocused() instanceof EditBox) && Minecraft.getInstance().options.keyInventory.matches(event)) {
            screen.onClose();
            return true;
        }
        return false;
    }
}
