package live.kadalyn.unifiedbrewing.mixin.irons;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.EmptyAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.recipe_types.alchemist_cauldron.FillAlchemistCauldronRecipe;
import io.redspace.ironsspellbooks.registries.RecipeRegistry;
import live.kadalyn.unifiedbrewing.Config;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import live.kadalyn.unifiedbrewing.compat.HexaliaCompat;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.fluids.BrewFluid;
import live.kadalyn.unifiedbrewing.particles.SwirlParticle;
import live.kadalyn.unifiedbrewing.recipes.MixFinder;
import live.kadalyn.unifiedbrewing.registries.*;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.recipes.HexaliaRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(AlchemistCauldronTile.class)
public abstract class AlchemistCauldronTileMixin {
	@Shadow
	@Final
	private int[] cooktimes;

	@Shadow
	public AlchemistCauldronTile.AlchemistCauldronFluidHandler fluidInventory;
	@Shadow
	@Final
	public NonNullList<ItemStack> inputItems;
	@Unique
	public int unifiedbrewing$cookTime = 0;
	@Unique
	public int unifiedbrewing$noIngredientCookTimeSoFar = 0;
	@Unique
	public MixFinder.MixResult unifiedbrewing$mixResult;

	// Brewing hooks
	@Inject(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		at = @At("HEAD")
	)
	private void tryMeltInput(
		ItemStack itemStack,
		CallbackInfo ci,
		@Share("itemStack") LocalRef<ItemStack> itemStackRef
	) {
		itemStackRef.set(itemStack);
	}

	@ModifyConstant(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		constant = @Constant(intValue = 0, ordinal = 0)
	)
	private int tryMeltInput$tryInfusion(
		int constant,
		@Share("itemStack") LocalRef<ItemStack> itemStackRef
	) {
		var itemStack = itemStackRef.get();
		if (itemStack != null && HerbalBrewsCompat.isInfusion(itemStack)) {
			var water = fluidInventory.drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
			if (water.getAmount() > 0) {
				var fluid = IronsCompat.from(PotionContents.createItemStack(Items.POTION, UBPotions.HERBAL_TEA));
				fluid.setAmount(water.getAmount());
				fluidInventory.fill(fluid, FluidAction.EXECUTE);
				return 1;
			}
			return 0;
		}
		return constant;
	}

	@WrapOperation(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		at = @At(
			value = "INVOKE",
			target = "Lio/redspace/ironsspellbooks/block/alchemist_cauldron/AlchemistCauldronTile$AlchemistCauldronFluidHandler;contains(Lnet/neoforged/neoforge/fluids/FluidStack;I)Z"
		)
	)
	private boolean tryMeltInput$contains(
		AlchemistCauldronTile.AlchemistCauldronFluidHandler instance,
		FluidStack stack,
		int minAmount,
		Operation<Boolean> original
	) {
		((Util.IAlchemistCauldronFluidHandlerExtras) (Object) instance).unifiedbrewing$setIgnores(true);
		var res = original.call(instance, stack, minAmount);
		((Util.IAlchemistCauldronFluidHandlerExtras) (Object) instance).unifiedbrewing$setIgnores(false);
		return res;
	}

	@WrapOperation(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		at = @At(
			value = "INVOKE",
			target = "Lio/redspace/ironsspellbooks/block/alchemist_cauldron/AlchemistCauldronTile$AlchemistCauldronFluidHandler;drain(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"
		)
	)
	private FluidStack tryMeltInput$drain(
		AlchemistCauldronTile.AlchemistCauldronFluidHandler instance,
		FluidStack resource,
		FluidAction action,
		Operation<FluidStack> original
	) {
		((Util.IAlchemistCauldronFluidHandlerExtras) (Object) instance).unifiedbrewing$setIgnores(true);
		var res = original.call(instance, resource, action);
		((Util.IAlchemistCauldronFluidHandlerExtras) (Object) instance).unifiedbrewing$setIgnores(false);
		return res;
	}

	@ModifyArg(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		at = @At(
			value = "INVOKE",
			target = "Lio/redspace/ironsspellbooks/block/alchemist_cauldron/AlchemistCauldronTile$AlchemistCauldronFluidHandler;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"
		)
	)
	private FluidStack tryMeltInput$fill(
		FluidStack resource,
		@Share("itemStack") LocalRef<ItemStack> itemStack
	) {
		PotionContents contents = resource.get(DataComponents.POTION_CONTENTS);
		if (contents != null && contents.is(Potions.MUNDANE)) {
			resource.set(UBDataComponents.MUNDANE_INGREDIENT.get(), UBDataComponents.SingleItemComponent.of(itemStack.get()));
		}
		return resource;
	}

	@WrapOperation(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		at = @At(
			value = "INVOKE",
			target = "Lio/redspace/ironsspellbooks/fluids/PotionFluid;from(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"
		)
	)
	private ItemStack tryMeltInput$from(
		FluidStack stack,
		Operation<ItemStack> original
	) {
		var potion = original.call(stack);
		potion.set(UBDataComponents.BLAZE_TOUCHED, stack.get(UBDataComponents.BLAZE_TOUCHED));
		return potion;
	}

	@WrapOperation(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/alchemy/PotionBrewing;hasPotionMix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
		)
	)
	private boolean tryMeltInput$hasPotionMix(
		PotionBrewing instance,
		ItemStack reagent,  // this is the potion
		ItemStack potionItem,  // this is the item being inserted
		Operation<Boolean> original
	) {
		if (!original.call(instance, reagent, potionItem))
			return false;
		if (Config.REQUIRE_BLAZING.getAsBoolean()) {
			if (Boolean.TRUE.equals(reagent.get(UBDataComponents.BLAZE_TOUCHED)))
				return true;
			for (int i = 0; i < inputItems.size(); ++i) {
				var item = inputItems.get(i);
				if (item == potionItem) continue;
				if (item.is(UBTags.BLAZING_PROVIDER)) {
					int dmg = item.getDamageValue() + 1;
					item.setDamageValue(dmg);
					if (dmg >= item.getMaxDamage()) {
						inputItems.set(i, ItemStack.EMPTY);
					}
					return true;
				}
				if (item.is(Items.BLAZE_POWDER)) {
					var depleted = new ItemStack(UBItems.DEPLETED_BLAZE_POWDER);
					depleted.setDamageValue(1);
					inputItems.set(i, depleted);
					return true;
				}
			}
		}
		return false;
	}

	@WrapOperation(
		method = "tryMeltInput(Lnet/minecraft/world/item/ItemStack;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/alchemy/PotionBrewing;hasContainerMix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
		)
	)
	private boolean tryMeltInput$hasContainerMix(
		PotionBrewing instance,
		ItemStack reagent,  // this is the potion
		ItemStack potionItem,  // this is the item being inserted
		Operation<Boolean> original
	) {
		if (!original.call(instance, reagent, potionItem))
			return false;
		if (Config.REQUIRE_BLAZING.getAsBoolean()) {
			if (Boolean.TRUE.equals(reagent.get(UBDataComponents.BLAZE_TOUCHED)))
				return true;
			for (int i = 0; i < inputItems.size(); ++i) {
				var item = inputItems.get(i);
				if (item.is(UBTags.BLAZING_PROVIDER)) {
					int dmg = item.getDamageValue() + 1;
					item.setDamageValue(dmg);
					if (dmg >= item.getMaxDamage()) {
						inputItems.set(i, ItemStack.EMPTY);
					}
					return true;
				}
			}
		}
		return false;
	}

	@WrapMethod(method = "isValidInput(Lnet/minecraft/world/item/ItemStack;)Z")
	public boolean isValidInput(ItemStack itemStack, Operation<Boolean> original) {
		return original.call(itemStack) || HerbalBrewsCompat.isInfusion(itemStack) || HexaliaRecipes.isValidInput(itemStack.getItem()) || itemStack.is(UBTags.BLAZING_PROVIDER);
	}

	// Fill/drain recipe hooks
	@Inject(
		method = "tryExecuteRecipeInteractions(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
		at = @At("HEAD")
	)
	private void tryExecuteRecipeInteractions(
		Level level,
		ItemStack itemStack,
		CallbackInfoReturnable<ItemStack> cir,
		@Share("itemStackFD") LocalRef<ItemStack> itemStackRef
	) {
		itemStackRef.set(itemStack);
	}

	@WrapOperation(
		method = "tryExecuteRecipeInteractions(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
		),
		remap = false
	)
	private <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> tryExecuteRecipeInteractions$getRecipeFor(
		RecipeManager instance,
		RecipeType<T> recipeType,
		I input,
		Level level,
		Operation<Optional<RecipeHolder<T>>> original,
		@Share("itemStackFD") LocalRef<ItemStack> itemStackRef
	) {
		var itemStack = itemStackRef.get();
		if (itemStack != null) {
			if (recipeType.equals(RecipeRegistry.ALCHEMIST_CAULDRON_FILL_TYPE.get())) {
				if (HexaliaCompat.isFullRusticBottle(itemStack)) {
					var fluid = BrewFluid.of(500, itemStack);
					@SuppressWarnings("unchecked")
					T recipe = (T) new FillAlchemistCauldronRecipe(Ingredient.of(itemStack), HexaliaCompat.rusticBottle(), fluid, true, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BOTTLE_EMPTY));
					return Util.recipeHolderOf(recipe);
				}
				else if (HerbalBrewsCompat.isFullFlask(itemStack)) {
					var fluids = HerbalBrewsCompat.flaskToFluids(itemStack);
					if (this.fluidInventory.canFit(fluids.stream().map(FluidStack::getAmount).reduce(Integer::sum).orElse(0))) {
						var bottle = HerbalBrewsCompat.getFlaskCounterpart(itemStack);
						var top = fluids.removeLast();
						fluids.forEach(fs -> this.fluidInventory.fill(fs, FluidAction.EXECUTE));
						@SuppressWarnings("unchecked")
						T recipe = (T) new FillAlchemistCauldronRecipe(Ingredient.of(itemStack), bottle, top, true, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BOTTLE_EMPTY));
						return Util.recipeHolderOf(recipe);
					}
				}
			} else if (input instanceof EmptyAlchemistCauldronRecipe.Input emptyRecipeInput) {
				if (
					HexaliaCompat.isEmptyRusticBottle(itemStack)
					&& emptyRecipeInput.fluid().getAmount() >= 500
				) {
					ItemStack brewStack = BrewFluid.from(emptyRecipeInput.fluid());
					if (!brewStack.isEmpty()) {
						@SuppressWarnings("unchecked")
						T recipe = (T) new EmptyAlchemistCauldronRecipe(Ingredient.of(itemStack), brewStack, emptyRecipeInput.fluid().copyWithAmount(500), BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BOTTLE_FILL));
						return Util.recipeHolderOf(recipe);
					}
				}
				else if (HerbalBrewsCompat.isEmptyFlask(itemStack)) {
					var flaskId = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);
					if (flaskId == null) flaskId = new CustomModelData(0);
					var fluids = this.fluidInventory.fluids();
					if (
						// Top fluid has to be one of the valid liquids
						emptyRecipeInput.fluid().has(DataComponents.POTION_CONTENTS)
						// One fluid must be mundane herbal tea
						&& fluids.stream()
							.map(f -> f.get(DataComponents.POTION_CONTENTS))
							.anyMatch(pc -> pc != null && pc.is(UBPotions.HERBAL_TEA))
					) {
						// Collect 250 mB of all valid fluids except the top (future code does that)
						var top = emptyRecipeInput.fluid();
						var valid = new ArrayList<>(fluids.stream().filter(fs ->
							fs.has(DataComponents.POTION_CONTENTS)
							&& fs.getAmount() >= 250
						).map(fs -> fs.copyWithAmount(250)).toList());
						// Mutates valid so we remove everything that wasn't used
						var result = HerbalBrewsCompat.stuffToFlask(valid, flaskId.value());
						// top was already drained
						valid.stream().filter(fs -> !FluidStack.matches(fs, top)).forEach(fs -> {
							this.fluidInventory.drain(fs, FluidAction.EXECUTE);
						});

						@SuppressWarnings("unchecked")
						T recipe = (T) new EmptyAlchemistCauldronRecipe(Ingredient.of(itemStack), result, top.copyWithAmount(250), BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BOTTLE_FILL));
						return Util.recipeHolderOf(recipe);
					}
				}
			}
		}
		return original.call(instance, recipeType, input, level);
	}

	@WrapOperation(
		method = "tryExecuteRecipeInteractions(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
		at = @At(
			value = "INVOKE",
			target = "Lio/redspace/ironsspellbooks/block/alchemist_cauldron/AlchemistCauldronTile$AlchemistCauldronFluidHandler;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"
		)
	)
	private int tryExecuteRecipeInteractions$fill(
		AlchemistCauldronTile.AlchemistCauldronFluidHandler instance,
		FluidStack resource,
		FluidAction action,
		Operation<Integer> original,
		@Share("itemStackFD") LocalRef<ItemStack> itemStackRef
	) {
		var executing = action.equals(FluidAction.EXECUTE);
		ItemStack itemStack = itemStackRef.get();
		if (executing)
			// Blaze-Touched on items shouldn't happen normally, but respecting it anyway
			Util.copyComponents(resource, itemStack, true);
		return original.call(instance, resource, action);
	}

	@WrapOperation(
		method = "tryExecuteRecipeInteractions(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
		at = @At(
			value = "INVOKE",
			target = "Lio/redspace/ironsspellbooks/block/alchemist_cauldron/AlchemistCauldronTile$AlchemistCauldronFluidHandler;drain(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"
		)
	)
	private FluidStack tryExecuteRecipeInteractions$drain(
		AlchemistCauldronTile.AlchemistCauldronFluidHandler instance,
		FluidStack resource,
		FluidAction action,
		Operation<FluidStack> original,
		@Share("fluidStack") LocalRef<FluidStack> fluidStackRef
	) {
		var fluidStack = original.call(instance, resource, action);
		fluidStackRef.set(fluidStack);
		return fluidStack;
	}

	@WrapOperation(
		method = "tryExecuteRecipeInteractions(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
		at = @At(
			value = "INVOKE",
			target = "Lio/redspace/ironsspellbooks/recipe_types/alchemist_cauldron/EmptyAlchemistCauldronRecipe;assemble(Lio/redspace/ironsspellbooks/recipe_types/alchemist_cauldron/EmptyAlchemistCauldronRecipe$Input;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;"
		)
	)
	private ItemStack tryExecuteRecipeInteractions$assemble(
		EmptyAlchemistCauldronRecipe instance,
		EmptyAlchemistCauldronRecipe.Input input,
		HolderLookup.Provider registries,
		Operation<ItemStack> original,
		@Share("fluidStack") LocalRef<FluidStack> fluidStackRef
	) {
		var fluidStack = fluidStackRef.get();
		var inputFS = input.fluid().copy();
		Util.copyComponents(inputFS, fluidStack);
		var newInput = new EmptyAlchemistCauldronRecipe.Input(input.item(), inputFS);
		var result = original.call(instance, newInput, registries);
		Util.copyComponents(result, fluidStack);
		return result;
	}

	// Mixing hooks
	@WrapMethod(method = "handleUse(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/ItemInteractionResult;")
	private ItemInteractionResult handleUse(
		BlockState blockState,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		Operation<ItemInteractionResult> original
	) {
		// TODO: make sure it's not animating or anything
		ItemStack itemStack = player.getItemInHand(hand);
		boolean didBlazeTouch = false;
		if (level instanceof ServerLevel) {
			if (itemStack.is(UBTags.LADLE)) {
				if (this.unifiedbrewing$cookTime > 0) {
					// Don't try to mix more just do the effect for fun
					IronsCompat.makeSwirlParticles(level, pos);
					return ItemInteractionResult.SUCCESS;
				}

				if (
					Config.REQUIRE_BLAZING.getAsBoolean()
					&& itemStack.is(UBItems.BLAZING_LADLE)
				) {
					var notBlazeTouched = false;
					for (var f : fluidInventory.fluids()) {
						int cur = Util.blazeTouched(f);
						int fluidAmount = f.getAmount();
						if (cur < fluidAmount) {
							notBlazeTouched = true;
							Util.blazeTouched(f, fluidAmount);
						}
					}
					if (notBlazeTouched) {
						itemStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
						level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
						didBlazeTouch = true;
					}
				}

				// All mixes require a full cauldron
				if (fluidInventory.fluidAmount() == 1000) {
					var fluidInputs = fluidInventory.fluids().stream()
						.filter(x -> !x.is(Fluids.WATER)).toList();
					List<MutableDataComponentHolder> ingredients = Stream.concat(
						fluidInputs.stream(),
						inputItems.stream().filter(x -> !x.isEmpty())
					).toList();

					// Try to mix a vanilla recipe (respects Iron's setting)
					if (ServerConfigs.ALLOW_CAULDRON_BREWING.get()) {
						MixFinder mixFinder = null;
						MixFinder.MixResult mixed = MixFinder.MixResult.EMPTY;
						for (int i = 0; i < fluidInputs.size() && mixed.isEmpty(); ++i) {
							var inputs = new ArrayList<>(ingredients);
							var base = inputs.remove(i);
							mixFinder = MixFinder.from(base, inputs);
							mixed = mixFinder.findBrewingStandResult(level);
						}

						if (mixFinder != null && !mixed.isEmpty()) {
							// We assume every mundane took blaze powder to make
							// mixing it with other things makes it net-neutral on
							// blaze consumption, so no requirement to deplete here.

							IronsCompat.makeSwirlParticles(level, pos);
							unifiedbrewing$cookTime = SwirlParticle.SWIRL_TIME;
							// Allow it to multiply
							UnifiedBrewing.LOGGER.info("mix result: {}", mixed);
							unifiedbrewing$mixResult = new MixFinder.MixResult(
								mixed.result().copyWithAmount(250),
								mixed.base().copyWithAmount(250),
								mixed.mixed(),
								SwirlParticle.SWIRL_TIME, 0);
							return ItemInteractionResult.SUCCESS;
						}
					}

					// Mix Small Cauldron stuff
					if (HexaliaCompat.isLoaded()) {
						// Second condition here is "iff all non-water fluids are mundane/awkward/thick"
						if (!ingredients.isEmpty() && !ingredients.contains(ItemStack.EMPTY)) {
							var bottle = HexaliaCompat.rusticBottle();
							var mixFinder = MixFinder.from(bottle, ingredients);
							var result = mixFinder.findSmallCauldronResult(level);
							if (!result.isEmpty()) {
								UnifiedBrewing.LOGGER.info("found recipe for {}", result.result().originalItem());
								unifiedbrewing$mixResult = result;
								unifiedbrewing$cookTime = result.ticksToBrew();
								IronsCompat.makeSwirlParticles(level, pos);
								return ItemInteractionResult.SUCCESS;
							}
						}
					}
				}
			}
		}
		// Prevent adding/removing ingredients and filling/emptying bottles when cooking
		var cookTime = ((AlchemistCauldronTile) (Object) this).getData(UBAttachments.COOK_TIME);
		if (cookTime != null && cookTime > 0) {
			UnifiedBrewing.LOGGER.info("cooking, can't add");
			return ItemInteractionResult.FAIL;
		}
		// Show animation when using Blazing Ladle just to blaze touch the contents
		if (didBlazeTouch)
			return ItemInteractionResult.SUCCESS;
		return original.call(blockState, level, pos, player, hand);
	}

	@Inject(
		method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lio/redspace/ironsspellbooks/block/alchemist_cauldron/AlchemistCauldronTile;)V",
		at = @At("HEAD")
	)
	private static void serverTick(
		Level level,
		BlockPos pos,
		BlockState blockState,
		AlchemistCauldronTile cauldronTile,
		CallbackInfo ci,
		@Share("level") LocalRef<Level> levelRef,
		@Share("pos") LocalRef<BlockPos> posRef,
		@Share("blockState") LocalRef<BlockState> blockStateRef,
		@Share("cauldronTile") LocalRef<AlchemistCauldronTile> cauldronTileRef
	) {
		levelRef.set(level);
		posRef.set(pos);
		blockStateRef.set(blockState);
		cauldronTileRef.set(cauldronTile);
	}

	@WrapOperation(
		method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lio/redspace/ironsspellbooks/block/alchemist_cauldron/AlchemistCauldronTile;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/NonNullList;size()I"
		)
	)
	private static int serverTick$size(
		NonNullList<ItemStack> instance,
		Operation<Integer> original,
		@Share("level") LocalRef<Level> levelRef,
		@Share("pos") LocalRef<BlockPos> posRef,
		@Share("blockState") LocalRef<BlockState> blockStateRef,
		@Share("cauldronTile") LocalRef<AlchemistCauldronTile> cauldronTileRef
	) {
		var level = levelRef.get();
		var pos = posRef.get();
		var blockState = blockStateRef.get();
		var cauldronTile = cauldronTileRef.get();
		var size = original.call(instance);  // always 4
		if (level != null && pos != null && blockState != null && cauldronTile != null) {
			var cauldronTileMixin = (AlchemistCauldronTileMixin) (Object) cauldronTile;
			int minCookTime = cauldronTileMixin.unifiedbrewing$cookTime;
			if (cauldronTileMixin.unifiedbrewing$cookTime > 0) {
				int ingredientsCooked = 0, found = 0;
				for (int i = 0; i < size; i++) {
					ItemStack itemStack = cauldronTile.inputItems.get(i);
					if (!itemStack.isEmpty() && cauldronTile.isBoiling(blockState)) {
						found++;
						var ct = ++cauldronTileMixin.cooktimes[i];
						if (ct < minCookTime) minCookTime = ct;
						if ((ct + i * 2) % 6 == 0) {
							RandomSource random = Utils.random;
							double waterLevel = Mth.lerp((float)cauldronTile.getFluidAmount() / 1000.0F, 0.25F, 0.9F);
							MagicManager.spawnParticles(level, ParticleTypes.SMOKE,
								(float)pos.getX() + Mth.randomBetween(random, 0.2F, 0.8F),
								(float)pos.getY() + waterLevel + 0.01,
								(float)pos.getZ() + Mth.randomBetween(random, 0.2F, 0.8F),
								1, 0.0F, 0.0F, 0.0F, 0.0F, false);
						}
					} else {
						cauldronTileMixin.cooktimes[i] = 0;
					}

					if (cauldronTileMixin.cooktimes[i] > cauldronTileMixin.unifiedbrewing$cookTime) {
						ingredientsCooked++;
					}
				}
				if (
					found == 0
					&& ++cauldronTileMixin.unifiedbrewing$noIngredientCookTimeSoFar
						> cauldronTileMixin.unifiedbrewing$cookTime
					|| found > 0 && ingredientsCooked == found
				) {
					cauldronTileMixin.unifiedbrewing$mixResult.finishCooking(level, pos);
					// Zero cook times for anything that was used up
					for (int i = 0; i < size; i++) {
						if (cauldronTile.inputItems.get(i).isEmpty())
							cauldronTileMixin.cooktimes[i] = 0;
					}
					cauldronTileMixin.unifiedbrewing$cookTime = 0;
					cauldronTileMixin.unifiedbrewing$noIngredientCookTimeSoFar = 0;
					cauldronTileMixin.unifiedbrewing$mixResult = null;
					cauldronTile.removeData(UBAttachments.COOK_TIME);
				}
				else {
					cauldronTile.setData(UBAttachments.COOK_TIME, cauldronTileMixin.unifiedbrewing$cookTime - minCookTime);
				}
				cauldronTile.setChanged();
				return 0;
			}
		}
		return size;
	}

	@Inject(method = "loadAdditional(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V", at=@At("HEAD"))
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registryAccess, CallbackInfo ci) {
		unifiedbrewing$cookTime = tag.getInt("ub$CookTime");
		unifiedbrewing$noIngredientCookTimeSoFar = tag.getInt("ub$CookProgress");
		unifiedbrewing$mixResult = tag.contains("ub$MixResult")
			? MixFinder.MixResult.load(tag.getCompound("ub$MixResult"), registryAccess)
			: null;
	}

	@Inject(method = "saveAdditional(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V", at=@At("HEAD"))
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registryAccess, CallbackInfo ci) {
		tag.putInt("ub$CookTime", unifiedbrewing$cookTime);
		tag.putInt("ub$CookProgress", unifiedbrewing$noIngredientCookTimeSoFar);
		if (unifiedbrewing$mixResult != null)
			tag.put("ub$MixResult", unifiedbrewing$mixResult.save(registryAccess));
	}
}
