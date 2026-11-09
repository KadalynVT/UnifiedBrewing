package live.kadalyn.unifiedbrewing.fluids;

import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

@MethodsReturnNonnullByDefault
public class BrewFluidType extends FluidType {
	public BrewFluidType(Properties properties) {
		super(properties);
	}

	@Override
	public String getDescriptionId(FluidStack stack) {
		var brewItem = stack.get(UBDataComponents.BREW_ITEM);
		if (brewItem != null && !brewItem.isEmpty()) {
			return brewItem.getItem().getDescriptionId();
		}
		return super.getDescriptionId(stack);
	}
}
