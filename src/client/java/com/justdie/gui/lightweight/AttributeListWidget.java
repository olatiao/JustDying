package com.justdie.gui.lightweight;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 属性列表组件
 */
public class AttributeListWidget {
    private final LightweightAttributeScreen parent;
    private final Consumer<AttributeEntry> upgradeCallback;
    private int x;
    private int y;
    private int width;
    private int height;
    private int itemHeight;
    
    private List<AttributeItemEntry> entries = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_SCROLL_SPEED = 10;
    
    public AttributeListWidget(LightweightAttributeScreen parent, int x, int y, int width, int height, 
                             int itemHeight, Consumer<AttributeEntry> upgradeCallback) {
        this.parent = parent;
        this.upgradeCallback = upgradeCallback;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.itemHeight = itemHeight;
    }
    
    public void addEntry(AttributeEntry entry) {
        this.entries.add(new AttributeItemEntry(entry, this.width, this.upgradeCallback));
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制背景
        context.fill(x, y, x + width, y + height, 0x40000000);
        
        // 计算可见条目
        int visibleItems = height / itemHeight;
        int startIndex = Math.max(0, Math.min(scrollOffset, entries.size() - visibleItems));
        
        // 渲染可见条目
        for (int i = 0; i < visibleItems && i + startIndex < entries.size(); i++) {
            AttributeItemEntry entry = entries.get(i + startIndex);
            entry.render(context, x, y + i * itemHeight, width, itemHeight, mouseX, mouseY);
        }
        
        // 如果需要滚动条
        if (entries.size() > visibleItems) {
            renderScrollbar(context, startIndex, visibleItems);
        }
    }
    
    private void renderScrollbar(DrawContext context, int startIndex, int visibleItems) {
        int scrollbarHeight = Math.max(20, height * visibleItems / entries.size());
        int scrollbarY = y + (height - scrollbarHeight) * startIndex / Math.max(1, entries.size() - visibleItems);
        
        // 绘制滚动条
        context.fill(x + width - 6, scrollbarY, x + width - 2, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }
        
        // 计算可见条目
        int visibleItems = height / itemHeight;
        int startIndex = Math.max(0, Math.min(scrollOffset, entries.size() - visibleItems));
        
        // 检查点击的条目
        for (int i = 0; i < visibleItems && i + startIndex < entries.size(); i++) {
            AttributeItemEntry entry = entries.get(i + startIndex);
            if (mouseY >= y + i * itemHeight && mouseY < y + (i + 1) * itemHeight) {
                return entry.mouseClicked(mouseX, mouseY, button);
            }
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }
        
        // 计算滚动
        int visibleItems = height / itemHeight;
        int maxScroll = Math.max(0, entries.size() - visibleItems);
        
        // 更新滚动偏移
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(amount * 3)));
        
        return true;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getX() {
        return this.x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public List<Element> children() {
        List<Element> result = new ArrayList<>();
        for (AttributeItemEntry entry : entries) {
            result.addAll(entry.getButtons());
        }
        return result;
    }
    
    /**
     * 属性条目组件
     */
    public static class AttributeItemEntry {
        private static final int BUTTON_WIDTH = 20;
        private static final int BUTTON_HEIGHT = 20;
        private static final int BUTTON_PADDING = 5;
        
        private final AttributeEntry entry;
        private final Consumer<AttributeEntry> upgradeCallback;
        private final TextRenderer textRenderer;
        private final ButtonWidget increaseButton;
        private final ButtonWidget decreaseButton;
        
        private final int width;
        
        public AttributeItemEntry(AttributeEntry entry, int width,
                                  Consumer<AttributeEntry> upgradeCallback) {
            this.entry = entry;
            this.upgradeCallback = upgradeCallback;
            this.textRenderer = MinecraftClient.getInstance().textRenderer;
            this.width = width;
            
            // 创建增加按钮
            this.increaseButton = createButton("+", width - 45, (button) -> {
                if (upgradeCallback != null) {
                    upgradeCallback.accept(entry);
                }
            });
            
            // 创建减少按钮
            this.decreaseButton = createButton("-", width - 70, (button) -> {
                // 减少属性点
                if (entry.currentValue > entry.minValue) {
                    entry.currentValue--;
                    // 这里可以添加更多逻辑，如发送到服务器
                }
            });
        }
        
        /**
         * 创建按钮的工具方法
         */
        private ButtonWidget createButton(String text, int x, ButtonWidget.PressAction action) {
            return ButtonWidget.builder(
                    Text.literal(text),
                    action)
                    .dimensions(x, BUTTON_PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
        }
        
        public List<Element> getButtons() {
            return List.of(increaseButton, decreaseButton);
        }
        
        public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
            // 计算背景颜色 - 处理高亮效果
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            int backgroundColor = calculateBackgroundColor(hovered);
            
            // 渲染背景和边框
            renderBackground(context, x, y, width, height, backgroundColor);
            
            // 渲染文本内容
            renderTexts(context, x, y);
            
            // 更新和渲染按钮
            updateAndRenderButtons(context, x, y, mouseX, mouseY, 0);
        }
        
        /**
         * 计算背景颜色，处理高亮动画
         */
        private int calculateBackgroundColor(boolean hovered) {
            int backgroundColor = hovered ? 0x40444444 : 0x20222222;
            
            // 如果有高亮动画，调整背景色
            if (entry.highlightAnimation != null && entry.highlightAnimation.isRunning()) {
                float highlightValue = entry.highlightAnimation.getValue();
                int highlightColor = (int)(highlightValue * 128) << 24 | 0x005500;
                backgroundColor = backgroundColor | highlightColor;
            }
            
            return backgroundColor;
        }
        
        /**
         * 渲染背景和边框
         */
        private void renderBackground(DrawContext context, int x, int y, int width, int height, int backgroundColor) {
            context.fill(x, y, x + width, y + height, backgroundColor);
            context.drawBorder(x, y, width, height, 0x80808080);
        }
        
        /**
         * 渲染文本内容
         */
        private void renderTexts(DrawContext context, int x, int y) {
            // 渲染属性名称
            context.drawTextWithShadow(textRenderer, entry.name, x + 5, y + 5, 0xFFFFFF);
            
            // 渲染属性值
            String valueText = entry.currentValue + " / " + entry.maxValue;
            context.drawTextWithShadow(textRenderer, valueText, x + 5, y + 15, 0xCCCCCC);
            
            // 渲染属性效果
            String effectText = entry.getEffectText();
            context.drawTextWithShadow(textRenderer, effectText, x + width / 2, y + 15, 0xAAFFAA);
            
            // 渲染花费点数
            String costText = "花费: " + entry.costPerPoint;
            int costWidth = textRenderer.getWidth(costText);
            context.drawTextWithShadow(textRenderer, costText, x + width - costWidth - 80, y + 5, 0xFFDD88);
        }
        
        /**
         * 更新和渲染按钮
         */
        private void updateAndRenderButtons(DrawContext context, int x, int y, int mouseX, int mouseY, float tickDelta) {
            // 更新按钮位置
            increaseButton.setX(x + width - 45);
            increaseButton.setY(y + BUTTON_PADDING);
            decreaseButton.setX(x + width - 70);
            decreaseButton.setY(y + BUTTON_PADDING);
            
            // 渲染按钮
            increaseButton.render(context, mouseX, mouseY, tickDelta);
            decreaseButton.render(context, mouseX, mouseY, tickDelta);
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean result = increaseButton.mouseClicked(mouseX, mouseY, button);
            if (!result) {
                result = decreaseButton.mouseClicked(mouseX, mouseY, button);
            }
            return result;
        }
    }
} 