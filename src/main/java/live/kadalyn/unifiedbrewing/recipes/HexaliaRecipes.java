package live.kadalyn.unifiedbrewing.recipes;

import live.kadalyn.unifiedbrewing.compat.HexaliaCompat;
import live.kadalyn.unifiedbrewing.fluids.BrewFluid;
import net.astralya.hexalia.recipe.ModRecipeTypes;
import net.astralya.hexalia.recipe.SmallCauldronRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Arrays;
import java.util.List;

public class HexaliaRecipes {
	private static List<ItemStack> validInputs = List.of();

	public static boolean isValidInput(Item itemStack) {
		if (!HexaliaCompat.isLoaded())
			return false;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			if (validInputs.isEmpty()) {
				validInputs = server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SMALL_CAULDRON.value()).stream()
					.flatMap(r -> r.value().getIngredients().stream())
					.flatMap(x -> Arrays.stream(x.getItems())).toList();
			}
			return validInputs.stream().anyMatch(x -> x.is(itemStack));
		}
		return false;
	}
}
