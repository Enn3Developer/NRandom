package com.enn3developer.nrandom.spectreglass;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.enn3developer.nrandom.NRandom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockNSpectreGlass extends BlockGlass {

    public static final String NAME = "n_spectre_glass";

    public BlockNSpectreGlass() {
        super(Material.glass, false);
        setBlockName(NRandom.MODID + "." + NAME);
        setBlockTextureName(NRandom.MODID + ":" + NAME);
        setHardness(0.3F);
        setStepSound(soundTypeGlass);
    }

    private static boolean passesThrough(Entity entity) {
        return entity instanceof EntityLiving && !(entity.riddenByEntity instanceof EntityPlayer);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> list,
        Entity entity) {
        if (!passesThrough(entity)) {
            super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);
        }
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return true;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName());
    }
}
