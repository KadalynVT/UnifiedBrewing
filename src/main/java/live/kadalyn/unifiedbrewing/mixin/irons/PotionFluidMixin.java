package live.kadalyn.unifiedbrewing.mixin.irons;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PotionFluid.class)
public class PotionFluidMixin {
	@WrapMethod(method = "from(Lnet/minecraft/world/item/ItemStack;)Lnet/neoforged/neoforge/fluids/FluidStack;")
	private static FluidStack from(ItemStack stack, Operation<FluidStack> original) {
		var result = original.call(stack);
		Util.copyComponents(result, stack);
		return result;
	}

	@WrapMethod(method = "from(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;")
	private static ItemStack from(FluidStack stack, Operation<ItemStack> original) {
		var result = original.call(stack);
		Util.copyComponents(result, stack);
		return result;
	}
}
