package com.justdie.gui;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeManager;
import com.justdie.attribute.AttributeHelper;
import com.justdie.attribute.JustDyingAttribute;
import com.justdie.attribute.JustDyingAttributeType;
import com.justdie.network.ClientAttributePackets;
import com.justdie.attribute.LevelExchangeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.Vec2f;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector4f;

import java.util.Map;
import java.util.Objects;
import java.util.Comparator;

/**
 * 属性面板屏幕
 * 负责显示和管理玩家的属性点分配
 */
public class AttributeScreen extends Screen {
    // GUI常量
    private final Vector4f lineColor = new Vector4f(1f, .85f, .7f, 1f);
    private final Vector4f radialButtonColor = new Vector4f(.04f, .03f, .01f, .6f);
    private final Vector4f highlightColor = new Vector4f(.8f, .7f, .55f, .7f);
    private final Vector4f panelBackgroundColor = new Vector4f(.15f, .12f, .1f, .9f);
    private final Vector4f infoBackgroundColor = new Vector4f(.2f, .18f, .15f, .95f);

    // 轮盘配置
    private final double ringInnerEdge = 20;
    private final double[] ringRadii = { 80, 130, 180 }; // 多层轮盘的半径
    private final int MAX_ITEMS_PER_RING = 6; // 每环最多显示的属性数量

    // 页面控制变量
    private final PlayerEntity player;
    private final List<JustDyingAttribute> attributes;
    private int availablePoints = 0; // 可用点数
    private int requiredLevel = 0; // 所需等级

    // 预缓存的物品图标堆栈
    private final Map<String, ItemStack> itemStackCache = new ConcurrentHashMap<>();

    private final boolean enableLevelExchange = JustDying.getConfig().levelExchange.enableLevelExchange;

    private int wheelSelection = -1;
    private int selectedRing = -1; // 当前选中的环
    private JustDyingAttribute selectedAttribute = null;
    private boolean isLevelExchangeSelected = false;
    
    // 用于标记是否锁定了选择
    private boolean selectionLocked = false;
    private int lockedRing = -1; 
    private int lockedWheelSelection = -1;
    private JustDyingAttribute lockedAttribute = null;
    private boolean lockedLevelExchange = false;

    // 按钮区域的常量
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 5;

    // 增减按钮
    private ButtonWidget increaseButton;
    private ButtonWidget decreaseButton;
    private ButtonWidget exchangeButton;

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

        // 更新数据
        this.availablePoints = AttributeHelper.getAvailablePoints(player);
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            this.requiredLevel = LevelExchangeManager.calculateRequiredLevel(player);
        }

        // 添加按钮，但初始时设为不可见
        addButtons();
        updateButtonVisibility();

        JustDying.LOGGER.debug("初始化属性面板 - 可用点数: {}, 所需等级: {}", availablePoints, requiredLevel);
    }

    /**
     * 添加控制按钮
     */
    private void addButtons() {
        // 按钮放在右上角
        int buttonX = this.width - BUTTON_WIDTH - 10;
        int buttonY = 50;

        // 增加按钮
        increaseButton = this.addDrawableChild(new ButtonWidget.Builder(
                Text.literal("+"),
                button -> {
                    if (selectedAttribute != null) {
                        increaseAttribute(selectedAttribute);
                    }
                })
                .position(buttonX, buttonY)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        // 减少按钮 - 仅当配置允许时显示
        if (JustDying.getConfig().attributes.showDecreaseButtons) {
            decreaseButton = this.addDrawableChild(new ButtonWidget.Builder(
                    Text.literal("-"),
                    button -> {
                        if (selectedAttribute != null) {
                            decreaseAttribute(selectedAttribute);
                        }
                    })
                    .position(buttonX, buttonY + BUTTON_HEIGHT + BUTTON_SPACING)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        // 等级兑换按钮 - 放在底部中央
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            Text buttonText = Text.translatable("gui.justdying.exchange_level", this.requiredLevel);
            exchangeButton = this.addDrawableChild(new ButtonWidget.Builder(
                    buttonText,
                    button -> exchangeLevelForPoint())
                    .position(this.width / 2 - 80, this.height - 40)
                    .size(160, BUTTON_HEIGHT)
                    .build());
        }

        // 初始状态下隐藏所有按钮
        updateButtonVisibility();
    }

    /**
     * 更新按钮可见性
     */
    private void updateButtonVisibility() {
        // 默认隐藏所有按钮
        boolean showAttributeButtons = selectedAttribute != null && !isLevelExchangeSelected;
        boolean showExchangeButton = isLevelExchangeSelected;

        if (increaseButton != null) {
            increaseButton.visible = showAttributeButtons;
            if (showAttributeButtons) {
                increaseButton.active = availablePoints > 0 &&
                        AttributeHelper.getAttributeValue(player, selectedAttribute.getId()) < selectedAttribute
                                .getMaxValue();
            }
        }

        if (decreaseButton != null) {
            decreaseButton.visible = showAttributeButtons && JustDying.getConfig().attributes.showDecreaseButtons;
            if (showAttributeButtons) {
                decreaseButton.active = AttributeHelper.getAttributeValue(player,
                        selectedAttribute.getId()) > selectedAttribute.getMinValue();
            }
        }

        if (exchangeButton != null) {
            exchangeButton.visible = showExchangeButton;
            if (showExchangeButton) {
                exchangeButton.active = player.experienceLevel >= requiredLevel;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制默认的深色背景
        this.renderBackground(context);
        MatrixStack matrices = context.getMatrices();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        Vec2f screenCenter = new Vec2f(this.width / 2f, this.height / 2f);
        Vec2f mousePos = new Vec2f(mouseX, mouseY);

        // 检测鼠标位置，确定悬停的属性
        int oldWheelSelection = wheelSelection;
        int oldSelectedRing = selectedRing;
        
        // 如果没有锁定选择，则根据鼠标位置更新选择
        if (!selectionLocked) {
            // 计算鼠标与中心的距离
            double mouseDistance = mousePos.distanceSquared(screenCenter);
            double mouseAngle = getAngle(mousePos, screenCenter);
            
            // 默认清除选择
            wheelSelection = -1;
            selectedRing = -1;
            selectedAttribute = null;
            isLevelExchangeSelected = false;
            
            // 检查鼠标是否在轮盘范围内
            for (int ringIndex = 0; ringIndex < ringRadii.length; ringIndex++) {
                double ringOuterEdge = ringRadii[ringIndex];
                double ringInnerRadius = ringIndex == 0 ? ringInnerEdge : ringRadii[ringIndex - 1];
                
                if (mouseDistance >= ringInnerRadius * ringInnerRadius && mouseDistance <= ringOuterEdge * ringOuterEdge) {
                    // 确定选中的环
                    selectedRing = ringIndex;
                    
                    // 计算这个环中的项目数量
                    int startIndex = 0;
                    for (int i = 0; i < ringIndex; i++) {
                        startIndex += MAX_ITEMS_PER_RING;
                    }
                    
                    int itemsInThisRing = Math.min(MAX_ITEMS_PER_RING, attributes.size() - startIndex);
                    if (ringIndex == ringRadii.length - 1 && enableLevelExchange && (startIndex + itemsInThisRing < attributes.size() + 1)) {
                        // 在最外环添加等级兑换
                        itemsInThisRing++;
                    }
                    
                    if (itemsInThisRing > 0) {
                        double radiansPerItem = 2 * Math.PI / itemsInThisRing;
                        wheelSelection = (int) (((mouseAngle + Math.PI / 2) % (2 * Math.PI)) / radiansPerItem);
                        
                        // 计算实际索引
                        int actualIndex = startIndex + wheelSelection;
                        
                        // 检查是否是有效的属性或者等级兑换
                        if (actualIndex < attributes.size()) {
                            selectedAttribute = attributes.get(actualIndex);
                            isLevelExchangeSelected = false;
                        } else if (enableLevelExchange && actualIndex == attributes.size()) {
                            selectedAttribute = null;
                            isLevelExchangeSelected = true;
                        } else {
                            // 无效的选择
                            wheelSelection = -1;
                            selectedRing = -1;
                        }
                    }
                    
                    break;
                }
            }
        } else {
            // 如果锁定了选择，则使用锁定的值
            selectedRing = lockedRing;
            wheelSelection = lockedWheelSelection;
            selectedAttribute = lockedAttribute;
            isLevelExchangeSelected = lockedLevelExchange;
        }
        
        // 如果选择变化了，更新按钮状态
        if (oldWheelSelection != wheelSelection || oldSelectedRing != selectedRing) {
            updateButtonVisibility();
        }

        // 开始渲染轮盘
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 为不同环绘制轮盘背景和分隔线
        drawRings(context, centerX, centerY);
        
        // 绘制属性图标
        drawAttributeIcons(context, centerX, centerY);
        
        // 如果有选中的属性，绘制详细信息
        if (selectedAttribute != null) {
            drawInfoPanel(context, centerX, centerY + 50, selectedAttribute);
        } else if (isLevelExchangeSelected) {
            drawExchangeInfoPanel(context, centerX, centerY + 50);
        }

        // 绘制可用点数
        Text pointsText = Text.translatable("gui." + JustDying.MOD_ID + ".available_points", this.availablePoints);
        context.drawTextWithShadow(this.textRenderer, pointsText,
                10, 10, 0xFFFFFF);

        // 绘制当前等级
        if (enableLevelExchange) {
            Text levelText = Text.translatable("gui." + JustDying.MOD_ID + ".current_level", player.experienceLevel);
            context.drawTextWithShadow(this.textRenderer, levelText,
                    10, 25, 0xFFFFFF);
        }
        
        // 绘制鼠标锁定状态提示
        if (selectionLocked) {
            Text lockText = Text.translatable("gui." + JustDying.MOD_ID + ".selection_locked");
            if (lockText.getString().isEmpty()) {
                // 如果翻译键不存在，使用默认文本
                lockText = Text.literal("点击空白处取消选择");
            }
            context.drawTextWithShadow(this.textRenderer, lockText,
                    10, 40, 0xFFAA00);
        }

        // 确保调用父类的render方法，这样按钮才会被正确渲染
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * 绘制轮盘环
     */
    private void drawRings(DrawContext context, int centerX, int centerY) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        // 为每个环设置不同的颜色和透明度
        Vector4f[] ringColors = {
            new Vector4f(radialButtonColor.x, radialButtonColor.y, radialButtonColor.z, 0.7f),
            new Vector4f(radialButtonColor.x * 1.1f, radialButtonColor.y * 1.1f, radialButtonColor.z * 1.1f, 0.6f),
            new Vector4f(radialButtonColor.x * 1.2f, radialButtonColor.y * 1.2f, radialButtonColor.z * 1.2f, 0.5f)
        };
        
        for (int ringIndex = 0; ringIndex < ringRadii.length; ringIndex++) {
            double ringOuterEdge = ringRadii[ringIndex];
            double ringInnerRadius = ringIndex == 0 ? ringInnerEdge : ringRadii[ringIndex - 1];
            
            // 计算这个环中的项目数量
            int startIndex = 0;
            for (int i = 0; i < ringIndex; i++) {
                startIndex += MAX_ITEMS_PER_RING;
            }
            
            int itemsInThisRing = Math.min(MAX_ITEMS_PER_RING, attributes.size() - startIndex);
            if (ringIndex == ringRadii.length - 1 && enableLevelExchange && (startIndex + itemsInThisRing < attributes.size() + 1)) {
                // 在最外环添加等级兑换
                itemsInThisRing++;
            }
            
            if (itemsInThisRing <= 0) continue;
            
            double radiansPerItem = 2 * Math.PI / itemsInThisRing;
            int segments = itemsInThisRing * 6; // 每个项目6个细分
            
            // 绘制环背景
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            
            // 环中心
            buffer.vertex(centerX, centerY, 0).color(
                ringColors[ringIndex].x, 
                ringColors[ringIndex].y, 
                ringColors[ringIndex].z, 
                0.0f).next();
            
            // 绘制环的外边缘
            for (int i = 0; i <= segments; i++) {
                double angle = i * (2 * Math.PI / segments);
                double x = centerX + Math.cos(angle) * ringOuterEdge;
                double y = centerY + Math.sin(angle) * ringOuterEdge;
                buffer.vertex(x, y, 0).color(
                    ringColors[ringIndex].x, 
                    ringColors[ringIndex].y, 
                    ringColors[ringIndex].z, 
                    ringColors[ringIndex].w).next();
            }
            tessellator.draw();
            
            // 绘制环的内边缘（挖洞）
            buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(centerX, centerY, 0).color(0, 0, 0, 0).next();
            for (int i = 0; i <= segments; i++) {
                double angle = i * (2 * Math.PI / segments);
                double x = centerX + Math.cos(angle) * ringInnerRadius;
                double y = centerY + Math.sin(angle) * ringInnerRadius;
                buffer.vertex(x, y, 0).color(0, 0, 0, 0).next();
            }
            tessellator.draw();

            // 绘制选中项目的高亮扇形
            for (int i = 0; i < itemsInThisRing; i++) {
                double angle = i * radiansPerItem;
                boolean isHighlighted = selectedRing == ringIndex && wheelSelection == i;
                
                if (isHighlighted) {
                    Vector4f color = highlightColor;
                    // 绘制高亮扇形
                    buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                    buffer.vertex(centerX, centerY, 0).color(color.x, color.y, color.z, color.w * 0.5f).next();
                    
                    int subSegments = 12; // 高亮扇形的细分数
                    for (int j = 0; j <= subSegments; j++) {
                        double segmentAngle = angle + (j * radiansPerItem / subSegments);
                        double x = centerX + Math.cos(segmentAngle) * ringOuterEdge;
                        double y = centerY + Math.sin(segmentAngle) * ringOuterEdge;
                        buffer.vertex(x, y, 0).color(color.x, color.y, color.z, color.w * 0.7f).next();
                    }
                    tessellator.draw();
                }
            }
            
            // 单独绘制更明显的分隔线
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < itemsInThisRing; i++) {
                double angle = i * radiansPerItem;
                double x1 = centerX + Math.cos(angle) * ringInnerRadius;
                double y1 = centerY + Math.sin(angle) * ringInnerRadius;
                double x2 = centerX + Math.cos(angle) * ringOuterEdge;
                double y2 = centerY + Math.sin(angle) * ringOuterEdge;
                
                // 计算垂直于半径方向的向量
                double dx = (x2 - x1);
                double dy = (y2 - y1);
                double length = Math.sqrt(dx * dx + dy * dy);
                double perpX = -dy / length * 1.5; // 1.5是线宽的一半
                double perpY = dx / length * 1.5;
                
                // 绘制可见的线条（四边形）
                buffer.vertex(x1 + perpX, y1 + perpY, 0).color(1f, 1f, 1f, 0.6f).next();
                buffer.vertex(x1 - perpX, y1 - perpY, 0).color(1f, 1f, 1f, 0.6f).next();
                buffer.vertex(x2 - perpX, y2 - perpY, 0).color(1f, 1f, 1f, 0.3f).next();
                buffer.vertex(x2 + perpX, y2 + perpY, 0).color(1f, 1f, 1f, 0.3f).next();
            }
            tessellator.draw();
        }
    }
    
    /**
     * 绘制属性图标
     */
    private void drawAttributeIcons(DrawContext context, int centerX, int centerY) {
        int totalItemsDrawn = 0;
        
        for (int ringIndex = 0; ringIndex < ringRadii.length; ringIndex++) {
            double ringOuterEdge = ringRadii[ringIndex];
            double ringInnerRadius = ringIndex == 0 ? ringInnerEdge : ringRadii[ringIndex - 1];
            double iconRadius = (ringInnerRadius + ringOuterEdge) / 2;
            
            // 计算这个环中的项目起始索引
            int startIndex = 0;
            for (int i = 0; i < ringIndex; i++) {
                startIndex += MAX_ITEMS_PER_RING;
            }
            
            // 计算这个环中的项目数量
            int itemsInThisRing = Math.min(MAX_ITEMS_PER_RING, attributes.size() - startIndex);
            if (ringIndex == ringRadii.length - 1 && enableLevelExchange && (totalItemsDrawn + itemsInThisRing < attributes.size() + 1)) {
                // 在最外环添加等级兑换
                itemsInThisRing++;
            }
            
            if (itemsInThisRing <= 0) continue;
            
            double radiansPerItem = 2 * Math.PI / itemsInThisRing;
            
            for (int i = 0; i < itemsInThisRing; i++) {
                int actualIndex = startIndex + i;
                double angle = i * radiansPerItem;
                
                double iconX = centerX + Math.cos(angle) * iconRadius;
                double iconY = centerY + Math.sin(angle) * iconRadius;
                
                if (actualIndex < attributes.size()) {
                    // 渲染属性图标
                    JustDyingAttribute attr = attributes.get(actualIndex);
                    renderAttributeIcon(context, attr, (int)iconX - 8, (int)iconY - 8);
                } else if (enableLevelExchange && actualIndex == attributes.size()) {
                    // 渲染等级兑换图标
                    ItemStack exchangeIcon = AttributeIcons.getLevelExchangeIcon();
                    context.drawItem(exchangeIcon, (int)iconX - 8, (int)iconY - 8);
                }
            }
            
            totalItemsDrawn += itemsInThisRing;
        }
    }
    
    /**
     * 绘制信息面板
     */
    private void drawInfoPanel(DrawContext context, int centerX, int centerY, JustDyingAttribute attribute) {
        int panelWidth = 240;
        int panelHeight = 120;
        int left = centerX - panelWidth / 2;
        int top = centerY - panelHeight / 2;
        
        // 绘制面板背景
        context.fill(left, top, left + panelWidth, top + panelHeight, getARGB(infoBackgroundColor));
        context.drawBorder(left, top, panelWidth, panelHeight, 0xFF000000);
        
        // 获取属性值
        int value = AttributeHelper.getAttributeValue(player, attribute.getId());
        
        // 绘制属性图标和名称
        renderAttributeIcon(context, attribute, left + 10, top + 10);
        context.drawTextWithShadow(this.textRenderer, attribute.getName(), 
                left + 32, top + 14, 0xFFFFFF);
        
        // 绘制属性值
        String valueString = value + "/" + attribute.getMaxValue();
        int valueWidth = this.textRenderer.getWidth(valueString);
        context.drawTextWithShadow(this.textRenderer, valueString, 
                left + panelWidth - 10 - valueWidth, top + 14, 0xFFFFFF);
        
        // 绘制分隔线
        context.fill(left + 10, top + 30, left + panelWidth - 10, top + 31, 0x66FFFFFF);
        
        // 绘制属性描述
        if (attribute.getDescription() != null) {
            context.drawTextWithShadow(this.textRenderer, attribute.getDescription(), 
                    left + 10, top + 40, 0xCCCCCC);
        }
        
        // 如果有原版属性加成，显示加成值
        if (attribute.getVanillaAttribute() != null) {
            double bonus = attribute.calculateAttributeBonus(value);
            Text bonusText = Text.literal("+" + String.format("%.2f", bonus) + " " + 
                    attribute.getVanillaAttribute().getTranslationKey())
                    .formatted(Formatting.BLUE);
                    
            context.drawTextWithShadow(this.textRenderer, bonusText, 
                    left + 10, top + 60, 0x55FFFF);
        }
        
        // 绘制使用提示
        if (availablePoints > 0) {
            Text hintText = Text.translatable("gui." + JustDying.MOD_ID + ".increase_hint");
            context.drawTextWithShadow(this.textRenderer, hintText, 
                    left + 10, top + panelHeight - 20, 0xAAAAAA);
        }
    }
    
    /**
     * 绘制等级兑换信息面板
     */
    private void drawExchangeInfoPanel(DrawContext context, int centerX, int centerY) {
        int panelWidth = 240;
        int panelHeight = 120;
        int left = centerX - panelWidth / 2;
        int top = centerY - panelHeight / 2;
        
        // 绘制面板背景
        context.fill(left, top, left + panelWidth, top + panelHeight, getARGB(infoBackgroundColor));
        context.drawBorder(left, top, panelWidth, panelHeight, 0xFF000000);
        
        // 绘制图标和标题
        ItemStack exchangeIcon = AttributeIcons.getLevelExchangeIcon();
        context.drawItem(exchangeIcon, left + 10, top + 10);
        
        Text titleText = Text.translatable("gui." + JustDying.MOD_ID + ".level_exchange");
        context.drawTextWithShadow(this.textRenderer, titleText, 
                left + 32, top + 14, 0xFFFFFF);
        
        // 绘制分隔线
        context.fill(left + 10, top + 30, left + panelWidth - 10, top + 31, 0x66FFFFFF);
        
        // 绘制当前等级和所需等级
        Text curLevelText = Text.translatable("gui." + JustDying.MOD_ID + ".current_level", player.experienceLevel);
        context.drawTextWithShadow(this.textRenderer, curLevelText, 
                left + 10, top + 40, player.experienceLevel >= requiredLevel ? 0x55FF55 : 0xFF5555);
                
        Text reqText = Text.translatable("gui." + JustDying.MOD_ID + ".required_level", this.requiredLevel);
        context.drawTextWithShadow(this.textRenderer, reqText, 
                left + 10, top + 55, 0xFFFFFF);
        
        // 绘制描述
        Text descText = Text.translatable("gui." + JustDying.MOD_ID + ".exchange_description");
        context.drawTextWithShadow(this.textRenderer, descText, 
                left + 10, top + 75, 0xBBBBBB);
        
        // 绘制使用提示
        if (player.experienceLevel >= requiredLevel) {
            Text hintText = Text.translatable("gui." + JustDying.MOD_ID + ".exchange_hint");
            context.drawTextWithShadow(this.textRenderer, hintText, 
                    left + 10, top + panelHeight - 20, 0xAAAAAA);
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
     * 尝试使用等级兑换属性点
     */
    private void exchangeLevelForPoint() {
        if (JustDying.getConfig().levelExchange.enableLevelExchange && player.experienceLevel >= requiredLevel) {
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

            // 更新按钮状态
            updateButtonVisibility();
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

                // 更新按钮状态
                updateButtonVisibility();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Vec2f screenCenter = new Vec2f(this.width / 2f, this.height / 2f);
        Vec2f mousePos = new Vec2f((float) mouseX, (float) mouseY);
        double mouseDistance = mousePos.distanceSquared(screenCenter);
        double mouseAngle = getAngle(mousePos, screenCenter);
        
        boolean clickedOnWheel = false;
        
        // 检查是否点击了轮盘区域
        for (int ringIndex = 0; ringIndex < ringRadii.length; ringIndex++) {
            double ringOuterEdge = ringRadii[ringIndex];
            double ringInnerRadius = ringIndex == 0 ? ringInnerEdge : ringRadii[ringIndex - 1];
            
            if (mouseDistance >= ringInnerRadius * ringInnerRadius && mouseDistance <= ringOuterEdge * ringOuterEdge) {
                clickedOnWheel = true;
                
                // 计算该环中的项目数量
                int startIndex = 0;
                for (int i = 0; i < ringIndex; i++) {
                    startIndex += MAX_ITEMS_PER_RING;
                }
                
                int itemsInThisRing = Math.min(MAX_ITEMS_PER_RING, attributes.size() - startIndex);
                if (ringIndex == ringRadii.length - 1 && enableLevelExchange && (startIndex + itemsInThisRing < attributes.size() + 1)) {
                    // 在最外环添加等级兑换
                    itemsInThisRing++;
                }
                
                if (itemsInThisRing > 0) {
                    double radiansPerItem = 2 * Math.PI / itemsInThisRing;
                    int clickedSelection = (int) (((mouseAngle + Math.PI / 2) % (2 * Math.PI)) / radiansPerItem);
                    
                    // 计算实际索引
                    int actualIndex = startIndex + clickedSelection;
                    
                    // 切换锁定状态
                    if (selectionLocked && 
                       lockedRing == ringIndex && 
                       lockedWheelSelection == clickedSelection) {
                        // 如果点击了已锁定的项目，取消锁定
                        selectionLocked = false;
                        lockedRing = -1;
                        lockedWheelSelection = -1;
                        lockedAttribute = null;
                        lockedLevelExchange = false;
                        
                        // 更新当前选中状态为鼠标位置
                        selectedRing = ringIndex;
                        wheelSelection = clickedSelection;
                        if (actualIndex < attributes.size()) {
                            selectedAttribute = attributes.get(actualIndex);
                            isLevelExchangeSelected = false;
                        } else if (enableLevelExchange && actualIndex == attributes.size()) {
                            selectedAttribute = null;
                            isLevelExchangeSelected = true;
                        }
                    } else {
                        // 锁定新选择的项目
                        selectionLocked = true;
                        lockedRing = ringIndex;
                        lockedWheelSelection = clickedSelection;
                        
                        if (actualIndex < attributes.size()) {
                            lockedAttribute = attributes.get(actualIndex);
                            lockedLevelExchange = false;
                            selectedAttribute = lockedAttribute;
                            isLevelExchangeSelected = false;
                        } else if (enableLevelExchange && actualIndex == attributes.size()) {
                            lockedAttribute = null;
                            lockedLevelExchange = true;
                            selectedAttribute = null;
                            isLevelExchangeSelected = true;
                        }
                    }
                    
                    updateButtonVisibility();
                    return true;
                }
                
                break;
            }
        }
        
        // 如果点击了空白区域且当前有锁定选择，则取消锁定
        if (!clickedOnWheel && selectionLocked) {
            selectionLocked = false;
            lockedRing = -1;
            lockedWheelSelection = -1;
            lockedAttribute = null;
            lockedLevelExchange = false;
            
            // 清除选择
            selectedRing = -1;
            wheelSelection = -1;
            selectedAttribute = null;
            isLevelExchangeSelected = false;
            
            updateButtonVisibility();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
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

        // 更新按钮
        updateButtonVisibility();

        JustDying.LOGGER.debug("属性面板已刷新 - 可用点数: {}", availablePoints);
    }

    /**
     * 将Vector4f颜色转换为ARGB整数
     */
    private int getARGB(Vector4f color) {
        int a = (int)(color.w * 255) & 0xFF;
        int r = (int)(color.x * 255) & 0xFF;
        int g = (int)(color.y * 255) & 0xFF;
        int b = (int)(color.z * 255) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // 计算两点之间的角度（0-2π）
    private double getAngle(Vec2f a, Vec2f b) {
        double angle = Math.atan2(a.y - b.y, a.x - b.x);
        return angle < 0 ? angle + 2 * Math.PI : angle;
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