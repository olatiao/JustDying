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
import java.util.Comparator;

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
    // 增加面板宽度，解决中文字符被遮挡问题
    private static final int GUI_WIDTH = 260; // 从200增加到260
    private static final int GUI_HEIGHT = 240; // 标准高度
    private static final int GUI_HEIGHT_COMPACT = 200; // 紧凑高度（无翻页按钮）
    private static final int GUI_HEIGHT_NO_EXCHANGE = 210; // 无等级兑换按钮高度
    private static final int GUI_HEIGHT_COMPACT_NO_EXCHANGE = 180; // 紧凑且无等级兑换按钮高度
    private static final int BUTTON_AREA_TOP_MARGIN = 20; // 按钮区域顶部间距

    // 页面控制变量
    private final PlayerEntity player;
    private final List<JustDyingAttribute> attributes;
    private int scrollOffset = 0;
    private int availablePoints = 0; // 可用点数
    private int requiredLevel = 0; // 所需等级
    private int actualGuiHeight; // 动态计算的GUI高度

    // 预缓存的物品图标堆栈
    private final Map<String, ItemStack> itemStackCache = new ConcurrentHashMap<>();

    // 翻页按钮
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    // 等级兑换按钮
    private ButtonWidget exchangeButton;

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

        // 获取所有属性并按照排序值排序
        List<JustDyingAttribute> allAttributes = new ArrayList<>(AttributeManager.getAllAttributes());
        allAttributes.sort(Comparator.comparingInt(attr -> {
            String attributePath = attr.getId().getPath();
            // 检查配置中是否存在该属性的配置
            if (JustDying.getConfig().attributes.attributes.containsKey(attributePath)) {
                return JustDying.getConfig().attributes.attributes.get(attributePath).sortOrder;
            }
            return Integer.MAX_VALUE; // 如果没有配置，放到最后
        }));
        this.attributes = allAttributes;

        // 打开面板时请求服务器同步最新数据
        requestDataSync();
        
        // 计算实际GUI高度
        updateGUIHeight();
    }

    /**
     * 根据当前配置和属性数量动态更新GUI高度
     */
    private void updateGUIHeight() {
        boolean needsPagination = attributes.size() > getVisibleAttributeCount();
        boolean enableLevelExchange = JustDying.getConfig().levelExchange.enableLevelExchange;
        
        if (needsPagination && enableLevelExchange) {
            actualGuiHeight = GUI_HEIGHT; // 标准高度
        } else if (needsPagination && !enableLevelExchange) {
            actualGuiHeight = GUI_HEIGHT_NO_EXCHANGE; // 无等级兑换按钮高度
        } else if (!needsPagination && enableLevelExchange) {
            actualGuiHeight = GUI_HEIGHT_COMPACT; // 紧凑高度
        } else {
            actualGuiHeight = GUI_HEIGHT_COMPACT_NO_EXCHANGE; // 紧凑且无等级兑换按钮高度
        }
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
        
        // 更新GUI高度
        updateGUIHeight();

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
            cachedTop = cachedCenterY - actualGuiHeight / 2;

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
        
        // 只有当需要分页时才添加翻页按钮
        if (attributes.size() > getVisibleAttributeCount()) {
            // 添加翻页按钮 - 移到面板左下角
            int buttonY = cachedTop + actualGuiHeight - 30;
            
            // 上翻按钮
            upButton = this.addDrawableChild(new AttributeButton(
                    cachedLeft + PADDING,
                    buttonY,
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    Text.literal("↑"),
                    button -> scrollUp()));
    
            // 下翻按钮
            downButton = this.addDrawableChild(new AttributeButton(
                    cachedLeft + PADDING + BUTTON_WIDTH + BUTTON_SPACING,
                    buttonY,
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    Text.literal("↓"),
                    button -> scrollDown()));
        }

        // 添加等级兑换按钮 - 如果启用了等级兑换
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            int exchangeButtonY = cachedTop + actualGuiHeight - 60;
            Text buttonText = Text.translatable("gui.justdying.exchange_level", this.requiredLevel);
            exchangeButton = this.addDrawableChild(new AttributeButton(
                    cachedCenterX - 80,
                    exchangeButtonY,
                    160, BUTTON_HEIGHT,
                    buttonText,
                    button -> exchangeLevelForPoint()));
        }

        // 为每个可见的属性添加增减按钮
        addAttributeButtons();

        // 更新按钮状态
        updateButtonStates();
    }

    /**
     * 为每个属性添加增减按钮
     */
    private void addAttributeButtons() {
        int start = scrollOffset;
        int end = Math.min(scrollOffset + getVisibleAttributeCount(), attributes.size());
        int visibleCount = end - start;

        for (int i = 0; i < visibleCount; i++) {
            final int index = start + i;
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
        // 只有当按钮存在时才更新其状态
        if (upButton != null && downButton != null) {
            // 更新翻页按钮状态
            upButton.active = scrollOffset > 0;
            downButton.active = scrollOffset + getVisibleAttributeCount() < attributes.size();
        }

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

        // 更新等级兑换按钮状态
        if (exchangeButton != null) {
            exchangeButton.active = player.experienceLevel >= requiredLevel;
        }
    }

    /**
     * 清除所有按钮
     */
    private void resetButtons() {
        this.clearChildren();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制默认的深色背景
        this.renderBackground(context);

        // 更新页面布局缓存
        updateLayoutCache();
        
        // 绘制面板背景
        context.fillGradient(cachedLeft, cachedTop, cachedLeft + GUI_WIDTH, cachedTop + actualGuiHeight,
                0xC0101010, 0xD0101010);
        context.drawBorder(cachedLeft, cachedTop, GUI_WIDTH, actualGuiHeight, 0xFF000000);

        // 绘制标题
        context.drawCenteredTextWithShadow(textRenderer, this.title, cachedCenterX, cachedTop + 10, 0xFFFFFF);

        // 绘制可用点数
        Text pointsText = Text.translatable("gui." + JustDying.MOD_ID + ".available_points", this.availablePoints);
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

        // 绘制分页信息 - 右下角
        if (attributes.size() > getVisibleAttributeCount()) {
            int maxPage = (int) Math.ceil((double) attributes.size() / getVisibleAttributeCount());
            int currentPage = (scrollOffset / getVisibleAttributeCount()) + 1;
            String pageInfo = currentPage + "/" + maxPage;
            // 右下角位置
            context.drawTextWithShadow(textRenderer, pageInfo, 
                    cachedLeft + GUI_WIDTH - PADDING - textRenderer.getWidth(pageInfo), 
                    cachedTop + actualGuiHeight - 15, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 渲染属性列表
     */
    private void renderAttributeList(DrawContext context, int mouseX, int mouseY) {
        // 计算当前页显示的属性范围
        int start = scrollOffset;
        int end = Math.min(scrollOffset + getVisibleAttributeCount(), attributes.size());
        int visibleCount = end - start;

        for (int i = 0; i < visibleCount; i++) {
            int index = start + i;
            JustDyingAttribute attribute = attributes.get(index);
            int attributeY = cachedTop + 40 + i * ATTRIBUTE_HEIGHT;

            // 渲染属性图标
            renderAttributeIcon(context, attribute, cachedLeft + PADDING, attributeY);

            // 绘制属性名称 - 添加更多空间
            context.drawTextWithShadow(textRenderer, attribute.getName(),
                    cachedLeft + PADDING + 24, attributeY + 5, 0xFFFFFF);

            // 绘制属性值 - 调整位置以适应更宽的面板
            int value = AttributeHelper.getAttributeValue(player, attribute.getId());
            String valueText = value + "/" + attribute.getMaxValue();
            context.drawTextWithShadow(textRenderer, valueText,
                    cachedLeft + GUI_WIDTH - PADDING - 50 - textRenderer.getWidth(valueText),
                    attributeY + 5, 0xFFFFFF);

            // 如果鼠标悬停在属性上，显示属性的详细信息
            if (isMouseOverAttribute(mouseX, mouseY, attributeY)) {
                renderAttributeTooltip(context, mouseX, mouseY, attribute, value);
            }
        }
    }

    /**
     * 渲染属性图标
     */
    private void renderAttributeIcon(DrawContext context, JustDyingAttribute attribute, int x, int y) {
        // 先尝试使用物品图标
        if (attribute.getIconItem() != null) {
            // 从缓存获取或创建物品堆栈
            String key = attribute.getId().toString();
            ItemStack stack = itemStackCache.computeIfAbsent(key, k -> attribute.getIconItem().getDefaultStack());
            context.drawItem(stack, x, y);
        } else {
            // 如果无法获取物品，使用默认图标
            try {
                JustDyingAttributeType attributeType = JustDyingAttributeType.valueOf(
                        attribute.getId().getPath().toUpperCase());
                AttributeIcons.renderIcon(context, attributeType, x, y);
            } catch (IllegalArgumentException e) {
                // 如果无法获取枚举值，使用字符串方法
                AttributeIcons.renderIcon(context, attribute.getId().getPath(), x, y);
                JustDying.LOGGER.warn("无法将 {} 转换为属性类型枚举", attribute.getId().getPath());
            }
        }
    }

    /**
     * 检查鼠标是否悬停在属性上
     */
    private boolean isMouseOverAttribute(int mouseX, int mouseY, int attributeY) {
        return mouseX >= cachedLeft + PADDING && mouseX <= cachedLeft + GUI_WIDTH - PADDING - 50 &&
                mouseY >= attributeY && mouseY <= attributeY + ATTRIBUTE_HEIGHT;
    }

    /**
     * 渲染属性工具提示
     */
    private void renderAttributeTooltip(DrawContext context, int mouseX, int mouseY,
            JustDyingAttribute attribute, int value) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(attribute.getName());
        tooltip.add(Text.literal("").append(attribute.getDescription()).formatted(Formatting.GRAY));

        // 排序值信息（调试用）
        String attributePath = attribute.getId().getPath();
        int sortOrder = 0;
        if (JustDying.getConfig().attributes.attributes.containsKey(attributePath)) {
            sortOrder = JustDying.getConfig().attributes.attributes.get(attributePath).sortOrder;
        }
        tooltip.add(Text.literal("排序值: " + sortOrder).formatted(Formatting.DARK_GRAY));

        // 如果属性关联原版属性，显示加成数值
        if (attribute.getVanillaAttribute() != null) {
            double bonus = attribute.calculateAttributeBonus(value);
            tooltip.add(Text.literal("+" + String.format("%.2f", bonus) + " " +
                    attribute.getVanillaAttribute().getTranslationKey())
                    .formatted(Formatting.BLUE));
        }

        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }

    /**
     * 获取一页可显示的属性数量
     */
    private int getVisibleAttributeCount() {
        // 根据界面高度计算可见的属性数量
        return 5; // 设置为固定值，确保一致的布局
    }

    /**
     * 向上翻页
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
     * 向下翻页
     */
    private void scrollDown() {
        // 计算最大翻页偏移量
        int maxOffset = ((attributes.size() - 1) / getVisibleAttributeCount()) * getVisibleAttributeCount();
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
     * 尝试使用等级兑换属性点
     */
    private void exchangeLevelForPoint() {
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            ClientAttributePackets.sendExchangeLevelPacket();
        }
    }

    /**
     * 减少属性点
     */
    private void decreaseAttribute(JustDyingAttribute attribute) {
        int currentValue = AttributeHelper.getAttributeValue(player, attribute.getId());
        if (currentValue > attribute.getMinValue()) {
            // 发送减少属性点的网络包
            ClientAttributePackets.sendDecreasePacket(attribute.getId());

            // 客户端预测性更新
            AttributeHelper.setAttributeValue(player, attribute.getId(), currentValue - 1);
            AttributeHelper.addPoints(player, 1);
            this.availablePoints = AttributeHelper.getAvailablePoints(player);

            // 刷新界面
            updateButtonStates();
        }
    }

    /**
     * 增加属性点
     */
    private void increaseAttribute(JustDyingAttribute attribute) {
        if (availablePoints > 0) {
            int currentValue = AttributeHelper.getAttributeValue(player, attribute.getId());
            if (currentValue < attribute.getMaxValue()) {
                // 发送增加属性点的网络包
                ClientAttributePackets.sendIncreasePacket(attribute.getId());

                // 客户端预测性更新
                AttributeHelper.setAttributeValue(player, attribute.getId(), currentValue + 1);
                AttributeHelper.usePoints(player, 1);
                this.availablePoints = AttributeHelper.getAvailablePoints(player);

                // 刷新界面
                updateButtonStates();
            }
        }
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
        
        // 更新GUI高度
        updateGUIHeight();
        
        // 更新布局缓存
        updateLayoutCache();

        // 重新初始化界面
        resetButtons();
        addButtons();
        updateButtonStates();

        JustDying.LOGGER.debug("属性面板已刷新 - 可用点数: {}", availablePoints);
    }

    @Override
    public boolean shouldPause() {
        return false;
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
}