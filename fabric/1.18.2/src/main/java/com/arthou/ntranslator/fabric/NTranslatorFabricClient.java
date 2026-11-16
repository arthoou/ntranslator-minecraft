package com.arthou.ntranslator.fabric;

import net.fabricmc.api.ClientModInitializer;

public final class NTranslatorFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientBootstrap.init();
    }
}
