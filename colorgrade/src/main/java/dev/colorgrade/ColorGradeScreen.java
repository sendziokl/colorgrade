package dev.colorgrade;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Consumer;

public class ColorGradeScreen extends Screen {
    private static final int W = 180, H = 18, GAP = 2, X = 10;
    private final Screen parent;

    public ColorGradeScreen(Screen parent) {
        super(Text.literal("ColorGrade"));
        this.parent = parent;
    }

    private static class ParamSlider extends SliderWidget {
        private final String label;
        private final float min, max;
        private final Consumer<Float> setter;

        ParamSlider(int x, int y, String label, float min, float max, float cur, Consumer<Float> setter) {
            super(x, y, W, H, Text.empty(), (cur - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float v = min + (max - min) * (float) this.value;
            setMessage(Text.literal(label + ": " + String.format(Locale.ROOT, "%.2f", v)));
        }

        @Override
        protected void applyValue() {
            setter.accept(min + (max - min) * (float) this.value);
        }
    }

    @Override
    protected void init() {
        ColorConfig c = ColorConfig.INSTANCE;
        int y = 8;

        addDrawableChild(new ParamSlider(X, y, "Jasność", -0.5f, 0.5f, c.brightness, v -> c.brightness = v));
        y += H + GAP;
        addDrawableChild(new ParamSlider(X, y, "Kontrast", 0.2f, 2f, c.contrast, v -> c.contrast = v));
        y += H + GAP;
        addDrawableChild(new ParamSlider(X, y, "Gamma", 0.3f, 2.5f, c.gamma, v -> c.gamma = v));
        y += H + GAP;
        addDrawableChild(new ParamSlider(X, y, "Nasycenie", 0f, 2.5f, c.saturation, v -> c.saturation = v));
        y += H + GAP;
        addDrawableChild(new ParamSlider(X, y, "Odcień (hue)", -180f, 180f, c.hue, v -> c.hue = v));
        y += H + GAP;
        addDrawableChild(new ParamSlider(X, y, "Czerwony", 0f, 2f, c.red, v -> c.red = v));
        y += H + GAP;
        addDrawableChild(new ParamSlider(X, y, "Zielony", 0f, 2f, c.green, v -> c.green = v));
        y += H + GAP;
        addDrawableChild(new ParamSlider(X, y, "Niebieski", 0f, 2f, c.blue, v -> c.blue = v));
        y += H + GAP + 6;

        addDrawableChild(ButtonWidget.builder(enabledText(), btn -> {
            c.enabled = !c.enabled;
            btn.setMessage(enabledText());
        }).dimensions(X, y, W, H).build());
        y += H + GAP;

        addDrawableChild(ButtonWidget.builder(Text.literal("Resetuj"), btn -> {
            c.reset();
            client.setScreen(new ColorGradeScreen(parent));
        }).dimensions(X, y, W / 2 - 1, H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), btn -> close())
                .dimensions(X + W / 2 + 1, y, W / 2 - 1, H).build());
    }

    private static Text enabledText() {
        return Text.literal("Efekt: " + (ColorConfig.INSTANCE.enabled ? "WŁĄCZONY" : "wyłączony"));
    }

    // Bez rozmycia/przyciemnienia tła, żeby widzieć efekt na żywo.
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, X + W + 10, this.height, 0xA0000000);
    }

    @Override
    public void removed() {
        ColorConfig.save();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
