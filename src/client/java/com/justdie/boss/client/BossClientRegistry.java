package com.justdie.boss.client;

import com.justdie.boss.client.renderer.SorcererBossRenderer;
import com.justdie.boss.registry.BossRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import software.bernie.geckolib.GeckoLib;

/**
 * 客户端注册类，用于注册BOSS渲染器
 */
@Environment(EnvType.CLIENT)
public class BossClientRegistry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 初始化GeckoLib
        GeckoLib.initialize();
        
        // 注册法师BOSS渲染器
        EntityRendererRegistry.register(BossRegistry.SORCERER_BOSS, SorcererBossRenderer::new);
    }
} 