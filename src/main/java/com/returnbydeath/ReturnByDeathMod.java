package com.returnbydeath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReturnByDeathMod implements ModInitializer {

    public static final String MOD_ID = "returnbydeath";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final int SNAPSHOT_INTERVAL = 400; // 20 sec

    private static int tickCounter = 0;
    public static WorldSnapshot currentSnapshot = null;
    public static final Set<UUID> rollingBack = new HashSet<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[ReturnByDeath] Mod loaded.");

        ReturnByDeathPackets.registerServer();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= SNAPSHOT_INTERVAL) {
                tickCounter = 0;
                saveSnapshot(server);
            }
        });

        ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
            UUID uuid = player.getUUID();
            if (rollingBack.contains(uuid)) return true;

            if (currentSnapshot != null) {
                LOGGER.info("[ReturnByDeath] {} died — rolling back!", player.getName().getString());
                ReturnByDeathPackets.sendDeathEffect(player);

                MinecraftServer server = player.server;
                server.execute(() -> rollbackWorld(server));
                return false;
            }
            return true;
        });
    }

    private void saveSnapshot(MinecraftServer server) {
        WorldSnapshot snapshot = new WorldSnapshot();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            snapshot.savePlayer(player);
        }
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) snapshot.saveChunks(overworld);
        currentSnapshot = snapshot;
    }

    public static void rollbackWorld(MinecraftServer server) {
        if (currentSnapshot == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) currentSnapshot.restoreChunks(overworld);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            rollingBack.add(uuid);
            currentSnapshot.restorePlayer(player);
            rollingBack.remove(uuid);
        }
        LOGGER.info("[ReturnByDeath] Rollback complete.");
    }
}
