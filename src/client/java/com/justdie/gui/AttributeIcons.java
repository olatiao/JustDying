package com.justdie.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import com.justdie.JustDying;

/**
 * 属性图标渲染器
 * 用于在GUI中渲染不同属性的图标
 */
public class AttributeIcons {
    // 图标纹理路径
    private static final Identifier TEXTURE = new Identifier(JustDying.MOD_ID, "textures/gui/attributes.png");

    // 图标尺寸
    public static final int ICON_SIZE = 16;

    // 纹理总宽度
    private static final int TEXTURE_SIZE = 128;

    /**
     * 渲染属性图标
     * 
     * @param context   绘制上下文
     * @param attributeId 属性ID
     * @param x         X坐标
     * @param y         Y坐标
     */
    public static void renderIcon(DrawContext context, String attributeId, int x, int y) {
        int iconIndex = getIconIndex(attributeId);
        int textureX = (iconIndex % (TEXTURE_SIZE / ICON_SIZE)) * ICON_SIZE;
        int textureY = (iconIndex / (TEXTURE_SIZE / ICON_SIZE)) * ICON_SIZE;
        
        context.drawTexture(TEXTURE, x, y, textureX, textureY, ICON_SIZE, ICON_SIZE);
    }

    /**
     * 获取属性图标的索引位置
     * 
     * @param attributeId 属性ID
     * @return 图标的索引位置
     */
    private static int getIconIndex(String attributeId) {
        // 使用哈希值来确定图标索引，确保相同的属性ID总是使用相同的图标
        // 将哈希值限制在可用图标数量范围内（假设有8个基础图标）
        return Math.abs(attributeId.hashCode() % 8);
    }
}