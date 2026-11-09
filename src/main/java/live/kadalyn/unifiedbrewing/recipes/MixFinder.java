package live.kadalyn.unifiedbrewing.recipes;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.BrewAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import live.kadalyn.unifiedbrewing.Config;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat.CauldronRecipeInput;
import live.kadalyn.unifiedbrewing.compat.HexaliaCompat;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.fluids.BrewFluid;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import live.kadalyn.unifiedbrewing.registries.UBFluids;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.astralya.hexalia.recipe.ModRecipeTypes;
import net.astralya.hexalia.recipe.SmallCauldronRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.satisfy.herbalbrews.core.registry.RecipeTypeRegistry;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record MixFinder(Normalized base, List<Normalized> inputs, ItemStack powder) {
	public static MixFinder from(MutableDataComponentHolder base, List<? extends MutableDataComponentHolder> inputs) {
		var normInputs = new ArrayList<Normalized>();
		var powder = ItemStack.EMPTY;
		for (var input : inputs) {
			if (input instanceof ItemStack itemStack && itemStack.is(UBTags.BLAZING_PROVIDER))
				powder = itemStack;
			else
				normInputs.add(Normalized.from(input));
		}
		return new MixFinder(Normalized.from(base), normInputs, powder);
	}

	public MixResult findResult(Level level) {
		return findResult(level, true);
	}

	public MixResult findResult(Level level, boolean blazed) {
		// Vanilla and Irons
		var result = findIncrementalBrewingResult(level, blazed);
		if (!result.isEmpty()) return result;
		if (HexaliaCompat.isLoaded()) {
			result = findSmallCauldronResult(level);
			if (!result.isEmpty()) return result;
		}
		if (HerbalBrewsCompat.isLoaded()) {
			result = findBrewingCauldronResult(level);
			if (!result.isEmpty()) return result;
		}
		return MixResult.EMPTY;
	}

	public MixResult findIncrementalBrewingResult(Level level) {
		return findIncrementalBrewingResult(level, true);
	}

	// Can find combinations of vanilla and Iron's recipes
	public MixResult findIncrementalBrewingResult(Level level, boolean blazed) {
		var ironsLoaded = IronsCompat.isLoaded();
		if (ironsLoaded) {
			var inkResult = findAlchemistCauldronInkExtraction();
			if (!inkResult.isEmpty())
				return inkResult;
		}
		if (Config.ALLOW_VANILLA.getAsBoolean() && (!Config.REQUIRE_BLAZING.getAsBoolean() || blazed))
			if (ironsLoaded)
				return findIncrementalMix(level, new MixResult(base, base, List.of(), 0, 0), this.inputs);
			else
				return findBrewingStandResult(level);
		else if (ironsLoaded)
			return findAlchemistCauldronResult(level);
		return MixResult.EMPTY;
	}

	private MixResult findIncrementalMix(Level level, MixResult intoPotion, List<Normalized> remaining) {
		PotionBrewing brewing = level.potionBrewing();
		for (var mix : remaining) {
			var nextRemaining = remaining.stream().filter(m -> m != mix).toList();
			for (var newMix : List.of(
				isIncrementalBrewingStandMix(brewing, intoPotion, mix),
				isIncrementalAlchemistCauldronMix(level, intoPotion, mix)
			)) {
				if (!newMix.isEmpty()) {
					if (nextRemaining.isEmpty())
						return newMix;
					var newResult = findIncrementalMix(level, newMix, nextRemaining);
					if (!newResult.isEmpty()) return newResult;
				}
			}
		}
		return MixResult.EMPTY;
	}

	private MixResult isIncrementalBrewingStandMix(PotionBrewing brewing, MixResult intoPotion, Normalized mix) {
		if (brewing.isIngredient(mix.ingredient)) {
			ItemStack result = brewing.mix(mix.ingredient, intoPotion.result.originalItem);
			if (!result.isEmpty() && result != intoPotion.result.originalItem) {
				var mb = Math.min(intoPotion.result.originalFluid.getAmount(), 1000);
				var mixed = new ArrayList<>(intoPotion.mixed);
				result.setCount(mb / 250);
				// only 1 required to brew into the entire base
				mixed.add(mix.copyWithAmount(250));
				var charges = HerbalBrewsCompat.isInfusion(mix.ingredient) ? 0 : 1;
				return new MixResult(
					Normalized.from(result), intoPotion.base, mixed,
					intoPotion.ticksToBrew + 400,
					intoPotion.blazeCharges + charges);
			}
		}
		return MixResult.EMPTY;
	}

	private MixResult isIncrementalAlchemistCauldronMix(Level level, MixResult intoPotion, Normalized mix) {
		var recipeInput = new BrewAlchemistCauldronRecipe.Input(intoPotion.result.originalFluid, mix.ingredient);
		var recipeHolder = level.getRecipeManager().getRecipeFor(
			RecipeRegistry.ALCHEMIST_CAULDRON_BREW_TYPE.get(),
			recipeInput, level);
		if (recipeHolder.isPresent() && recipeHolder.get().value() instanceof BrewAlchemistCauldronRecipe recipe) {
			if (intoPotion.result.originalFluid.getAmount() < recipe.fluidIn().getAmount())
				return MixResult.EMPTY;

			Normalized product = null;
			var fluidResults = recipe.results();
			if (fluidResults.isEmpty()) {
				var byproduct = recipe.byproduct();
				if (byproduct.isPresent() && !byproduct.get().isEmpty())
					product = Normalized.from(byproduct.get());
			} else if (fluidResults.size() == 1) {
				var fluid = fluidResults.getFirst();
				product = Normalized.from(fluid);
			}
			if (product != null) {
				var mixedIn = Stream.concat(intoPotion.mixed.stream(), Stream.of(mix)).toList();
				return new MixResult(
					product, intoPotion.base, mixedIn,
					intoPotion.ticksToBrew + 100,
					intoPotion.blazeCharges);
			}
		}
		return MixResult.EMPTY;
	}

	public MixResult findBrewingStandResult(Level level) {
		PotionBrewing pb = level.potionBrewing();
		return findBrewingStandMix(pb, new MixResult(base, base, List.of(), 0, 0), inputs);
	}

	private MixResult findBrewingStandMix(
		PotionBrewing brewing,
		MixResult intoPotion,
		List<Normalized> remaining
	) {
		for (var mix : remaining) {
			var result = isIncrementalBrewingStandMix(brewing, intoPotion, mix);
			if (!result.isEmpty()) {
				var nextRemaining = remaining.stream().filter(m -> m != mix).toList();
				if (nextRemaining.isEmpty())
					return result;
				var newResult = findBrewingStandMix(brewing, result, nextRemaining);
				if (!newResult.isEmpty())
					return newResult;
			}
		}
		return MixResult.EMPTY;
	}

	public MixResult findAlchemistCauldronResult(Level level) {
		var inkResult = findAlchemistCauldronInkExtraction();
		if (!inkResult.isEmpty())
			return inkResult;
		return findAlchemistCauldronMix(level, new MixResult(base, base, List.of(), 0, 0), this.inputs);
	}

	private MixResult findAlchemistCauldronInkExtraction() {
		// Extract ink
		if (
			base.originalFluid.is(Fluids.WATER)
			&& inputs.stream().allMatch(i -> i.originalItem.is(ItemRegistry.SCROLL))
		) {
			// Ensure all same rarity
			SpellRarity baseRarity = null;
			for (var scrollStack : inputs) {
				var spellData = ISpellContainer.get(scrollStack.originalItem).getSpellAtIndex(0);
				SpellRarity rarity = spellData.getSpell().getRarity(spellData.getLevel());
				if (baseRarity == null)
					baseRarity = rarity;
				else if (rarity != baseRarity)
					return MixResult.EMPTY;
			}
			if (baseRarity == null)
				return MixResult.EMPTY;

			int count = Math.min(base.originalItem.getCount(), inputs.size());
			List<Normalized> mixed = new ArrayList<>();
			int inkAmount = 0;
			for (int i = 0; i < count; i++) {
				var scroll = inputs.get(i);
				mixed.add(scroll);
				if (Utils.random.nextFloat() < ServerConfigs.SCROLL_RECYCLE_CHANCE.get()) {
					inkAmount++;
				}
			}

			var ink = InkItem.getInkForRarity(baseRarity).getDefaultInstance();
			ink.setCount(inkAmount);
			UnifiedBrewing.LOGGER.info("  amount earned {}",  inkAmount);
			return new MixResult(
				inkAmount > 0
					? Normalized.from(ink)
					: Normalized.EMPTY,
				base.copyWithAmount(inkAmount * 250),
				mixed, 100 * inputs.size(), 0
			);
		}
		return MixResult.EMPTY;
	}

	private MixResult findAlchemistCauldronMix(
		Level level,
		MixResult intoPotion,
		List<Normalized> remaining
	) {
		for (var mix : remaining) {
			var result = isIncrementalAlchemistCauldronMix(level, intoPotion, mix);
			if (!result.isEmpty()) {
				var nextRemaining = remaining.stream().filter(m -> m != mix).toList();
				if (nextRemaining.isEmpty())
					return result;
				var newResult = findAlchemistCauldronMix(level, result, nextRemaining);
				if (!newResult.isEmpty())
					return newResult;
			}
		}
		return MixResult.EMPTY;
	}

	public MixResult findSmallCauldronResult(Level level) {
		var optRecipe = level.getRecipeManager().getRecipeFor(
			ModRecipeTypes.SMALL_CAULDRON.value(),
			new SmallCauldronRecipeInput(inputs.stream().map(Normalized::ingredient).toList()),
			level
		);
		if (optRecipe.isPresent()) {
			var recipe = optRecipe.get().value();
			return standardRecipeOutput(
				recipe.getResultItem(level.registryAccess()).copyWithCount(2),
				recipe.getBrewTime());
		}
		return MixResult.EMPTY;
	}

	public MixResult findBrewingCauldronResult(Level level) {
		var optRecipe = level.getRecipeManager().getRecipeFor(
			RecipeTypeRegistry.CAULDRON_RECIPE_TYPE.value(),
			new CauldronRecipeInput(inputs.stream().map(Normalized::ingredient).toList()),
			level
		);
		if (optRecipe.isPresent()) {
			var recipe = optRecipe.get().value();
			return standardRecipeOutput(
				recipe.getResultItem(level.registryAccess()),
				HerbalBrewsCompat.getBrewTime());
		}
		return MixResult.EMPTY;
	}

	private MixResult standardRecipeOutput(ItemStack output, int brewTime) {
		if (!base.originalItem.is(Util.bottleOf(output).getItem()))
			return MixResult.EMPTY;

		return new MixResult(Normalized.from(output), base, inputs, brewTime, 0);
	}

	public boolean damagePowder(MixResult result) {
		int count = result.blazeCharges - result.numBlazing();
		if (count <= 0)
			return true;
		if (powder.isEmpty())
			return false;
		if (powder.getDamageValue() + count < powder.getMaxDamage()) {
			powder.setDamageValue(powder.getDamageValue() + count);
			return true;
		}
		return false;
	}

	public boolean powderGone() {
		return !powder.isEmpty() && powder.getDamageValue() >= powder.getMaxDamage();
	}

	public record Normalized(
		ItemStack ingredient,
		ItemStack originalItem,
		FluidStack originalFluid
	) {
		public static final Normalized EMPTY = new Normalized(ItemStack.EMPTY, ItemStack.EMPTY, FluidStack.EMPTY);

		public boolean isEmpty() {
			return ingredient.isEmpty() && originalItem.isEmpty() && originalFluid.isEmpty();
		}

		public static Normalized from(MutableDataComponentHolder input) {
			PotionContents potionContents = input.get(DataComponents.POTION_CONTENTS);
			UBDataComponents.SingleItemComponent mi = input.get(UBDataComponents.MUNDANE_INGREDIENT.get());
			ItemStack ingredient = mi != null ? mi.getItem() : ItemStack.EMPTY;

			if (mi == null && potionContents != null) {
				if (potionContents.is(Potions.AWKWARD))
					ingredient = Items.NETHER_WART.getDefaultInstance();
				else if (potionContents.is(Potions.THICK))
					ingredient = Items.GLOWSTONE_DUST.getDefaultInstance();
			}

			if (input instanceof ItemStack itemStack) {
				if (ingredient.isEmpty()) ingredient = itemStack;
				var fluidStack = FluidStack.EMPTY;
				if (IronsCompat.isLoaded()) {
					// We only care about the fluid for Alchemist Cauldron recipes
					fluidStack = IronsCompat.from(itemStack);
					if (fluidStack.isEmpty() && HexaliaCompat.isLoaded())
						if (itemStack.is(net.astralya.hexalia.util.ModTags.Items.BREWS))
							fluidStack = BrewFluid.from(itemStack);
				}
				else if (potionContents != null && potionContents.is(Potions.WATER)) {
					// and if it's water... (IronsCompat also handles water)
					fluidStack = new FluidStack(Fluids.WATER, 250);
				}

				if (!fluidStack.isEmpty()) {
					fluidStack.setAmount(mbPerBottle(itemStack) * itemStack.getCount());
				}
				return new Normalized(ingredient, itemStack, fluidStack);
			}
			else if (input instanceof FluidStack fluidStack) {
				ItemStack original = ItemStack.EMPTY;
				if (fluidStack.is(Fluids.WATER))
					original = PotionContents.createItemStack(Items.POTION, Potions.WATER);
				else {
					if (IronsCompat.isLoaded())
						original = IronsCompat.from(fluidStack);
					if (original.isEmpty() && HexaliaCompat.isLoaded())
						if (fluidStack.is(UBFluids.BREW_FLUID))
							original = BrewFluid.from(fluidStack);
				}

				if (!original.isEmpty()) {
					original.setCount(fluidStack.getAmount() / mbPerBottle(original));
				}
				return new Normalized(ingredient, original, fluidStack);
			}
			else
				throw new InvalidParameterException("MixFinder expects input of either ItemStack or FluidStack");
		}

		public Normalized copyWithAmount(int amount) {
			UnifiedBrewing.LOGGER.info("{}.copyWithAmount({})", this, amount);
			if (amount < 100 || originalFluid.isEmpty() && (amount /= 250) > 0) {
				if (!originalItem.isEmpty())
					return Normalized.from(originalItem.copyWithCount(amount));
				amount *= 250;
			}
			if (originalFluid.isEmpty())
				return Normalized.EMPTY;
			return Normalized.from(originalFluid.copyWithAmount(amount));
		}

		public boolean isBlazing() {
			return Util.blazeTouched(originalItem) > 0
				|| Util.blazeTouched(originalFluid) > 0;
		}

		public static int mbPerBottle(ItemStack itemStack) {
			if (HexaliaCompat.isRusticBottle(itemStack))
				return 500;
			if (HerbalBrewsCompat.isFlask(itemStack))
				return 1000;
			return 250;
		}

		public int mbPerBottle() {
			if (HexaliaCompat.isRusticBottle(originalItem))
				return 500;
			if (HerbalBrewsCompat.isFlask(originalItem))
				return 1000;
			return 250;
		}

		public static Normalized load(Tag tag, HolderLookup.Provider registryAccess) {
			if (tag instanceof CompoundTag root) {
				if (!root.contains("Fluid") || (tag = root.get("Fluid")) == null) {
					Tag itemTag;
					if (root.contains("Item") && (itemTag = root.get("Item")) != null) {
						var is = ItemStack.parse(registryAccess, itemTag);
						if (is.isPresent())
							return Normalized.from(is.get());
					}
					return Normalized.EMPTY;
				}
			}
			var fs = FluidStack.parse(registryAccess, tag);
			if (fs.isPresent())
				return Normalized.from(fs.get());
			return Normalized.EMPTY;
		}

		public Tag save(HolderLookup.Provider registryAccess) {
			var root = new CompoundTag();
			if (!originalFluid.isEmpty())
				root.put("Fluid", originalFluid.save(registryAccess));
			else if (!originalItem.isEmpty())
				root.put("Item", originalItem.save(registryAccess));
			return root;
		}

		@Override
		public @NotNull String toString() {
			if (isEmpty()) return "Normalized.EMPTY";
			var args = new ArrayList<String>();
			if (!ingredient.isEmpty()) args.add("ingredient=" + ingredient);
			if (!originalItem.isEmpty()) args.add("originalItem=" + originalItem);
			if (!originalFluid.isEmpty()) args.add("originalFluid=" + originalFluid);
			return "Normalized[" + String.join(", ", args) + "]";
		}
	}

	public record MixResult(Normalized result, Normalized base, List<Normalized> mixed, int ticksToBrew, int blazeCharges) {
		public static final MixResult EMPTY = new MixResult(Normalized.EMPTY, Normalized.EMPTY, List.of(), 0, 0);

		public boolean isEmpty() {
			return result.isEmpty() && base.isEmpty() && mixed.isEmpty();
		}

		public int consumeBottles() {
			if (!base.originalItem.isEmpty())
				return base.originalItem.getCount();
			if (!base.originalFluid.isEmpty())
				return base.originalFluid.getAmount() / result.mbPerBottle();
			return 0;
		}

		public void finishCooking(Level level, BlockPos pos) {
			var blockEntity = level.getBlockEntity(pos);
			if (IronsCompat.isLoaded() && blockEntity instanceof AlchemistCauldronTile tile) {
				int times = 0;
				while (canDrain(tile.fluidInventory)) {
					drain(tile.fluidInventory);
					times++;
				}

				var usedInputItems = new ArrayList<ItemStack>();
				for (var m : mixed) {
					if (m.originalItem.isEmpty()) continue;
					var item = m.originalItem.getItem();
					for (int i = 0; i < 4; ++i) {
						var input = tile.inputItems.get(i);
						if (input.is(item)) {
							usedInputItems.add(input);
							tile.inputItems.set(i, ItemStack.EMPTY);
						}
					}
				}

				var output = result.originalFluid;
				if (times > 1)
					output = output.copyWithAmount(output.getAmount() * times);
				UnifiedBrewing.LOGGER.info("output {}", output);
				// drain all the water
				tile.fluidInventory.drain(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

				// Attempt to mix ingredients into remaining fluids
				// (this is for gunpowder etc)
				if (tile.fluidInventory.fluidAmount() > 0 && ServerConfigs.ALLOW_CAULDRON_BREWING.get()) {
					for (var fluid : tile.fluidInventory.fluids()) {
						var res = MixFinder.from(fluid, usedInputItems).findBrewingStandResult(level);
						if (!res.isEmpty()) {
							tile.fluidInventory.drain(res.base.originalFluid, IFluidHandler.FluidAction.EXECUTE);
							tile.fluidInventory.fill(res.result.originalFluid, IFluidHandler.FluidAction.EXECUTE);
						}
					}
				}

				// Fill with output
				tile.fluidInventory.fill(output, IFluidHandler.FluidAction.EXECUTE);
			}
		}

		private boolean canDrain(IFluidHandler fluidInventory) {
			return drain(fluidInventory, IFluidHandler.FluidAction.SIMULATE);
		}

		private void drain(IFluidHandler fluidInventory) {
			drain(fluidInventory, IFluidHandler.FluidAction.EXECUTE);
		}

		private boolean drain(IFluidHandler fluidInventory, IFluidHandler.FluidAction action) {
			var result = fluidInventory.drain(base.originalFluid, action);
			if (result.getAmount() < base.originalFluid.getAmount())
				return false;
			for (var mix : mixed) {
				if (mix.originalFluid.isEmpty()) continue;
				result = fluidInventory.drain(mix.originalFluid, action);
				if (result.getAmount() < mix.originalFluid.getAmount())
					return false;
			}
			return true;
		}

		public int numBlazing() {
			return mixed.stream().mapToInt(x -> x.isBlazing() ? 1 : 0).sum();
		}

		public static MixResult load(CompoundTag tag, HolderLookup.Provider registryAccess) {
			var result = Normalized.load(tag, registryAccess);
			var base = Normalized.load(tag.get("Base"), registryAccess);
			var list = new ArrayList<Normalized>();
			for (var mix : tag.getList("Mixed", 10)) {
				var norm = Normalized.load(mix, registryAccess);
				if (!norm.isEmpty())
					list.add(norm);
			}
			return new MixResult(
				result, base, list,
				tag.getInt("TicksToBrew"),
				tag.getInt("BlazeCharges")
			);
		}

		public CompoundTag save(HolderLookup.Provider registryAccess) {
			var root = new CompoundTag();
			var list = new ListTag();
			root.put("Result", result.save(registryAccess));
			for (var mix : mixed) {
				list.add(mix.save(registryAccess));
			}
			root.put("Base", base.save(registryAccess));
			root.put("Mixed", list);
			root.putInt("TicksToBrew", ticksToBrew);
			root.putInt("BlazeCharges", blazeCharges);
			return root;
		}
	}
}
