package com.enn3developer.nrandom.spectreglass;

import java.util.ArrayList;
import java.util.List;

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
    private static final int SEARCH_SIDE = 2 * SEARCH_RADIUS + 1;
    private static final int SEARCH_VOLUME = SEARCH_SIDE * SEARCH_SIDE * SEARCH_SIDE;
    private static final int NOT_FOUND = -1;

    private static final int[][] NEIGHBOURS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    // Reused by every search so mob farms dropping into the glass don't allocate per drop. Safe because the handler
    // only runs on the server thread.
    private final int[] queue = new int[SEARCH_VOLUME];
    /** A cell was visited by the current search if its entry equals {@link #searchId}, so nothing needs clearing. */
    private final int[] visitedBy = new int[SEARCH_VOLUME];
    private int searchId;
    private final AxisAlignedBB candidateBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    private final List<AxisAlignedBB> colliding = new ArrayList<>();

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

        int startX = MathHelper.floor_double(entity.posX);
        int startY = MathHelper.floor_double(entity.boundingBox.minY);
        int startZ = MathHelper.floor_double(entity.posZ);

        int cell = findNearestFreeCell(event.world, entity, startX, startY, startZ);
        if (cell != NOT_FOUND) {
            moveTo(entity, startX + cellX(cell), startY + cellY(cell), startZ + cellZ(cell));
            return;
        }

        int freeY = findFreeYAbove(event.world, entity, startX, startY, startZ);
        if (freeY != NOT_FOUND) {
            moveTo(entity, startX, freeY, startZ);
        }
    }

    private static void moveTo(Entity entity, int x, int y, int z) {
        entity.setPosition(x + 0.5, y + entity.yOffset, z + 0.5);
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

    /**
     * Breadth-first search, so the first free block found is the closest one by steps. Returns the cell index of that
     * block in the search cube centred on the start, or {@link #NOT_FOUND}.
     */
    private int findNearestFreeCell(World world, Entity entity, int startX, int startY, int startZ) {
        searchId++;
        int head = 0;
        int tail = 0;
        int startCell = cell(0, 0, 0);
        queue[tail++] = startCell;
        visitedBy[startCell] = searchId;

        while (head < tail) {
            int current = queue[head++];
            int dx = cellX(current);
            int dy = cellY(current);
            int dz = cellZ(current);
            if (fitsAt(world, entity, startX + dx, startY + dy, startZ + dz)) {
                return current;
            }

            for (int[] offset : NEIGHBOURS) {
                int nx = dx + offset[0];
                int ny = dy + offset[1];
                int nz = dz + offset[2];
                if (outsideSearch(nx) || outsideSearch(ny) || outsideSearch(nz)) {
                    continue;
                }
                if (startY + ny < 0 || startY + ny >= world.getHeight()) {
                    continue;
                }
                int next = cell(nx, ny, nz);
                if (visitedBy[next] == searchId) {
                    continue;
                }
                visitedBy[next] = searchId;
                queue[tail++] = next;
            }
        }
        return NOT_FOUND;
    }

    /** Fallback for drops buried deep in glass: the first free block straight above them. */
    private int findFreeYAbove(World world, Entity entity, int x, int startY, int z) {
        for (int y = startY; y < world.getHeight(); y++) {
            if (fitsAt(world, entity, x, y, z)) {
                return y;
            }
        }
        return NOT_FOUND;
    }

    /**
     * Whether the entity, centred on the bottom of this block, collides with nothing. Uses the same collision boxes
     * vanilla movement uses for this entity, so N Spectre Glass counts as solid.
     */
    private boolean fitsAt(World world, Entity entity, int x, int y, int z) {
        double halfWidth = entity.width / 2;
        AxisAlignedBB box = candidateBox.setBounds(
            x + 0.5 - halfWidth,
            y,
            z + 0.5 - halfWidth,
            x + 0.5 + halfWidth,
            y + entity.height,
            z + 0.5 + halfWidth);
        colliding.clear();
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

    private static boolean outsideSearch(int offset) {
        return Math.abs(offset) > SEARCH_RADIUS;
    }

    /** Packs an offset from the search start, each axis in [-SEARCH_RADIUS, SEARCH_RADIUS], into an array index. */
    private static int cell(int dx, int dy, int dz) {
        return ((dx + SEARCH_RADIUS) * SEARCH_SIDE + dy + SEARCH_RADIUS) * SEARCH_SIDE + dz + SEARCH_RADIUS;
    }

    private static int cellX(int cell) {
        return cell / (SEARCH_SIDE * SEARCH_SIDE) - SEARCH_RADIUS;
    }

    private static int cellY(int cell) {
        return cell / SEARCH_SIDE % SEARCH_SIDE - SEARCH_RADIUS;
    }

    private static int cellZ(int cell) {
        return cell % SEARCH_SIDE - SEARCH_RADIUS;
    }
}
