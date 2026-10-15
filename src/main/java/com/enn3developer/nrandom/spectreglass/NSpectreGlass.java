package com.enn3developer.nrandom.spectreglass;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.enn3developer.nrandom.Config;
import com.enn3developer.nrandom.NRandom;

import cpw.mods.fml.common.registry.GameRegistry;

public final class NSpectreGlass {

    private static BlockNSpectreGlass block;

    private NSpectreGlass() {}

    public static void preInit() {
        block = new BlockNSpectreGlass();
        GameRegistry.registerBlock(block, ItemBlockNSpectreGlass.class, BlockNSpectreGlass.NAME);
        MinecraftForge.EVENT_BUS.register(new SpectreGlassDropRescue());
    }

    public static void init() {
        Block spectreGlass = GameRegistry.findBlock("RandomThings", "spectreGlass");
        if (spectreGlass == null) {
            NRandom.LOG.warn("Random Things Spectre Glass is missing, N Spectre Glass gets no recipe and no migration");
            return;
        }

        GameRegistry.addShapelessRecipe(new ItemStack(block), new ItemStack(spectreGlass));

        if (Config.migrateSpectreGlass) {
            MinecraftForge.EVENT_BUS.register(new SpectreGlassMigration(spectreGlass, block));
        }
    }
}
