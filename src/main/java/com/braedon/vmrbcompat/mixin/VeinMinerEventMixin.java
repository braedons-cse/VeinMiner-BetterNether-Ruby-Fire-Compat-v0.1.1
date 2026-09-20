package com.braedon.vmrbcompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility shim for MiraculixxT/Veinminer 2.10.1 on Minecraft 1.21.1.
 *
 * VeinMiner's secondary blocks use Block.getDrops(...) directly, bypassing
 * BetterNether Ruby Fire's Block.dropResources(...) mixin. We redirect only
 * that getDrops call and apply the same blasting-recipe conversion used by
 * BetterNether. Everything else in VeinMiner's block-breaking path remains
 * untouched.
 */
@Pseudo
@Mixin(targets = "de.miraculixx.veinminer.event.VeinMinerEvent", remap = false)
public abstract class VeinMinerEventMixin {

    @Unique
    private static final ResourceKey<Enchantment> VMRB_RUBY_FIRE = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("betternether", "ruby_fire")
    );

    @Unique
    private static final TagKey<Block> VMRB_IS_OBSIDIAN = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("wover", "is_obsidian")
    );

    @Unique
    private static final Map<Item, RecipeHolder<BlastingRecipe>> VMRB_FIRE_CONVERSIONS = new HashMap<>();

    @Unique
    private static boolean VMRB_WARNED = false;

    @Redirect(
            method = "improvedDropResources",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"
            ),
            remap = false
    )
    private List<ItemStack> vmrb$applyRubyFireToVeinDrops(
            BlockState brokenBlock,
            ServerLevel level,
            BlockPos blockPos,
            BlockEntity blockEntity,
            Entity breaker,
            ItemStack breakingItem
    ) {
        // Always begin with VeinMiner's exact normal drop calculation.
        List<ItemStack> drops = Block.getDrops(
                brokenBlock,
                level,
                blockPos,
                blockEntity,
                breaker,
                breakingItem
        );

        if (!(breaker instanceof Player player) || drops.isEmpty()) {
            return drops;
        }

        try {
            Holder<Enchantment> rubyFire = player.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(VMRB_RUBY_FIRE);

            if (EnchantmentHelper.getItemEnchantmentLevel(rubyFire, breakingItem) <= 0) {
                return drops;
            }

            // Match BetterNether: Silk Touch disables Ruby Fire completely.
            Holder<Enchantment> silkTouch = player.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SILK_TOUCH);
            if (EnchantmentHelper.getItemEnchantmentLevel(silkTouch, breakingItem) > 0) {
                return drops;
            }

            vmrb$ensureConversionTable(level);

            boolean convertedAny = false;
            int xpDrop = 0;
            List<ItemStack> converted = new ArrayList<>(drops.size());

            for (ItemStack stack : drops) {
                RecipeHolder<BlastingRecipe> resultHolder = VMRB_FIRE_CONVERSIONS.get(stack.getItem());
                BlastingRecipe recipe = resultHolder != null ? resultHolder.value() : null;

                if (recipe == null) {
                    converted.add(stack);
                    continue;
                }

                convertedAny = true;
                ItemStack resultStack = recipe.getResultItem(level.registryAccess());
                converted.add(resultStack.copyWithCount(
                        resultStack.getCount() * stack.getCount()
                ));

                // Mirror BetterNether's RubyFire implementation: recipe XP
                // is accumulated once for each converted ItemStack.
                xpDrop += recipe.getExperience();
            }

            if (!convertedAny) {
                return drops;
            }

            if (xpDrop > 0 && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                ExperienceOrb.award(level, Vec3.atCenterOf(blockPos), xpDrop);
            }

            return converted;
        } catch (RuntimeException | LinkageError ex) {
            // Fail open: if another mod version changes an API, preserve VeinMiner's
            // original drops instead of crashing or deleting items. Log only once.
            if (!VMRB_WARNED) {
                VMRB_WARNED = true;
                System.err.println("[VM-RubyFire-Compat] Ruby Fire compatibility path failed; falling back to normal VeinMiner drops: " + ex);
            }
            return drops;
        }
    }

    @Unique
    private static void vmrb$ensureConversionTable(ServerLevel level) {
        if (!VMRB_FIRE_CONVERSIONS.isEmpty()) {
            return;
        }

        for (RecipeHolder<BlastingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.BLASTING)) {
            BlastingRecipe recipe = holder.value();
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (stack.getItem() instanceof BlockItem blockItem
                            && blockItem.getBlock().defaultBlockState().is(VMRB_IS_OBSIDIAN)) {
                        continue;
                    }
                    VMRB_FIRE_CONVERSIONS.put(stack.getItem(), holder);
                }
            }
        }
    }
}
