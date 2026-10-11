package com.enn3developer.nrandom.spectreglass;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;

import com.enn3developer.nrandom.NRandom;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class SpectreGlassMigration {

    private static final String MIGRATED_TAG = NRandom.MODID + ":spectreGlassMigrated";

    private final Block from;
    private final Block to;

    public SpectreGlassMigration(Block from, Block to) {
        this.from = from;
        this.to = to;
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkDataEvent.Load event) {
        if (event.getData()
            .getBoolean(MIGRATED_TAG)) {
            return;
        }

        Chunk chunk = event.getChunk();
        int replaced = replace(chunk);
        if (replaced > 0) {
            chunk.setChunkModified();
            NRandom.LOG.info(
                "Replaced {} Spectre Glass in chunk ({}, {}) of dimension {}",
                replaced,
                chunk.xPosition,
                chunk.zPosition,
                chunk.worldObj.provider.dimensionId);
        }
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        event.getData()
            .setBoolean(MIGRATED_TAG, true);
    }

    private int replace(Chunk chunk) {
        int replaced = 0;
        for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
            if (storage == null) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (storage.getBlockByExtId(x, y, z) == from) {
                            storage.func_150818_a(x, y, z, to);
                            storage.setExtBlockMetadata(x, y, z, 0);
                            replaced++;
                        }
                    }
                }
            }
        }
        return replaced;
    }
}
