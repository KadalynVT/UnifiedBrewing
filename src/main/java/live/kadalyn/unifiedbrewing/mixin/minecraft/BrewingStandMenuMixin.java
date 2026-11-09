package live.kadalyn.unifiedbrewing.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandMenuMixin extends AbstractContainerMenu {
	@Shadow @Final
	private Container brewingStand;

	protected BrewingStandMenuMixin(@Nullable MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@ModifyArg(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/SimpleContainer;<init>(I)V"
		),
		index = 0
	)
	private static int changeContainerSize(int size) {
		return 6;
	}

	@Inject(
		method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addDataSlots(Lnet/minecraft/world/inventory/ContainerData;)V")
	)
	public void BrewingStandMenu(
		int containerId,
		Inventory playerInventory,
		Container brewingStandContainer,
		ContainerData brewingStandData,
		CallbackInfo ci
	) {
		PotionBrewing potionbrewing = playerInventory.player.level().potionBrewing();
		addSlot(new BrewingStandMenu.PotionSlot(potionbrewing, brewingStandContainer, 5, 111, 51));
	}

	@ModifyArg(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/BrewingStandMenu;checkContainerSize(Lnet/minecraft/world/Container;I)V"
		),
		index = 1
	)
	protected int checkContainerSize(int minSize) {
		return minSize + 1;
	}

	@ModifyArg(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;"
		),
		index = 0
	)
	protected Slot init$addSlot(Slot slot) {
		if (slot.container == brewingStand) {
			switch (slot.getSlotIndex()) {
				case 0: ((SlotAccessor) slot).setX(50); break;
				case 1: ((SlotAccessor) slot).setX(70); ((SlotAccessor) slot).setY(64); break;
				case 2: ((SlotAccessor) slot).setX(91); ((SlotAccessor) slot).setY(64); break;
				case 3: ((SlotAccessor) slot).setX(80); break;
			}
		}
		return slot;
	}

	@Inject(method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
	public void quickMoveStack(
		Player player, int index,
		CallbackInfoReturnable<ItemStack> cir,
		@Share("index") LocalRef<Integer> indexRef
	) {
		indexRef.set(index);
	}

	@ModifyConstant(
		method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;",
				ordinal = 0
			),
			to = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/inventory/BrewingStandMenu$FuelSlot;mayPlaceItem(Lnet/minecraft/world/item/ItemStack;)Z",
				ordinal = 0
			)
		),
		constant = @Constant(intValue = 4)
	)
	public int quickMoveStack$alsoExclude5(int c, @Share("index") LocalRef<Integer> indexRef) {
		Integer index = indexRef.get();
		return index == 5 ? 5 : c;
	}

	@ModifyConstant(
		method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/inventory/BrewingStandMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",
				ordinal = 0
			)
		),
		constant = {@Constant(intValue = 5), @Constant(intValue = 32), @Constant(intValue = 41)}
	)
	public int quickMoveStack$5to6(int c) {
		return c + 1;
	}

	@WrapOperation(
		method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/BrewingStandMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"
		)
	)
	public boolean quickMoveStack$orMoveTo5(
		BrewingStandMenu instance,
		ItemStack itemStack,
		int startIndex, int endIndex,
		boolean reverseDirection,
		Operation<Boolean> original
	) {
		if (original.call(instance, itemStack, startIndex, endIndex, reverseDirection))
			return true;
		if (startIndex == 0 && endIndex == 3)
			return original.call(instance, itemStack, 5, 6, reverseDirection);
		return false;
	}
}
