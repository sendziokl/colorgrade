package dev.colorgrade.mixin;

import dev.colorgrade.ColorPostProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // Po wyrenderowaniu świata (z ręką), ale PRZED GUI/HUD -> interfejs zostaje bez zmian.
    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void colorgrade$apply(RenderTickCounter tickCounter, CallbackInfo ci) {
        ColorPostProcessor.apply(MinecraftClient.getInstance());
    }
}
