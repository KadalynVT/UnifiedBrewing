package live.kadalyn.unifiedbrewing.fluids;

import io.redspace.ironsspellbooks.fluids.SimpleClientFluidType;
import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.fluids.FluidStack;

public class BrewFluidClientType extends SimpleClientFluidType {
	public BrewFluidClientType(ResourceLocation texture) {
		super(texture);
	}

	@Override
	public int getTintColor(FluidStack stack) {
		var brew = stack.get(UBDataComponents.BREW_ITEM.get());
		return (brew != null ? brew.getColor() : PotionContents.getColor(Potions.WATER)) | 0xFF000000; // force full ARGB alpha ('or' on alpha component)
	}
}
