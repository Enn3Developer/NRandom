package com.enn3developer.nrandom;

import com.enn3developer.nrandom.spectreglass.NSpectreGlass;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        NSpectreGlass.preInit();
    }

    public void init(FMLInitializationEvent event) {
        NSpectreGlass.init();
    }
}
