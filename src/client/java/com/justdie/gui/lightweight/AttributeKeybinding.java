package com.justdie.gui.lightweight;

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
    private static final String KEY_CATEGORY = "category." + JustDying.MOD_ID + ".general";
    private static final String KEY_NAME = "key." + JustDying.MOD_ID + ".open_lightweight_attributes";
    private static final int DEFAULT_KEY = GLFW.GLFW_KEY_L;
    
    private static KeyBinding attributeKey;
    
    /**
     * 注册按键绑定
     */
    public static void register() {
        // 创建按键绑定
        attributeKey = createAndRegisterKeyBinding(
                KEY_NAME,
                DEFAULT_KEY,
                KEY_CATEGORY);
        
        // 注册按键事件
        registerKeyPressEvent();
    }
    
    /**
     * 创建并注册按键绑定
     */
    private static KeyBinding createAndRegisterKeyBinding(String name, int defaultKey, String category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                name,
                InputUtil.Type.KEYSYM,
                defaultKey,
                category
        ));
    }
    
    /**
     * 注册按键事件
     */
    private static void registerKeyPressEvent() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (attributeKey.wasPressed() && client.player != null) {
                openAttributeScreen(client);
            }
        });
    }
    
    /**
     * 打开属性面板
     */
    private static void openAttributeScreen(MinecraftClient client) {
        client.setScreen(new LightweightAttributeScreen(client.player));
    }
} 