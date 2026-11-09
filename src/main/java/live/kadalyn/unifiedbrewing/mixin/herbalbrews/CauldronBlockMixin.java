package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.herbalbrews.core.blocks.CauldronBlock;
import net.satisfy.herbalbrews.core.blocks.entity.CauldronBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CauldronBlock.class)
public class CauldronBlockMixin {
	@Inject(method = "stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V", at=@At("HEAD"))
	public void stepOn(
		Level world,
		BlockPos pos,
		BlockState state,
		Entity entity,
		CallbackInfo ci,
		@Share("Level") LocalRef<Level> levelRef,
		@Share("BlockPos") LocalRef<BlockPos> posRef
	) {
		levelRef.set(world);
		posRef.set(pos);
	}

	@WrapOperation(
		method = "stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
		)
	)
	boolean extractBlood(
		LivingEntity instance,
		DamageSource source,
		float amount,
		Operation<Boolean> original,
		@Share("Level") LocalRef<Level> levelRef,
		@Share("BlockPos") LocalRef<BlockPos> posRef
	) {
		var res = original.call(instance, source, amount);
		if (res && IronsCompat.isLoaded()) {
			var world = levelRef.get();
			var pos = posRef.get();
			if (world != null && pos != null) {
				var blockEntity = world.getBlockEntity(pos);
				if (blockEntity instanceof CauldronBlockEntity tile) {
					var items = tile.getItems();
					var output = items.get(3);
					if (output.is(Items.GLASS_BOTTLE)) {
						output.shrink(1);
						if (output.getCount() > 0) {
							// Try to set them into the inputs
							for (int i = 0; i < 3 && output.getCount() > 0; ++i) {
								if (items.get(i).isEmpty()) {
									items.set(i, output.copyWithCount(1));
									output.shrink(1);
								}
							}
							// Drop the rest
							if (output.getCount() > 0) {
								ItemEntity itementity = new ItemEntity(world, pos.getX(), pos.getY() + 1, pos.getZ(), output);
								itementity.setDefaultPickUpDelay();
								world.addFreshEntity(itementity);
							}
						}
						items.set(3, IronsCompat.getBloodVial(1));
					}
					else if (output.is(UBTags.BLOOD_BOTTLE)) {
						for (int i = 0; i < 3 && output.getCount() > 0; ++i) {
							var input = items.get(i);
							if (input.is(Items.GLASS_BOTTLE)) {
								input.shrink(1);
								output.grow(1);
								break;
							}
						}
					}
				}
			}
		}
		return res;
	}
}
