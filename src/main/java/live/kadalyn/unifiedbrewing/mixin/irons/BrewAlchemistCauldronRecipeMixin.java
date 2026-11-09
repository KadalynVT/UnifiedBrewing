package live.kadalyn.unifiedbrewing.mixin.irons;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.BrewAlchemistCauldronRecipe;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BrewAlchemistCauldronRecipe.class)
public abstract class BrewAlchemistCauldronRecipeMixin {
	@Shadow
	@Final
	private List<FluidStack> results;

	@WrapOperation(
		method = "matches(Lio/redspace/ironsspellbooks/recipe_types/alchemist_cauldron/BrewAlchemistCauldronRecipe$Input;Lnet/minecraft/world/level/Level;)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/fluids/FluidStack;isSameFluidSameComponents(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Z"
		)
	)
	private boolean isSameFluidSameComponents(FluidStack first, FluidStack second, Operation<Boolean> original) {
		first = Util.removeComponents(first.copy(), true);
		second = Util.removeComponents(second.copy(), true);
		return original.call(first, second);
	}
}
