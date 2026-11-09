package live.kadalyn.unifiedbrewing.registries;

import com.mojang.serialization.MapCodec;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class UBConditions {
	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
		DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, UnifiedBrewing.MODID);

	public static final Supplier<MapCodec<UBConfigCondition>> CONFIG =
		CONDITION_CODECS.register("config", () -> UBConfigCondition.CODEC);

	public static void register(IEventBus eventBus) {
		CONDITION_CODECS.register(eventBus);
	}

}
