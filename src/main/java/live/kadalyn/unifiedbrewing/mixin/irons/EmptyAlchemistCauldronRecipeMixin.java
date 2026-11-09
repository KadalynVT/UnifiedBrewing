package live.kadalyn.unifiedbrewing.mixin.irons;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.EmptyAlchemistCauldronRecipe;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmptyAlchemistCauldronRecipe.class)
public class EmptyAlchemistCauldronRecipeMixin {
	@WrapOperation(
		method = "matches(Lio/redspace/ironsspellbooks/recipe_types/alchemist_cauldron/EmptyAlchemistCauldronRecipe$Input;Lnet/minecraft/world/level/Level;)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/fluids/FluidStack;isSameFluidSameComponents(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Z"
		)
	)
	private static boolean isSameFluidSameComponents(FluidStack first, FluidStack second, Operation<Boolean> original) {
		first = Util.removeComponents(first.copy(), true);
		second = Util.removeComponents(second.copy(), true);
		return original.call(first, second);
	}
}
