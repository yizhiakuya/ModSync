package com.mcmodsync;

import com.mcmodsync.client.gui.ModSyncScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = McModSync.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            // 在主菜单添加按钮
            int buttonWidth = 200;
            int buttonHeight = 20;
            int x = event.getScreen().width / 2 - 100;
            int y = event.getScreen().height / 4 + 48 + 72; // 单人游戏下方

            Button syncButton = Button.builder(
                Component.literal("同步 Mod"),
                button -> Minecraft.getInstance().setScreen(new ModSyncScreen(event.getScreen()))
            ).bounds(x, y, buttonWidth, buttonHeight).build();

            event.addListener(syncButton);
        }
    }
}
