package com.justdie.gui.lightweight;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeHelper;
import com.justdie.attribute.AttributeRegistry;
import com.justdie.network.ClientAttributePackets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;

/**
 * 轻量级属性屏幕
 * 使用更简洁的界面设计
 */
public class LightweightAttributeScreen extends Screen {
    // 常量
    protected static final int PANEL_WIDTH = 280;
    protected static final int ITEM_HEIGHT = 24;
    protected static final int PADDING = 10;
    protected static final int ANIMATION_DURATION = 300;
    protected static final int FADE_DURATION = 200;
    protected static final int PROGRESS_BAR_HEIGHT = 4;
    protected static final int PROGRESS_BAR_WIDTH = 100;
    protected static final int BUTTON_SIZE = 16;
    
    // 动画
    protected SimpleAnimation slideAnimation;
    protected SimpleAnimation fadeAnimation;
    
    // UI位置
    protected int panelHeight;
    protected int panelX;
    protected int panelY;
    protected int targetY;
    
    // 属性列表滚动
    protected int scrollOffset = 0;
    protected int maxScrollOffset = 0;
    
    // 数据
    protected final PlayerEntity player;
    protected int availablePoints;
    protected List<AttributeEntry> attributes = new ArrayList<>();
    protected Map<String, Integer> mainAttributes = new HashMap<>();
    
    // 装备栏
    protected ItemStack[] equipmentItems = new ItemStack[4]; // 头、胸、腿、脚
    protected String[] equipmentSlotNames = {"头部", "胸部", "腿部", "脚部"};
    
    // UI元素
    protected TextRenderer textRenderer;
    protected ButtonWidget closeButton;
    protected ButtonWidget scrollUpButton;
    protected ButtonWidget scrollDownButton;
    
    // 属性按钮映射
    protected Map<AttributeEntry, ButtonWidget> increaseButtons = new HashMap<>();
    protected Map<AttributeEntry, ButtonWidget> decreaseButtons = new HashMap<>();
    
    public LightweightAttributeScreen(PlayerEntity player) {
        super(Text.translatable("gui.justdying.attributes"));
        this.player = player;
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 加载属性数据
        loadAttributeData();
        
        // 加载装备数据
        loadEquipmentData();
        
        // 设置主要显示属性
        setupMainAttributes();
        
        // 计算面板高度
        panelHeight = 320; // 增加高度以容纳更多属性
        
        // 计算面板位置
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = height; // 开始位置在屏幕外
        targetY = (height - panelHeight) / 2;
        
        // 创建入场动画
        slideAnimation = SimpleAnimation.slideIn(ANIMATION_DURATION, panelY, targetY);
        fadeAnimation = SimpleAnimation.fadeIn(FADE_DURATION);
        
        // 添加关闭按钮
        initCloseButton();
        
        // 添加滚动按钮
        initScrollButtons();
        
        // 请求服务器同步最新数据
        requestDataSync();
    }
    
    /**
     * 初始化滚动按钮
     */
    private void initScrollButtons() {
        // 上滚动按钮
        scrollUpButton = ButtonWidget.builder(
                Text.literal("▲"),
                button -> scrollUp())
                .dimensions(
                        panelX + PANEL_WIDTH - 30,
                        targetY + 170,
                        20, 20)
                .build();
        addDrawableChild(scrollUpButton);
        
        // 下滚动按钮
        scrollDownButton = ButtonWidget.builder(
                Text.literal("▼"),
                button -> scrollDown())
                .dimensions(
                        panelX + PANEL_WIDTH - 30,
                        targetY + 250,
                        20, 20)
                .build();
        addDrawableChild(scrollDownButton);
        
        // 更新最大滚动偏移
        updateMaxScrollOffset();
    }
    
    /**
     * 向上滚动
     */
    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            updateAttributeButtons();
        }
    }
    
    /**
     * 向下滚动
     */
    private void scrollDown() {
        if (scrollOffset < maxScrollOffset) {
            scrollOffset++;
            updateAttributeButtons();
        }
    }
    
    /**
     * 更新最大滚动偏移
     */
    private void updateMaxScrollOffset() {
        int visibleItems = 4; // 可见属性数量
        maxScrollOffset = Math.max(0, attributes.size() - visibleItems);
    }
    
    /**
     * 加载装备数据
     */
    private void loadEquipmentData() {
        equipmentItems[0] = player.getEquippedStack(EquipmentSlot.HEAD);
        equipmentItems[1] = player.getEquippedStack(EquipmentSlot.CHEST);
        equipmentItems[2] = player.getEquippedStack(EquipmentSlot.LEGS);
        equipmentItems[3] = player.getEquippedStack(EquipmentSlot.FEET);
    }
    
    /**
     * 设置主要显示属性（顶部显示的三个属性）
     */
    private void setupMainAttributes() {
        mainAttributes.clear();
        
        // 示例：选择一些重要属性作为主要显示
        // 这里可以根据实际需求选择要显示的属性
        mainAttributes.put("耐久", getAttributeValueByPath("health"));
        mainAttributes.put("速度", getAttributeValueByPath("speed"));
        mainAttributes.put("伤害", getAttributeValueByPath("damage"));
    }
    
    /**
     * 根据属性路径获取属性值
     */
    private int getAttributeValueByPath(String path) {
        for (AttributeEntry entry : attributes) {
            if (entry.id.getPath().equals(path)) {
                return entry.currentValue;
            }
        }
        return 0;
    }
    
    /**
     * 初始化关闭按钮
     */
    private void initCloseButton() {
        closeButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> close())
                .dimensions(
                        panelX + PANEL_WIDTH / 2 - 40,
                        targetY + panelHeight - 30,
                        80, 20)
                .build();
        addDrawableChild(closeButton);
    }
    
    /**
     * 创建属性按钮
     */
    private void updateAttributeButtons() {
        // 清除现有按钮
        for (ButtonWidget button : increaseButtons.values()) {
            remove(button);
        }
        for (ButtonWidget button : decreaseButtons.values()) {
            remove(button);
        }
        increaseButtons.clear();
        decreaseButtons.clear();
        
        int startY = targetY + 170;
        int itemHeight = 24;
        int maxVisibleItems = 4; // 可显示的最大属性数量
        
        // 为可见的属性创建按钮
        for (int i = 0; i < Math.min(maxVisibleItems, attributes.size() - scrollOffset); i++) {
            AttributeEntry entry = attributes.get(i + scrollOffset);
            int y = startY + i * itemHeight;
            
            // 创建增加按钮
            ButtonWidget increaseButton = ButtonWidget.builder(
                    Text.literal("+"),
                    button -> onAttributeUpgrade(entry))
                    .dimensions(
                            panelX + PANEL_WIDTH - 50,
                            y - 2,
                            BUTTON_SIZE, BUTTON_SIZE)
                    .build();
            addDrawableChild(increaseButton);
            increaseButtons.put(entry, increaseButton);
            
            // 创建减少按钮
            ButtonWidget decreaseButton = ButtonWidget.builder(
                    Text.literal("-"),
                    button -> onAttributeDowngrade(entry))
                    .dimensions(
                            panelX + PANEL_WIDTH - 70,
                            y - 2,
                            BUTTON_SIZE, BUTTON_SIZE)
                    .build();
            addDrawableChild(decreaseButton);
            decreaseButtons.put(entry, decreaseButton);
        }
    }
    
    /**
     * 请求服务器同步最新数据
     */
    private void requestDataSync() {
        // 请求同步所有属性数据
        for (AttributeEntry attribute : attributes) {
            ClientAttributePackets.sendSyncRequestPacket(attribute.id);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染半透明背景
        float fadeAlpha = fadeAnimation.getValue();
        int backgroundAlpha = (int)(fadeAlpha * 128) << 24;
        context.fill(0, 0, width, height, backgroundAlpha | 0x000000);
        
        // 获取当前面板位置
        int currentY = (int)slideAnimation.getValue();
        
        // 渲染面板背景
        context.fill(
                panelX, 
                currentY, 
                panelX + PANEL_WIDTH, 
                currentY + panelHeight, 
                0xE0222222);
        
        // 绘制边框
        context.drawBorder(
                panelX, 
                currentY, 
                PANEL_WIDTH, 
                panelHeight, 
                0xFFAAAAAA);
        
        // 渲染标题
        String title = this.title.getString();
        context.drawCenteredTextWithShadow(
                textRenderer, 
                title, 
                panelX + PANEL_WIDTH / 2, 
                currentY + 10, 
                0xFFDDA0);
        
        // 渲染主要属性
        renderMainAttributes(context, currentY);
        
        // 渲染可用点数
        String pointsText = Text.translatable("gui.justdying.available_points", availablePoints).getString();
        context.drawCenteredTextWithShadow(
                textRenderer, 
                pointsText, 
                panelX + PANEL_WIDTH / 2, 
                currentY + 100, 
                0xE0E0E0);
        
        // 渲染装备栏
        renderEquipmentSlots(context, currentY + 120);
        
        // 渲染属性列表
        renderAttributeList(context, currentY + 170);
        
        // 更新按钮位置
        closeButton.setY(currentY + panelHeight - 30);
        scrollUpButton.setY(currentY + 170);
        scrollDownButton.setY(currentY + 250);
        
        // 更新属性按钮位置
        int startY = currentY + 170;
        int itemHeight = 24;
        int i = 0;
        for (AttributeEntry entry : increaseButtons.keySet()) {
            ButtonWidget increaseButton = increaseButtons.get(entry);
            ButtonWidget decreaseButton = decreaseButtons.get(entry);
            
            int index = attributes.indexOf(entry) - scrollOffset;
            if (index >= 0 && index < 4) {
                int y = startY + index * itemHeight;
                increaseButton.setY(y - 2);
                decreaseButton.setY(y - 2);
                
                // 禁用或启用按钮
                increaseButton.active = availablePoints >= entry.costPerPoint && entry.currentValue < entry.maxValue;
                decreaseButton.active = entry.currentValue > entry.minValue;
            }
        }
        
        // 渲染按钮和其他元素
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * 渲染主要属性（顶部显示的耐久、速度、伤害）
     */
    private void renderMainAttributes(DrawContext context, int startY) {
        int y = startY + 35;
        int step = 20;
        int xLeft = panelX + 20;
        int xRight = panelX + PANEL_WIDTH - 20;
        
        // 获取主要属性列表
        String[] attrNames = mainAttributes.keySet().toArray(new String[0]);
        Integer[] attrValues = mainAttributes.values().toArray(new Integer[0]);
        
        // 确保至少有两个主要属性
        if (attrNames.length >= 2) {
            // 渲染左右两侧的数值
            String leftValue = String.format("%.2f", attrValues[0] / 100.0f);
            String rightValue = String.format("%.2f", attrValues[attrNames.length-1] / 100.0f);
            
            context.drawTextWithShadow(textRenderer, leftValue, xLeft, y, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, rightValue, xRight - textRenderer.getWidth(rightValue), y, 0xFFFFFF);
            
            // 渲染中间的属性图标和名称
            context.drawCenteredTextWithShadow(textRenderer, attrNames[0], panelX + PANEL_WIDTH/4, y + 20, 0xAAAAAA);
            context.drawCenteredTextWithShadow(textRenderer, attrNames[attrNames.length-1], panelX + PANEL_WIDTH*3/4, y + 20, 0xAAAAAA);
            
            // 渲染中间图标
            // 这里可以添加图标的渲染，比如使用物品图标或自定义纹理
            
            // 渲染进度条
            renderProgressBar(context, 
                    panelX + 50, y + 10, 
                    PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, 
                    attrValues[0], 100, 0x7FFFAA);
            
            renderProgressBar(context, 
                    panelX + PANEL_WIDTH - 50 - PROGRESS_BAR_WIDTH, y + 10, 
                    PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, 
                    attrValues[attrNames.length-1], 100, 0xFF7A59);
            
            // 如果有中间属性，渲染在中间位置
            if (attrNames.length >= 3) {
                String midValue = String.format("%.2f", attrValues[1] / 100.0f);
                context.drawCenteredTextWithShadow(textRenderer, midValue, panelX + PANEL_WIDTH/2, y, 0xFFFFFF);
                context.drawCenteredTextWithShadow(textRenderer, attrNames[1], panelX + PANEL_WIDTH/2, y + 20, 0xAAAAAA);
                
                renderProgressBar(context, 
                        panelX + PANEL_WIDTH/2 - PROGRESS_BAR_WIDTH/2, y + 10, 
                        PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, 
                        attrValues[1], 100, 0x59BEFF);
            }
        }
    }
    
    /**
     * 渲染进度条
     */
    private void renderProgressBar(DrawContext context, int x, int y, int width, int height, int value, int maxValue, int color) {
        // 计算进度比例
        float progress = Math.min(1.0f, Math.max(0, (float)value / maxValue));
        int progressWidth = (int)(width * progress);
        
        // 渲染背景
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // 渲染进度
        if (progressWidth > 0) {
            context.fill(x, y, x + progressWidth, y + height, color);
        }
        
        // 渲染边框
        context.drawBorder(x, y, width, height, 0x80FFFFFF);
    }
    
    /**
     * 渲染装备栏
     */
    private void renderEquipmentSlots(DrawContext context, int startY) {
        int slotSize = 20;
        int slotSpacing = 10;
        int totalWidth = (slotSize + slotSpacing) * 4 - slotSpacing;
        int startX = panelX + (PANEL_WIDTH - totalWidth) / 2;
        
        // 渲染标题
        context.drawCenteredTextWithShadow(
                textRenderer, 
                "装备", 
                panelX + PANEL_WIDTH / 2, 
                startY - 15, 
                0xCCCCCC);
        
        // 渲染装备槽
        for (int i = 0; i < 4; i++) {
            int x = startX + i * (slotSize + slotSpacing);
            
            // 渲染槽位背景
            context.fill(x, startY, x + slotSize, startY + slotSize, 0x80000000);
            context.drawBorder(x, startY, slotSize, slotSize, 0x80FFFFFF);
            
            // 如果有装备，渲染装备图标
            if (equipmentItems[i] != null && !equipmentItems[i].isEmpty()) {
                context.drawItem(equipmentItems[i], x + 2, startY + 2);
            }
            
            // 渲染槽位名称
            context.drawCenteredTextWithShadow(
                    textRenderer, 
                    equipmentSlotNames[i], 
                    x + slotSize / 2, 
                    startY + slotSize + 3, 
                    0xAAAAAA);
        }
    }
    
    /**
     * 渲染属性列表
     */
    private void renderAttributeList(DrawContext context, int startY) {
        int itemHeight = 24;
        int maxVisibleItems = 4; // 可显示的最大属性数量
        
        // 渲染属性列表标题
        context.drawCenteredTextWithShadow(
                textRenderer, 
                "属性列表", 
                panelX + PANEL_WIDTH / 2, 
                startY - 15, 
                0xCCCCCC);
        
        // 渲染滚动指示器
        if (attributes.size() > maxVisibleItems) {
            String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxVisibleItems, attributes.size()) + "/" + attributes.size();
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    scrollInfo,
                    panelX + PANEL_WIDTH / 2,
                    startY + maxVisibleItems * itemHeight + 5,
                    0xAAAAAA);
        }
        
        // 渲染属性条目
        for (int i = 0; i < Math.min(maxVisibleItems, attributes.size() - scrollOffset); i++) {
            AttributeEntry entry = attributes.get(i + scrollOffset);
            int y = startY + i * itemHeight;
            
            // 渲染属性名
            context.drawTextWithShadow(
                    textRenderer, 
                    entry.name, 
                    panelX + 20, 
                    y, 
                    0xFFFFFF);
            
            // 渲染属性值
            String valueText = entry.currentValue + "/" + entry.maxValue;
            context.drawTextWithShadow(
                    textRenderer, 
                    valueText, 
                    panelX + PANEL_WIDTH - 90 - textRenderer.getWidth(valueText), 
                    y, 
                    0xFFFFFF);
            
            // 渲染进度条
            renderProgressBar(context, 
                    panelX + 20, 
                    y + 12, 
                    PANEL_WIDTH - 100, 
                    PROGRESS_BAR_HEIGHT, 
                    entry.currentValue, 
                    entry.maxValue, 
                    0x59BEFF);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // 检查是否在属性列表区域内
        int startY = (int)slideAnimation.getValue() + 170;
        int endY = startY + 4 * 24; // 4个属性的高度
        
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && 
            mouseY >= startY && mouseY <= endY) {
            // 处理滚动
            if (amount > 0) {
                scrollUp();
            } else if (amount < 0) {
                scrollDown();
            }
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 检查退出动画是否完成
        if (slideAnimation != null && !slideAnimation.isRunning() && 
            fadeAnimation != null && !fadeAnimation.isRunning()) {
            // 如果两个动画都结束了且处于关闭状态，那么真正关闭屏幕
            if (slideAnimation.getType() == SimpleAnimation.AnimationType.TRANSLATE_Y &&
                slideAnimation.getValue() >= height) {
                super.close();
            }
        }
    }
    
    @Override
    public void close() {
        // 创建退出动画
        slideAnimation = SimpleAnimation.slideOut(ANIMATION_DURATION - 100, (int)slideAnimation.getValue(), height);
        fadeAnimation = SimpleAnimation.fadeOut(FADE_DURATION - 50);
        
        // 动画结束后关闭屏幕
        slideAnimation.setOnComplete(() -> super.close());
    }
    
    /**
     * 加载属性数据
     */
    protected void loadAttributeData() {
        if (player == null) {
            return;
        }
        
        // 清除旧数据
        attributes.clear();
        
        // 获取可用点数
        availablePoints = AttributeHelper.getAvailablePoints(player);
        
        try {
            // 获取所有已注册的可分配属性
            for (AttributeRegistry.AttributePointDefinition definition : 
                    AttributeRegistry.getInstance().getRegisteredAttributes()) {
                    
                EntityAttribute attribute = definition.getAttribute();
                
                // 获取属性ID
                Identifier id = Registries.ATTRIBUTE.getId(attribute);
                
                // 获取当前值 - 添加错误处理
                double currentValue = 0;
                try {
                    currentValue = AttributeHelper.getAttributeValue(player, id);
                } catch (Exception e) {
                    JustDying.LOGGER.error("无法获取属性值 {}: {}", id, e.getMessage());
                    // 使用默认值
                    currentValue = definition.getDefaultValue();
                }
                
                // 获取属性效果格式
                String effectFormat = AttributeEntry.getDefaultEffectFormat(id.getPath());
                
                // 创建属性条目
                AttributeEntry entry = new AttributeEntry(
                    id,
                    Text.translatable(attribute.getTranslationKey()).getString(),
                    Text.translatable(attribute.getTranslationKey() + ".description").getString(),
                    (int)definition.getMinValue(),
                    (int)definition.getMaxValue(),
                    (int)currentValue,
                    definition.getCostPerPoint(),
                    effectFormat
                );
                
                attributes.add(entry);
            }
            
            // 更新主要属性显示
            setupMainAttributes();
            
            // 更新最大滚动偏移
            updateMaxScrollOffset();
            
            // 更新属性按钮
            updateAttributeButtons();
        } catch (Exception e) {
            JustDying.LOGGER.error("加载属性数据时出错: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 属性升级回调
     */
    private void onAttributeUpgrade(AttributeEntry attribute) {
        // 检查是否有足够的点数
        if (availablePoints < attribute.costPerPoint) {
            playDeniedSound();
            return;
        }
        
        // 检查是否已达到最大值
        if (attribute.currentValue >= attribute.maxValue) {
            playDeniedSound();
            return;
        }
        
        try {
            // 发送请求到服务器
            sendAttributeUpgradeRequest(attribute.id);
            
            // 本地预更新UI
            attribute.currentValue++;
            availablePoints -= attribute.costPerPoint;
            
            // 更新主要属性显示
            setupMainAttributes();
            
            // 播放升级音效
            playUpgradeSound();
            
            // 添加视觉反馈
            attribute.startUpgradeAnimation();
            
            // 尝试在客户端同步更新属性
            try {
                // 更新客户端显示
                AttributeHelper.setAttributeValue(player, attribute.id, attribute.currentValue);
            } catch (Exception e) {
                JustDying.LOGGER.error("客户端属性更新失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            JustDying.LOGGER.error("处理属性升级时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 属性降级回调
     */
    private void onAttributeDowngrade(AttributeEntry attribute) {
        // 检查是否已达到最小值
        if (attribute.currentValue <= attribute.minValue) {
            playDeniedSound();
            return;
        }
        
        try {
            // 发送请求到服务器
            sendAttributeDowngradeRequest(attribute.id);
            
            // 本地预更新UI
            attribute.currentValue--;
            availablePoints += attribute.costPerPoint;
            
            // 更新主要属性显示
            setupMainAttributes();
            
            // 播放降级音效
            playDeniedSound();
            
            // 添加视觉反馈
            attribute.startUpgradeAnimation();
            
            // 尝试在客户端同步更新属性
            try {
                // 更新客户端显示
                AttributeHelper.setAttributeValue(player, attribute.id, attribute.currentValue);
            } catch (Exception e) {
                JustDying.LOGGER.error("客户端属性更新失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            JustDying.LOGGER.error("处理属性降级时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 发送属性升级请求
     */
    private void sendAttributeUpgradeRequest(Identifier attributeId) {
        // 发送网络包到服务器
        ClientAttributePackets.sendIncreasePacket(attributeId);
    }
    
    /**
     * 发送属性降级请求
     */
    private void sendAttributeDowngradeRequest(Identifier attributeId) {
        // 发送网络包到服务器
        ClientAttributePackets.sendDecreasePacket(attributeId);
    }
    
    /**
     * 播放升级音效
     */
    private void playUpgradeSound() {
        SoundEvent sound = Registries.SOUND_EVENT.get(new Identifier("entity.player.levelup"));
        player.playSound(sound, SoundCategory.PLAYERS, 0.5f, 1.5f);
    }
    
    /**
     * 播放拒绝音效
     */
    private void playDeniedSound() {
        SoundEvent sound = Registries.SOUND_EVENT.get(new Identifier("block.note_block.bass"));
        player.playSound(sound, SoundCategory.PLAYERS, 1.0f, 0.5f);
    }
    
    /**
     * 刷新数据
     */
    public void refreshData() {
        // 重新加载属性数据
        loadAttributeData();
        
        // 重新加载装备数据
        loadEquipmentData();
    }
}