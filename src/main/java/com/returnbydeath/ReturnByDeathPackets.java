package com.returnbydeath;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ReturnByDeathPackets {

    public static final ResourceLocation DEATH_EFFECT_ID =
        ResourceLocation.fromNamespaceAndPath(ReturnByDeathMod.MOD_ID, "death_effect");

    public record DeathEffectPayload() implements CustomPacketPayload {
        public static final Type<DeathEffectPayload> TYPE = new Type<>(DEATH_EFFECT_ID);
        public static final StreamCodec<FriendlyByteBuf, DeathEffectPayload> CODEC =
            StreamCodec.unit(new DeathEffectPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerServer() {
        PayloadTypeRegistry.clientboundPlay().register(DeathEffectPayload.TYPE, DeathEffectPayload.CODEC);
    }

    public static void sendDeathEffect(ServerPlayer player) {
        ServerPlayNetworking.send(player, new DeathEffectPayload());
    }
}
