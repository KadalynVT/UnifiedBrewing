package live.kadalyn.unifiedbrewing;

import live.kadalyn.unifiedbrewing.compat.HerbalBrewsCompat;
import live.kadalyn.unifiedbrewing.compat.HexaliaCompat;
import live.kadalyn.unifiedbrewing.recipes.RecipeRemover;
import live.kadalyn.unifiedbrewing.registries.*;
import net.astralya.hexalia.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.satisfy.herbalbrews.core.registry.ObjectRegistry;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(UnifiedBrewing.MODID)
@EventBusSubscriber(modid = UnifiedBrewing.MODID)
public class UnifiedBrewing {
	// Define mod id in a common place for everything to reference
	public static final String MODID = "unifiedbrewing";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();

	public static ResourceLocation identifier(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	// The constructor for the mod class is the first code that is run when your mod is loaded.
	// FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
	public UnifiedBrewing(IEventBus modEventBus, ModContainer modContainer) {
		UBAttachments.register(modEventBus);
		UBConditions.register(modEventBus);
		UBDataComponents.register(modEventBus);
		UBFluids.register(modEventBus);
		UBItems.register(modEventBus);
		UBParticles.register(modEventBus);
		UBPotions.register(modEventBus);
		UBSounds.register(modEventBus);

		// Register our mod's ModConfigSpec so that FML can create and load the config file for us
		modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}

	@SubscribeEvent
	public static void buildContents(BuildCreativeModeTabContentsEvent event) {
		var tab = event.getTabKey();
		if (tab == CreativeModeTabs.INGREDIENTS && HerbalBrewsCompat.isLoaded()) {
			var flask = UBItems.EMPTY_FLASK.get();
			for (int i = 1; i < 7; ++i) {
				var x = new ItemStack(flask);
				x.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(i));
				event.accept(x);
			}
		}
		else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(UBItems.BLAZING_LADLE.get());
		}
	}

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		var generator = event.getGenerator();
		var packOutput = generator.getPackOutput();
		var lookupProvider = event.getLookupProvider();

		generator.addProvider(event.includeServer(),
			new UBRecipeProvider(packOutput, lookupProvider));
		generator.addProvider(event.includeServer(),
			new UBTagsProvider(packOutput, lookupProvider, event.getExistingFileHelper()));
	}

	@SubscribeEvent
	public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
		PotionBrewing.Builder builder = event.getBuilder();

		if (HerbalBrewsCompat.isLoaded()) {
			builder.addMix(
				Potions.WATER,
				ObjectRegistry.HERBAL_INFUSION.get(),
				UBPotions.HERBAL_TEA.getDelegate()
			);
		}
	}

	@SubscribeEvent
	public static void addReloadListeners(AddReloadListenerEvent event) {
		event.addListener(new RecipeRemover(event.getServerResources()));
	}

	@SubscribeEvent
	public static void modifyItems(ModifyDefaultComponentsEvent event) {
		if (!Config.SPEC.isLoaded()) return; // datagen
		modifyStackSize(event, Items.POTION, Config.POTION_STACK);
		modifyStackSize(event, Items.SPLASH_POTION, Config.POTION_STACK);
		modifyStackSize(event, Items.LINGERING_POTION, Config.POTION_STACK);
		if (HerbalBrewsCompat.isLoaded())
			modifyStackSize(event, ObjectRegistry.FLASK.get(), Config.FLASK_STACK);
		if (HexaliaCompat.isLoaded()) {
			event.getAllItems().filter(i -> i.builtInRegistryHolder().is(ModTags.Items.BREWS)).forEach(i -> {
				modifyStackSize(event, i, Config.BREW_STACK);
			});
		}
	}

	public static void modifyStackSize(ModifyDefaultComponentsEvent event, ItemLike item, ModConfigSpec.IntValue stackSize) {
		int num = stackSize.get();
		LOGGER.info("setting stack for {} to {}", item, num);
		event.modify(item, builder -> builder.set(DataComponents.MAX_STACK_SIZE, num));
	}
}
