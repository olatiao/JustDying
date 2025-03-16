package com.justdie.gui;

import com.justdie.JustDying;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * 属性面板按键绑定
 */
public class AttributeKeybinding {
    private static KeyBinding attributeKey;
    
    /**
     * 注册按键绑定
     */
    public static void register() {
        // 创建按键绑定
        attributeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + JustDying.MOD_ID + ".open_attributes",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category." + JustDying.MOD_ID + ".general"
        ));
        
        // 注册按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (attributeKey.wasPressed()) {
                openAttributeScreen(client);
            }
        });
    }
    
    /**
     * 打开属性面板
     * 
     * @param client Minecraft客户端实例
     */
    private static void openAttributeScreen(MinecraftClient client) {
        if (client.player != null) {
            client.setScreen(new AttributeScreen(client.player));
        }
    }
} 