package com.justdie.affix;

import com.justdie.JustDying;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 词缀系统客户端初始化类
 */
public class AffixSystemClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(JustDying.MOD_ID + "_affix_client");
    
    @Override
    public void onInitializeClient() {
        if (!JustDying.getConfig().affixes.enableAffixes) {
            LOGGER.info("词缀系统已在配置中禁用，客户端初始化跳过");
            return;
        }
        
        LOGGER.info("初始化词缀系统客户端...");
        
        // 注册词缀事件处理器
        AffixEventHandlerClient.register();
        
        LOGGER.info("词缀系统客户端初始化完成");
    }
} 