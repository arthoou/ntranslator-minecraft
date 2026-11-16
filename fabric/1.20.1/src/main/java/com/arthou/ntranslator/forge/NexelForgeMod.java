package com.arthou.ntranslator.forge;

import com.arthou.ntranslator.NTranslator;
import com.arthou.ntranslator.network.UTServerNetworking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

@Mod(NTranslator.MOD_ID)
public final class NexelForgeMod {
    public static NTranslator INSTANCE;

    public NexelForgeMod() {
        INSTANCE = new NTranslator();
        NeoForge.EVENT_BUS.addListener(this::onPermissionGather);
        NeoForge.EVENT_BUS.addListener(this::onServerChat);
        if (FMLLoader.getDist() == Dist.CLIENT) {
            ForgeClientBootstrap.init();
        }
    }

    private void onPermissionGather(PermissionGatherEvent.Nodes event) {
        INSTANCE.getProxy().registerPermissions(event);
    }

    private void onServerChat(ServerChatEvent event) {
        if (!UTServerNetworking.INSTANCE.isBubbleChatEnabled(event.getPlayer())) {
            return;
        }

        var rawText = event.getRawText();
        if (rawText == null || rawText.isBlank()) {
            return;
        }

        event.setCanceled(true);
        UTServerNetworking.INSTANCE.handleBubbleChat(event.getPlayer(), rawText);
    }
}
