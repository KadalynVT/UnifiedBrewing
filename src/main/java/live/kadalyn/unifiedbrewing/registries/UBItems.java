package live.kadalyn.unifiedbrewing.registries;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class UBItems {
	public static final DeferredRegister<Item> ITEMS =
		DeferredRegister.create(Registries.ITEM, UnifiedBrewing.MODID);

	public static final DeferredHolder<Item, Item> EMPTY_FLASK =
		ITEMS.register("empty_flask", () -> new Item(new Item.Properties().stacksTo(64)));

	public static final DeferredHolder<Item, Item> BLAZING_LADLE =
		ITEMS.register("blazing_ladle", () -> new Item(new Item.Properties().durability(80)));

	public static final DeferredHolder<Item, Item> DEPLETED_BLAZE_POWDER =
		ITEMS.register("depleted_blaze_powder", () -> new Item(new Item.Properties()
			.durability(20)
			.setNoRepair()
		));

	public static void register(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}
}
