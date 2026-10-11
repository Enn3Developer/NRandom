package com.enn3developer.nrandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = NRandom.MODID,
    version = Tags.VERSION,
    name = NRandom.NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "after:RandomThings")
public class NRandom {

    public static final String MODID = "nrandom";
    public static final String NAME = "NRandom";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "com.enn3developer.nrandom.ClientProxy",
        serverSide = "com.enn3developer.nrandom.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG.info("Loading NRandom {}", Tags.VERSION);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
