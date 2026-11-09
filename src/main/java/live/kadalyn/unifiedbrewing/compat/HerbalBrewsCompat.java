package live.kadalyn.unifiedbrewing.compat;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.registries.UBItems;
import live.kadalyn.unifiedbrewing.registries.UBPotions;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.satisfy.herbalbrews.core.registry.ObjectRegistry;
import net.satisfy.herbalbrews.core.registry.RecipeTypeRegistry;
import net.satisfy.herbalbrews.neoforge.config.HerbalBrewsNeoForgeConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

public class HerbalBrewsCompat {
	private static Map<String, List<Entry>> potionList = null;
	public static boolean isLoaded() {
		return ModList.get().isLoaded("herbalbrews");
	}

	public static boolean isInfusion(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(ObjectRegistry.HERBAL_INFUSION);
		return false;
	}

	public static boolean isFlask(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(UBItems.EMPTY_FLASK) || itemStack.is(ObjectRegistry.FLASK);
		return false;
	}

	public static boolean isEmptyFlask(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(UBItems.EMPTY_FLASK);
		return false;
	}

	public static boolean isFullFlask(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(ObjectRegistry.FLASK);
		return false;
	}

	public static ItemStack getFlaskCounterpart(ItemStack itemStack) {
		if (isLoaded()) {
			var cmd = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
			ItemStack item;
			if (itemStack.is(ObjectRegistry.FLASK))
				item = new ItemStack(UBItems.EMPTY_FLASK);
			else
				item = new ItemStack(ObjectRegistry.FLASK);
			item.set(DataComponents.CUSTOM_MODEL_DATA, cmd);
			return item;
		}
		return ItemStack.EMPTY;
	}

	public static List<PotionContents> flaskToPotionContents(ItemStack itemStack) {
		if (isFullFlask(itemStack)) {
			var potionContents = itemStack.get(DataComponents.POTION_CONTENTS);
			if (potionContents == null) return List.of();
			if (potionList == null) loadPotions();

			var countsFor = new HashMap<String, Integer>();
			var unknowns = new ArrayList<MobEffectInstance>();
			for (var effect : potionContents.getAllEffects()) {
				var key = getEffectKey(effect);
				if (potionList.containsKey(key)) {
					for (var entry : potionList.get(key)) {
						var k = entry.toString();
						if (countsFor.containsKey(k))
							countsFor.put(k, countsFor.get(k) - 1);
						else
							countsFor.put(k, entry.numEffects - 1);
					}
				}
				else {
					unknowns.add(effect);
				}
			}

			var valid = new ArrayList<PotionContents>();
			for (var k : countsFor.keySet()) {
				if (countsFor.get(k) == 0) {
					var p = BuiltInRegistries.POTION.get(ResourceLocation.parse(k.split("\\.")[0]));
					if (p != null) {
						var h = BuiltInRegistries.POTION.wrapAsHolder(p);
						valid.add(new PotionContents(h));
					}
					else
						UnifiedBrewing.LOGGER.error("illegal code branch in HerbalBrewsCompat.flaskToPotions");
				}
			}

			if (!unknowns.isEmpty()) {
				if (unknowns.size() <= 3 - valid.size()) {
					// Split them into separate potions
					for (var e : unknowns) {
						valid.add(new PotionContents(Optional.empty(), Optional.empty(), List.of(e)));
					}
				}
				else {
					// Make a mixed potion
					valid.add(new PotionContents(Optional.empty(), Optional.empty(), unknowns));
				}
			}

			return valid;
		}
		return List.of();
	}

	public static List<ItemStack> flaskToPotions(ItemStack itemStack) {
		var potions = new ArrayList<ItemStack>();
		for (var pc : flaskToPotionContents(itemStack)) {
			var potion = Items.POTION.getDefaultInstance();
			potion.set(DataComponents.POTION_CONTENTS, pc);
			potions.add(potion);
		}
		return potions;
	}

	public static List<FluidStack> flaskToFluids(ItemStack itemStack) {
		var fluids = new ArrayList<FluidStack>();
		var potionContents = flaskToPotionContents(itemStack);
		potionContents.add(new PotionContents(UBPotions.HERBAL_TEA.getDelegate()));
		for (var pc : potionContents) {
			fluids.add(PotionFluid.of(250, pc, PotionFluid.BottleType.REGULAR));
		}

		return fluids;
	}

	public static ItemStack stuffToFlask(List<? extends MutableDataComponentHolder> stuff, int flaskId) {
		if (isLoaded()) {
			var mutableStuff = new HashMap<String, MutableDataComponentHolder>();
			var uniqueEffectsMap = new HashMap<String, MobEffectInstance>();
			for (var x : stuff) {
				var pc = x.get(DataComponents.POTION_CONTENTS);
				// This is presumed but...
				if (pc != null) {
					for (var effect : pc.getAllEffects()) {
						var name = effect.getEffect().getRegisteredName();
						if (!uniqueEffectsMap.containsKey(name) || effect.getAmplifier() > uniqueEffectsMap.get(name).getAmplifier()) {
							// this is what HerbalBrews does, idk
							mutableStuff.put(name, x);
							uniqueEffectsMap.put(name, new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier()));
						}
					}
				}
			}

			var outputPotion = new ItemStack(ObjectRegistry.FLASK);
			var potionContents = PotionContents.EMPTY;

			if (flaskId < 1) flaskId = (int) (1 + Math.random() * 6);
			outputPotion.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(flaskId));

			if (stuff instanceof ArrayList<? extends MutableDataComponentHolder> arrayList) {
				arrayList.removeIf(x -> {
					var pc = x.get(DataComponents.POTION_CONTENTS);
					return !mutableStuff.containsValue(x)
						&& (pc == null || !Util.oneOf(pc, UBPotions.HERBAL_TEA));
				});
			}

			for (var effect : uniqueEffectsMap.values()) {
				potionContents = potionContents.withEffectAdded(effect);
			}

			outputPotion.set(DataComponents.POTION_CONTENTS, potionContents);
			return outputPotion;
		}
		return ItemStack.EMPTY;
	}

	public static List<Ingredient> getAllIngredientsBesidesPotions(Level level) {
		if (!isLoaded()) return List.of();
		return level.getRecipeManager().getAllRecipesFor(RecipeTypeRegistry.CAULDRON_RECIPE_TYPE.get())
			.stream().flatMap(r -> r.value().getIngredients().stream())
			.filter(x -> Arrays.stream(x.getItems()).anyMatch(y -> y.is(Tags.Items.POTIONS)))
			.toList();
	}

	private static void loadPotions() {
		potionList = new HashMap<>();
		for (var holder : BuiltInRegistries.POTION.asHolderIdMap()) {
			var holderKey = holder.getKey();
			if (holderKey == null) continue;
			var item = holderKey.location();
			var potion = holder.value();
			var effects = potion.getEffects();
			for (var effect : effects) {
				var key = getEffectKey(effect);
				if (!potionList.containsKey(key))
					potionList.put(key, new ArrayList<>());
				potionList.get(key).add(new Entry(item, effects.size()));
			}
		}
	}

	private static String getEffectKey(MobEffectInstance effect) {
		return effect.getEffect().getRegisteredName() + '.' + effect.getDuration() + '.' + effect.getAmplifier();
	}

	public static int getBrewTime() {
		return HerbalBrewsNeoForgeConfig.BLOCKS_BREWING_DURATION.get();
	}

	private record Entry(ResourceLocation item, int numEffects) {
		public @NotNull String toString() {
			return item.toString() + '.' + numEffects;
		}
	}

	@MethodsReturnNonnullByDefault
	public record CauldronRecipeInput(List<ItemStack> items) implements RecipeInput {
		public ItemStack getItem(int index) { return this.items.get(index); }
		public int size() { return this.items.size(); }
	}

	@ParametersAreNonnullByDefault
	@MethodsReturnNonnullByDefault
	public static class OutputSlot extends SlotItemHandler {
		public OutputSlot(IItemHandler container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack itemStack) {
			var fuel = getItemHandler().getStackInSlot(4);

			if (fuel.is(ObjectRegistry.HERBAL_INFUSION) || fuel.isEmpty()) {
				// Herbal Infusion uses the original interface, so only allows flasks
				// Allow glass bottles if the fuel is empty in order to collect blood
				return isEmptyFlask(itemStack) || fuel.isEmpty() && mayPlaceAbnormalItem(itemStack);
			}
			else if (IronsCompat.isLoaded()) {
				// Blood vial is okay as an ingredient, it just also allows the vellum recipe
				if (fuel.is(UBTags.BLOOD_BOTTLE) && itemStack.is(ItemRegistry.HOGSKIN))
					return true;
			}

			return mayPlaceAbnormalItem(itemStack);
		}

		private static boolean mayPlaceAbnormalItem(ItemStack itemStack) {
			return itemStack.is(Items.GLASS_BOTTLE)
				|| Util.oneOf(itemStack, Tags.Items.POTION_BOTTLE, UBTags.NON_POTION_BREWABLE)
				|| HexaliaCompat.isEmptyRusticBottle(itemStack);
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return HerbalBrewsCompat.getMaxOutputStackSize(stack);
		}
	}

	public static int getMaxOutputStackSize(ItemStack stack) {
		if (
			stack.is(Tags.Items.POTION_BOTTLE)
			|| stack.is(Items.GLASS_BOTTLE)
			|| stack.is(UBTags.NON_POTION_BREWABLE)
		)
			return 4;
		if (HexaliaCompat.isRusticBottle(stack))
			return 2;
		if (isFlask(stack))
			return 1;
		return stack.getMaxStackSize();
	}
}
