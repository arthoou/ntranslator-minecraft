package com.arthou.ntranslator.fabric;

import com.arthou.ntranslator.NTranslator;
import net.fabricmc.api.ModInitializer;

public final class NTranslatorFabricMod implements ModInitializer {
    public static NTranslator INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE = new NTranslator();
    }
}
