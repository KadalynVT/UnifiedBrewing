package live.kadalyn.unifiedbrewing.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin {
	@Shadow @Final @Mutable
	private static int[] SLOTS_FOR_SIDES;

	@Shadow @Final @Mutable
	private static int[] SLOTS_FOR_DOWN;

	@Shadow
	private NonNullList<ItemStack> items;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void overwriteSlots(CallbackInfo ci) {
		SLOTS_FOR_DOWN = new int[]{0, 1, 2, 5, 3};
		SLOTS_FOR_SIDES = new int[]{0, 1, 2, 5, 4};
	}

	@ModifyArg(
		method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/NonNullList;withSize(ILjava/lang/Object;)Lnet/minecraft/core/NonNullList;"
		),
		index = 0
	)
	public int init$changeItemsListSize(int size) {
		return size + 1;
	}

	@ModifyReturnValue(
		method = "getPotionBits()[Z",
		at = @At("RETURN")
	)
	private boolean[] getPotionBits(boolean[] original) {
		original = Arrays.copyOf(original, original.length + 1);
		original[original.length - 1] = !(items.get(5)).isEmpty();
		return original;
	}

	@Inject(
		method = "isBrewable(Lnet/minecraft/world/item/alchemy/PotionBrewing;Lnet/minecraft/core/NonNullList;)Z",
		at = @At(value = "RETURN", ordinal = 2),
		cancellable = true
	)
	private static void isBrewable(PotionBrewing potionBrewing, NonNullList<ItemStack> items, CallbackInfoReturnable<Boolean> cir) {
		ItemStack ingredient = items.get(3);
		ItemStack finalPotion = items.get(5);
		if (!finalPotion.isEmpty() && potionBrewing.hasMix(finalPotion, ingredient))
			cir.setReturnValue(true);
	}

	@Inject(
		method = "doBrew(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/NonNullList;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/event/EventHooks;onPotionBrewed(Lnet/minecraft/core/NonNullList;)V"
		)
	)
	private static void doBrew(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo ci) {
		ItemStack ingredient = items.get(3);
		ItemStack finalPotion = items.get(5);
		items.set(5, level.potionBrewing().mix(ingredient, finalPotion));
	}
}
