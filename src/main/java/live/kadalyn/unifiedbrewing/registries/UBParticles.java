package live.kadalyn.unifiedbrewing.registries;

import com.mojang.serialization.MapCodec;
import live.kadalyn.unifiedbrewing.UnifiedBrewing;
import live.kadalyn.unifiedbrewing.particles.CauldronSwirlParticleOptions;
import live.kadalyn.unifiedbrewing.particles.SwirlParticleOptions;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class UBParticles {
	public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
		DeferredRegister.create(Registries.PARTICLE_TYPE, UnifiedBrewing.MODID);

	public static void register(IEventBus eventBus) {
		PARTICLE_TYPES.register(eventBus);
	}

	@MethodsReturnNonnullByDefault
	public static final Supplier<ParticleType<CauldronSwirlParticleOptions>> CAULDRON_SWIRL_PARTICLE =
		PARTICLE_TYPES.register("cauldron_swirl", () -> new ParticleType<CauldronSwirlParticleOptions>(false) {
			public MapCodec<CauldronSwirlParticleOptions> codec() {
				return CauldronSwirlParticleOptions.codec(this);
			}

			public StreamCodec<? super RegistryFriendlyByteBuf, CauldronSwirlParticleOptions> streamCodec() {
				return CauldronSwirlParticleOptions.streamCodec(this);
			}
		});

	@MethodsReturnNonnullByDefault
	public static final Supplier<ParticleType<SwirlParticleOptions>> SWIRL_PARTICLE =
		PARTICLE_TYPES.register("swirl", () -> new ParticleType<SwirlParticleOptions>(false) {
			public MapCodec<SwirlParticleOptions> codec() {
				return SwirlParticleOptions.codec(this);
			}

			public StreamCodec<? super RegistryFriendlyByteBuf, SwirlParticleOptions> streamCodec() {
				return SwirlParticleOptions.streamCodec(this);
			}
		});
}
