package com.returnbydeath;

import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class DeathEffectRenderer {

    private static boolean active = false;
    private static int ticksElapsed = 0;
    private static boolean soundPlayed = false;

    private static final int FADE_IN_TICKS = 6;  // 0.3 sec
    private static final int HOLD_TICKS = 40;     // 2 sec
    private static final int TOTAL_TICKS = FADE_IN_TICKS + HOLD_TICKS;

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

        HudLayerRegistrationCallback.EVENT.register(layeredDraw -> {
            layeredDraw.add(
                IdentifiedLayer.of(
                    ResourceLocation.fromNamespaceAndPath(ReturnByDeathMod.MOD_ID, "death_overlay"),
                    (guiGraphics, deltaTick) -> renderOverlay(guiGraphics)
                )
            );
        });
    }

    private static void renderOverlay(GuiGraphics guiGraphics) {
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
        int greyColor = (overlayAlpha << 24) | 0x808080;
        guiGraphics.fill(0, 0, screenW, screenH, greyColor);

        int vignetteAlpha = (int)(alpha * 130);
        int vignetteColor = (vignetteAlpha << 24) | 0x000000;
        int vigSize = (int)(Math.min(screenW, screenH) * 0.15f);
        guiGraphics.fill(0, 0, screenW, vigSize, vignetteColor);
        guiGraphics.fill(0, screenH - vigSize, screenW, screenH, vignetteColor);
        guiGraphics.fill(0, 0, vigSize, screenH, vignetteColor);
        guiGraphics.fill(screenW - vigSize, 0, screenW, screenH, vignetteColor);
    }

    private static void playReturnSound(Minecraft client) {
        if (client.player == null) return;
        ResourceLocation soundId = ResourceLocation.fromNamespaceAndPath(
            ReturnByDeathMod.MOD_ID, "return_by_death"
        );
        client.player.playSound(SoundEvent.createVariableRangeEvent(soundId), 1.0f, 1.0f);
    }
}
