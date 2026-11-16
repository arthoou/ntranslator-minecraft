package com.arthou.ntranslator.fabric;

import com.arthou.ntranslator.client.books.BookTranslationManager;
import com.arthou.ntranslator.client.signs.SignTranslationManager;
import com.arthou.ntranslator.mixin.ScreenAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SignBlock;

import java.util.function.Consumer;

public final class FabricClientBootstrap {
    private static boolean initialized;

    private FabricClientBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

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

            if (player.getItemInHand(hand).getItem() instanceof BlockItem
                && ((BlockItem) player.getItemInHand(hand).getItem()).getBlock() instanceof SignBlock) {
                SignTranslationManager.INSTANCE.allowVanillaEdit();
                return InteractionResult.PASS;
            }

            SignTranslationManager.INSTANCE.openTranslatedSign(hitResult.getBlockPos());
            return InteractionResult.SUCCESS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            onScreenInit(screen);

            ScreenEvents.remove(screen).register(removedScreen -> {
                if (removedScreen instanceof BookViewScreen || removedScreen instanceof BookEditScreen) {
                    BookTranslationManager.INSTANCE.stopTts();
                }
            });
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BookTranslationManager.INSTANCE.stopTts());
    }

    private static Consumer<AbstractWidget> adder(Screen screen) {
        return widget -> ((ScreenAccessor) screen).ntranslator$addRenderableWidget(widget);
    }

    private static void onScreenInit(Screen screen) {
        if (SignTranslationManager.INSTANCE.shouldReplaceSignScreen(screen)) {
            SignTranslationManager.INSTANCE.reopenPendingTranslation();
            return;
        }

        if (screen instanceof BookViewScreen) {
            BookTranslationManager.INSTANCE.installButton((BookViewScreen) screen, adder(screen));
            return;
        }

        if (screen instanceof BookEditScreen) {
            BookTranslationManager.INSTANCE.installWritableButton((BookEditScreen) screen, adder(screen));
        }
    }
}
