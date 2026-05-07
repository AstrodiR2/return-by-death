package com.returnbydeath;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class WorldSnapshot {

    // Blocks: position -> blockstate
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();

    // Players: uuid -> saved state
    private final Map<UUID, PlayerSnapshot> players = new HashMap<>();

    // ---- PLAYER ----

    public void savePlayer(ServerPlayer player) {
        players.put(player.getUUID(), new PlayerSnapshot(player));
    }

    public void restorePlayer(ServerPlayer player) {
        PlayerSnapshot snap = players.get(player.getUUID());
        if (snap == null) return;
        snap.restore(player);
    }

    // ---- CHUNKS / BLOCKS ----

    public void saveChunks(ServerLevel level) {
        blocks.clear();
        // Iterate all loaded chunks
        level.getChunkSource().chunkMap.getChunks().forEach(holder -> {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk == null) return;

            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight();

            for (int x = startX; x < startX + 16; x++) {
                for (int z = startZ; z < startZ + 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir()) {
                            blocks.put(pos.immutable(), state);
                        }
                    }
                }
            }
        });
    }

    public void restoreChunks(ServerLevel level) {
        // First pass: clear all non-air blocks in loaded chunks that aren't in snapshot
        level.getChunkSource().chunkMap.getChunks().forEach(holder -> {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk == null) return;

            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
            int minY = level.getMinBuildHeight();
            int maxY = level.getMaxBuildHeight();

            for (int x = startX; x < startX + 16; x++) {
                for (int z = startZ; z < startZ + 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState saved = blocks.get(pos);
                        BlockState current = level.getBlockState(pos);

                        if (saved == null) {
                            // Block didn't exist in snapshot — remove it
                            if (!current.isAir()) {
                                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            }
                        } else if (!current.equals(saved)) {
                            // Block changed — restore it
                            level.setBlock(pos, saved, 3);
                        }
                    }
                }
            }
        });
    }

    // ---- INNER CLASS: Player Snapshot ----

    public static class PlayerSnapshot {
        private final double x, y, z;
        private final float yaw, pitch;
        private final float health;
        private final int foodLevel;
        private final float saturation;
        private final int xpLevel;
        private final float xpProgress;
        private final List<ItemStack> inventoryItems;
        private final List<ItemStack> armorItems;
        private final List<ItemStack> offhand;

        public PlayerSnapshot(ServerPlayer player) {
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            yaw = player.getYRot();
            pitch = player.getXRot();
            health = player.getHealth();
            foodLevel = player.getFoodData().getFoodLevel();
            saturation = player.getFoodData().getSaturationLevel();
            xpLevel = player.experienceLevel;
            xpProgress = player.experienceProgress;

            // Save inventory
            inventoryItems = new ArrayList<>();
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                inventoryItems.add(player.getInventory().items.get(i).copy());
            }
            armorItems = new ArrayList<>();
            for (int i = 0; i < player.getInventory().armor.size(); i++) {
                armorItems.add(player.getInventory().armor.get(i).copy());
            }
            offhand = new ArrayList<>();
            for (int i = 0; i < player.getInventory().offhand.size(); i++) {
                offhand.add(player.getInventory().offhand.get(i).copy());
            }
        }

        public void restore(ServerPlayer player) {
            // Teleport
            player.teleportTo(x, y, z);
            player.setYRot(yaw);
            player.setXRot(pitch);

            // Health / food
            player.setHealth(health > 0 ? health : 20f);
            player.getFoodData().setFoodLevel(foodLevel);
            player.getFoodData().setSaturation(saturation);

            // XP
            player.experienceLevel = xpLevel;
            player.experienceProgress = xpProgress;

            // Inventory
            for (int i = 0; i < inventoryItems.size() && i < player.getInventory().items.size(); i++) {
                player.getInventory().items.set(i, inventoryItems.get(i).copy());
            }
            for (int i = 0; i < armorItems.size() && i < player.getInventory().armor.size(); i++) {
                player.getInventory().armor.set(i, armorItems.get(i).copy());
            }
            for (int i = 0; i < offhand.size() && i < player.getInventory().offhand.size(); i++) {
                player.getInventory().offhand.set(i, offhand.get(i).copy());
            }

            // Sync to client
            player.inventoryMenu.broadcastChanges();
            player.hurtMarked = true;
        }
    }
}
