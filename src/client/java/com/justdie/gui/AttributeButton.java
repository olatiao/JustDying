package com.justdie.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 自定义属性按钮
 * 提供与属性面板背景协调的样式
 */
public class AttributeButton extends ButtonWidget {
    // 按钮颜色
    private static final int NORMAL_COLOR = 0xFF303030;
    private static final int HOVERED_COLOR = 0xFF404040;
    private static final int DISABLED_COLOR = 0xFF202020;
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    
    public AttributeButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }
    
    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        // 选择按钮颜色
        int backgroundColor;
        if (!this.active) {
            backgroundColor = DISABLED_COLOR; // 禁用状态
        } else if (this.isHovered()) {
            backgroundColor = HOVERED_COLOR; // 悬停状态
        } else {
            backgroundColor = NORMAL_COLOR; // 正常状态
        }
        
        // 绘制按钮背景
        context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), backgroundColor);
        
        // 绘制按钮边框
        context.drawBorder(this.getX(), this.getY(), this.getWidth(), this.getHeight(), BORDER_COLOR);
        
        // 获取文本渲染器
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // 绘制按钮文本 - 针对不同大小的按钮进行调整
        String text = this.getMessage().getString();
        int textWidth = textRenderer.getWidth(this.getMessage());
        
        // 计算文本位置 - 根据按钮大小调整
        int textX;
        int textY;
        
        if (this.getWidth() <= 15 && this.getHeight() <= 15) {
            // 小按钮特殊处理
            if (text.equals("+") || text.equals("-") || text.equals("↑") || text.equals("↓")) {
                // 单字符符号按钮居中显示
                textX = this.getX() + (this.getWidth() - textRenderer.getWidth(text)) / 2;
                textY = this.getY() + (this.getHeight() - 8) / 2 + 1; // 垂直微调以更好地居中
            } else {
                // 其他文本
                textX = this.getX() + (this.getWidth() - textWidth) / 2;
                textY = this.getY() + (this.getHeight() - 8) / 2;
            }
        } else {
            // 正常大小按钮
            textX = this.getX() + (this.getWidth() - textWidth) / 2;
            textY = this.getY() + (this.getHeight() - 8) / 2;
        }
        
        // 绘制文本阴影
        context.drawTextWithShadow(textRenderer, this.getMessage(), textX, textY, TEXT_COLOR);
    }
} 