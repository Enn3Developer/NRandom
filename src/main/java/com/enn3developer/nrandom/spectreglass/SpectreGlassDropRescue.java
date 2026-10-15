package com.enn3developer.nrandom.spectreglass;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Moves items and XP orbs that spawn inside N Spectre Glass to the nearest free block.
 * <p>
 * Mobs walk through the glass, so they often die inside it and their drops spawn inside a block that is solid for
 * items. Vanilla then pushes the item out with {@link Entity#func_145771_j}, which treats every block that is not a
 * normal cube as free: glass is not, so the item gets pushed into more glass or straight up, with {@code noClip} on
 * the whole time, and ends up passing through solid blocks too. Relocating the entity before its first tick means
 * that push never starts. See https://github.com/Enn3Developer/NRandom/issues/1
 */
public class SpectreGlassDropRescue {

    private static final int SEARCH_RADIUS = 4;

    private static final int[][] NEIGHBOURS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.entity;
        if (event.world.isRemote) {
            return;
        }
        if (!(entity instanceof EntityItem) && !(entity instanceof EntityXPOrb)) {
            return;
        }
        if (!touchesSpectreGlass(event.world, entity.boundingBox)) {
            return;
        }

        int[] free = findNearestFreeBlock(event.world, entity);
        if (free == null) {
            free = findFreeBlockAbove(event.world, entity);
        }
        if (free == null) {
            return;
        }

        entity.setPosition(free[0] + 0.5, free[1] + entity.yOffset, free[2] + 0.5);
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
    }

    private static boolean touchesSpectreGlass(World world, AxisAlignedBB box) {
        for (int x = MathHelper.floor_double(box.minX); x <= MathHelper.floor_double(box.maxX); x++) {
            for (int y = MathHelper.floor_double(box.minY); y <= MathHelper.floor_double(box.maxY); y++) {
                for (int z = MathHelper.floor_double(box.minZ); z <= MathHelper.floor_double(box.maxZ); z++) {
                    if (world.getBlock(x, y, z) instanceof BlockNSpectreGlass) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Breadth-first search, so the first free block found is the closest one by steps. */
    private static int[] findNearestFreeBlock(World world, Entity entity) {
        int startX = MathHelper.floor_double(entity.posX);
        int startY = MathHelper.floor_double(entity.boundingBox.minY);
        int startZ = MathHelper.floor_double(entity.posZ);

        Queue<int[]> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(new int[] { startX, startY, startZ });
        visited.add(key(startX, startY, startZ));

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            if (fitsAt(world, entity, pos[0], pos[1], pos[2])) {
                return pos;
            }

            for (int[] offset : NEIGHBOURS) {
                int x = pos[0] + offset[0];
                int y = pos[1] + offset[1];
                int z = pos[2] + offset[2];
                boolean outOfRange = Math.abs(x - startX) > SEARCH_RADIUS || Math.abs(y - startY) > SEARCH_RADIUS
                    || Math.abs(z - startZ) > SEARCH_RADIUS;
                if (outOfRange || y < 0 || y >= world.getHeight() || !visited.add(key(x, y, z))) {
                    continue;
                }
                queue.add(new int[] { x, y, z });
            }
        }
        return null;
    }

    /** Fallback for drops buried deep in glass: the first free block straight above them. */
    private static int[] findFreeBlockAbove(World world, Entity entity) {
        int x = MathHelper.floor_double(entity.posX);
        int z = MathHelper.floor_double(entity.posZ);
        for (int y = MathHelper.floor_double(entity.boundingBox.minY); y < world.getHeight(); y++) {
            if (fitsAt(world, entity, x, y, z)) {
                return new int[] { x, y, z };
            }
        }
        return null;
    }

    /**
     * Whether the entity, centred on the bottom of this block, collides with nothing. Uses the same collision boxes
     * vanilla movement uses for this entity, so N Spectre Glass counts as solid.
     */
    private static boolean fitsAt(World world, Entity entity, int x, int y, int z) {
        double halfWidth = entity.width / 2;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            x + 0.5 - halfWidth,
            y,
            z + 0.5 - halfWidth,
            x + 0.5 + halfWidth,
            y + entity.height,
            z + 0.5 + halfWidth);
        List<AxisAlignedBB> colliding = new ArrayList<>();
        for (int bx = MathHelper.floor_double(box.minX); bx <= MathHelper.floor_double(box.maxX); bx++) {
            for (int by = MathHelper.floor_double(box.minY); by <= MathHelper.floor_double(box.maxY); by++) {
                for (int bz = MathHelper.floor_double(box.minZ); bz <= MathHelper.floor_double(box.maxZ); bz++) {
                    world.getBlock(bx, by, bz)
                        .addCollisionBoxesToList(world, bx, by, bz, box, colliding, entity);
                }
            }
        }
        return colliding.isEmpty();
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | (long) z & 0x3FFFFFF;
    }
}
