package com.github.kay9.dragonmounts.dragon.breed;

import com.github.kay9.dragonmounts.DragonMountsLegacy;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public class BreedRegistry
{
    public static final ResourceKey<Registry<DragonBreed>> REGISTRY_KEY = ResourceKey.createRegistryKey(DragonMountsLegacy.id("dragon_breeds"));
    public static final DeferredRegister<DragonBreed> DEFERRED_REGISTRY = DeferredRegister.create(REGISTRY_KEY, DragonMountsLegacy.MOD_ID);
    public static final Supplier<IForgeRegistry<DragonBreed>> REGISTRY = DEFERRED_REGISTRY.makeRegistry(DragonBreed.class, () -> new RegistryBuilder<DragonBreed>()
            .disableSaving()
            .dataPackRegistry(DragonBreed.CODEC, DragonBreed.NETWORK_CODEC)
            .setDefaultKey(DragonBreed.BuiltIn.FIRE_BUILTIN.getId()));

    public static DragonBreed get(String byString, RegistryAccess reg)
    {
        return get(new ResourceLocation(byString), reg);
    }

    public static DragonBreed get(ResourceLocation byId, RegistryAccess reg)
    {
        var breed = registry(reg).get(byId);
        if (breed == null) breed = getFallback(reg); // guard for if/when the registry is not defaulted...
        return breed;
    }

    public static DragonBreed getFallback(RegistryAccess reg)
    {
        return get(DragonBreed.BuiltIn.FIRE_BUILTIN.getId(), reg);
    }

    public static Registry<DragonBreed> registry(RegistryAccess reg)
    {
        var opt = reg.registry(REGISTRY_KEY);
        if (opt.isPresent()) return opt.get();
        return RegistryAccess.BUILTIN.get().registryOrThrow(REGISTRY_KEY); // last resort.
    }
}
