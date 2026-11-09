package live.kadalyn.unifiedbrewing.mixin.herbalbrews;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import live.kadalyn.unifiedbrewing.mixin.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.satisfy.herbalbrews.client.gui.CauldronGui;
import net.satisfy.herbalbrews.client.gui.handler.CauldronGuiHandler;
import net.satisfy.herbalbrews.core.registry.ObjectRegistry;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Mixin(CauldronGui.class)
public abstract class CauldronGuiMixin extends AbstractContainerScreen<CauldronGuiHandler> implements Util.ICauldronGuiExtras {
	@Shadow @Final
	private static ResourceLocation BACKGROUND;
	@Unique
	private static final ResourceLocation unifiedbrewing$BACKGROUND2 = UnifiedBrewing.identifier("textures/gui/cauldron.png");
	@Shadow @Final
	private static int CATALYST_X;
	@Shadow @Final
	private static int CATALYST_Y;
	@Shadow @Final
	private static int CATALYST_TEX_X;
	@Shadow @Final
	private static int CATALYST_TEX_Y;
	@Shadow @Final
	private static int CATALYST_WIDTH;
	@Shadow @Final
	private static int CATALYST_HEIGHT;
	@Shadow @Final
	private Vector2i screenPos;

	public CauldronGuiMixin(CauldronGuiHandler menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@Unique
	private boolean unifiedbrewing$isBG2() {
		var output = menu.getSlot(3).getItem();
		var catalyst = menu.getSlot(4).getItem();
		return !catalyst.isEmpty() && !catalyst.is(ObjectRegistry.HERBAL_INFUSION)
			|| !output.isEmpty() && !HerbalBrewsCompat.isEmptyFlask(output);
	}

	@ModifyArg(
		method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
			ordinal = 0
		),
		index = 0
	)
	public ResourceLocation renderBg$blit(ResourceLocation atlasLocation) {
		if (unifiedbrewing$isBG2())
			return unifiedbrewing$BACKGROUND2;
		return atlasLocation;
	}

	@Inject(
		method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/satisfy/herbalbrews/client/gui/handler/CauldronGuiHandler;getSlot(I)Lnet/minecraft/world/inventory/Slot;"
		),
		cancellable = true
	)
	public void renderBgBeforeCatalyst(
		GuiGraphics guiGraphics,
		float partialTick,
		int mouseX, int mouseY,
		CallbackInfo ci
	) {
		if (!unifiedbrewing$isBG2()) {
			// Four charges per Herbal Infusion to match Alchemist Cauldron
			// note: this mod requires the empty flasks as balance so not recommended
			// to disable difficultFlaskRecipes at all really
			var fuel = ((Util.ICauldronGuiHandlerExtras) menu).unifiedbrewing$getFuel();
			var catalyst = menu.getSlot(4).getItem();
			if (fuel == 0 && catalyst.is(ObjectRegistry.HERBAL_INFUSION)) fuel = 4;
			int width = CATALYST_WIDTH * fuel / 4;
			// Height is too long but it doesn't matter lol
			guiGraphics.blit(BACKGROUND, screenPos.x() + CATALYST_X, screenPos.y() + CATALYST_Y,
				CATALYST_TEX_X, CATALYST_TEX_Y, width, CATALYST_HEIGHT);
		}
		ci.cancel();
	}

	@Inject(
		method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"
		)
	)
	public void renderMouseOverFuel(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (unifiedbrewing$isMouseOverFuel(mouseX, mouseY)) {
			int fuel = ((Util.ICauldronGuiHandlerExtras) menu).unifiedbrewing$getFuel();
			Component tooltip = Component.translatable("tooltip.unifiedbrewing.cauldron.remaining_fuel", fuel);
			guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
		}
	}

	@Unique
	public boolean unifiedbrewing$isMouseOverFuel(int mouseX, int mouseY) {
		ItemStack catalyst = menu.getSlot(4).getItem();
		if (!unifiedbrewing$isBG2()) {
			int left = screenPos.x() + CATALYST_X;
			int top = screenPos.y() + CATALYST_Y;
			// Height being too long matters here
			return mouseX >= left && mouseX < left + CATALYST_WIDTH && mouseY >= top && mouseY < top + 4;
		}
		return false;
	}
}
