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
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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
    private static final int BUTTON_WIDTH = 15;
    private static final int BUTTON_HEIGHT = 15;
    private static final int BUTTON_SPACING = 5; // 按钮之间的间距
    // 缩减面板宽度，但保证足够的空间避免重叠
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 280; // 增加标准高度以适配玩家模型
    private static final int GUI_HEIGHT_COMPACT = 240; // 紧凑高度（无翻页按钮）
    private static final int GUI_HEIGHT_NO_EXCHANGE = 250; // 无等级兑换按钮高度
    private static final int GUI_HEIGHT_COMPACT_NO_EXCHANGE = 220; // 紧凑且无等级兑换按钮高度
    private static final int ATTR_CELL_WIDTH = 100; // 每个属性单元格的宽度
    private static final int ATTRS_PER_PAGE = 10;
    private static final int PLAYER_MODEL_SIZE = 35; // 玩家模型大小
    private static final int PLAYER_MODEL_Y_OFFSET = 60; // 玩家模型Y轴偏移量

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
            String path = attr.getId().getPath();
            return JustDying.getConfig().attributes.attributes.containsKey(path)
                    ? JustDying.getConfig().attributes.attributes.get(path).sortOrder
                    : Integer.MAX_VALUE;
        }));
        this.attributes = allAttributes;

        // 打开面板时请求服务器同步最新数据
        for (JustDyingAttribute attribute : attributes) {
            ClientAttributePackets.sendSyncRequestPacket(attribute.getId());
        }

        // 计算实际GUI高度
        updateGUIHeight();
    }

    /**
     * 根据当前配置和属性数量动态更新GUI高度
     */
    private void updateGUIHeight() {
        boolean needsPagination = attributes.size() > ATTRS_PER_PAGE;
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
        if (attributes.size() > ATTRS_PER_PAGE) {
            // 添加翻页按钮 - 移到面板右下角
            int buttonY = cachedTop + actualGuiHeight - 30;

            // 上翻按钮
            upButton = this.addDrawableChild(new AttributeButton(
                    cachedLeft + GUI_WIDTH - PADDING - BUTTON_WIDTH * 2 - BUTTON_SPACING,
                    buttonY,
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    Text.literal("↑"),
                    button -> scrollPage(-1)));

            // 下翻按钮
            downButton = this.addDrawableChild(new AttributeButton(
                    cachedLeft + GUI_WIDTH - PADDING - BUTTON_WIDTH,
                    buttonY,
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    Text.literal("↓"),
                    button -> scrollPage(1)));
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
                    button -> ClientAttributePackets.sendExchangeLevelPacket()));
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
        int end = Math.min(start + ATTRS_PER_PAGE, attributes.size());
        int visibleCount = end - start;
        int columns = 2; // 每行显示两个属性

        for (int i = 0; i < visibleCount; i++) {
            final int index = start + i;
            JustDyingAttribute attribute = attributes.get(index);

            // 计算行和列
            int row = i / columns;
            int col = i % columns;

            // 计算位置 - 与属性列表一致
            int attributeX = cachedLeft + PADDING + col * ATTR_CELL_WIDTH;
            int attributeY = cachedTop + 100 + row * ATTRIBUTE_HEIGHT; // 与属性列表的起始位置一致

            // 增加按钮 - 向右移动以避免重叠
            int buttonX = attributeX + 55; // 调整位置，避免与图标和文本重叠

            // 减少按钮 - 仅当配置允许时显示
            if (JustDying.getConfig().attributes.showDecreaseButtons) {
                this.addDrawableChild(new AttributeButton(
                        buttonX,
                        attributeY + 3, // 垂直居中
                        BUTTON_WIDTH, BUTTON_HEIGHT,
                        Text.literal("-"),
                        button -> editAttribute(attribute, 0)));
            }

            // 增加按钮
            this.addDrawableChild(new AttributeButton(
                    buttonX + BUTTON_WIDTH + BUTTON_SPACING,
                    attributeY + 3, // 垂直居中
                    BUTTON_WIDTH, BUTTON_HEIGHT,
                    Text.literal("+"),
                    button -> editAttribute(attribute, 1)));
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
            downButton.active = scrollOffset + ATTRS_PER_PAGE < attributes.size();
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
        // 绘制基础信息（包含玩家模型）
        renderBaseInfo(context, mouseX, mouseY);
        // 绘制当前等级和所需等级
        renderLevelInfo(context);
        // 绘制属性列表
        renderAttributeList(context, mouseX, mouseY);
        // 绘制分页信息 - 右下角
        renderPageInfo(context);
        //
        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 绘制面板背景和标题
     */
    private void renderBaseInfo(DrawContext context, int mouseX, int mouseY) {
        // 绘制面板背景
        context.fillGradient(cachedLeft, cachedTop, cachedLeft + GUI_WIDTH, cachedTop + actualGuiHeight,
                0xC0101010, 0xD0101010);
        context.drawBorder(cachedLeft, cachedTop, GUI_WIDTH, actualGuiHeight, 0xFF000000);
        // 绘制标题
        context.drawCenteredTextWithShadow(textRenderer, this.title, cachedCenterX, cachedTop + 10, 0xFFFFFF);
        
        // 绘制玩家模型（放在左侧）
        int modelX = cachedLeft + PADDING + 30;
        int modelY = cachedTop + PLAYER_MODEL_Y_OFFSET + PLAYER_MODEL_SIZE;
        InventoryScreen.drawEntity(context, 
                modelX, // X位置
                modelY, // Y位置
                PLAYER_MODEL_SIZE, // 模型大小
                modelX - mouseX, // 鼠标X轴差值影响旋转
                modelY - 50 - mouseY, // 鼠标Y轴差值影响旋转
                this.player);
                
        // 绘制可用点数（放在右侧上方）
        Text pointsText = Text.translatable("gui." + JustDying.MOD_ID + ".available_points", this.availablePoints);
        context.drawTextWithShadow(textRenderer, pointsText, 
                cachedLeft + GUI_WIDTH - PADDING - textRenderer.getWidth(pointsText), // 右对齐
                cachedTop + 40, // 上方位置
                0xFFFFFF);
    }

    /**
     * 绘制当前等级和所需等级
     */
    private void renderLevelInfo(DrawContext context) {
        // 如果启用了等级兑换功能，显示当前等级（放在右侧下方）
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            String levelText = Text.translatable("gui." + JustDying.MOD_ID + ".current_level", player.experienceLevel)
                    .getString();
            context.drawTextWithShadow(textRenderer, levelText,
                    cachedLeft + GUI_WIDTH - PADDING - textRenderer.getWidth(levelText), // 右对齐
                    cachedTop + 60, // 下方位置
                    0xFFFFFF);
        }
    }

    /**
     * 绘制分页信息
     */
    private void renderPageInfo(DrawContext context) {
        if (attributes.size() > ATTRS_PER_PAGE) {
            int maxPage = (int) Math.ceil((double) attributes.size() / ATTRS_PER_PAGE);
            int currentPage = (scrollOffset / ATTRS_PER_PAGE) + 1;
            String pageInfo = currentPage + "/" + maxPage;
            // 右下角位置
            context.drawTextWithShadow(textRenderer, pageInfo,
                    cachedLeft + GUI_WIDTH - PADDING - textRenderer.getWidth(pageInfo),
                    cachedTop + actualGuiHeight - 15, 0xAAAAAA);
        }
    }

    /**
     * 渲染属性列表
     */
    private void renderAttributeList(DrawContext context, int mouseX, int mouseY) {
        // 计算当前页显示的属性范围
        int start = scrollOffset;
        int end = Math.min(start + ATTRS_PER_PAGE, attributes.size());
        int visibleCount = end - start;
        int columns = 2; // 每行显示两个属性

        for (int i = 0; i < visibleCount; i++) {
            int index = start + i;
            JustDyingAttribute attribute = attributes.get(index);

            // 计算行和列
            int row = i / columns;
            int col = i % columns;

            // 计算位置 - 将属性列表下移以给玩家模型留出空间
            int attributeX = cachedLeft + PADDING + col * ATTR_CELL_WIDTH;
            int attributeY = cachedTop + 100 + row * ATTRIBUTE_HEIGHT; // 增加Y轴起始位置

            // 渲染属性图标
            renderAttributeIcon(context, attribute, attributeX, attributeY + 3); // 垂直居中

            // 不再显示属性名称，只显示属性值
            int value = AttributeHelper.getAttributeValue(player, attribute.getId());
            String valueText = value + "/" + attribute.getMaxValue();

            // 绘制属性值 - 调整位置避免重叠
            int textX = attributeX + 20; // 图标宽度+间距
            context.drawTextWithShadow(textRenderer, valueText,
                    textX, attributeY + 8, 0xFFFFFF); // 垂直居中

            // 如果鼠标悬停在属性上，显示属性的详细信息
            if (isMouseOverAttribute(mouseX, mouseY, attributeX, attributeY, col)) {
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
     * 渲染属性工具提示
     */
    private void renderAttributeTooltip(DrawContext context, int mouseX, int mouseY,
            JustDyingAttribute attribute, int value) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(attribute.getName());
        tooltip.add(Text.literal("").append(attribute.getDescription()).formatted(Formatting.GRAY));

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
        resetAndAddButtons();
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
     * 翻页方法
     */
    private void scrollPage(int direction) {
        if (direction < 0 && scrollOffset > 0) {
            scrollOffset = Math.max(0, scrollOffset - ATTRS_PER_PAGE);
            resetAndAddButtons();
        } else if (direction > 0) {
            int maxOffset = ((attributes.size() - 1) / ATTRS_PER_PAGE) * ATTRS_PER_PAGE;
            if (scrollOffset < maxOffset) {
                scrollOffset = Math.min(maxOffset, scrollOffset + ATTRS_PER_PAGE);
                resetAndAddButtons();
            }
        }
    }

    /**
     * 增加或者减少属性点
     */
    private void editAttribute(JustDyingAttribute attribute, int type) {
        int currentValue = AttributeHelper.getAttributeValue(player, attribute.getId());
        // 如果是增加的话
        if (type == 1 && availablePoints > 0 && currentValue < attribute.getMaxValue()) {
            // 发送增加属性点的网络包
            ClientAttributePackets.sendIncreasePacket(attribute.getId());
            // 客户端预测性更新
            AttributeHelper.setAttributeValue(player, attribute.getId(), currentValue + 1);
            AttributeHelper.usePoints(player, 1);
        }
        // 如果是减少的话
        if (type == 0 && currentValue > attribute.getMinValue()) {
            // 发送减少属性点的网络包
            ClientAttributePackets.sendDecreasePacket(attribute.getId());
            // 客户端预测性更新
            AttributeHelper.setAttributeValue(player, attribute.getId(), currentValue - 1);
            AttributeHelper.addPoints(player, 1);
        }
        this.availablePoints = AttributeHelper.getAvailablePoints(player);
        // 刷新界面
        updateButtonStates();
    }

    /**
     * 检查鼠标是否悬停在属性上
     */
    private boolean isMouseOverAttribute(int mouseX, int mouseY, int attributeX, int attributeY, int col) {
        // 调整检测区域，避免与按钮重叠
        return mouseX >= attributeX && mouseX <= attributeX + 60 && // 限制检测宽度，避免与按钮区域重叠
                mouseY >= attributeY && mouseY <= attributeY + ATTRIBUTE_HEIGHT;
    }

    /**
     * 重置并添加按钮
     */
    private void resetAndAddButtons() {
        clearChildren();
        addButtons();
    }
}