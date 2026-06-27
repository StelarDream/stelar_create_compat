package com.night_sky_eternity.stelar_create_compat;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(StelarCreateCompat.ID)
public class StelarCreateCompat {
    public static final String ID = "stelar_create_compat";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public StelarCreateCompat(IEventBus modBus) {
        modBus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.ItemHandler.BLOCK,
            (level, pos, state, be, side) -> side == Direction.UP ? LavaCauldronItemHandler.INSTANCE : null,
            Blocks.LAVA_CAULDRON
        );
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

}
