package live.kadalyn.unifiedbrewing.mixin.hexalia;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import live.kadalyn.unifiedbrewing.Config;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import live.kadalyn.unifiedbrewing.compat.HexaliaCompat;
import live.kadalyn.unifiedbrewing.compat.IronsCompat;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.recipes.MixFinder;
import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import live.kadalyn.unifiedbrewing.registries.UBPotions;
import live.kadalyn.unifiedbrewing.registries.UBSounds;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.astralya.hexalia.Hexalia;
import net.astralya.hexalia.gameplay.smallcauldron.SmallCauldronContents;
import net.astralya.hexalia.recipe.ModRecipeTypes;
import net.astralya.hexalia.recipe.SmallCauldronRecipe;
import net.astralya.hexalia.recipe.SmallCauldronRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(SmallCauldronContents.class)
public abstract class SmallCauldronContentsMixin implements Util.ISmallCauldronContentsExtras {
	@Unique
	private static final ResourceLocation unifiedbrewing$mixedId = UnifiedBrewing.identifier("mixed");
	@Unique
	private static final ResourceLocation unifiedbrewing$teaTouchId = UnifiedBrewing.identifier("tea_touch");

	@Unique
	private SmallCauldronRecipe unifiedbrewing$lockedMixResult = null;

	@Shadow
	protected abstract List<ItemStack> getIngredientListForRecipe();

	@Shadow
	private @Nullable String lockedRecipeId;

	@Shadow
	private int waterMb;

	@Shadow
	public abstract boolean canUseWaterContainer(ItemStack stack);

	@Shadow
	protected abstract int fillWater(int amountMb);

	@Shadow
	private int servings;

	@Unique
	private int unifiedbrewing$maxServings;

	@Shadow
	private ItemStack mixtureResult;

	@Shadow
	private SmallCauldronContents.ContentsKind kind;

	@Shadow
	private boolean overcooked;

	@Shadow
	private int mixtureColor;

	@Unique private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
	@Unique private static final ResourceLocation MILK_STILL = ResourceLocation.fromNamespaceAndPath("neoforge", "block/milk_still");
	@Unique
	private ResourceLocation unifiedbrewing$mixtureTexture = WATER_STILL;

	@Shadow
	private int cookProgress;

	@Shadow
	private int mixtureAgeTicks;

	@Shadow
	private static int getMixtureColor(ItemStack result) {
		throw new UnsupportedOperationException("Implemented via mixin");
	}

	@Shadow
	protected abstract void markDirty();

	@Shadow
	public abstract boolean canScoopMixtureWithRusticBottle();

	@Shadow
	public abstract void resetToEmpty();

	@Shadow
	@Final
	private NonNullList<ItemStack> ingredients;

	@Shadow
	private int ingredientCount;

	@Shadow
	protected abstract Optional<RecipeHolder<SmallCauldronRecipe>> findRecipeMatch(ServerLevel level);

	@Shadow
	private int stirsDone;

	@Shadow
	protected abstract void clearIngredientsAndCount();

	@Shadow
	public abstract void setIngredient(int slot, ItemStack stack);

	@Accessor("stirsDone")
	abstract public int getStirsDone();

	@Redirect(
		method = "canExtractOneIngredient(Z)Z",
		at = @At(
			value = "FIELD",
			target = "Lnet/astralya/hexalia/gameplay/smallcauldron/SmallCauldronContents;kind:Lnet/astralya/hexalia/gameplay/smallcauldron/SmallCauldronContents$ContentsKind;",
			opcode = Opcodes.GETFIELD
		)
	)
	public SmallCauldronContents.ContentsKind canExtractOneIngredient$getKind(SmallCauldronContents instance) {
		// Mixture is okay too
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE)
			return SmallCauldronContents.ContentsKind.WATER;
		return kind;
	}

	@WrapMethod(method = "canInsertOne(Lnet/minecraft/world/item/ItemStack;)Z")
	public boolean canInsertOne(ItemStack stack, Operation<Boolean> original) {
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE && HerbalBrewsCompat.isFullFlask(mixtureResult))
			return (
				HerbalBrewsCompat.isInfusion(stack)
				&& ingredientCount == 0
				&& !Boolean.TRUE.equals(mixtureResult.get(UBDataComponents.TEA_TOUCHED))
			);
		if (Config.REQUIRE_INGREDIENTS.getAsBoolean() && !(
			Util.isPotionIngredient(stack)
			|| HerbalBrewsCompat.isInfusion(stack)
		))
			return false;

		// Allow mixtures too
		return original.call(stack);
	}

	@Redirect(
		method = "canInsertOne(Lnet/minecraft/world/item/ItemStack;)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/astralya/hexalia/gameplay/smallcauldron/SmallCauldronContents;isRusticBottle(Lnet/minecraft/world/item/ItemStack;)Z"
		)
	)
	public boolean canInsertOne$isRusticBottle(SmallCauldronContents instance, ItemStack stack) {
		// Return true if it's a bottle of any sort
		return !Util.bottleOf(stack).isEmpty();
	}

	@Redirect(
		method = "canInsertOne(Lnet/minecraft/world/item/ItemStack;)Z",
		at = @At(
			value = "FIELD",
			target = "Lnet/astralya/hexalia/gameplay/smallcauldron/SmallCauldronContents;kind:Lnet/astralya/hexalia/gameplay/smallcauldron/SmallCauldronContents$ContentsKind;",
			opcode = Opcodes.GETFIELD
		)
	)
	public SmallCauldronContents.ContentsKind canInsertOne$getKind(SmallCauldronContents instance) {
		// Mixture is okay too
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE)
			return SmallCauldronContents.ContentsKind.WATER;
		return kind;
	}

	@WrapMethod(method = "canStir(Z)Z")
	public boolean canStir(boolean blockLit, Operation<Boolean> original) {
		// from original
		if (!(blockLit && ingredientCount > 0 && cookProgress == 0 && stirsDone < ingredientCount))
			return false;

		switch (kind) {
			case WATER: case MIXTURE: break;
			default: return false;
		}

		if (kind == SmallCauldronContents.ContentsKind.MIXTURE && HerbalBrewsCompat.isLoaded()) {
			var exactlyOneInfusion = ingredientCount == 1 && HerbalBrewsCompat.isInfusion(ingredients.getFirst());
			if (HerbalBrewsCompat.isFullFlask(mixtureResult))
				return exactlyOneInfusion;
			if (mixtureResult.is(Tags.Items.POTIONS) && exactlyOneInfusion)
				return true;
		}

		return true;
	}

	@WrapMethod(method = "isRusticBottle(Lnet/minecraft/world/item/ItemStack;)Z")
	public boolean isRusticBottle(ItemStack stack, Operation<Boolean> original) {
		var bottle = Util.bottleOf(mixtureResult);
		return !bottle.isEmpty() && (
			stack.is(bottle.getItem())
			// Allow splitting up flask contents
			|| (stack.is(Items.GLASS_BOTTLE) && HerbalBrewsCompat.isFullFlask(mixtureResult))
		);
	}

	@WrapMethod(method = "tryScoopBottle(Lnet/minecraft/server/level/ServerLevel;DDDLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)Z")
	public boolean tryScoopFlaskWithBottle(
		ServerLevel level,
		double x, double y, double z,
		Player player,
		InteractionHand hand,
		ItemStack held,
		Operation<Boolean> original,
		@Share("held") LocalRef<ItemStack> heldRef
	) {
		boolean ret = false;
		heldRef.set(held);
		if (
			canScoopMixtureWithRusticBottle()
			&& HerbalBrewsCompat.isFullFlask(mixtureResult)
			&& held.is(Items.GLASS_BOTTLE)
		) {
			ItemStack resultBottle;
			var teaTouched = mixtureResult.get(UBDataComponents.TEA_TOUCHED);
			if (Boolean.TRUE.equals(teaTouched)) {
				resultBottle = PotionContents.createItemStack(Items.POTION, UBPotions.HERBAL_TEA);
				mixtureResult.remove(UBDataComponents.TEA_TOUCHED);
			}
			else {
				var potions = HerbalBrewsCompat.flaskToPotions(mixtureResult);
				resultBottle = potions.removeLast();

				if (potions.isEmpty()) {
					var ing = NonNullList.copyOf(ingredients);
					var ingCount = ingredientCount;
					resetToEmpty();
					for (int i = 0; i < 4; i++)
						ingredients.set(i, ing.get(i));
					ingredientCount = ingCount;
				}
				else if (potions.size() == 1) {
					mixtureResult = potions.getFirst();
					unifiedbrewing$setMixtureColor();
					unifiedbrewing$maxServings = 4;
					waterMb = 0;
				}
				else {
					// Use flaskId from scooping bottle only
					mixtureResult = HerbalBrewsCompat.stuffToFlask(potions, 0);
					unifiedbrewing$setMixtureColor();
				}
			}

			if (waterMb > 0) {
				waterMb -= 250;
			}
			markDirty();

			held.consume(1, player);
			if (!player.getInventory().add(resultBottle)) {
				level.addFreshEntity(new ItemEntity(level, x, y, z, resultBottle));
			}

			ret = true;
		}
		if (!ret && (
			!HerbalBrewsCompat.isEmptyFlask(held)
			|| Boolean.TRUE.equals(mixtureResult.get(UBDataComponents.TEA_TOUCHED))
		))
			ret = original.call(level, x, y, z, player, hand, held);
		level.playSound(null, x, y, z,
			ret ? SoundEvents.BOTTLE_FILL : UBSounds.BOTTLE_WHIFF.get(),
			SoundSource.BLOCKS, 1.0F, 1.0F);
		return ret;
	}

	@WrapOperation(
		method = "tryScoopBottle(Lnet/minecraft/server/level/ServerLevel;DDDLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"
		)
	)
	public ItemStack tryScoopBottle$copy(ItemStack instance, Operation<ItemStack> original, @Share("held") LocalRef<ItemStack> heldRef) {
		var item = original.call(instance);
		var held = heldRef.get();
		if (held != null && HerbalBrewsCompat.isEmptyFlask(held)) {
			var potions = HerbalBrewsCompat.flaskToPotions(item);
			var numPotions = potions.size();
			if (numPotions > 3) {
				item = HerbalBrewsCompat.stuffToFlask(potions.subList(0, 3), 0);
				mixtureResult = numPotions == 4
					? potions.get(3)
					// not sure how tho
					: HerbalBrewsCompat.stuffToFlask(potions.subList(3, numPotions), 0);
				unifiedbrewing$setMixtureColor();
				servings = 2; // reduced to 1 later in tryScoopBottle
				if (HerbalBrewsCompat.isFullFlask(mixtureResult))
					waterMb = Math.max(0, waterMb - 750);
				else
					unifiedbrewing$maxServings = 4;
			}
			var cmd = held.get(DataComponents.CUSTOM_MODEL_DATA);
			item.set(DataComponents.CUSTOM_MODEL_DATA, cmd);
			item.remove(UBDataComponents.TEA_TOUCHED);
		}
		return item;
	}

	@ModifyReturnValue(
		method = "tryScoopBottle(Lnet/minecraft/server/level/ServerLevel;DDDLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)Z",
		at = @At("RETURN")
	)
	public boolean tryScoopBottle$updateColor(boolean original) {
		if (original)
			unifiedbrewing$setMixtureColor();
		return original;
	}

	@Inject(method = "save(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V", at = @At("HEAD"))
	public void save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
		tag.putInt("MaxServings", unifiedbrewing$maxServings);
	}

	@Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V", at = @At("HEAD"))
	public void load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
		unifiedbrewing$maxServings = Math.max(1, tag.getInt("MaxServings"));
	}

	@WrapMethod(method = "tryStartCooking(Lnet/minecraft/server/level/ServerLevel;)Z")
	private boolean tryStartCooking(ServerLevel level, Operation<Boolean> original) {
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE && this.ingredientCount > 0) {
			Optional<RecipeHolder<SmallCauldronRecipe>> match = findRecipeMatch(level);
			lockedRecipeId = match.map((holder) ->
				holder.id().toString()).orElse(null);
			mixtureResult = match.map((holder) ->
				holder.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
			if (HerbalBrewsCompat.isFullFlask(mixtureResult) && waterMb == 0)
				waterMb = 250; // when mixing an infusion into a single potion
			clearIngredientsAndCount();
			stirsDone = 0;
			cookProgress = 0;
			kind = SmallCauldronContents.ContentsKind.COOKING;
			markDirty();
			return true;
		}
		return original.call(level);
	}

	@WrapMethod(method = "finalizeCook(Lnet/minecraft/server/level/ServerLevel;)Z")
	private boolean finalizeCook(ServerLevel level, Operation<Boolean> original) {
		var origWaterLevel = waterMb;
		var didCook = original.call(level);
		if (didCook) {
			assert lockedRecipeId != null;  // didCook handles this, just shutting up IDEs
			if (Util.oneOf(mixtureResult, Potions.WATER)) {
				// When ink extraction fails
				var waterAmount = mixtureResult.getCount() * 250;
				resetToEmpty();
				lockedRecipeId = null;
				kind = SmallCauldronContents.ContentsKind.WATER;
				waterMb = waterAmount;
				return true;
			}
			if (Util.bottleOf(mixtureResult).isEmpty()) {
				// Returned a byproduct instead of a fluid
				var byproduct = mixtureResult;
				resetToEmpty();
				lockedRecipeId = null;
				setIngredient(0, byproduct);
				return true;
			}
			var key = ResourceLocation.tryParse(lockedRecipeId);
			// Hexalia recipes assume 2 output but the count on the item defaulted to 1
			servings = key != null && key.getNamespace().equals(Hexalia.MOD_ID) ? 2 : mixtureResult.getCount();
			mixtureResult.setCount(1);
			unifiedbrewing$setMixtureColor();
			if (HerbalBrewsCompat.isFlask(mixtureResult)) {
				unifiedbrewing$maxServings = 1;
				if (origWaterLevel <= 250) {
					var potionCount = HerbalBrewsCompat.flaskToPotionContents(mixtureResult).size();
					var isTeaTouched = Boolean.TRUE.equals(mixtureResult.get(UBDataComponents.TEA_TOUCHED));
					waterMb = potionCount * 250 + (isTeaTouched ? 250 : 0);
				}
				else
					waterMb = origWaterLevel;
			}
			else if (HexaliaCompat.isRusticBottle(mixtureResult))
				unifiedbrewing$maxServings = 2;
			else
				unifiedbrewing$maxServings = 4;
		}
		lockedRecipeId = null;
		unifiedbrewing$lockedMixResult = null;
		return didCook;
	}

	@WrapMethod(method = "getMixtureColor(Lnet/minecraft/world/item/ItemStack;)I")
	private static int getMixtureColor(ItemStack result, Operation<Integer> original) {
		if (IronsCompat.isLoaded()) {
			var fluidStack = IronsCompat.from(result);
			int tint;
			// Irons does this too idk why (AlchemistCauldronTile.getFluidTintColor)
			if (IronsCompat.isBlood(fluidStack))
				tint = 0x570001;
			else {
				var fluidType = IClientFluidTypeExtensions.of(fluidStack.getFluidType());
				tint = fluidType.getTintColor();
			}
			if (tint != -1)
				return tint & 0xffffff; // remove alpha, not used here ?
		}
		var potionContents = result.get(DataComponents.POTION_CONTENTS);
		if (potionContents != null)
			return potionContents.getColor();
		return original.call(result);
	}

	@Unique
	private void unifiedbrewing$setMixtureColor() {
		// Assumes we're using mixtureResult regardless of kind
		if (mixtureResult.is(UBTags.INK_BOTTLE))
			unifiedbrewing$mixtureTexture = MILK_STILL;
		else if (IronsCompat.isLoaded()) {
			var fluid = IClientFluidTypeExtensions.of(IronsCompat.from(mixtureResult).getFluidType());
			var texture = fluid.getStillTexture();
			unifiedbrewing$mixtureTexture = texture == null ? WATER_STILL : texture;
		}
		else unifiedbrewing$mixtureTexture = WATER_STILL;
		mixtureColor = getMixtureColor(mixtureResult);
	}

	@WrapMethod(method = "getVisualLiquidColor()I")
	public int getVisualLiquidColor(Operation<Integer> original) {
		if (kind == SmallCauldronContents.ContentsKind.COOKING && mixtureColor != 0)
			return mixtureColor;
		return original.call();
	}

	@WrapMethod(method = "getVisualLiquidFill01()F")
	public float getVisualLiquidFill01(Operation<Float> original) {
		return switch (kind) {
			case MIXTURE, COOKING -> (
				HerbalBrewsCompat.isFlask(mixtureResult)
				? waterMb / 1000f
				: (
					kind == SmallCauldronContents.ContentsKind.COOKING && servings == 0 ? 1f
					: (overcooked ? 1f : (float) servings) / Math.max(1, unifiedbrewing$maxServings)
				)
			);
			default -> original.call();
		};
	}

	@WrapMethod(method = "findRecipeMatch(Lnet/minecraft/server/level/ServerLevel;)Ljava/util/Optional;")
	private Optional<RecipeHolder<SmallCauldronRecipe>> findRecipeMatch(
		ServerLevel level,
		Operation<Optional<RecipeHolder<SmallCauldronRecipe>>> original
	) {
		return unifiedbrewing$findRecipeMatch(level);
	}

	@Unique
	public @NotNull Optional<RecipeHolder<SmallCauldronRecipe>> unifiedbrewing$findRecipeMatch(Level level) {
		if (lockedRecipeId != null && unifiedbrewing$lockedMixResult != null)
			return Optional.of(new RecipeHolder<>(ResourceLocation.parse(lockedRecipeId), unifiedbrewing$lockedMixResult));
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE) {
			if (HerbalBrewsCompat.isInfusion(ingredients.getFirst())) {
				if (HerbalBrewsCompat.isFullFlask(mixtureResult) || mixtureResult.is(Tags.Items.POTIONS)) {
					unifiedbrewing$lockedMixResult = unifiedbrewing$getTeaTouchRecipe();
					if (unifiedbrewing$lockedMixResult != null)
						return Optional.of(new RecipeHolder<>(unifiedbrewing$teaTouchId, unifiedbrewing$lockedMixResult));
				}
			}
			else if(!HerbalBrewsCompat.isFullFlask(mixtureResult)) {
				unifiedbrewing$lockedMixResult = unifiedbrewing$getMixResultAsRecipe(level);
				if (unifiedbrewing$lockedMixResult != null)
					return Optional.of(new RecipeHolder<>(unifiedbrewing$mixedId, unifiedbrewing$lockedMixResult));
			}
			return Optional.empty();
		}

		if (kind == SmallCauldronContents.ContentsKind.WATER && waterMb >= 1000) {
			// from original code, does not need to be ServerLevel
			var scr = level.getRecipeManager().getRecipeFor(ModRecipeTypes.SMALL_CAULDRON.get(), new SmallCauldronRecipeInput(getIngredientListForRecipe()), level);
			if (scr.isPresent())
				return scr;

			unifiedbrewing$lockedMixResult = unifiedbrewing$getMixResultAsRecipe(level);
			UnifiedBrewing.LOGGER.info("findRecipeMatch: {}", unifiedbrewing$lockedMixResult);
			if (unifiedbrewing$lockedMixResult != null)
				return Optional.of(new RecipeHolder<>(unifiedbrewing$mixedId, unifiedbrewing$lockedMixResult));
		}
		return Optional.empty();
	}

	@WrapMethod(method = "findLockedRecipe(Lnet/minecraft/server/level/ServerLevel;)Ljava/util/Optional;")
	private Optional<RecipeHolder<SmallCauldronRecipe>> findLockedRecipe(
		ServerLevel level,
		Operation<Optional<RecipeHolder<SmallCauldronRecipe>>> original
	) {
		if (lockedRecipeId != null) {
			if (lockedRecipeId.equals(unifiedbrewing$mixedId.toString())) {
				if (unifiedbrewing$lockedMixResult == null)
					unifiedbrewing$lockedMixResult = unifiedbrewing$getMixResultAsRecipe(level);
				if (unifiedbrewing$lockedMixResult != null)
					return Optional.of(new RecipeHolder<>(unifiedbrewing$mixedId, unifiedbrewing$lockedMixResult));
				return Optional.empty();
			}
			if (lockedRecipeId.equals(unifiedbrewing$teaTouchId.toString())) {
				if (unifiedbrewing$lockedMixResult == null)
					unifiedbrewing$lockedMixResult = unifiedbrewing$getTeaTouchRecipe();
				if (unifiedbrewing$lockedMixResult != null)
					return Optional.of(new RecipeHolder<>(unifiedbrewing$teaTouchId, unifiedbrewing$lockedMixResult));
				return Optional.empty();
			}
		}
		return original.call(level);
	}

	@Unique
	private SmallCauldronRecipe unifiedbrewing$getMixResultAsRecipe(Level level) {
		MutableDataComponentHolder base;
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE)
			base = mixtureResult.copyWithCount(servings);
		else
			base = new FluidStack(Fluids.WATER, waterMb);

		var mixFinder = MixFinder.from(base, getIngredientListForRecipe());
		var result = mixFinder.findResult(level);
		if (!result.isEmpty()) {
			var ingredients = result.mixed().stream()
				.map(MixFinder.Normalized::ingredient).map(Ingredient::of)
				.collect(Collectors.toCollection(NonNullList::create));
			// TODO: experience?
			ItemStack output;
			if (result.result().isEmpty()) {
				output = PotionContents.createItemStack(Items.POTION, Potions.WATER);
				output.setCount(waterMb / 250);
			}
			else
				output = result.result().originalItem();
			var ret = new SmallCauldronRecipe(
				ingredients, output, 0, result.ticksToBrew());
			((Util.ISmallCauldronRecipeExtras) (Object) ret).unifiedbrewing$setBlazeCharges(result.blazeCharges());
			return ret;
		}
		return null;
	}

	@Unique
	private SmallCauldronRecipe unifiedbrewing$getTeaTouchRecipe() {
		if (ingredientCount != 1 || !HerbalBrewsCompat.isInfusion(ingredients.getFirst()))
			return null;

		ItemStack result;
		result = HerbalBrewsCompat.isFullFlask(mixtureResult)
			? mixtureResult.copy()
			: HerbalBrewsCompat.stuffToFlask(List.of(mixtureResult), 0);
		result.set(UBDataComponents.TEA_TOUCHED, true);
		var ingredient = Ingredient.of(ingredients.getFirst());
		// 100 is the cook time for incremental brews in Alchemist Cauldron
		return new SmallCauldronRecipe(NonNullList.of(ingredient), result, 0, 100);
	}

	@Inject(
		method = "canUseWaterContainer(Lnet/minecraft/world/item/ItemStack;)Z",
		at = @At("HEAD"),
		cancellable = true
	)
	public void canUseWaterContainerBucketExtras(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE && mixtureResult.is(UBTags.BLOOD_BOTTLE)) {
			cir.setReturnValue(stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BUCKET) && servings >= unifiedbrewing$maxServings);
		}
	}

	@Inject(
		method = "canUseWaterContainer(Lnet/minecraft/world/item/ItemStack;)Z",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/world/item/Items;WATER_BUCKET:Lnet/minecraft/world/item/Item;",
			opcode = Opcodes.GETSTATIC
		),
		cancellable = true
	)
	public void canUseWaterContainerWaterBottle(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.is(Tags.Items.POTIONS) && Util.oneOf(stack, Potions.WATER)) {
			cir.setReturnValue(waterMb + 250 <= 1000);
		}
	}

	@Inject(
		method = "tryUseWaterContainer(Lnet/minecraft/server/level/ServerLevel;DDDLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)Z",
		at = @At("HEAD"),
		cancellable = true
	)
	public void tryUseWaterContainer(ServerLevel level, double x, double y, double z, Player player, InteractionHand hand, ItemStack held, CallbackInfoReturnable<Boolean> cir) {
		if (canUseWaterContainer(held)) {
			if (held.is(Tags.Items.POTIONS) && Util.oneOf(held, Potions.WATER)) {
				fillWater(250);
				if (!player.getAbilities().instabuild) {
					ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
					held.shrink(1);
					if (!player.getInventory().add(bottle)) {
						player.drop(bottle, false);
					}
				}

				level.playSound(null, x, y, z, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
				cir.setReturnValue(true);
			}
			if (kind == SmallCauldronContents.ContentsKind.MIXTURE && mixtureResult.is(UBTags.BLOOD_BOTTLE)) {
				if (IronsCompat.isLoaded()) {
					var fluidStack = IronsCompat.from(mixtureResult);
					if (fluidStack.isEmpty()) cir.setReturnValue(false);
					fluidStack.setAmount(servings * 1000 / unifiedbrewing$maxServings);
					var itemStack = IronsCompat.from(fluidStack, held);
					if (itemStack.isEmpty()) cir.setReturnValue(false);

					held.consume(1, player);
					if (!player.getInventory().add(itemStack)) {
						player.drop(itemStack, false);
					}
					resetToEmpty();
					markDirty();
					cir.setReturnValue(true);
				}
			}
		}
	}

	@Unique
	public boolean unifiedbrewing$isPotionLikeContainer(ItemStack held) {
		return Util.oneOf(held,
			Tags.Items.POTIONS,
			UBTags.NON_POTION_BREWABLE,
			UBTags.BLOOD_BOTTLE,
			UBTags.BLOOD_BUCKET
		) || HerbalBrewsCompat.isFullFlask(held);
	}

	@Unique
	public boolean unifiedbrewing$canUsePotionLikeContainer(ItemStack held) {
		var mixtureBloodOrEmpty = true;
		var mixturePotionOrEmpty = true;
		var mixtureFlaskOrEmpty = true;
		var spaceRemains = true;
		if (kind == SmallCauldronContents.ContentsKind.MIXTURE) {
			if (ItemStack.isSameItemSameComponents(held, mixtureResult))
				return true;
			mixtureBloodOrEmpty = mixtureResult.is(UBTags.BLOOD_BOTTLE);
			//noinspection DataFlowIssue
			mixturePotionOrEmpty = mixtureResult.is(Tags.Items.POTIONS);
			mixtureFlaskOrEmpty = HerbalBrewsCompat.isFullFlask(mixtureResult);
			if (mixtureFlaskOrEmpty)
				spaceRemains = waterMb < 1000;
			else
				spaceRemains = unifiedbrewing$maxServings == 0 || servings < unifiedbrewing$maxServings;
		}
		else if (kind != SmallCauldronContents.ContentsKind.EMPTY)
			return false;

		//noinspection DataFlowIssue
		return spaceRemains && ((
			IronsCompat.isLoaded()
			&& Util.oneOf(held, UBTags.BLOOD_BOTTLE, UBTags.BLOOD_BUCKET)
			&& mixtureBloodOrEmpty
		) || (
			held.is(Tags.Items.POTIONS)
			&& (
				Util.oneOf(held, UBPotions.HERBAL_TEA)
				|| held.has(DataComponents.POTION_CONTENTS)
				&& held.get(DataComponents.POTION_CONTENTS).getAllEffects().iterator().hasNext()
			)
			&& (mixturePotionOrEmpty || mixtureFlaskOrEmpty)
		) || (
			kind == SmallCauldronContents.ContentsKind.EMPTY
			&& (
				HerbalBrewsCompat.isFullFlask(held)
				|| held.is(UBTags.NON_POTION_BREWABLE)
				|| held.is(Tags.Items.POTIONS)
			)
		));
	}

	@Unique
	public boolean unifiedbrewing$tryUsePotionLikeContainer(
		ServerLevel level,
		double x, double y, double z,
		Player player,
		ItemStack held
	) {
		if (!unifiedbrewing$canUsePotionLikeContainer(held))
			return false;
		if (kind == SmallCauldronContents.ContentsKind.EMPTY) {
			kind = SmallCauldronContents.ContentsKind.MIXTURE;
			if (IronsCompat.isLoaded() && Util.oneOf(held, UBTags.BLOOD_BOTTLE, UBTags.BLOOD_BUCKET)) {
				if (held.is(UBTags.BLOOD_BOTTLE))
					mixtureResult = IronsCompat.getBloodVial(1);
				else if (held.is(UBTags.BLOOD_BUCKET))
					mixtureResult = IronsCompat.getBloodVial(4);
			}
			else {
				mixtureResult = held.copyWithCount(1);
			}
			servings = mixtureResult.getCount();
			mixtureResult.setCount(1);
			if (HerbalBrewsCompat.isFlask(mixtureResult)) {
				unifiedbrewing$maxServings = 1;
				mixtureResult.set(UBDataComponents.TEA_TOUCHED, true);
				waterMb = HerbalBrewsCompat.flaskToPotionContents(mixtureResult).size() * 250 + 250;
			}
			else if (HexaliaCompat.isRusticBottle(mixtureResult))
				unifiedbrewing$maxServings = 2;
			else
				unifiedbrewing$maxServings = 4;
			unifiedbrewing$setMixtureColor();
			cookProgress = 0;
			mixtureAgeTicks = 0;
			overcooked = false;
		}
		else if (kind == SmallCauldronContents.ContentsKind.MIXTURE) {
			if (ItemStack.isSameItemSameComponents(held, mixtureResult)) {
				if (servings >= unifiedbrewing$maxServings)
					return false;
				servings++;
			}
			else if (IronsCompat.isLoaded() && Util.oneOf(held, UBTags.BLOOD_BOTTLE, UBTags.BLOOD_BUCKET)) {
				if (!mixtureResult.is(UBTags.BLOOD_BOTTLE))
					return false;
				if (held.is(UBTags.BLOOD_BUCKET))
					servings = Math.min(4, unifiedbrewing$maxServings);
				else if (servings < unifiedbrewing$maxServings)
					servings++;
			}
			else if (HerbalBrewsCompat.isLoaded() && held.is(Tags.Items.POTIONS)) {
				// If the potion has no effects, fail
				var isEitherHerbalTea = false;
				for (var p : List.of(mixtureResult, held)) {
					var ht = Util.oneOf(p, UBPotions.HERBAL_TEA);
					if (isEitherHerbalTea && ht)
						return false;  // don't let ppl waste it
					isEitherHerbalTea = isEitherHerbalTea || ht;
					var pc = p.get(DataComponents.POTION_CONTENTS);
					if (!ht && (pc == null || !pc.getAllEffects().iterator().hasNext()))
						return false;
				}

				var teaTouched = Boolean.TRUE.equals(mixtureResult.get(UBDataComponents.TEA_TOUCHED));
				if (mixtureResult.is(Tags.Items.POTIONS)) {
					if (servings > 1)
						return false;
					waterMb = 500;  // repurposing
					unifiedbrewing$maxServings = 1;
				}
				else if (HerbalBrewsCompat.isFullFlask(mixtureResult) && waterMb < 1000) {
					if (teaTouched && isEitherHerbalTea)
						return false;  // don't let ppl waste it
					if (!isEitherHerbalTea) {
						var heldPotionContents = held.get(DataComponents.POTION_CONTENTS);
						var mixedPotionContents = HerbalBrewsCompat.flaskToPotionContents(mixtureResult);
						for (var pc : mixedPotionContents) {
							if (pc.equals(heldPotionContents))
								return false;  // adding duplicate, would be wasted
						}
					}
					waterMb += 250;
				}
				else
					return false;
				if (!Util.oneOf(held, UBPotions.HERBAL_TEA)) {
					mixtureResult = HerbalBrewsCompat.stuffToFlask(List.of(mixtureResult, held), 0);
					mixtureResult.set(UBDataComponents.TEA_TOUCHED, teaTouched);
					unifiedbrewing$setMixtureColor();
				}
				else if (mixtureResult.is(Tags.Items.POTIONS)) {
					mixtureResult = HerbalBrewsCompat.stuffToFlask(List.of(mixtureResult), 0);
				}
				if (isEitherHerbalTea)
					mixtureResult.set(UBDataComponents.TEA_TOUCHED, true);
			}
			else
				return false;
		}
		else
			return false;

		if (!player.getAbilities().instabuild) {
			ItemStack bottle = held.is(UBTags.BLOOD_BUCKET) ? new ItemStack(Items.BUCKET) : Util.bottleOf(held);
			held.shrink(1);
			if (!player.getInventory().add(bottle)) {
				player.drop(bottle, false);
			}
		}

		level.playSound(null, x, y, z, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
		markDirty();
		return true;
	}

	@Inject(
		method = "tickServer(Lnet/minecraft/server/level/ServerLevel;Z)V",
		at = @At(
			value = "FIELD",
			target = "Lnet/astralya/hexalia/gameplay/smallcauldron/SmallCauldronContents;mixtureAgeTicks:I",
			opcode = Opcodes.GETFIELD
		),
		cancellable = true
	)
	public void tickServer(ServerLevel level, boolean blockLit, CallbackInfo ci) {
		if (Config.PREVENT_OVERCOOKING.getAsBoolean())
			ci.cancel();
	}

	@WrapMethod(method = "isWaterContainer(Lnet/minecraft/world/item/ItemStack;)Z")
	public boolean isWaterContainer(ItemStack stack, Operation<Boolean> original) {
		return original.call(stack) || stack.is(Tags.Items.POTIONS) && Util.oneOf(stack, Potions.WATER);
	}

	@Inject(method = "resetToEmpty()V", at = @At("HEAD"))
	public void resetToEmpty(CallbackInfo ci) {
		unifiedbrewing$lockedMixResult = null;
		unifiedbrewing$maxServings = 0;
	}

	public @NotNull ResourceLocation unifiedbrewing$getMixtureTexture() {
		return unifiedbrewing$mixtureTexture;
	}
}
