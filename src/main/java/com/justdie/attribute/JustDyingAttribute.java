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

/**
 * 属性类，表示游戏中的一个可升级属性
 */
public class JustDyingAttribute {
    // 属性基本数值
    private int minValue;
    private int maxValue;
    private final int defaultValue;
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

    /**
     * 初始化属性
     * 
     * @param id              属性ID
     * @param name            属性名称
     * @param description     属性描述
     * @param iconItem        属性图标
     * @param minValue        最小值
     * @param maxValue        最大值
     * @param defaultValue    默认值
     * @param valueMultiplier 属性值乘数（用于计算实际影响）
     * @param vanillaAttribute 关联的原版属性
     */
    public JustDyingAttribute(
            Identifier id,
            Text name,
            Text description,
            Item iconItem,
            int minValue,
            int maxValue,
            int defaultValue,
            float valueMultiplier,
            EntityAttribute vanillaAttribute) {
        // 参数验证
        this.id = Objects.requireNonNull(id, "属性ID不能为空");
        this.name = name != null ? name : Text.of(id.getPath());
        this.description = description != null ? description : Text.empty();
        this.iconItem = iconItem != null ? iconItem : Items.STONE;
        
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.valueMultiplier = valueMultiplier;
        this.vanillaAttribute = vanillaAttribute;

        validateMaxValue(maxValue);
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
     * @param name 属性名称
     * @param config 属性配置
     * @return 创建的属性实例
     * @throws IllegalArgumentException 如果配置无效
     */
    public static JustDyingAttribute fromConfig(String name, JustDyingConfig.AttributeConfig config) {
        // 验证配置
        if (config == null) {
            throw new IllegalArgumentException("无效的属性配置");
        }
        
        if (!config.isValid()) {
            throw new IllegalArgumentException("属性配置验证失败: 名称为空或最大值小于最小值");
        }

        // 创建属性ID
        Identifier id = new Identifier(JustDying.MOD_ID, name);
        
        // 直接使用配置中的值创建属性名称和描述
        Text attributeName = Text.of(config.name);
        Text description = Text.of(config.description);
        
        // 获取物品ID对应的Minecraft物品
        Item iconItem = null;
        if (!config.iconItem.isEmpty()) {
            try {
                Identifier iconId = new Identifier(config.iconItem);
                iconItem = Registries.ITEM.get(iconId);
                
                if (iconItem == Items.AIR && !config.iconItem.equals("minecraft:air")) {
                    JustDying.LOGGER.warn("未找到物品: {}, 将使用默认图标", config.iconItem);
                    iconItem = Items.STONE;
                }
            } catch (Exception e) {
                JustDying.LOGGER.error("无效的物品ID: {}", config.iconItem, e);
                iconItem = Items.STONE;
            }
        } else {
            iconItem = Items.STONE;
        }
        
        // 尝试获取原版属性
        EntityAttribute vanillaAttr = null;
        if (config.vanillaAttribute != null && !config.vanillaAttribute.isEmpty()) {
            try {
                Identifier attrId = new Identifier(config.vanillaAttribute);
                vanillaAttr = Registries.ATTRIBUTE.get(attrId);
                
                if (vanillaAttr == null) {
                    JustDying.LOGGER.warn("未找到原版属性: {}", config.vanillaAttribute);
                }
            } catch (Exception e) {
                JustDying.LOGGER.error("获取原版属性失败: {}: {}", id, e.getMessage());
            }
        }
        
        // 创建属性实例
        JustDyingAttribute attribute = new JustDyingAttribute(
                id, attributeName, description, iconItem, 
                config.minValue, config.maxValue, config.defaultValue, 
                config.valueMultiplier, vanillaAttr);
                
        // 添加详细日志
        if (JustDying.getConfig() != null && JustDying.getConfig().debug) {
            JustDying.LOGGER.debug("从配置创建属性 {}: 类型={}, 最大值={}, 最小值={}, 默认值={}",
                    id, config.isDefault() ? "默认" : "自定义", config.maxValue, config.minValue, config.defaultValue);
        }
        
        return attribute;
    }

    /**
     * 获取属性ID
     * @return 属性ID
     */
    public Identifier getId() {
        return id;
    }

    /**
     * 获取属性名称
     * @return 属性名称
     */
    public Text getName() {
        return name;
    }

    /**
     * 获取属性描述
     * @return 属性描述
     */
    public Text getDescription() {
        return description;
    }

    /**
     * 获取属性图标物品
     * @return 图标物品
     */
    public Item getIconItem() {
        return iconItem;
    }

    /**
     * 获取属性最小值
     * @return 最小值
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * 获取属性最大值
     * @return 最大值
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * 设置属性的最大值
     * 
     * @param newMaxValue 新的最大值
     * @return 是否设置成功
     */
    public boolean setMaxValue(int newMaxValue) {
        try {
            validateMaxValue(newMaxValue);
            this.maxValue = newMaxValue;
            
            // 重置缓存值，确保下次计算使用新的最大值
            this.lastAttributeValue = -1;
            
            JustDying.LOGGER.info("已更新属性 {} 的最大值为 {}", id, newMaxValue);
            return true;
        } catch (Exception e) {
            JustDying.LOGGER.error("更新属性 {} 的最大值失败: {}", id, e.getMessage());
            return false;
        }
    }

    /**
     * 获取属性默认值
     * @return 默认值
     */
    public int getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * 获取属性值乘数
     * @return 值乘数
     */
    public float getValueMultiplier() {
        return valueMultiplier;
    }
    
    /**
     * 获取关联的原版属性
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
               ", default=" + defaultValue + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JustDyingAttribute that = (JustDyingAttribute) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
