package com.arthou.ntranslator.fabric;

import net.fabricmc.api.ClientModInitializer;

public final class NexelFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientBootstrap.init();
    }
}
