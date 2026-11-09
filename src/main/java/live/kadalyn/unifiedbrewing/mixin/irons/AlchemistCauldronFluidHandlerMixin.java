package live.kadalyn.unifiedbrewing.mixin.irons;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AlchemistCauldronTile.AlchemistCauldronFluidHandler.class)
public abstract class AlchemistCauldronFluidHandlerMixin implements Util.IAlchemistCauldronFluidHandlerExtras {
	@Unique
	private boolean unifiedbrewing$ignoreComponents = false;

	@Unique
	public void unifiedbrewing$setIgnores(boolean components) {
		unifiedbrewing$ignoreComponents = components;
	}

	@WrapOperation(
		method = "isTankCompatible(Lnet/neoforged/neoforge/fluids/IFluidTank;Lnet/neoforged/neoforge/fluids/FluidStack;)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/fluids/FluidStack;isSameFluidSameComponents(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Z"
		)
	)
	private boolean isSameFluidSameComponents(FluidStack first, FluidStack second, Operation<Boolean> original) {
		first = first.copy();
		second = second.copy();
		if (unifiedbrewing$ignoreComponents) {
			Util.removeComponents(first);
			Util.removeComponents(second);
		}
		Util.blazeTouched(first, 0);
		Util.blazeTouched(second, 0);
		return FluidStack.isSameFluidSameComponents(first, second);
	}

	@WrapOperation(
		method = "fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/fluids/IFluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"
		)
	)
	private int fill(IFluidTank instance, FluidStack fluidStack, IFluidHandler.FluidAction fluidAction, Operation<Integer> original) {
		var add = Util.blazeTouched(fluidStack);
		var second = fluidStack.copy();
		Util.blazeTouched(second, Util.blazeTouched(instance.getFluid()));
		var result = original.call(instance, second, fluidAction);
		if (result > 0) {
			var fs = instance.getFluid();
			if (!fs.isEmpty())
				Util.blazeTouched(fs, Util.blazeTouched(fs) + add);
		}
		return result;
	}

	@WrapOperation(
		method = "drain(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/fluids/IFluidTank;drain(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"
		)
	)
	private FluidStack drain(IFluidTank instance, FluidStack fluidStack, IFluidHandler.FluidAction fluidAction, Operation<FluidStack> original) {
		var second = fluidStack.copy();
		var fs = instance.getFluid();
		Util.blazeTouched(second, Util.blazeTouched(fs));
		if (unifiedbrewing$ignoreComponents) {
			Util.copyComponents(second, fs);
		}
		var result = original.call(instance, second, fluidAction);
		fs = instance.getFluid();
		Util.blazeTouched(fs, Math.min(fs.getAmount(), Util.blazeTouched(fs)));
		return result;
	}
}
