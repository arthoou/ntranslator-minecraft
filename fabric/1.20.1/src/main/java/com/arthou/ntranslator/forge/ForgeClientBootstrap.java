package com.arthou.ntranslator.forge;

import com.arthou.ntranslator.NTranslator;
import com.arthou.ntranslator.client.NTranslatorClient;
import com.arthou.ntranslator.client.books.BookTranslationManager;
import com.arthou.ntranslator.client.gui.UTConfigScreen;
import com.arthou.ntranslator.client.signs.SignTranslationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SignBlock;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class ForgeClientBootstrap {
    private static boolean initialized;

    private ForgeClientBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        new NTranslatorClient();
        NTranslatorClient.registerKeys();
        NeoForge.EVENT_BUS.addListener(ForgeClientBootstrap::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ForgeClientBootstrap::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ForgeClientBootstrap::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(ForgeClientBootstrap::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(ForgeClientBootstrap::onCharacterTyped);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof SignBlock)) {
            return;
        }

        if (event.getEntity().isShiftKeyDown()) {
            return;
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SignBlock) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        SignTranslationManager.INSTANCE.openTranslatedSign(event.getPos());
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (SignTranslationManager.INSTANCE.shouldReplaceSignScreen(event.getScreen())) {
            SignTranslationManager.INSTANCE.reopenPendingTranslation();
            return;
        }

        if (event.getScreen() instanceof BookViewScreen bookViewScreen) {
            BookTranslationManager.INSTANCE.installButton(bookViewScreen, event::addListener);
            return;
        }

        if (event.getScreen() instanceof BookEditScreen bookEditScreen) {
            BookTranslationManager.INSTANCE.installWritableButton(bookEditScreen, event::addListener);
            return;
        }

        if (!(event.getScreen() instanceof PauseScreen)) {
            return;
        }

        ButtonPosition position = findPauseMenuButtonPosition(event.getScreen().width, event.getScreen().height, event.getListenersList());
        int x = position.x();
        int y = position.y();
        event.addListener(new NexelConfigButton(x, y, event.getScreen()));
    }

    private static ButtonPosition findPauseMenuButtonPosition(int screenWidth, int screenHeight, List<GuiEventListener> listeners) {
        AbstractWidget options = findOptionsButton(listeners);
        if (options != null) {
            int x = options.getX() - NexelConfigButton.SIZE - 6;
            int y = options.getY() + (options.getHeight() - NexelConfigButton.SIZE) / 2;
            return new ButtonPosition(x, y);
        }

        return new ButtonPosition(screenWidth / 2 - 123, screenHeight / 4 + 82);
    }

    private static AbstractWidget findOptionsButton(List<GuiEventListener> listeners) {
        String optionsText = Component.translatable("menu.options").getString();
        for (GuiEventListener listener : listeners) {
            if (!(listener instanceof AbstractWidget widget)) {
                continue;
            }

            if (widget instanceof AbstractButton && widget.getMessage().getString().equals(optionsText)) {
                return widget;
            }
        }

        return null;
    }

    private record ButtonPosition(int x, int y) {
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof BookViewScreen || event.getScreen() instanceof BookEditScreen) {
            BookTranslationManager.INSTANCE.stopTts();
        }
    }

    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if ((event.getScreen() instanceof BookViewScreen || event.getScreen() instanceof BookEditScreen)
            && event.getKeyCode() == 256) {
            BookTranslationManager.INSTANCE.stopTts();
            return;
        }

        if (event.getScreen() instanceof BookEditScreen bookEditScreen
            && BookTranslationManager.INSTANCE.isWritableTranslated(bookEditScreen)) {
            event.setCanceled(true);
        }
    }

    private static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (event.getScreen() instanceof BookEditScreen bookEditScreen
            && BookTranslationManager.INSTANCE.isWritableTranslated(bookEditScreen)) {
            event.setCanceled(true);
        }
    }

    private static final class NexelConfigButton extends AbstractWidget {
        private static final int SIZE = 18;
        private final net.minecraft.client.gui.screens.Screen parent;

        private NexelConfigButton(int x, int y, net.minecraft.client.gui.screens.Screen parent) {
            super(x, y, SIZE, SIZE, Component.translatable("ntranslator.open_config"));
            this.parent = parent;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            Minecraft.getInstance().setScreen(new UTConfigScreen(parent));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int background = isHoveredOrFocused() ? 0xE63C4D63 : 0xCC202B38;
            int outline = isHoveredOrFocused() ? 0xFFF4B74E : 0xAA5D738F;
            int textColor = isHoveredOrFocused() ? 0xFFF7FBFF : 0xFFE7F0FF;

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            guiGraphics.renderOutline(getX(), getY(), width, height, outline);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, "N", getX() + width / 2, getY() + 5, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
