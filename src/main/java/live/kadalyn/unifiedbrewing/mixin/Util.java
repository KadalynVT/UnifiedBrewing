package live.kadalyn.unifiedbrewing.mixin;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.math.MethodsReturnNonnullByDefault;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import live.kadalyn.unifiedbrewing.compat.HexaliaCompat;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.recipes.MixFinder;
import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import net.astralya.hexalia.item.ModItems;
import net.astralya.hexalia.recipe.SmallCauldronRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Util {
	static public <T extends MutableDataComponentHolder, U extends DataComponentHolder> void copyComponents(T toX, U fromX) {
		copyComponents(toX, fromX, false);
	}

	static public <T extends MutableDataComponentHolder, U extends DataComponentHolder> void copyComponents(T toX, U fromX, boolean includeBlazeTouched) {
		UBDataComponents.SingleItemComponent mundaneIngredient = fromX.get(UBDataComponents.MUNDANE_INGREDIENT.get());
		if (mundaneIngredient != null)
			toX.set(UBDataComponents.MUNDANE_INGREDIENT.get(), mundaneIngredient);
		else
			toX.remove(UBDataComponents.MUNDANE_INGREDIENT.get());

		if (includeBlazeTouched) {
			blazeTouched(toX, blazeTouched(fromX));
		}
	}

	static public <T extends MutableDataComponentHolder> T removeComponents(T fromX) {
		return removeComponents(fromX, false);
	}

	static public <T extends MutableDataComponentHolder> T removeComponents(T fromX, boolean includeBlazeTouched) {
		fromX.remove(UBDataComponents.MUNDANE_INGREDIENT.get());
		if (includeBlazeTouched) Util.blazeTouched(fromX, 0);
		return fromX;
	}

	public static int blazeTouched(DataComponentHolder holder) {
		Short mb = holder.get(UBDataComponents.BLAZE_TOUCHED);
		if (mb == null) return 0;
		return mb;
	}

	public static void blazeTouched(MutableDataComponentHolder holder, int amount) {
		if (amount <= 0)
			holder.remove(UBDataComponents.BLAZE_TOUCHED);
		else
			holder.set(UBDataComponents.BLAZE_TOUCHED, (short) amount);
	}

	@SafeVarargs
	public static boolean oneOf(ItemStack potion, Holder<Potion>... potions) {
		if (potion.isEmpty()) return false;
		PotionContents potionContents = potion.get(DataComponents.POTION_CONTENTS);
		if (potionContents != null) {
			return oneOf(potionContents, potions);
		}
		return false;
	}

	@SafeVarargs
	public static boolean oneOf(PotionContents potion, Holder<Potion>... potions) {
		for (Holder<Potion> p : potions) {
			if (p != null && potion.is(p)) return true;
		}
		return false;
	}

	@SafeVarargs
	public static boolean oneOf(ItemStack itemStack, TagKey<Item>... tags) {
		if (itemStack.isEmpty()) return false;
		for (TagKey<Item> tag : tags) {
			if (itemStack.is(tag)) return true;
		}
		return false;
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> recipeHolderOf(T recipe) {
		return Optional.of(new RecipeHolder<>(ResourceLocation.parse("minecraft:empty"), recipe));
	}

	private static List<? extends Predicate<ItemStack>> allIngredients = List.of();

	public static boolean isPotionIngredient(ItemStack ingredient) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			Level overworld = server.getLevel(Level.OVERWORLD);
			if (overworld != null)
				return isPotionIngredient(overworld, ingredient);
		}
		return false;
	}

	public static boolean isPotionIngredient(Level level, ItemStack ingredient) {
		var potionBrewing = level.potionBrewing();
		if (potionBrewing.isIngredient(ingredient))
			return true;
		if (IronsCompat.isScroll(ingredient))
			return true;

		if (allIngredients.isEmpty()) {
			allIngredients = Lists.newArrayList(Iterables.concat(
				HerbalBrewsCompat.getAllIngredientsBesidesPotions(level),
				HexaliaCompat.getAllIngredients(level),
				IronsCompat.getAllIngredients(level)
			));
		}

		for (var x : allIngredients) {
			if (x.test(ingredient))
				return true;
		}
		return false;
	}

	public static int hasPotionRecipe(Level level, List<ItemStack> inputs, ItemStack container, boolean blazed) {
		var mixFinder = MixFinder.from(container, inputs);
		// Checks vanilla and Iron's
		var brewTime = mixFinder.findIncrementalBrewingResult(level, blazed).ticksToBrew();
		if (brewTime == 0 && HexaliaCompat.isLoaded())
			brewTime = mixFinder.findSmallCauldronResult(level).ticksToBrew();
		if (brewTime == 0 && HerbalBrewsCompat.isLoaded())
			brewTime = mixFinder.findBrewingCauldronResult(level).ticksToBrew();
		return brewTime;
	}

	public static ItemStack bottleOf(ItemStack stack) {
		if (stack.is(Tags.Items.POTION_BOTTLE) || stack.is(Items.GLASS_BOTTLE))
			return Items.GLASS_BOTTLE.getDefaultInstance();
		if (HexaliaCompat.isRusticBottle(stack))
			return ModItems.RUSTIC_BOTTLE.get().getDefaultInstance();
		if (HerbalBrewsCompat.isEmptyFlask(stack))
			return stack.copy();
		if (HerbalBrewsCompat.isFullFlask(stack))
			return HerbalBrewsCompat.getFlaskCounterpart(stack);
		if (IronsCompat.isLoaded())
			return IronsCompat.getBottle(stack);
		return ItemStack.EMPTY;
	}

	public interface ICauldronGuiHandlerExtras {
		int unifiedbrewing$getFuel();
	}

	public interface ICauldronGuiExtras {
		boolean unifiedbrewing$isMouseOverFuel(int mouseX, int mouseY);
	}

	public interface ICauldronBlockEntityExtras {
		int unifiedbrewing$getFuelRemaining();
		void unifiedbrewing$setFuelRemaining(int fuelRemaining);
	}

	public interface ISmallCauldronBlockEntityExtras {
		ResourceLocation unifiedbrewing$getMixtureTexture();
		boolean unifiedbrewing$isPotionLikeContainer(ItemStack held);
		boolean unifiedbrewing$canUsePotionLikeContainer(ItemStack held);
		boolean unifiedbrewing$tryUsePotionLikeContainer(Player player, ItemStack held);
	}

	@MethodsReturnNonnullByDefault
	public interface ISmallCauldronContentsExtras {
		int getStirsDone();
		ResourceLocation unifiedbrewing$getMixtureTexture();
		Optional<RecipeHolder<SmallCauldronRecipe>> unifiedbrewing$findRecipeMatch(Level level);
		boolean unifiedbrewing$isPotionLikeContainer(ItemStack held);
		boolean unifiedbrewing$canUsePotionLikeContainer(ItemStack held);
		boolean unifiedbrewing$tryUsePotionLikeContainer(
			ServerLevel level,
			double x, double y, double z,
			Player player,
			ItemStack held
		);
	}

	public interface ISmallCauldronRecipeExtras {
		int unifiedbrewing$getBlazeCharges();
		void unifiedbrewing$setBlazeCharges(int blazeCharges);
	}

	public interface IAlchemistCauldronFluidHandlerExtras {
		void unifiedbrewing$setIgnores(boolean components);
	}
}
