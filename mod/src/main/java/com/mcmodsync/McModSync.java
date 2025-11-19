package com.mcmodsync;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(McModSync.MODID)
public class McModSync {
    public static final String MODID = "mcmodsync";
    public static final Logger LOGGER = LogUtils.getLogger();

    public McModSync(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("MC Mod Sync initializing...");
        // NetworkHandler 使用 @EventBusSubscriber 自动注册
    }
}
