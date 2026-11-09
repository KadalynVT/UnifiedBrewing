package live.kadalyn.unifiedbrewing.registries;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class UBPotions {
	public static final DeferredRegister<Potion> POTIONS =
		DeferredRegister.create(Registries.POTION, UnifiedBrewing.MODID);

	public static DeferredHolder<Potion, Potion> HERBAL_TEA = null;

	public static void register(IEventBus eventBus) {
		if (HerbalBrewsCompat.isLoaded())
			HERBAL_TEA = POTIONS.register("herbal_tea", () -> new Potion());
		POTIONS.register(eventBus);
	}
}
