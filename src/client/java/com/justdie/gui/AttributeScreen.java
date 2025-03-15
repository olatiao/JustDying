package com.justdie.gui;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeManager;
import com.justdie.attribute.AttributeHelper;
import com.justdie.attribute.JustDyingAttribute;
import com.justdie.attribute.JustDyingAttributeType;
import com.justdie.network.ClientAttributePackets;
import com.justdie.attribute.LevelExchangeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 属性面板屏幕
 * 负责显示和管理玩家的属性点分配
 */
public class AttributeScreen extends Screen {
    // GUI常量
    private static final int PADDING = 10;
    private static final int ATTRIBUTE_HEIGHT = 26;
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 5; // 按钮之间的间距
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 240; 
    private static final int BUTTON_AREA_TOP_MARGIN = 20; // 按钮区域顶部间距

    // 页面控制变量
    private final PlayerEntity player;
    private final List<JustDyingAttribute> attributes;
    private int scrollOffset = 0;
    private int availablePoints = 0; // 可用点数
    private int requiredLevel = 0; // 所需等级
    
    // 预缓存的物品图标堆栈
    private final Map<String, ItemStack> itemStackCache = new ConcurrentHashMap<>();

    // 属性类型映射
    private final Map<String, JustDyingAttributeType> attributeTypeMap;

    // 翻页按钮
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    
    // 渲染缓存
    private int lastWidth = 0;
    private int lastHeight = 0;
    private int cachedCenterX = 0;
    private int cachedCenterY = 0;
    private int cachedLeft = 0;
    private int cachedTop = 0;

    /**
     * 创建属性面板
     * 
     * @param player 玩家实体
     */
    public AttributeScreen(PlayerEntity player) {
        super(Text.translatable("gui." + JustDying.MOD_ID + ".attributes"));
        this.player = Objects.requireNonNull(player, "玩家对象不能为空");
        this.attributes = new ArrayList<>(AttributeManager.getAllAttributes());

        // 初始化属性类型映射
        this.attributeTypeMap = new ConcurrentHashMap<>();
        initAttributeTypeMap();

        // 打开面板时请求服务器同步最新数据
        requestDataSync();
    }
    
    /**
     * 初始化属性类型映射
     */
    private void initAttributeTypeMap() {
        attributeTypeMap.put("constitution", JustDyingAttributeType.CONSTITUTION);
        attributeTypeMap.put("strength", JustDyingAttributeType.STRENGTH);
        attributeTypeMap.put("defense", JustDyingAttributeType.DEFENSE);
        attributeTypeMap.put("speed", JustDyingAttributeType.SPEED);
        attributeTypeMap.put("luck", JustDyingAttributeType.LUCK);
    }

    /**
     * 请求服务器同步最新数据
     */
    private void requestDataSync() {
        // 请求同步所有属性数据
        for (JustDyingAttribute attribute : attributes) {
            ClientAttributePackets.sendSyncRequestPacket(attribute.getId());
        }

        JustDying.LOGGER.debug("请求同步所有属性数据");
    }

    @Override
    protected void init() {
        super.init();

        // 更新页面布局缓存
        updateLayoutCache();
        
        // 更新数据
        this.availablePoints = AttributeHelper.getAvailablePoints(player);
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            this.requiredLevel = LevelExchangeManager.calculateRequiredLevel(player);
        }

        JustDying.LOGGER.debug("初始化属性面板 - 可用点数: {}, 所需等级: {}", availablePoints, requiredLevel);

        // 添加控制按钮
        addButtons();
    }
    
    /**
     * 更新页面布局缓存
     */
    private void updateLayoutCache() {
        if (width != lastWidth || height != lastHeight) {
            cachedCenterX = width / 2;
            cachedCenterY = height / 2;
            cachedLeft = cachedCenterX - GUI_WIDTH / 2;
            cachedTop = cachedCenterY - GUI_HEIGHT / 2;
            
            lastWidth = width;
            lastHeight = height;
        }
    }
    
    /**
     * 添加控制按钮
     */
    private void addButtons() {
        // 清除旧按钮
        resetButtons();
        
        // 添加翻页按钮
        int buttonY = cachedTop + GUI_HEIGHT - 30;
        
        // 上翻按钮
        upButton = this.addDrawableChild(new AttributeButton(
                cachedLeft + GUI_WIDTH / 2 - BUTTON_WIDTH - BUTTON_SPACING,
                buttonY,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                Text.literal("↑"),
                button -> scrollUp()));

        // 下翻按钮
        downButton = this.addDrawableChild(new AttributeButton(
                cachedLeft + GUI_WIDTH / 2 + BUTTON_SPACING,
                buttonY,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                Text.literal("↓"),
                button -> scrollDown()));
        
        // 为每个可见的属性添加增减按钮
        addAttributeButtons();
        
        // 更新按钮状态
        updateButtonStates();
    }
    
    /**
     * 为每个属性添加增减按钮
     */
    private void addAttributeButtons() {
        int visibleCount = Math.min(getVisibleAttributeCount(), attributes.size());
        
        for (int i = 0; i < visibleCount; i++) {
            final int index = scrollOffset + i;
            if (index >= attributes.size()) break;
            
            JustDyingAttribute attribute = attributes.get(index);
            int attributeY = cachedTop + 40 + i * ATTRIBUTE_HEIGHT;
            
            // 增加按钮
            ButtonWidget increaseButton = this.addDrawableChild(new AttributeButton(
                    cachedLeft + GUI_WIDTH - PADDING - BUTTON_WIDTH * 2 - BUTTON_SPACING,
                    attributeY,
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    Text.literal("+"),
                    button -> increaseAttribute(attribute)));
            
            // 减少按钮 - 仅当配置允许时显示
            if (JustDying.getConfig().attributes.showDecreaseButtons) {
                ButtonWidget decreaseButton = this.addDrawableChild(new AttributeButton(
                        cachedLeft + GUI_WIDTH - PADDING - BUTTON_WIDTH,
                        attributeY,
                        BUTTON_WIDTH, BUTTON_HEIGHT,
                        Text.literal("-"),
                        button -> decreaseAttribute(attribute)));
            }
        }
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        // 更新翻页按钮状态
        upButton.active = scrollOffset > 0;
        downButton.active = scrollOffset + getVisibleAttributeCount() < attributes.size();
        
        // 更新增减按钮状态 - 遍历所有子元素
        for (Element element : this.children()) {
            if (element instanceof ButtonWidget) {
                ButtonWidget button = (ButtonWidget) element;
                // 根据按钮文本判断类型
                if (button.getMessage().getString().equals("+")) {
                    button.active = availablePoints > 0;
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 更新页面布局缓存（窗口大小可能在渲染之间改变）
        updateLayoutCache();
        
        // 绘制默认的Minecraft半透明黑色背景
        this.renderBackground(context);

        // 绘制Minecraft风格的深色背景面板
        context.fillGradient(cachedLeft, cachedTop, cachedLeft + GUI_WIDTH, cachedTop + GUI_HEIGHT,
                0xC0101010, 0xD0101010);
        context.drawBorder(cachedLeft, cachedTop, GUI_WIDTH, GUI_HEIGHT, 0xFF000000);

        // 绘制标题
        context.drawCenteredTextWithShadow(textRenderer, this.title, cachedCenterX, cachedTop + 10, 0xFFFFFF);

        // 绘制可用点数
        String pointsText = Text.translatable("gui." + JustDying.MOD_ID + ".available_points", this.availablePoints)
                .getString();
        context.drawTextWithShadow(textRenderer, pointsText, cachedLeft + PADDING, cachedTop + 25, 0xFFFFFF);

        // 如果启用了等级兑换功能，显示当前等级和所需等级
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            String levelText = Text.translatable("gui." + JustDying.MOD_ID + ".current_level", player.experienceLevel)
                    .getString();
            context.drawTextWithShadow(textRenderer, levelText,
                    cachedLeft + GUI_WIDTH - PADDING - textRenderer.getWidth(levelText), cachedTop + 25, 0xFFFFFF);
        }

        // 绘制属性列表
        renderAttributeList(context, mouseX, mouseY);

        // 显示总页数信息
        renderPageInfo(context);

        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * 渲染属性列表
     */
    private void renderAttributeList(DrawContext context, int mouseX, int mouseY) {
        int visibleCount = Math.min(getVisibleAttributeCount(), attributes.size());
        
        for (int i = 0; i < visibleCount; i++) {
            int index = scrollOffset + i;
            if (index >= attributes.size())
                break;

            JustDyingAttribute attribute = attributes.get(index);
            int attributeY = cachedTop + 40 + i * ATTRIBUTE_HEIGHT;

            // 获取属性ID路径
            String attributePath = attribute.getId().getPath();
            
            // 绘制物品图标 - 使用缓存提高性能
            if (attribute.getIconItem() != null) {
                // 使用缓存的属性物品作为图标
                ItemStack stack = getOrCreateItemStack(attribute);
                context.drawItem(stack, cachedLeft + PADDING, attributeY);
            } else {
                // 如果物品获取失败，使用预定义图标作为后备选项
                JustDyingAttributeType attributeType = attributeTypeMap.getOrDefault(attributePath, JustDyingAttributeType.CONSTITUTION);
                AttributeIcons.renderIcon(context, attributeType, cachedLeft + PADDING, attributeY);
            }

            // 绘制属性名称
            context.drawTextWithShadow(textRenderer, attribute.getName(), cachedLeft + PADDING + 22, attributeY + 5,
                    0xFFFFFF);

            // 绘制属性值
            int value = AttributeHelper.getAttributeValue(player, attribute.getId());
            String valueText = value + "/" + attribute.getMaxValue();
            int valueWidth = textRenderer.getWidth(valueText);
            context.drawTextWithShadow(textRenderer, valueText, cachedLeft + GUI_WIDTH - 95 - valueWidth, attributeY + 5,
                    0xFFFFFF);

            // 如果鼠标悬停在属性上，显示属性描述
            if (isMouseOverAttribute(mouseX, mouseY, attributeY)) {
                renderAttributeTooltip(context, mouseX, mouseY, attribute, value);
            }
        }
    }
    
    /**
     * 获取或创建物品堆栈，使用缓存提高性能
     */
    private ItemStack getOrCreateItemStack(JustDyingAttribute attribute) {
        String key = attribute.getId().toString();
        return itemStackCache.computeIfAbsent(key, k -> {
            if (attribute.getIconItem() != null) {
                return attribute.getIconItem().getDefaultStack();
            }
            return null;
        });
    }
    
    /**
     * 检查鼠标是否悬停在属性上
     */
    private boolean isMouseOverAttribute(int mouseX, int mouseY, int attributeY) {
        return mouseX >= cachedLeft + PADDING && mouseX <= cachedLeft + GUI_WIDTH - 95 &&
                mouseY >= attributeY && mouseY <= attributeY + ATTRIBUTE_HEIGHT;
    }
    
    /**
     * 渲染属性工具提示
     */
    private void renderAttributeTooltip(DrawContext context, int mouseX, int mouseY, JustDyingAttribute attribute, int value) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(attribute.getName());
        tooltip.add(Text.literal("").append(attribute.getDescription()).formatted(Formatting.GRAY));

        // 添加属性效果说明
        if (attribute.getVanillaAttribute() != null) {
            double bonus = attribute.calculateAttributeBonus(value);
            tooltip.add(Text.literal("+" + String.format("%.1f", bonus) + " " +
                    attribute.getVanillaAttribute().getTranslationKey())
                    .formatted(Formatting.BLUE));
        }

        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }
    
    /**
     * 渲染页面信息
     */
    private void renderPageInfo(DrawContext context) {
        if (attributes.size() > getVisibleAttributeCount()) {
            int currentPage = (scrollOffset / getVisibleAttributeCount()) + 1;
            int totalPages = (int) Math.ceil((double) attributes.size() / getVisibleAttributeCount());
            String pageInfo = String.format("%d/%d", currentPage, totalPages);
            
            int visibleCount = Math.min(getVisibleAttributeCount(), attributes.size());
            context.drawTextWithShadow(textRenderer, pageInfo, 
                cachedLeft + GUI_WIDTH - PADDING - textRenderer.getWidth(pageInfo), 
                cachedTop + 40 + visibleCount * ATTRIBUTE_HEIGHT + 5, 0xAAAAAA);
        }
    }
    
    /**
     * 释放屏幕资源
     */
    @Override
    public void removed() {
        super.removed();
        // 清理缓存
        itemStackCache.clear();
    }

    /**
     * 清除所有按钮
     */
    private void resetButtons() {
        this.clearChildren();
    }

    /**
     * 获取可见属性数量
     */
    private int getVisibleAttributeCount() {
        // 根据当前GUI高度计算可见的属性数量
        // 确保至少显示5个属性
        return 5;
    }

    /**
     * 向上滚动属性列表
     */
    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset -= getVisibleAttributeCount();
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
            resetButtons();
            addButtons();
            updateButtonStates();
        }
    }

    /**
     * 向下滚动属性列表
     */
    private void scrollDown() {
        int maxOffset = Math.max(0, attributes.size() - getVisibleAttributeCount());
        if (scrollOffset < maxOffset) {
            scrollOffset += getVisibleAttributeCount();
            if (scrollOffset > maxOffset) {
                scrollOffset = maxOffset;
            }
            resetButtons();
            addButtons();
            updateButtonStates();
        }
    }

    /**
     * 减少属性值
     */
    private void decreaseAttribute(JustDyingAttribute attribute) {
        int currentValue = AttributeHelper.getAttributeValue(player, attribute.getId());
        if (currentValue > attribute.getMinValue()) {
            // 发送网络包到服务器
            ClientAttributePackets.sendDecreasePacket(attribute.getId());
            
            // 客户端预测性更新（实际值将由服务器同步）
            AttributeHelper.setAttributeValue(player, attribute.getId(), currentValue - 1);
            AttributeHelper.addPoints(player, 1);
            
            // 记录日志
            JustDying.LOGGER.debug("减少属性 {} 至 {}", attribute.getId(), currentValue - 1);
            
            // 更新可用点数
            this.availablePoints = AttributeHelper.getAvailablePoints(player);
            updateButtonStates();
        }
    }
    
    /**
     * 增加属性值
     */
    private void increaseAttribute(JustDyingAttribute attribute) {
        if (availablePoints > 0) {
            int currentValue = AttributeHelper.getAttributeValue(player, attribute.getId());
            if (currentValue < attribute.getMaxValue()) {
                // 发送网络包到服务器
                ClientAttributePackets.sendIncreasePacket(attribute.getId());
                
                // 客户端预测性更新（实际值将由服务器同步）
                AttributeHelper.setAttributeValue(player, attribute.getId(), currentValue + 1);
                AttributeHelper.usePoints(player, 1);
                
                // 记录日志
                JustDying.LOGGER.debug("增加属性 {} 至 {}", attribute.getId(), currentValue + 1);
                
                // 更新可用点数
                this.availablePoints = AttributeHelper.getAvailablePoints(player);
                updateButtonStates();
            }
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * 刷新属性面板
     * 更新数据和界面元素
     */
    public void refreshScreen() {
        // 更新数据
        this.availablePoints = AttributeHelper.getAvailablePoints(player);
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            this.requiredLevel = LevelExchangeManager.calculateRequiredLevel(player);
        }

        // 重新初始化界面
        resetButtons();
        addButtons();
        updateButtonStates();
        
        JustDying.LOGGER.debug("属性面板已刷新 - 可用点数: {}", availablePoints);
    }
}