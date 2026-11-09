package live.kadalyn.unifiedbrewing.mixin.hexalia;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.astralya.hexalia.block.custom.SmallCauldronBlock;
import net.astralya.hexalia.block.entity.custom.SmallCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SmallCauldronBlock.class)
public class SmallCauldronBlockMixin {
	@Shadow
	@Final
	public static BooleanProperty LIT;

	@WrapMethod(
		method = "useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/ItemInteractionResult;"
	)
	protected ItemInteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hit,
		Operation<ItemInteractionResult> original
	) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof SmallCauldronBlockEntity) {
			if (stack.is(ItemTags.SHOVELS)) {
				if (state.getValue(LIT)) {
					level.setBlock(pos, state.setValue(LIT, false), 3);
					level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
					return ItemInteractionResult.SUCCESS;
				}
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}
		}
		return original.call(stack, state, level, pos, player, hand, hit);
	}

	@Redirect(
		method = "tryStirWithLadle(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/astralya/hexalia/block/entity/custom/SmallCauldronBlockEntity;)Lnet/minecraft/world/ItemInteractionResult;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
		)
	)
	private boolean tryStirWithLadle$is(ItemStack stack, Item item) {
		return stack.is(UBTags.LADLE);
	}

	@WrapMethod(method = "tryWaterContainer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/astralya/hexalia/block/entity/custom/SmallCauldronBlockEntity;)Lnet/minecraft/world/ItemInteractionResult;")
	private @Nullable ItemInteractionResult tryWaterContainer(
		ItemStack stack,
		Level level,
		Player player,
		InteractionHand hand,
		SmallCauldronBlockEntity cauldron,
		Operation<ItemInteractionResult> original
	) {
		if (cauldron.isWaterContainer(stack))
			return original.call(stack, level, player, hand, cauldron);

		if (cauldron.isEmpty() || cauldron.hasMixture()) {
			Util.ISmallCauldronBlockEntityExtras c = (Util.ISmallCauldronBlockEntityExtras) (Object) cauldron;
			if (!c.unifiedbrewing$isPotionLikeContainer(stack))
				return null;

			var fail = ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
				fail = ItemInteractionResult.SUCCESS;

			if (level.isClientSide())
				return c.unifiedbrewing$canUsePotionLikeContainer(stack)
					? ItemInteractionResult.SUCCESS
					: fail;
			else
				return c.unifiedbrewing$tryUsePotionLikeContainer(player, stack)
					? ItemInteractionResult.CONSUME
					: fail;
		}
		return null;
	}

	@ModifyReturnValue(
		method = "tryIgniteWithFireStarter(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/ItemInteractionResult;",
		at = @At("RETURN")
	)
	private ItemInteractionResult tryIgniteWithFireStarter(ItemInteractionResult original) {
		if (original == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return ItemInteractionResult.SUCCESS;
		return original;
	}
}