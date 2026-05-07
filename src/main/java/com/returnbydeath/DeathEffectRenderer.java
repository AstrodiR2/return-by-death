package com.returnbydeath;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.GameRenderer;

public class DeathEffectRenderer {

    // States
    private static boolean active = false;
    private static int ticksElapsed = 0;

    // Timings in ticks (20 ticks = 1 second)
    private static final int FADE_IN_TICKS = 6;   // 0.3 sec grey fade in
    private static final int HOLD_TICKS = 40;       // 2 sec hold
    private static final int TOTAL_TICKS = FADE_IN_TICKS + HOLD_TICKS;

    private static boolean soundPlayed = false;

    public static void triggerDeathEffect(Minecraft client) {
        active = true;
        ticksElapsed = 0;
        soundPlayed = false;
    }

    public static void registerEvents() {
        // Tick counter
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active) return;
            ticksElapsed++;

            // Play sound at start of hold phase
            if (ticksElapsed == FADE_IN_TICKS && !soundPlayed) {
                soundPlayed = true;
                playReturnSound(client);
            }

            // End effect after total duration
            if (ticksElapsed >= TOTAL_TICKS) {
                active = false;
                ticksElapsed = 0;
            }
        });

        // Render greyscale overlay
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            if (!active) return;
            Minecraft client = Minecraft.getInstance();
            int screenW = client.getWindow().getGuiScaledWidth();
            int screenH = client.getWindow().getGuiScaledHeight();

            float alpha;
            if (ticksElapsed <= FADE_IN_TICKS) {
                // Fade in: 0 → 1
                alpha = (float) ticksElapsed / FADE_IN_TICKS;
            } else {
                // Hold at full grey
                alpha = 1.0f;
            }

            // Draw desaturation overlay (dark grey semi-transparent)
            // We use multiple layers to simulate greyscale
            int overlayAlpha = (int)(alpha * 180); // 0-180 out of 255
            int color = (overlayAlpha << 24) | 0x808080; // grey

            guiGraphics.fill(0, 0, screenW, screenH, color);

            // Additional dark vignette at edges
            int vignetteAlpha = (int)(alpha * 120);
            int vignetteColor = (vignetteAlpha << 24) | 0x000000;
            int vigSize = (int)(Math.min(screenW, screenH) * 0.15f);
            // Top
            guiGraphics.fill(0, 0, screenW, vigSize, vignetteColor);
            // Bottom
            guiGraphics.fill(0, screenH - vigSize, screenW, screenH, vignetteColor);
            // Left
            guiGraphics.fill(0, 0, vigSize, screenH, vignetteColor);
            // Right
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
