package com.arthou.ntranslator.fabric;

import com.arthou.ntranslator.client.books.BookTranslationManager;
import com.arthou.ntranslator.client.signs.SignTranslationManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SignBlock;

import java.util.List;

public final class FabricClientBootstrap {
    private static boolean initialized;

    private FabricClientBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BookTranslationManager.INSTANCE.tick(client);
            SignTranslationManager.INSTANCE.tick();
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            if (!(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof SignBlock)) {
                return InteractionResult.PASS;
            }

            // Agachado ou com uma placa na mao: e a interacao normal do jogo.
            // Avisamos o gerenciador para nao trocar a tela de edicao.
            if (player.isShiftKeyDown()) {
                SignTranslationManager.INSTANCE.allowVanillaEdit();
                return InteractionResult.PASS;
            }

            if (player.getItemInHand(hand).getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SignBlock) {
                SignTranslationManager.INSTANCE.allowVanillaEdit();
                return InteractionResult.PASS;
            }

            SignTranslationManager.INSTANCE.openTranslatedSign(hitResult.getBlockPos());
            return InteractionResult.SUCCESS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            onScreenInit(screen, Screens.getButtons(screen));

            ScreenEvents.remove(screen).register(removedScreen -> {
                if (removedScreen instanceof BookViewScreen || removedScreen instanceof BookEditScreen) {
                    BookTranslationManager.INSTANCE.stopTts();
                }
            });
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BookTranslationManager.INSTANCE.stopTts());
    }

    private static void onScreenInit(net.minecraft.client.gui.screens.Screen screen, List<? extends GuiEventListener> listeners) {
        if (SignTranslationManager.INSTANCE.shouldReplaceSignScreen(screen)) {
            SignTranslationManager.INSTANCE.reopenPendingTranslation();
            return;
        }

        if (screen instanceof BookViewScreen bookViewScreen) {
            BookTranslationManager.INSTANCE.installButton(bookViewScreen, widget -> {
                Screens.getButtons(screen).add((AbstractWidget) widget);
            });
            return;
        }

        if (screen instanceof BookEditScreen bookEditScreen) {
            BookTranslationManager.INSTANCE.installWritableButton(bookEditScreen, widget -> {
                Screens.getButtons(screen).add((AbstractWidget) widget);
            });
        }
    }
}
