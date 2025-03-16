package com.justdie.attribute;

import com.justdie.JustDying;
import com.justdie.config.JustDyingConfig;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.item.Items;

import java.util.Objects;
import java.util.UUID;

/**
 * 属性类，表示游戏中的一个可升级属性
 */
public class JustDyingAttribute {
    // 属性基本数值
    private int minValue;
    private int maxValue;
    private final int initialValue;
    private final float valueMultiplier;

    // 属性标识和显示信息
    private final Identifier id;
    private final Text name;
    private final Text description;
    private final Item iconItem;
    private final EntityAttribute vanillaAttribute;

    // 缓存值计算，避免重复计算
    private double lastCalculatedValue = 0;
    private int lastAttributeValue = -1;

    // 修饰符ID
    private final UUID modifierId;

    /**
     * 初始化属性
     * 
     * @param id               属性ID
     * @param name             属性名称
     * @param description      属性描述
     * @param iconItem         属性图标
     * @param minValue         最小值
     * @param maxValue         最大值
     * @param initialValue     初始值
     * @param valueMultiplier  属性值乘数（用于计算实际影响）
     * @param vanillaAttribute 关联的原版属性
     */
    public JustDyingAttribute(
            Identifier id,
            Text name,
            Text description,
            Item iconItem,
            int minValue,
            int maxValue,
            int initialValue,
            float valueMultiplier,
            EntityAttribute vanillaAttribute) {
        // 参数验证
        this.id = Objects.requireNonNull(id, "属性ID不能为空");
        this.name = name != null ? name : Text.of(id.getPath());
        this.description = description != null ? description : Text.empty();
        this.iconItem = iconItem != null ? iconItem : Items.STONE;

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.initialValue = initialValue;
        this.valueMultiplier = valueMultiplier;
        this.vanillaAttribute = vanillaAttribute;

        validateMaxValue(maxValue);

        // 为每个属性生成唯一的UUID作为修饰符ID
        this.modifierId = UUID.nameUUIDFromBytes(id.toString().getBytes());
    }

    /**
     * 验证最大值是否有效
     * 
     * @param newMaxValue 新的最大值
     */
    private void validateMaxValue(int newMaxValue) {
        if (newMaxValue < minValue + 1) {
            throw new IllegalStateException(
                    "属性 '%s' 的最大值为 %d，应该大于等于 %d".formatted(id, newMaxValue, minValue + 1));
        }
    }

    /**
     * 从配置创建属性实例
     * 
     * @param id               属性ID
     * @param config           属性配置
     * @param vanillaAttribute 原版属性
     * @return 属性实例
     */
    public static JustDyingAttribute fromConfig(
            Identifier id,
            JustDyingConfig.AttributeConfig config,
            EntityAttribute vanillaAttribute) {

        Item iconItem = Registries.ITEM.get(new Identifier(config.iconItem));

        // 添加详细日志
        if (JustDying.getConfig() != null && JustDying.getConfig().debug) {
            JustDying.LOGGER.debug("创建属性 {}: 最大值={}, 最小值={}, 初始值={}",
                    id, config.maxValue, config.minValue, config.initialValue);
        }

        return new JustDyingAttribute(
                id,
                Text.of(config.name),
                Text.of(config.description),
                iconItem,
                config.minValue,
                config.maxValue,
                config.initialValue,
                config.valueMultiplier,
                vanillaAttribute);
    }

    /**
     * 获取属性ID
     * 
     * @return 属性ID
     */
    public Identifier getId() {
        return id;
    }

    /**
     * 获取属性名称
     * 
     * @return 属性名称
     */
    public Text getName() {
        return name;
    }

    /**
     * 获取属性描述
     * 
     * @return 属性描述
     */
    public Text getDescription() {
        return description;
    }

    /**
     * 获取属性图标物品
     * 
     * @return 图标物品
     */
    public Item getIconItem() {
        return iconItem;
    }

    /**
     * 获取属性最小值
     * 
     * @return 最小值
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * 获取属性最大值
     * 
     * @return 最大值
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * 设置属性最大值
     * 
     * @param newMaxValue 新的最大值
     * @return 是否设置成功
     */
    public boolean setMaxValue(int newMaxValue) {
        try {
            // 验证新的最大值
            validateMaxValue(newMaxValue);
            this.maxValue = newMaxValue;
            return true;
        } catch (Exception e) {
            JustDying.LOGGER.error("设置属性 {} 的最大值失败: {}", this.id, e.getMessage());
            return false;
        }
    }

    /**
     * 获取属性初始值
     * 
     * @return 初始值
     */
    public int getInitialValue() {
        return initialValue;
    }

    /**
     * 获取属性值乘数
     * 
     * @return 值乘数
     */
    public float getValueMultiplier() {
        return valueMultiplier;
    }

    /**
     * 获取关联的原版属性
     * 
     * @return 原版属性，可能为null
     */
    public EntityAttribute getVanillaAttribute() {
        return vanillaAttribute;
    }

    /**
     * 计算属性对原版属性的实际影响值，使用缓存提高性能
     * 
     * @param attributeValue 当前属性值
     * @return 对原版属性的影响值
     */
    public double calculateAttributeBonus(int attributeValue) {
        // 使用缓存，避免重复计算
        if (attributeValue == lastAttributeValue) {
            return lastCalculatedValue;
        }

        double result = attributeValue * valueMultiplier;

        // 更新缓存
        lastAttributeValue = attributeValue;
        lastCalculatedValue = result;

        return result;
    }

    /**
     * 检查属性值是否在有效范围内
     * 
     * @param value 要检查的值
     * @return 是否有效
     */
    public boolean isValidValue(int value) {
        return value >= minValue && value <= maxValue;
    }

    /**
     * 限制值在属性的有效范围内
     * 
     * @param value 原始值
     * @return 限制后的值
     */
    public int clampValue(int value) {
        return Math.max(minValue, Math.min(value, maxValue));
    }

    @Override
    public String toString() {
        return "JustDyingAttribute{id=" + id + ", name=" + name.getString() +
                ", min=" + minValue + ", max=" + maxValue +
                ", initial=" + initialValue + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JustDyingAttribute that = (JustDyingAttribute) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
