package com.returnbydeath;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ReturnByDeathClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(
            ReturnByDeathPackets.DeathEffectPayload.TYPE,
            ReturnByDeathPackets.DeathEffectPayload.CODEC
        );

        DeathEffectRenderer.registerEvents();

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
