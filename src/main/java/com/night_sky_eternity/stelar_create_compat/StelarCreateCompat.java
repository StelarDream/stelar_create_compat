package com.night_sky_eternity.stelar_create_compat;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(StelarCreateCompat.ID)
public class StelarCreateCompat {
    public static final String ID = "stelar_create_compat";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public StelarCreateCompat(IEventBus modBus) {

    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

}
