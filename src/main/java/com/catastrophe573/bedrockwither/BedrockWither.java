package com.catastrophe573.bedrockwither;

import com.catastrophe573.bedrockwither.entity.EntityBedrockWither;
import com.catastrophe573.bedrockwither.entity.EntityBedrockWitherSkull;
import com.catastrophe573.bedrockwither.renderer.RendererBedrockWither;
import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("bwither")
public class BedrockWither
{
	// Directly reference a slf4j logger
	private static final Logger LOGGER = LogUtils.getLogger();
	private static int TEMP_LOGGING_LEVEL = 0;

	public static final String MOD_ID = "bwither";
	public static final String BEDROCK_WITHER_ID = "bedrock_wither";
	public static final String BEDROCK_WITHER_SKULL_ID = "bwither_skull";

	public static final DeferredRegister<EntityType<?>> ENTITY_REG = DeferredRegister.create(ForgeRegistries.ENTITIES, BedrockWither.MOD_ID);

	public static final RegistryObject<EntityType<EntityBedrockWither>> BEDROCK_WITHER = ENTITY_REG.register(BEDROCK_WITHER_ID, () -> EntityType.Builder.of(EntityBedrockWither::new, MobCategory.MONSTER).sized(1.0f, 3.0f).fireImmune().immuneTo(Blocks.WITHER_ROSE).setUpdateInterval(2).clientTrackingRange(10).setShouldReceiveVelocityUpdates(true).build("bedrock_wither"));

	public static final RegistryObject<EntityType<EntityBedrockWitherSkull>> BEDROCK_WITHER_SKULL = ENTITY_REG.register(BEDROCK_WITHER_SKULL_ID, () -> EntityType.Builder.<EntityBedrockWitherSkull>of(EntityBedrockWitherSkull::new, MobCategory.MISC).sized(0.3125f, 0.3125f).clientTrackingRange(4).updateInterval(10).build("bedrock_wither_skull"));

	public static final ForgeEvents eventHandler = new ForgeEvents();
		
	public BedrockWither()
	{
		// Register the setup method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		// Register the enqueueIMC method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		// Register the processIMC method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

		ENTITY_REG.register(FMLJavaModLoadingContext.get().getModEventBus());

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(eventHandler);
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void setup(final FMLCommonSetupEvent event)
	{
		//LOGGER.info("Mod Bedrock Wither starting up!");
	}

	private void enqueueIMC(final InterModEnqueueEvent event)
	{
	}

	private void processIMC(final InterModProcessEvent event)
	{
	}

	// You can use SubscribeEvent and let the Event Bus discover methods to call
	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event)
	{
	}

	// You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD Event bus for receiving Registry Events)
	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = MOD_ID)
	public static class RegistryEvents
	{
		@SubscribeEvent
		public static void onAttributeCreate(EntityAttributeCreationEvent event)
		{
			event.put(BEDROCK_WITHER.get(), EntityBedrockWither.createAttributes().build());
		}

		@SubscribeEvent
		public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event)
		{
		}

		@SubscribeEvent
		public static void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event)
		{
			event.registerEntityRenderer(BEDROCK_WITHER.get(), RendererBedrockWither::new);
			event.registerEntityRenderer(BEDROCK_WITHER_SKULL.get(), WitherSkullRenderer::new);
		}
	}
	
	public static void logInfo(String message)
	{
		if ( TEMP_LOGGING_LEVEL == 1 )
		{
			LOGGER.info(message);
		}
	}

	public static void logError(String message)
	{
		LOGGER.error(message);
	}
}
