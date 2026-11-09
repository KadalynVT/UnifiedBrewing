package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.satisfy.herbalbrews.client.gui.handler.CauldronGuiHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Mixin(CauldronGuiHandler.class)
public abstract class CauldronGuiHandlerMixin extends AbstractContainerMenu implements Util.ICauldronGuiHandlerExtras {
	@Shadow @Final
	private ContainerData data;

	protected CauldronGuiHandlerMixin(@Nullable MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@ModifyArgs(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/satisfy/herbalbrews/client/gui/handler/CauldronGuiHandler;<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V"
		)
	)
	private static void init(Args args) {
		// Add fuel amount
		args.set(3, new SimpleContainerData(3));
	}

	@ModifyArgs(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/satisfy/herbalbrews/client/gui/handler/CauldronGuiHandler;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;"
		)
	)
	protected void modifyOutputSlot(Args args) {
		Slot slot = args.get(0);
		if (slot.getSlotIndex() == 3 && !(slot.container instanceof Inventory)) {
			IItemHandler wrappedInventory = new InvWrapper(slot.container);
			args.set(0, new HerbalBrewsCompat.OutputSlot(wrappedInventory, 3, slot.x, slot.y));
		}
	}

	@Intrinsic
	public void broadcastChanges() {
		// this is bad, sorry
		var itemStack = this.slots.get(3).getItem();
		var origMax = itemStack.get(DataComponents.MAX_STACK_SIZE);
		itemStack.set(DataComponents.MAX_STACK_SIZE, HerbalBrewsCompat.getMaxOutputStackSize(itemStack));
		super.broadcastChanges();
		itemStack.set(DataComponents.MAX_STACK_SIZE, origMax);
	}

	@WrapMethod(method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;")
	public @NotNull ItemStack quickMoveStack(Player player, int index, Operation<ItemStack> original) {
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			var itemStack2 = slot.getItem();
			var itemStack = itemStack2.copy();
			if (index < 0 || index > 4) {
				for (int i : new int[]{3, 4, 0}) {
					if (this.slots.get(i).mayPlace(itemStack2)) {
						if (!this.moveItemStackTo(itemStack2, i, i == 0 ? 3 : i+1, false)) {
							continue;
						}

						if (itemStack2.isEmpty()) {
							slot.setByPlayer(ItemStack.EMPTY);
						} else {
							slot.setChanged();
						}

						if (itemStack2.getCount() == itemStack.getCount()) {
							return ItemStack.EMPTY;
						}

						slot.onTake(player, itemStack);
						return itemStack;
					}
				}
			}
		}
		return original.call(player, index);
	}

	@Intrinsic
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (clickType == ClickType.SWAP && slotId == 3 && button >= 0 && button < 9) {
			var slot = this.slots.get(slotId);
			var slottedItem = slot.getItem();
			if (slottedItem.getCount() > slottedItem.getMaxStackSize()) {
				if (slot.mayPickup(player)) {
					var inventory = player.getInventory();
					if (inventory.getItem(button).isEmpty()) {
						var take = slottedItem.split(slottedItem.getMaxStackSize());
						inventory.setItem(button, take);
						slot.onTake(player, take);
					}
				}
				return;
			}
		}
		super.clicked(slotId, button, clickType, player);
	}

	@Unique
	public int unifiedbrewing$getFuel() { return data.get(2); }
}
