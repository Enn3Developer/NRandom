package com.enn3developer.nrandom;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean migrateSpectreGlass = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        migrateSpectreGlass = configuration.getBoolean(
            "migrateSpectreGlass",
            Configuration.CATEGORY_GENERAL,
            migrateSpectreGlass,
            "Replace Random Things Spectre Glass with N Spectre Glass in every chunk the first time it loads with this mod. Spectre Glass placed afterwards is left alone");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
