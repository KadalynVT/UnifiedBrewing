package live.kadalyn.unifiedbrewing.compat;

import com.google.common.collect.Streams;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.EmptyAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.FillAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.particles.CauldronSwirlParticleOptions;
import live.kadalyn.unifiedbrewing.recipes.MixFinder.Normalized;
import live.kadalyn.unifiedbrewing.recipes.PotionAsIngredient;
import live.kadalyn.unifiedbrewing.registries.UBFluids;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@EventBusSubscriber(modid = UnifiedBrewing.MODID, value = Dist.CLIENT)
public class IronsCompat {
	@SubscribeEvent
	public static void registerClient(RegisterClientExtensionsEvent event) {
		var modList = ModList.get();
		if (modList.isLoaded("irons_spellbooks")) {
			UnifiedBrewing.LOGGER.info("irons_spellbooks is loaded");
			if (modList.isLoaded("hexalia")) {
				UnifiedBrewing.LOGGER.info("hexalia is loaded");
				event.registerFluidType(new live.kadalyn.unifiedbrewing.fluids.BrewFluidClientType(
					ResourceLocation.withDefaultNamespace("block/water_still")), UBFluids.BREW_FLUID_TYPE);
			}
		}
	}

	public static boolean isLoaded() {
		return ModList.get().isLoaded("irons_spellbooks");
	}

	public static boolean isScroll(ItemStack itemStack) {
		if (isLoaded())
			return itemStack.is(ItemRegistry.SCROLL);
		return false;
	}

	public static boolean isBlood(FluidStack fluidStack) {
		if (isLoaded())
			return fluidStack.is(FluidRegistry.BLOOD);
		return false;
	}

	public static void makeSwirlParticles(Level level, BlockPos pos) {
		var blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile tile) {
			float waterLevel = Mth.lerp(tile.getFluidAmount() / 1000f, .25f, .9f);
			io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level, CauldronSwirlParticleOptions.create(pos, 0.3f),
				pos.getX() + 0.5f, pos.getY() + waterLevel + 0.05, pos.getZ() + 0.5f,
				1, 0, 0, 0, 0, false);
		}
	}

	public static ItemStack from(FluidStack stack) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return from(stack, server.getLevel(Level.OVERWORLD));
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack from(FluidStack stack, ItemStack bottle) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return from(stack, bottle, server.getLevel(Level.OVERWORLD));
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack from(FluidStack stack, Level level) {
		if (!isLoaded()) return ItemStack.EMPTY;
		if (stack.is(FluidRegistry.POTION_FLUID))
			return PotionFluid.from(stack);

		return from(stack, new ItemStack(Items.GLASS_BOTTLE), level);
	}

	public static ItemStack from(FluidStack stack, ItemStack bottle, Level level) {
		// Presumes 250+ in fluid stack, ignoring overall amount tho
		var emptyRecipeInput = new EmptyAlchemistCauldronRecipe.Input(bottle, stack);
		var emptyRecipe = level.getRecipeManager().getRecipeFor(
			RecipeRegistry.ALCHEMIST_CAULDRON_EMPTY_TYPE.get(), emptyRecipeInput, level);
		if (emptyRecipe.isPresent())
			return emptyRecipe.get().value().getResultItem(null);

		return ItemStack.EMPTY;
	}

	public static FluidStack from(ItemStack stack) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return from(stack, server.getLevel(Level.OVERWORLD));
		}
		return FluidStack.EMPTY;
	}

	public static FluidStack from(ItemStack stack, Level level) {
		if (!isLoaded()) return FluidStack.EMPTY;
		var fillRecipe = fillRecipeFor(stack, level);
		if (fillRecipe.isPresent())
			return fillRecipe.get().value().result().copy();

		if (stack.is(Tags.Items.POTIONS)) {
			var potionContents = stack.get(DataComponents.POTION_CONTENTS);
			if (potionContents != null && potionContents.is(Potions.WATER))
				return new FluidStack(Fluids.WATER, 250);
			return PotionFluid.from(stack);
		}

		return FluidStack.EMPTY;
	}

	public static ItemStack getBottle(ItemStack stack) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return getBottle(stack, server.getLevel(Level.OVERWORLD));
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack getBottle(ItemStack stack, Level level) {
		var fillRecipe = fillRecipeFor(stack, level);
		if (fillRecipe.isPresent()) {
			var bottle = fillRecipe.get().value().returned().copy();
			if (bottle.is(UBTags.EMPTY_POTION_BOTTLE))
				return bottle;
		}

		return ItemStack.EMPTY;
	}

	public static ItemStack getBloodVial(int count) {
		if (isLoaded())
			return new ItemStack(ItemRegistry.BLOOD_VIAL, count);
		return ItemStack.EMPTY;
	}

	private static Optional<RecipeHolder<FillAlchemistCauldronRecipe>> fillRecipeFor(ItemStack stack, Level level) {
		SingleRecipeInput fillRecipeInput = new SingleRecipeInput(stack);
		return level.getRecipeManager().getRecipeFor(
			RecipeRegistry.ALCHEMIST_CAULDRON_FILL_TYPE.get(), fillRecipeInput, level);
	}

	public static List<Predicate<ItemStack>> getAllIngredients(Level level) {
		if (!isLoaded()) return List.of();
		var recipes = level.getRecipeManager().getAllRecipesFor(RecipeRegistry.ALCHEMIST_CAULDRON_BREW_TYPE.get());
		return Streams.concat(
			recipes.stream()
				.map(r -> r.value().fluidIn())
				.map(Normalized::from)
				.map(Normalized::originalItem)
				.filter(is -> !is.isEmpty())
				.map(PotionAsIngredient::new),
			recipes.stream()
				.map(r -> r.value().reagent())
		).toList();
	}
}
