package com.returnbydeath;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class DeathEffectRenderer {

    private static boolean active = false;
    private static int ticksElapsed = 0;

    private static final int FADE_IN_TICKS = 6;
    private static final int HOLD_TICKS = 40;
    private static final int TOTAL_TICKS = FADE_IN_TICKS + HOLD_TICKS;

    private static boolean soundPlayed = false;

    public static void triggerDeathEffect(Minecraft client) {
        active = true;
        ticksElapsed = 0;
        soundPlayed = false;
    }

    public static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active) return;
            ticksElapsed++;

            if (ticksElapsed == FADE_IN_TICKS && !soundPlayed) {
                soundPlayed = true;
                playReturnSound(client);
            }

            if (ticksElapsed >= TOTAL_TICKS) {
                active = false;
                ticksElapsed = 0;
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            if (!active) return;
            Minecraft client = Minecraft.getInstance();
            int screenW = client.getWindow().getGuiScaledWidth();
            int screenH = client.getWindow().getGuiScaledHeight();

            float alpha;
            if (ticksElapsed <= FADE_IN_TICKS) {
                alpha = (float) ticksElapsed / FADE_IN_TICKS;
            } else {
                alpha = 1.0f;
            }

            int overlayAlpha = (int)(alpha * 180);
            int color = (overlayAlpha << 24) | 0x808080;
            guiGraphics.fill(0, 0, screenW, screenH, color);

            int vignetteAlpha = (int)(alpha * 120);
            int vignetteColor = (vignetteAlpha << 24) | 0x000000;
            int vigSize = (int)(Math.min(screenW, screenH) * 0.15f);
            guiGraphics.fill(0, 0, screenW, vigSize, vignetteColor);
            guiGraphics.fill(0, screenH - vigSize, screenW, screenH, vignetteColor);
            guiGraphics.fill(0, 0, vigSize, screenH, vignetteColor);
            guiGraphics.fill(screenW - vigSize, 0, screenW, screenH, vignetteColor);
        });
    }

    private static void playReturnSound(Minecraft client) {
        if (client.player == null) return;
        ResourceLocation soundId = ResourceLocation.fromNamespaceAndPath(
            ReturnByDeathMod.MOD_ID, "return_by_death"
        );
        SoundEvent sound = SoundEvent.createVariableRangeEvent(soundId);
        client.player.playSound(sound, 1.0f, 1.0f);
    }
}
