package com.tkisor.nekoprobe;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(NekoProbe.MODID)
public class NekoProbe {
    public static final String MODID = "nekoprobe";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NekoProbe(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                LOGGER.info("[NekoProbe] 正在扫描 NekoJS 事件注册表...");
                ProbeGenerator.generate();
            } catch (Exception e) {
                LOGGER.error("[NekoProbe] 生成类型声明文件失败！", e);
            }
        });
    }
}