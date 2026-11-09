package live.kadalyn.unifiedbrewing.registries;

import com.mojang.serialization.Codec;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class UBAttachments {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
		DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, UnifiedBrewing.MODID);

	public static final Supplier<AttachmentType<Integer>> COOK_TIME =
		ATTACHMENTS.register("cook_time", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

	public static void register(IEventBus eventBus) {
		ATTACHMENTS.register(eventBus);
	}
}
