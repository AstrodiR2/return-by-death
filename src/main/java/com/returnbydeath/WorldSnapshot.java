package com.returnbydeath;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;

public class WorldSnapshot {

    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Map<UUID, PlayerSnapshot> players = new HashMap<>();

    public void savePlayer(ServerPlayer player) {
        players.put(player.getUUID(), new PlayerSnapshot(player));
    }

    public void restorePlayer(ServerPlayer player) {
        PlayerSnapshot snap = players.get(player.getUUID());
        if (snap != null) snap.restore(player);
    }

    public void saveChunks(ServerLevel level) {
        blocks.clear();
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        level.getChunkSource().chunkMap.getChunks().forEach(holder -> {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk == null) return;
            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
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
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        level.getChunkSource().chunkMap.getChunks().forEach(holder -> {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk == null) return;
            int startX = chunk.getPos().getMinBlockX();
            int startZ = chunk.getPos().getMinBlockZ();
            for (int x = startX; x < startX + 16; x++) {
                for (int z = startZ; z < startZ + 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState saved = blocks.get(pos);
                        BlockState current = level.getBlockState(pos);
                        if (saved == null) {
                            if (!current.isAir()) {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            }
                        } else if (!current.equals(saved)) {
                            level.setBlock(pos, saved, 3);
                        }
                    }
                }
            }
        });
    }

    public static class PlayerSnapshot {
        private final double x, y, z;
        private final float yaw, pitch;
        private final float health;
        private final int foodLevel;
        private final float saturation;
        private final int xpLevel;
        private final float xpProgress;
        private final List<ItemStack> allItems;

        public PlayerSnapshot(ServerPlayer player) {
            x = player.getX(); y = player.getY(); z = player.getZ();
            yaw = player.getYRot(); pitch = player.getXRot();
            health = player.getHealth();
            foodLevel = player.getFoodData().getFoodLevel();
            saturation = player.getFoodData().getSaturationLevel();
            xpLevel = player.experienceLevel;
            xpProgress = player.experienceProgress;

            allItems = new ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                allItems.add(player.getInventory().getItem(i).copy());
            }
        }

        public void restore(ServerPlayer player) {
            player.teleportTo(x, y, z);
            player.setYRot(yaw); player.setXRot(pitch);
            player.setHealth(health > 0 ? health : 20f);
            player.getFoodData().setFoodLevel(foodLevel);
            player.getFoodData().setSaturation(saturation);
            player.experienceLevel = xpLevel;
            player.experienceProgress = xpProgress;

            for (int i = 0; i < allItems.size() && i < player.getInventory().getContainerSize(); i++) {
                player.getInventory().setItem(i, allItems.get(i).copy());
            }
            player.inventoryMenu.broadcastChanges();
        }
    }
}
