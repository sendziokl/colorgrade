package dev.colorgrade;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColorGradeClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("colorgrade");

    private static KeyBinding openKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        ColorConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.colorgrade.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.colorgrade"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.colorgrade.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.colorgrade"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ColorGradeScreen(null));
                }
            }
            while (toggleKey.wasPressed()) {
                ColorConfig.INSTANCE.enabled = !ColorConfig.INSTANCE.enabled;
                ColorConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("ColorGrade: "
                            + (ColorConfig.INSTANCE.enabled ? "włączony" : "wyłączony")), true);
                }
            }
        });
    }
}
