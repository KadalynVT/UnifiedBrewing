package live.kadalyn.unifiedbrewing.registries;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.fluids.BrewFluid;
import live.kadalyn.unifiedbrewing.fluids.BrewFluidType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Function;
import java.util.function.Supplier;

public class UBFluids {
	private static final DeferredRegister<Fluid> FLUIDS =
		DeferredRegister.create(Registries.FLUID, UnifiedBrewing.MODID);
	private static final DeferredRegister<FluidType> FLUID_TYPES =
		DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, UnifiedBrewing.MODID);;

	public static final DeferredHolder<FluidType, FluidType> BREW_FLUID_TYPE =
		FLUID_TYPES.register("brew", () -> new BrewFluidType(FluidType.Properties.create()));

	public static final DeferredHolder<Fluid, BrewFluid> BREW_FLUID =
		register("brew", BREW_FLUID_TYPE::value, p -> () -> new BrewFluid(p));

	public static void register(IEventBus eventBus) {
		FLUIDS.register(eventBus);
		FLUID_TYPES.register(eventBus);
	}

	private static <T extends Fluid> DeferredHolder<Fluid, T> register(
		String name,
		Supplier<FluidType> fluidType,
		Function<BaseFlowingFluid.Properties, Supplier<Fluid>> cb
	) {
		DeferredHolder<Fluid, T> holder = DeferredHolder.create(Registries.FLUID,
			ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, name));
		BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(fluidType, holder::value, holder::value).bucket(() -> Items.AIR);
		FLUIDS.register(name, cb.apply(properties));
		return holder;
	}
}
