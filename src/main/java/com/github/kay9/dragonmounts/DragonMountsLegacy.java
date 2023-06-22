package com.github.kay9.dragonmounts;

import com.github.kay9.dragonmounts.client.*;
import com.github.kay9.dragonmounts.dragon.DragonSpawnEgg;
import com.github.kay9.dragonmounts.dragon.TameableDragon;
import com.github.kay9.dragonmounts.dragon.breed.BreedRegistry;
import com.github.kay9.dragonmounts.dragon.egg.HatchableEggBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DragonMountsLegacy.MOD_ID)
public class DragonMountsLegacy
{
    public static final String MOD_ID = "dragonmounts";
    public static final Logger LOG = LogManager.getLogger();
    public static final SimpleChannel NETWORK;

    public DragonMountsLegacy()
    {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();

        DMLRegistry.init(bus);
//        BreedRegistry.DEFERRED_REGISTRY.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DMLConfig.COMMON);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DMLConfig.SERVER);
//        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, DMLConfig.CLIENT);

        setupEvents();
    }

    public static ResourceLocation id(String path)
    {
        return new ResourceLocation(MOD_ID, path);
    }

    static
    {
        var PROTOCOL_VERSION = "1.O";
        NETWORK = NetworkRegistry.ChannelBuilder.named(id("network"))
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .simpleChannel();
    }

    private static void setupEvents()
    {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.addListener(DragonMountsLegacy::attemptVanillaEggReplacement);

        bus.addListener((EntityAttributeCreationEvent e) -> e.put(DMLRegistry.DRAGON.get(), TameableDragon.createAttributes().build()));
        bus.addListener(BreedRegistry::hookRegistry);

        if (FMLLoader.getDist() == Dist.CLIENT) // Client Events
        {
            MinecraftForge.EVENT_BUS.addListener(DragonMountsLegacy::cameraAngles);
            MinecraftForge.EVENT_BUS.addListener(Keybinds::handleKeyPress);
            MinecraftForge.EVENT_BUS.addListener(MountControlsMessenger::tick);

            bus.addListener(DragonMountsLegacy::registerEggModelLoader);
            bus.addListener(DragonMountsLegacy::addToCreativeTab);
            bus.addListener((RegisterColorHandlersEvent.Item e) -> e.getItemColors().register(DragonSpawnEgg::getColor, DMLRegistry.SPAWN_EGG.get()));
            bus.addListener(DragonMountsLegacy::rendererRegistry);
            bus.addListener(Keybinds::registerKeybinds);
        }
    }

    private static void attemptVanillaEggReplacement(PlayerInteractEvent.RightClickBlock evt)
    {
        if (HatchableEggBlock.overrideVanillaDragonEgg(evt.getLevel(), evt.getPos(), evt.getEntity())) evt.setCanceled(true);
    }

    private static void addToCreativeTab(CreativeModeTabEvent.BuildContents evt)
    {
        if (evt.getTab() == CreativeModeTabs.SPAWN_EGGS) DragonSpawnEgg.populateTab(evt);
        if (evt.getTab() == CreativeModeTabs.FUNCTIONAL_BLOCKS) HatchableEggBlock.populateTab(evt);
    }

//    private static void defineBlockModels(ModelEvent.RegisterAdditional evt)
//    {
//        var dir = "models/block/dragon_eggs";
//        var length = "models/".length();
//        var suffixLength = ".json".length();
//        for (var entry : Minecraft.getInstance().getResourceManager().listResources(dir, f -> f.getPath().endsWith(".json")).entrySet())
//        {
//            var rl = entry.getKey();
//            var path = rl.getPath();
//            path = path.substring(length, path.length() - suffixLength);
//            var model = new ResourceLocation(rl.getNamespace(), path);
//            var id = path.substring("block/dragon_eggs/".length(), path.length() - "_dragon_egg".length());
//
//            evt.register(model);
//            DragonEggRenderer.MODEL_CACHE.put(new ResourceLocation(rl.getNamespace(), id), model);
//        }
//    }

    private static void registerEggModelLoader(ModelEvent.RegisterGeometryLoaders evt)
    {
        evt.register("dragon_egg", DragonEggModel.Loader.INSTANCE);
    }

    private static void rendererRegistry(EntityRenderersEvent.RegisterRenderers e)
    {
        e.registerEntityRenderer(DMLRegistry.DRAGON.get(), DragonRenderer::new);
        ForgeHooksClient.registerLayerDefinition(DragonRenderer.LAYER_LOCATION, DragonModel::createBodyLayer);

//        e.registerBlockEntityRenderer(DMLRegistry.EGG_BLOCK_ENTITY.get(), DragonEggRenderer::instance);
    }

    private static void cameraAngles(ViewportEvent.ComputeCameraAngles evt)
    {
        if (Minecraft.getInstance().player.getVehicle() instanceof TameableDragon)
        {
            var camera = evt.getCamera();
            var distance = 0;
            var vertical = 0;
            switch (Minecraft.getInstance().options.getCameraType())
            {
                case THIRD_PERSON_FRONT -> distance = 6;
                case THIRD_PERSON_BACK -> {
                    distance = 6;
                    vertical = 4;
                }
            }
            camera.move(-camera.getMaxZoom(distance), vertical, 0);
        }
    }
}