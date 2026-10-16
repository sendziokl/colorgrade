package dev.colorgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ColorConfig {
    public boolean enabled = true;
    public float brightness = 0f;   // -0.5 .. 0.5
    public float contrast = 1f;     // 0.2 .. 2.0
    public float gamma = 1f;        // 0.3 .. 2.5
    public float saturation = 1f;   // 0 .. 2.5
    public float hue = 0f;          // -180 .. 180 (stopnie)
    public float red = 1f;          // 0 .. 2
    public float green = 1f;
    public float blue = 1f;

    public static ColorConfig INSTANCE = new ColorConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("colorgrade.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                ColorConfig c = GSON.fromJson(Files.readString(p), ColorConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            ColorGradeClient.LOGGER.warn("Nie udało się wczytać configu", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(INSTANCE));
        } catch (IOException e) {
            ColorGradeClient.LOGGER.warn("Nie udało się zapisać configu", e);
        }
    }

    public void reset() {
        brightness = 0f; contrast = 1f; gamma = 1f; saturation = 1f; hue = 0f;
        red = 1f; green = 1f; blue = 1f;
    }

    public boolean isNeutral() {
        return brightness == 0f && contrast == 1f && gamma == 1f && saturation == 1f
                && hue == 0f && red == 1f && green == 1f && blue == 1f;
    }
}
