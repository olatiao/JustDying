package com.justdie.gui;

import com.justdie.attribute.JustDyingAttributeType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * 属性图标渲染器
 * 用于在GUI中渲染不同属性的图标
 */
public class AttributeIcons {
    // 图标纹理路径
    private static final Identifier TEXTURE = new Identifier("justdying", "textures/gui/effects.png");

    // 图标尺寸
    public static final int ICON_SIZE = 18;

    // 纹理总宽度
    private static final int TEXTURE_WIDTH = 594;

    /**
     * 渲染属性图标
     * 
     * @param context   绘制上下文
     * @param attribute 属性类型
     * @param x         X坐标
     * @param y         Y坐标
     */
    public static void renderIcon(DrawContext context, JustDyingAttributeType attribute, int x, int y) {
        int iconIndex = getIconIndex(attribute);
        int u = iconIndex * ICON_SIZE;
        int v = 0;

        context.drawTexture(TEXTURE, x, y, u, v, ICON_SIZE, ICON_SIZE, TEXTURE_WIDTH, ICON_SIZE);
    }

    /**
     * 获取属性图标的索引位置
     * 
     * @param attribute 属性类型
     * @return 图标的索引位置
     */
    private static int getIconIndex(JustDyingAttributeType attribute) {
        return switch (attribute) {
            case CONSTITUTION -> 21; // 体质图标在第22个位置（索引从0开始，所以是21）
            case STRENGTH -> 17; // 力量图标在第18个位置
            case DEFENSE -> 22; // 防御图标在第23个位置
            case SPEED -> 26; // 速度图标在第27个位置
            case LUCK -> 16; // 运气图标在第17个位置
        };
    }
}