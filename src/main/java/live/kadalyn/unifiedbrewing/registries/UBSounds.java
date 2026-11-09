package live.kadalyn.unifiedbrewing.registries;

import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class UBSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS =
		DeferredRegister.create(Registries.SOUND_EVENT, UnifiedBrewing.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> BOTTLE_WHIFF = registerSoundEvent("bottle_whiff");

	private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(UnifiedBrewing.MODID, name)));
	}

	public static void register(IEventBus eventBus) {
		SOUNDS.register(eventBus);
	}
}
