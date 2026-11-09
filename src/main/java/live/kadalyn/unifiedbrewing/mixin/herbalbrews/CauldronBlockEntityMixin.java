package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import live.kadalyn.unifiedbrewing.Config;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.mixin.Util;
import live.kadalyn.unifiedbrewing.recipes.MixFinder;
import live.kadalyn.unifiedbrewing.registries.UBDataComponents;
import live.kadalyn.unifiedbrewing.registries.UBItems;
import live.kadalyn.unifiedbrewing.registries.UBTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.herbalbrews.core.blocks.entity.CauldronBlockEntity;
import net.satisfy.herbalbrews.core.registry.ObjectRegistry;
import net.satisfy.herbalbrews.core.world.ImplementedInventory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@Mixin(CauldronBlockEntity.class)
public abstract class CauldronBlockEntityMixin extends BlockEntity implements ImplementedInventory, Util.ICauldronBlockEntityExtras {
	@Shadow @Final @Mutable
	private static int[] SLOTS_FOR_SIDE;
	@Shadow @Final @Mutable
	private static int[] SLOTS_FOR_UP;
	@Shadow @Final @Mutable
	private static int[] SLOTS_FOR_DOWN;

	@Unique
	private int unifiedbrewing$fuelRemaining = 0;

	@Unique
	private Boolean unifiedbrewing$canCraftOther = null;

	@Shadow
	private int brewingTime;
	@Shadow
	private int totalBrewingTime;

	@Shadow
	private NonNullList<ItemStack> inventory;

	public CauldronBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void overwriteSlots(CallbackInfo ci) {
		// Original slots were stupid
		SLOTS_FOR_SIDE = new int[]{4};
		SLOTS_FOR_UP = new int[]{0, 1, 2};
		SLOTS_FOR_DOWN = new int[]{3};
	}

	// Add fuel data
	@Unique
	public int unifiedbrewing$getFuelRemaining() {
		return unifiedbrewing$fuelRemaining;
	}

	@Unique
	public void unifiedbrewing$setFuelRemaining(int fuelRemaining) {
		this.unifiedbrewing$fuelRemaining = fuelRemaining;
	}

	@Inject(
		method = "loadAdditional(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
		at = @At("TAIL")
	)
	protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
		this.unifiedbrewing$fuelRemaining = compoundTag.getInt("FuelRemaining");
	}

	@Inject(
		method = "saveAdditional(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
		at = @At("TAIL")
	)
	protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider, CallbackInfo ci) {
		compoundTag.putInt("FuelRemaining", this.unifiedbrewing$fuelRemaining);
	}

	@Unique @Override
	public void setChanged() {
		unifiedbrewing$canCraftOther = null;
		super.setChanged();
	}

	@Inject(
		method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/herbalbrews/core/blocks/entity/CauldronBlockEntity;)V",
		at = @At("HEAD")
	)
	public void tick(
		Level world, BlockPos pos, BlockState state, CauldronBlockEntity blockEntity,
		CallbackInfo ci, @Share("level") LocalRef<Level> levelRef
	) {
		levelRef.set(world);
	}

	@Redirect(
		method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/satisfy/herbalbrews/core/blocks/entity/CauldronBlockEntity;)V",
		at = @At(value = "INVOKE", target = "Lnet/satisfy/herbalbrews/core/blocks/entity/CauldronBlockEntity;canCraft()Z")
	)
	private boolean canCraft(CauldronBlockEntity instance, @Share("level") LocalRef<Level> levelRef) {
		ItemStack catalyst = getItem(4);
		if (unifiedbrewing$isOriginalMode(catalyst)) {
			for(int i = 0; i < 3; ++i) {
				ItemStack stack = getItem(i);
				if (stack.isEmpty() || !(stack.getItem() instanceof PotionItem)) {
					return false;
				}
			}
			if (getItem(3).is(UBItems.EMPTY_FLASK)) {
				// Since this method is only used by tick right before starting brewing,
				// we can consume the Herbal Infusion for fuel here
				if (unifiedbrewing$fuelRemaining == 0) {
					unifiedbrewing$fuelRemaining = 4;
					this.removeItem(4, 1);
				}
				return true;
			}
		}

		if (unifiedbrewing$canCraftOther != null)
			return unifiedbrewing$canCraftOther;

		List<ItemStack> items = new ArrayList<>(getItems().subList(0, 3).stream()
			.filter(item -> !item.isEmpty() && !item.is(Items.AIR)).toList());
		var container = getItem(3);
		int brewTime = 0;
		var mustBeBlazed = catalyst.is(UBTags.BLAZING_PROVIDER);
		if (Config.REQUIRE_BLAZING.getAsBoolean() && (catalyst.is(Items.BLAZE_POWDER) || mustBeBlazed)) {
			// Attempt once without the catalyst and once with
			var mixFinder = MixFinder.from(container, items);
			var result = mixFinder.findResult(levelRef.get(), true);
			if (!result.isEmpty()) {
				if (mustBeBlazed && catalyst.getDamageValue() + result.blazeCharges() > catalyst.getMaxDamage()) {
					unifiedbrewing$canCraftOther = false;
					return false;
				}
				brewTime = result.ticksToBrew();
			}
			else if (mustBeBlazed) {
				unifiedbrewing$canCraftOther = false;
				return false;
			}
		}
		if (brewTime == 0) {
			items.add(catalyst);
			brewTime = Util.hasPotionRecipe(levelRef.get(), items, container, false);
			if (brewTime == 0) {
				unifiedbrewing$canCraftOther = false;
				return false;
			}
		}
		totalBrewingTime = brewTime;
		unifiedbrewing$canCraftOther = true;
		return true;
	}

	@WrapMethod(method = "craft(Lnet/minecraft/world/level/Level;)V")
	private void craft(Level world, Operation<Void> original) {
		List<ItemStack> items = new ArrayList<>(getItems().subList(0, 3).stream()
			.filter(item -> !item.isEmpty() && !item.is(Items.AIR)).toList());
		var container = getItem(3);

		MixFinder.MixResult result = MixFinder.MixResult.EMPTY;

		if (unifiedbrewing$isOriginalMode()) {
			var mixFinder = MixFinder.from(container, items);
			result = mixFinder.findBrewingCauldronResult(world);
			if (!result.isEmpty()) {
				unifiedbrewing$fuelRemaining--;
			}
			else {
				original.call(world);
				return;
			}
		}
		else {
			// Craft other mixes
			ItemStack catalyst = getItem(4);
			var isBlazePowder = catalyst.is(Items.BLAZE_POWDER);
			if (Config.REQUIRE_BLAZING.getAsBoolean() && (catalyst.is(UBTags.BLAZING_PROVIDER) || isBlazePowder)) {
				var mixFinder = MixFinder.from(container, items);
				result = mixFinder.findResult(world, true);
				if (!result.isEmpty()) {
					if (isBlazePowder) {
						var newCatalyst = new ItemStack(UBItems.DEPLETED_BLAZE_POWDER);
						newCatalyst.setDamageValue(result.blazeCharges());
						setItem(4, newCatalyst);
					}
					else {
						var dmg = catalyst.getDamageValue() + result.blazeCharges();
						catalyst.setDamageValue(dmg);
						if (dmg >= catalyst.getMaxDamage())
							setItem(4, ItemStack.EMPTY);
						else
							setItem(4, catalyst);
					}
				}
			}
			if (result.isEmpty()) {
				if (!catalyst.isEmpty())
					items.add(catalyst);

				var mixFinder = MixFinder.from(container, items);
				result = mixFinder.findResult(world, false);
			}
		}

		// Drop any emptied bottles or any excess from the output slot
		List<ItemStack> drops = new ArrayList<>();

		if (!result.isEmpty()) {
			int[] slots = {0, 1, 2, 4};
			for (var ingredient : result.mixed()) {
				for (int i : slots) {
					// Assuming that no recipe takes the same item twice...
					// If it does, it will take 2 from the first stack
					// instead of 1 from each
					var item = getItem(i);
					if (ItemStack.isSameItemSameComponents(item, ingredient.originalItem())) {
						var bottle = Util.bottleOf(item);
						removeItem(i, 1);
						if (!bottle.isEmpty()) {
							bottle.setCount(1);
							if (getItem(i).isEmpty())
								setItem(i, bottle);
							else
								drops.add(bottle);
						}
						break;
					}
				}
			}

			// We can assume the result item isn't a flask so don't need to worry
			// about custom model data or anything
			var newOutput = result.result().originalItem();
			var potionContents = newOutput.get(DataComponents.POTION_CONTENTS);
			if (potionContents != null && potionContents.is(Potions.MUNDANE))
				if (result.mixed().size() == 1)
					newOutput.set(UBDataComponents.MUNDANE_INGREDIENT,
						UBDataComponents.SingleItemComponent.of(result.mixed().getFirst().ingredient()));

			if (!newOutput.isEmpty()) {
				var currentOutput = getItem(3);
				if (!currentOutput.isEmpty()) {
					var bottle = Util.bottleOf(currentOutput);
					var amount = Util.bottleOf(newOutput).isEmpty() ? 0 : newOutput.getCount();
					var currentAmount = result.consumeBottles();
					removeItem(3, result.consumeBottles());
					if (amount < currentAmount) {
						bottle.setCount(currentAmount - amount);
						drops.add(bottle);
					}
				}
				drops.add(getItem(3));
			}

			for (var drop : drops) {
				if (!drop.isEmpty()) {
					// Drop the leftovers on the block
					var blockPos = getBlockPos();
					ItemEntity itementity = new ItemEntity(world, blockPos.getX(), blockPos.getY() + 1, blockPos.getZ(), drop);
					itementity.setDefaultPickUpDelay();
					world.addFreshEntity(itementity);
				}
			}
			if (newOutput.isEmpty())
				world.playSound(null, getBlockPos(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.MASTER, 1.0F, 1.0F);
			else
				setItem(3, newOutput);
		}
	}

	@WrapOperation(
		method = "craft(Lnet/minecraft/world/level/Level;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/satisfy/herbalbrews/core/blocks/entity/CauldronBlockEntity;removeItem(II)Lnet/minecraft/world/item/ItemStack;"
		)
	)
	@NotNull ItemStack removeItem(CauldronBlockEntity instance, int slot, int count, Operation<ItemStack> original) {
		if (slot == 4) {
			unifiedbrewing$fuelRemaining--;
			return instance.getItem(slot);
		}
		return original.call(instance, slot, count);
	}

	@WrapOperation(
		method = "craft(Lnet/minecraft/world/level/Level;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"
		)
	)
	public <T> T set(ItemStack instance, DataComponentType<? super T> component, T value, Operation<T> original) {
		UnifiedBrewing.LOGGER.info("set({}, {})", component, value);
		if (component == DataComponents.CUSTOM_MODEL_DATA) {
			var modelId = getItem(3).get(DataComponents.CUSTOM_MODEL_DATA);
			UnifiedBrewing.LOGGER.info("custom model data: {}", modelId == null ? null : modelId.value());
			return original.call(instance, component, modelId);
		}
		return original.call(instance, component, value);
	}

	@WrapMethod(method = "setItem(ILnet/minecraft/world/item/ItemStack;)V")
	public void setItem(int slot, ItemStack stack, Operation<Void> original) {
		ItemStack stackInSlot = inventory.get(slot);
		original.call(slot, stack);
		if (slot == 3) {
			if (!ItemStack.isSameItem(stack, stackInSlot))
				brewingTime = 0;
			setChanged();
		}
	}

	@Unique
	public boolean unifiedbrewing$isOriginalMode() {
		return unifiedbrewing$isOriginalMode(getItem(4));
	}

	@Unique
	public boolean unifiedbrewing$isOriginalMode(ItemStack catalyst) {
		return catalyst.is(ObjectRegistry.HERBAL_INFUSION) || catalyst.isEmpty() && unifiedbrewing$fuelRemaining > 0;
	}
}
