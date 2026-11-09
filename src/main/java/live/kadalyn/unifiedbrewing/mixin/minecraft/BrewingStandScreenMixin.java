package live.kadalyn.unifiedbrewing.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends AbstractContainerScreen<BrewingStandMenu> {
	@Shadow @Final
	private static ResourceLocation BREW_PROGRESS_SPRITE;

	public BrewingStandScreenMixin(BrewingStandMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
	}

	@WrapOperation(
		method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"
		)
	)
	protected void renderBg$blitSprite(GuiGraphics instance, ResourceLocation sprite, int textureWidth, int textureHeight, int uPosition, int vPosition, int x, int y, int uWidth, int vHeight, Operation<Void> original) {
		// Adjust sprites for the wider work area
		if (sprite == BREW_PROGRESS_SPRITE)
			x += 3;
		else
			x--;
		original.call(instance, sprite, textureWidth, textureHeight, uPosition, vPosition, x, y, uWidth, vHeight);
	}
}
