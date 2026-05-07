package com.returnbydeath;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReturnByDeathMod implements ModInitializer {

    public static final String MOD_ID = "returnbydeath";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // How often to save snapshot (20 seconds * 20 ticks = 400 ticks)
    public static final int SNAPSHOT_INTERVAL = 400;

    private static int tickCounter = 0;

    // The one snapshot — always overwritten
    public static WorldSnapshot currentSnapshot = null;

    // Players currently being rolled back (to avoid death loop)
    public static final Set<UUID> rollingBack = new HashSet<>();

    @Override
    public void onInitialize() {
        LOGGER.info("[ReturnByDeath] Mod loaded.");

        // Save snapshot every 20 seconds
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= SNAPSHOT_INTERVAL) {
                tickCounter = 0;
                saveSnapshot(server);
            }
        });

        // On player death — trigger rollback
        ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
            MinecraftServer server = player.getServer();
            if (server == null) return true;

            UUID uuid = player.getUUID();
            if (rollingBack.contains(uuid)) return true; // allow death during rollback

            if (currentSnapshot != null) {
                LOGGER.info("[ReturnByDeath] Player {} died — rolling back world!", player.getName().getString());

                // Tell client to play death effect
                ReturnByDeathPackets.sendDeathEffect(player);

                // Schedule rollback on next tick (can't modify world mid-death)
                server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 1, () -> {
                    rollbackWorld(server);
                }));

                // Cancel death
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

        // Save all loaded chunks in overworld
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            snapshot.saveChunks(overworld);
        }

        currentSnapshot = snapshot;
        LOGGER.debug("[ReturnByDeath] Snapshot saved.");
    }

    public static void rollbackWorld(MinecraftServer server) {
        if (currentSnapshot == null) return;

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        // Restore blocks
        if (overworld != null) {
            currentSnapshot.restoreChunks(overworld);
        }

        // Restore all players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            rollingBack.add(uuid);
            currentSnapshot.restorePlayer(player);
            rollingBack.remove(uuid);
        }

        LOGGER.info("[ReturnByDeath] World rolled back successfully.");
    }
}
