package com.returnbydeath;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ReturnByDeathClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register packet type on client side too
        PayloadTypeRegistry.playS2C().register(
            ReturnByDeathPackets.DeathEffectPayload.TYPE,
            ReturnByDeathPackets.DeathEffectPayload.CODEC
        );

        // Register HUD renderer and tick events
        DeathEffectRenderer.registerEvents();

        // Handle death effect packet
        ClientPlayNetworking.registerGlobalReceiver(
            ReturnByDeathPackets.DeathEffectPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    DeathEffectRenderer.triggerDeathEffect(context.client());
                });
            }
        );
    }
}
