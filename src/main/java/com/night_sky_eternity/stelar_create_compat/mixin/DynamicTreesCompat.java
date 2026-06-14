package com.night_sky_eternity.stelar_create_compat.mixin;

import java.util.function.BiConsumer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.systems.nodemapper.NetVolumeNode;
import com.simibubi.create.compat.dynamictrees.DynamicTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Re-enables Create's DynamicTrees compatibility stub.
 *
 * Create ships a DynamicTree class (compat/dynamictrees/DynamicTree.java) that
 * SawMovementBehaviour already calls into via TreeCutter.findDynamicTree(). It's
 * just stubbed out: isDynamicBranch() always returns false, and destroyBlocks()
 * does nothing.
 *
 * We DON'T need to reimplement saw logic, contraption storage handling, or
 * world-dropping -- SawMovementBehaviour.dropItemFromCutTree() already does all
 * of that for us via the `drop` BiConsumer passed into destroyBlocks(). Our job
 * is just:
 *   1. Tell Create "yes, this block is a dynamic tree branch"
 *   2. When asked to destroy it, call DynamicTrees' own destroyBranchFromNode()
 *      (same call we validated in v3/v4) and forward every resulting item drop
 *      back through Create's `drop` callback.
 */
@Mixin(DynamicTree.class)
public class DynamicTreesCompat {

    // "Shadow" lets us read a private field declared on the real DynamicTree
    // class as if it were our own. Mixins are merged into the target class at
    // the bytecode level, so this is legal even though the field is private.
    @Shadow
    private BlockPos startCutPos;

    /**
     * isDynamicBranch(Block) is a static method on DynamicTree.
     * Original body: "return false;" (always).
     *
     * We inject at the very start (HEAD). If the block is a Dynamic Trees
     * BranchBlock, we cancel the original method and force the return value
     * to true. Otherwise we do nothing and let "return false" run as normal.
     *
     * Note: this method is `static`, so our injected method must be `static`
     * too, and it can't use `this` / @Shadow fields.
     */
    @Inject(method = "isDynamicBranch", at = @At("HEAD"), cancellable = true)
    private static void onIsDynamicBranch(Block block, CallbackInfoReturnable<Boolean> cir) {
        if (block instanceof BranchBlock) {
            cir.setReturnValue(true);
        }
        // else: fall through to the original "return false"
    }

    /**
     * destroyBlocks(...) is the method SawMovementBehaviour calls once it knows
     * (via isDynamicBranch) that it's looking at a dynamic tree.
     *
     * Signature recap (Java -> Python-ish translation):
     *   void destroyBlocks(
     *       Level world,                                  # the Minecraft world
     *       ItemStack toDamage,                           # tool to apply durability damage to (may be empty)
     *       @Nullable Player playerEntity,                # who's breaking it (null if a machine did it)
     *       BiConsumer<BlockPos, ItemStack> drop          # callback(pos, stack) -> Create handles drop/storage
     *   )
     *
     * A BiConsumer is just a function taking two arguments and returning
     * nothing -- think `def drop(pos, stack): ...`. We call drop.accept(pos, stack)
     * the same way you'd call drop(pos, stack) in Python.
     *
     * We inject at HEAD and fully cancel -- the original body is empty/commented
     * anyway, so there's nothing to "merge" with.
     */
    @Inject(method = "destroyBlocks", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlocks(Level world, ItemStack toDamage, @Nullable Player playerEntity,
                                 BiConsumer<BlockPos, ItemStack> drop, CallbackInfo ci) {

        BlockState stateAtCut = world.getBlockState(startCutPos);

        if (!(stateAtCut.getBlock() instanceof BranchBlock branch)) {
            // Not actually a branch (shouldn't normally happen since
            // isDynamicBranch already filtered for us) -- bail out safely.
            ci.cancel();
            return;
        }

        // Play vanilla block-break particles/sound at the cut position, since
        // destroyBranchFromNode (below) doesn't trigger these itself. This is
        // the same levelEvent call the original (commented-out) stub used.
        if (!world.isClientSide) {
            world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, startCutPos, Block.getId(stateAtCut));
        }

        // This is the exact call validated in v3/v4: destroying the branch
        // returns a BranchDestructionData bundle containing the wood volume
        // and a list of leaf drops with their positions.
        BranchDestructionData destructionData =
                branch.destroyBranchFromNode(world, startCutPos, Direction.DOWN, false, playerEntity);

        // --- Wood drops ---
        // getBranchesDrops wants the tool ItemStack to factor in things like
        // Fortune/Silk Touch. toDamage is empty if there's no relevant tool
        // (e.g. a saw on a contraption), which getBranchesDrops handles fine.
        NetVolumeNode.Volume woodVolume = destructionData.woodVolume;
        List<ItemStack> woodDrops =
                destructionData.species.getBranchesDrops(world, woodVolume, toDamage);

        for (ItemStack stack : woodDrops) {
            // All wood drops come from the single cut position.
            drop.accept(startCutPos, stack);
        }

        // --- Leaf drops ---
        // Each leaf drop carries its own relative position (offset from the
        // cut point), so we add it to startCutPos to get a world position.
        for (BranchBlock.ItemStackPos leafDrop : destructionData.leavesDrops) {
            drop.accept(leafDrop.pos.offset(startCutPos), leafDrop.stack);
        }

        ci.cancel();
    }
}