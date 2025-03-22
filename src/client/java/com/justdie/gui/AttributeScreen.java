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
import net.minecraft.util.math.MathHelper;
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
    private final Vector4f textColor = new Vector4f(1f, 1f, 1f, 1f);

    private final double ringInnerEdge = 20;
    private double ringOuterEdge = 80;
    private final double ringOuterEdgeMax = 80;
    private final double ringOuterEdgeMin = 65;

    // 页面控制变量
    private final PlayerEntity player;
    private final List<JustDyingAttribute> attributes;
    private int availablePoints = 0; // 可用点数
    private int requiredLevel = 0; // 所需等级

    // 预缓存的物品图标堆栈
    private final Map<String, ItemStack> itemStackCache = new ConcurrentHashMap<>();

    private final boolean enableLevelExchange = JustDying.getConfig().levelExchange.enableLevelExchange;

    private int wheelSelection = -1;
    private JustDyingAttribute selectedAttribute = null;
    private boolean isLevelExchangeSelected = false;

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
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 增加按钮
        increaseButton = this.addDrawableChild(new ButtonWidget.Builder(
                Text.literal("+"),
                button -> {
                    if (selectedAttribute != null) {
                        increaseAttribute(selectedAttribute);
                    }
                })
                .position(centerX + 100, centerY - BUTTON_HEIGHT / 2)
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
                    .position(centerX + 100 + BUTTON_WIDTH + BUTTON_SPACING, centerY - BUTTON_HEIGHT / 2)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        // 等级兑换按钮
        if (JustDying.getConfig().levelExchange.enableLevelExchange) {
            Text buttonText = Text.translatable("gui.justdying.exchange_level", this.requiredLevel);
            exchangeButton = this.addDrawableChild(new ButtonWidget.Builder(
                    buttonText,
                    button -> exchangeLevelForPoint())
                    .position(centerX - 80, centerY + 100)
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
        MatrixStack matrices = context.getMatrices();

        // 判断可用属性数量
        int visibleCount = attributes.size();
        if (enableLevelExchange) {
            visibleCount += 1;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        Vec2f screenCenter = new Vec2f(this.width / 2f, this.height / 2f);
        Vec2f mousePos = new Vec2f(mouseX, mouseY);
        double radiansPerSpell = Math.toRadians(360.0 / visibleCount);

        float mouseRotation = (getAngle(mousePos, screenCenter) + 1.570f + (float) radiansPerSpell * .5f) % 6.283f;

        int oldWheelSelection = wheelSelection;
        wheelSelection = (int) MathHelper.clamp(mouseRotation / radiansPerSpell, 0, visibleCount - 1);
        double mouseDistance = mousePos.distanceSquared(screenCenter);

        // 只有在轮盘范围内才进行选择
        if (mouseDistance < ringOuterEdge * ringOuterEdge && mouseDistance > ringInnerEdge * ringInnerEdge) {
            if (wheelSelection < attributes.size()) {
                selectedAttribute = attributes.get(wheelSelection);
                isLevelExchangeSelected = false;
            } else {
                // 选择了等级兑换
                selectedAttribute = null;
                isLevelExchangeSelected = true;
            }
        } else if (mouseDistance <= ringInnerEdge * ringInnerEdge) {
            // 在中心区域清除选择
            wheelSelection = -1;
            selectedAttribute = null;
            isLevelExchangeSelected = false;
        }

        // 如果选择变化了，更新按钮状态
        if (oldWheelSelection != wheelSelection) {
            updateButtonVisibility();
        }

        // 开始渲染轮盘
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 渲染轮盘背景
        drawRadialBackgrounds(buffer, centerX, centerY, wheelSelection, radiansPerSpell, visibleCount);
        drawDividingLines(buffer, centerX, centerY, radiansPerSpell, visibleCount);

        tessellator.draw();

        // 绘制属性图标和名称
        for (int i = 0; i < visibleCount; i++) {
            double angle = i * radiansPerSpell - (Math.PI / 2);
            double radius = (ringInnerEdge + ringOuterEdge) / 2;

            double iconX = centerX + Math.cos(angle) * radius;
            double iconY = centerY + Math.sin(angle) * radius;

            if (i < attributes.size()) {
                // 渲染属性图标
                JustDyingAttribute attr = attributes.get(i);
                renderAttributeIcon(context, attr, (int) iconX - 8, (int) iconY - 8);

                // 如果被选中，渲染属性名称和值
                if (i == wheelSelection) {
                    renderSelectedAttributeInfo(context, attr, centerX, centerY, mouseX, mouseY);
                }
            } else {
                // 渲染等级兑换图标
                ItemStack exchangeIcon = AttributeIcons.getLevelExchangeIcon();
                context.drawItem(exchangeIcon, (int) iconX - 8, (int) iconY - 8);

                // 如果被选中，渲染等级兑换信息
                if (i == wheelSelection) {
                    renderLevelExchangeInfo(context, centerX, centerY);
                }
            }
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

        // 确保调用父类的render方法，这样按钮才会被正确渲染
        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 渲染选中的属性信息
     */
    private void renderSelectedAttributeInfo(DrawContext context, JustDyingAttribute attribute,
            int centerX, int centerY, int mouseX, int mouseY) {
        int value = AttributeHelper.getAttributeValue(player, attribute.getId());

        // 绘制属性名称
        Text nameText = attribute.getName();
        context.drawCenteredTextWithShadow(this.textRenderer, nameText,
                centerX, centerY - 50, 0xFFFFFF);

        // 绘制属性值
        Text valueText = Text.literal(value + "/" + attribute.getMaxValue());
        context.drawCenteredTextWithShadow(this.textRenderer, valueText,
                centerX, centerY - 35, 0xFFFFFF);

        // 如果属性有描述，绘制描述
        if (attribute.getDescription() != null) {
            Text descText = attribute.getDescription();
            int descWidth = Math.min(200, this.textRenderer.getWidth(descText) + 10);

            drawTextBackground(context.getMatrices(), centerX, centerY, 10, descWidth, 20);

            context.drawCenteredTextWithShadow(this.textRenderer, descText,
                    centerX, centerY - 10, 0xBBBBBB);
        }

        // 如果属性关联原版属性，显示加成数值
        if (attribute.getVanillaAttribute() != null) {
            double bonus = attribute.calculateAttributeBonus(value);
            Text bonusText = Text.literal("+" + String.format("%.2f", bonus) + " " +
                    attribute.getVanillaAttribute().getTranslationKey())
                    .formatted(Formatting.BLUE);

            context.drawCenteredTextWithShadow(this.textRenderer, bonusText,
                    centerX, centerY + 10, 0x55FFFF);
        }
    }

    /**
     * 渲染等级兑换信息
     */
    private void renderLevelExchangeInfo(DrawContext context, int centerX, int centerY) {
        // 绘制等级兑换标题
        Text titleText = Text.translatable("gui." + JustDying.MOD_ID + ".level_exchange");
        context.drawCenteredTextWithShadow(this.textRenderer, titleText,
                centerX, centerY - 50, 0xFFFFFF);

        // 绘制所需等级
        Text reqText = Text.translatable("gui." + JustDying.MOD_ID + ".required_level", this.requiredLevel);
        context.drawCenteredTextWithShadow(this.textRenderer, reqText,
                centerX, centerY - 35, 0xFFFFFF);

        // 绘制当前等级
        Text curLevelText = Text.translatable("gui." + JustDying.MOD_ID + ".current_level", player.experienceLevel);
        context.drawCenteredTextWithShadow(this.textRenderer, curLevelText,
                centerX, centerY - 20, player.experienceLevel >= requiredLevel ? 0x55FF55 : 0xFF5555);

        // 绘制描述
        Text descText = Text.translatable("gui." + JustDying.MOD_ID + ".exchange_description");
        int descWidth = Math.min(200, this.textRenderer.getWidth(descText) + 10);

        drawTextBackground(context.getMatrices(), centerX, centerY, -5, descWidth, 20);

        context.drawCenteredTextWithShadow(this.textRenderer, descText,
                centerX, centerY + 5, 0xBBBBBB);
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
        // 处理鼠标点击
        Vec2f screenCenter = new Vec2f(this.width / 2f, this.height / 2f);
        Vec2f mousePos = new Vec2f((float) mouseX, (float) mouseY);
        double mouseDistance = mousePos.distanceSquared(screenCenter);

        // 检查是否点击了轮盘中的某个选项
        if (mouseDistance < ringOuterEdge * ringOuterEdge && mouseDistance > ringInnerEdge * ringInnerEdge) {
            if (wheelSelection >= 0) {
                if (wheelSelection < attributes.size()) {
                    // 点击了属性
                    selectedAttribute = attributes.get(wheelSelection);
                    isLevelExchangeSelected = false;
                } else {
                    // 点击了等级兑换
                    selectedAttribute = null;
                    isLevelExchangeSelected = true;
                }
                updateButtonVisibility();
                return true;
            }
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

    private void drawRadialBackgrounds(BufferBuilder buffer, double centerX, double centerY,
            int selectedSpellIndex, double radiansPerSpell, int totalSpells) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        double quarterCircle = Math.PI / 2;
        int segments;
        if (totalSpells < 6) {
            segments = totalSpells % 2 == 1 ? 15 : 12;
        } else {
            segments = totalSpells * 2;
        }
        double radiansPerObject = 2 * Math.PI / segments;
        ringOuterEdge = Math.max(ringOuterEdgeMin, ringOuterEdgeMax);

        for (int i = 0; i < segments; i++) {
            final double beginRadians = i * radiansPerObject - (quarterCircle + (radiansPerSpell / 2));
            final double endRadians = (i + 1) * radiansPerObject - (quarterCircle + (radiansPerSpell / 2));

            final double x1m1 = Math.cos(beginRadians) * ringInnerEdge;
            final double x2m1 = Math.cos(endRadians) * ringInnerEdge;
            final double y1m1 = Math.sin(beginRadians) * ringInnerEdge;
            final double y2m1 = Math.sin(endRadians) * ringInnerEdge;

            final double x1m2 = Math.cos(beginRadians) * ringOuterEdge;
            final double x2m2 = Math.cos(endRadians) * ringOuterEdge;
            final double y1m2 = Math.sin(beginRadians) * ringOuterEdge;
            final double y2m2 = Math.sin(endRadians) * ringOuterEdge;

            boolean isHighlighted = (i * totalSpells) / segments == selectedSpellIndex;

            Vector4f color = isHighlighted ? highlightColor : radialButtonColor;

            buffer.vertex(centerX + x1m1, centerY + y1m1, 0).color(color.x, color.y, color.z, color.w).next();
            buffer.vertex(centerX + x2m1, centerY + y2m1, 0).color(color.x, color.y, color.z, color.w).next();
            buffer.vertex(centerX + x2m2, centerY + y2m2, 0).color(color.x, color.y, color.z, 0).next();
            buffer.vertex(centerX + x1m2, centerY + y1m2, 0).color(color.x, color.y, color.z, 0).next();

            // 分类线
            color = lineColor;
            double categoryLineWidth = 2;
            final double categoryLineOuterEdge = ringInnerEdge + categoryLineWidth;

            final double x1m3 = Math.cos(beginRadians) * categoryLineOuterEdge;
            final double x2m3 = Math.cos(endRadians) * categoryLineOuterEdge;
            final double y1m3 = Math.sin(beginRadians) * categoryLineOuterEdge;
            final double y2m3 = Math.sin(endRadians) * categoryLineOuterEdge;

            buffer.vertex(centerX + x1m1, centerY + y1m1, 0).color(color.x, color.y, color.z, color.w).next();
            buffer.vertex(centerX + x2m1, centerY + y2m1, 0).color(color.x, color.y, color.z, color.w).next();
            buffer.vertex(centerX + x2m3, centerY + y2m3, 0).color(color.x, color.y, color.z, color.w).next();
            buffer.vertex(centerX + x1m3, centerY + y1m3, 0).color(color.x, color.y, color.z, color.w).next();
        }
    }

    private void drawDividingLines(BufferBuilder buffer, double centerX, double centerY,
            double radiansPerSpell, int totalSpells) {
        if (totalSpells <= 1)
            return;

        double quarterCircle = Math.PI / 2;
        ringOuterEdge = Math.max(ringOuterEdgeMin, ringOuterEdgeMax);

        for (int i = 0; i < totalSpells; i++) {
            final double closeWidth = 8 * Math.PI / 180; // 8度
            final double farWidth = closeWidth / 4;
            final double beginCloseRadians = i * radiansPerSpell - (quarterCircle + (radiansPerSpell / 2))
                    - (closeWidth / 4);
            final double endCloseRadians = beginCloseRadians + closeWidth;
            final double beginFarRadians = i * radiansPerSpell - (quarterCircle + (radiansPerSpell / 2))
                    - (farWidth / 4);
            final double endFarRadians = beginCloseRadians + farWidth;

            final double x1m1 = Math.cos(beginCloseRadians) * ringInnerEdge;
            final double x2m1 = Math.cos(endCloseRadians) * ringInnerEdge;
            final double y1m1 = Math.sin(beginCloseRadians) * ringInnerEdge;
            final double y2m1 = Math.sin(endCloseRadians) * ringInnerEdge;

            final double x1m2 = Math.cos(beginFarRadians) * ringOuterEdge * 1.4;
            final double x2m2 = Math.cos(endFarRadians) * ringOuterEdge * 1.4;
            final double y1m2 = Math.sin(beginFarRadians) * ringOuterEdge * 1.4;
            final double y2m2 = Math.sin(endFarRadians) * ringOuterEdge * 1.4;

            Vector4f color = lineColor;
            buffer.vertex(centerX + x1m1, centerY + y1m1, 0).color(color.x, color.y, color.z, color.w).next();
            buffer.vertex(centerX + x2m1, centerY + y2m1, 0).color(color.x, color.y, color.z, color.w).next();
            buffer.vertex(centerX + x2m2, centerY + y2m2, 0).color(color.x, color.y, color.z, 0).next();
            buffer.vertex(centerX + x1m2, centerY + y1m2, 0).color(color.x, color.y, color.z, 0).next();
        }
    }

    private void drawTextBackground(MatrixStack matrices, double centerX, double centerY,
            double textYOffset, int textWidth, int textHeight) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        centerY = centerY + textYOffset;
        int heightMax = textHeight / 2;
        int heightMin = -heightMax;

        int widthMax = textWidth / 2;
        int widthMin = -widthMax;

        buffer.vertex(centerX + widthMin, centerY + heightMin, 0)
                .color(radialButtonColor.x, radialButtonColor.y, radialButtonColor.z, radialButtonColor.w).next();
        buffer.vertex(centerX + widthMin, centerY + heightMax, 0)
                .color(radialButtonColor.x, radialButtonColor.y, radialButtonColor.z, radialButtonColor.w).next();
        buffer.vertex(centerX + widthMax, centerY + heightMax, 0)
                .color(radialButtonColor.x, radialButtonColor.y, radialButtonColor.z, radialButtonColor.w).next();
        buffer.vertex(centerX + widthMax, centerY + heightMin, 0)
                .color(radialButtonColor.x, radialButtonColor.y, radialButtonColor.z, radialButtonColor.w).next();

        tessellator.draw();
        RenderSystem.disableBlend();
    }

    // 计算两点之间的角度
    private float getAngle(Vec2f a, Vec2f b) {
        return (float) Math.atan2(a.y - b.y, a.x - b.x);
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