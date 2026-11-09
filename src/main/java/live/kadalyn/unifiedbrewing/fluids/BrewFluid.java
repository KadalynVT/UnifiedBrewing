package live.kadalyn.unifiedbrewing.fluids;

import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import live.kadalyn.unifiedbrewing.registries.UBDataComponents.BrewItemComponent;
import live.kadalyn.unifiedbrewing.registries.UBFluids;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BrewFluid extends BaseFlowingFluid {
	public BrewFluid(Properties properties) {
		super(properties);
	}

	public static FluidStack of(int amount, ItemStack item) {
		FluidStack fluidStack = new FluidStack(UBFluids.BREW_FLUID, amount);
		return addBrewToFluidStack(fluidStack, BrewItemComponent.of(item));
	}

	public static FluidStack addBrewToFluidStack(FluidStack fs, BrewItemComponent item) {
		if (item == BrewItemComponent.EMPTY) {
			fs.remove(UBDataComponents.BREW_ITEM);
			return fs;
		} else {
			fs.set(UBDataComponents.BREW_ITEM.get(), item);
			return fs;
		}
	}

	public static FluidStack from(ItemStack stack) {
		if (stack.is(net.astralya.hexalia.util.ModTags.Items.BREWS)) {
			FluidStack fs = new FluidStack(UBFluids.BREW_FLUID, 250);
			fs.set(UBDataComponents.BREW_ITEM, BrewItemComponent.of(stack));
			return fs;
		}
		return FluidStack.EMPTY;
	}

	public static ItemStack from(FluidStack stack) {
		if (stack.getAmount() >= 250 && (stack.is(UBFluids.BREW_FLUID))) {
			BrewItemComponent brewItem = stack.get(UBDataComponents.BREW_ITEM);
			if (brewItem != null && !brewItem.isEmpty()) {
				return brewItem.getItem();
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public Item getBucket() {
		return Items.AIR;
	}

	@Override
	protected BlockState createLegacyBlock(FluidState state) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public boolean isSource(FluidState fluidState) {
		return true;
	}

	@Override
	public int getAmount(FluidState fluidState) {
		return 0;
	}
}
