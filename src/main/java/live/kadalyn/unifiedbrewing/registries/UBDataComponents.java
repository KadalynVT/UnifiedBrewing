package live.kadalyn.unifiedbrewing.registries;

import com.mojang.serialization.Codec;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.UnaryOperator;

public final class UBDataComponents {
	private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, UnifiedBrewing.MODID);

	public static void register(IEventBus eventBus) {
		COMPONENTS.register(eventBus);
	}

	private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String pName, UnaryOperator<DataComponentType.Builder<T>> pBuilder) {
		return COMPONENTS.register(pName, () -> pBuilder.apply(DataComponentType.builder()).build());
	}

	// Components
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<SingleItemComponent>> MUNDANE_INGREDIENT = register(
		"mundane_ingredient", builder -> builder.persistent(SingleItemComponent.CODEC).networkSynchronized(SingleItemComponent.STREAM_CODEC).cacheEncoding());
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Short>> BLAZE_TOUCHED = register(
		"blaze_touched", builder -> builder.persistent(Codec.SHORT).networkSynchronized(ByteBufCodecs.SHORT));
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TEA_TOUCHED = register(
		"tea_touched", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BrewItemComponent>> BREW_ITEM = register(
		"brew_item", builder -> builder.persistent(BrewItemComponent.CODEC).networkSynchronized(BrewItemComponent.STREAM_CODEC).cacheEncoding());

	public static class SingleItemComponent {
		public static final SingleItemComponent EMPTY = SingleItemComponent.of(ItemStack.EMPTY);
		public static final Codec<SingleItemComponent> CODEC = ItemStack.ITEM_NON_AIR_CODEC.xmap(
			SingleItemComponent::new, SingleItemComponent::getItemHolder);
		public static final StreamCodec<RegistryFriendlyByteBuf, SingleItemComponent> STREAM_CODEC = ItemStack.STREAM_CODEC
			.map(SingleItemComponent::new, item -> item.item);
		private final ItemStack item;

		private SingleItemComponent(ItemStack item) {
			this.item = item;
		}

		private SingleItemComponent(Holder<Item> item) {
			this.item = new ItemStack(item.value());
		}

		public static SingleItemComponent of(ItemStack itemStack) {
			return new SingleItemComponent(itemStack.copy());
		}

		public boolean is(Item item) {
			return this.item.is(item);
		}

		public ItemStack getItem() {
			return this.item.copy();
		}

		public Holder<Item> getItemHolder() {
			return this.item.getItemHolder();
		}

		public boolean isEmpty() {
			return this.item == ItemStack.EMPTY;
		}

		public boolean equals(Object object) {
			return this == object || (
				object instanceof SingleItemComponent singleItem
					&& this.item.is(singleItem.item.getItem()));
		}

		public int hashCode() {
			return ItemStack.hashItemAndComponents(this.item);
		}

		public String toString() {
			return "SingleItemComponent[item=" + this.item + "]";
		}
	}

	public static class BrewItemComponent extends SingleItemComponent {
		public static final Codec<BrewItemComponent> CODEC = ItemStack.ITEM_NON_AIR_CODEC.xmap(
			BrewItemComponent::new, BrewItemComponent::getItemHolder);
		public static final StreamCodec<RegistryFriendlyByteBuf, BrewItemComponent> STREAM_CODEC = ItemStack.STREAM_CODEC
			.map(BrewItemComponent::new, BrewItemComponent::getItem);

		private BrewItemComponent(ItemStack item) {
			super(item);
		}

		private BrewItemComponent(Holder<Item> item) {
			super(item);
		}

		public static BrewItemComponent of(ItemStack itemStack) {
			return new BrewItemComponent(itemStack.copy());
		}

		public int getColor() {
			if (getItem().getItem() instanceof net.astralya.hexalia.item.custom.BrewItem brew) {
				return brew.getBrewColor();
			}
			return PotionContents.getColor(Potions.WATER);
		}
	}
}
