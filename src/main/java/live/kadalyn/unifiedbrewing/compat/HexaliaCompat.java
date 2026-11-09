package live.kadalyn.unifiedbrewing.compat;

import net.astralya.hexalia.item.ModItems;
import net.astralya.hexalia.recipe.ModRecipeTypes;
import net.astralya.hexalia.util.ModTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.List;

public class HexaliaCompat {
	public static boolean isLoaded() {
		return ModList.get().isLoaded("hexalia");
	}

	public static ItemStack rusticBottle() {
		if (isLoaded())
			return new ItemStack(ModItems.RUSTIC_BOTTLE);
		return ItemStack.EMPTY;
	}

	public static boolean isRusticBottle(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(ModTags.Items.BREWS);
		return false;
	}

	public static boolean isEmptyRusticBottle(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(ModItems.RUSTIC_BOTTLE);
		return false;
	}

	public static boolean isFullRusticBottle(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(ModTags.Items.BREWS) && !itemStack.is(ModItems.RUSTIC_BOTTLE);
		return false;
	}

	public static List<Ingredient> getAllIngredients(Level level) {
		if (!isLoaded()) return List.of();
		return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SMALL_CAULDRON.get())
			.stream().flatMap(r -> r.value().getIngredients().stream())
			.toList();
	}
}
