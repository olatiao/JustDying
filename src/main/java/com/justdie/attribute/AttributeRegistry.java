package com.justdie.attribute;

import com.justdie.JustDying;
import com.justdie.config.JustDyingConfig;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 属性注册表 - 改进版
 * 使用Minecraft原生的实体属性系统，提高兼容性和性能
 */
public class AttributeRegistry {
    // 存储所有已注册的属性
    private static final List<AttributePointDefinition> REGISTERED_ATTRIBUTES = new ArrayList<>();
    
    // 映射表：根据ID快速查找属性定义
    private static final Map<Identifier, AttributePointDefinition> ID_TO_DEFINITION = new HashMap<>();
    
    // 映射表：根据原版属性查找属性定义
    private static final Map<EntityAttribute, AttributePointDefinition> ATTRIBUTE_TO_DEFINITION = new HashMap<>();
    
    // 实例
    private static AttributeRegistry INSTANCE;
    
    /**
     * 获取注册表实例
     */
    public static AttributeRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AttributeRegistry();
        }
        return INSTANCE;
    }
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private AttributeRegistry() {
        // 私有构造函数
    }
    
    /**
     * 从配置加载所有属性
     * 
     * @param config 配置对象
     */
    public void loadFromConfig(JustDyingConfig config) {
        // 清除已有属性
        clearAttributes();
        
        // 检查是否启用属性系统
        if (!config.attributes.enableAttributeSystem) {
            JustDying.LOGGER.info("属性系统已在配置中禁用");
            return;
        }
        
        // 统计
        int totalAttributes = 0;
        int loadedAttributes = 0;
        int failedAttributes = 0;
        
        // 加载所有属性
        for (Map.Entry<String, JustDyingConfig.AttributeConfig> entry : 
                config.attributes.attributes.entrySet()) {
            
            totalAttributes++;
            
            if (!entry.getValue().enabled) {
                if (config.debug) {
                    JustDying.LOGGER.debug("跳过已禁用的属性: {}", entry.getKey());
                }
                continue;
            }
            
            try {
                String attributeName = entry.getKey();
                JustDyingConfig.AttributeConfig attrConfig = entry.getValue();
                
                // 创建并注册原版属性
                EntityAttribute attribute = registerVanillaAttribute(
                        attributeName,
                        attrConfig.minValue,
                        attrConfig.maxValue,
                        attrConfig.defaultValue
                );
                
                // 创建属性点定义
                AttributePointDefinition definition = new AttributePointDefinition(
                        attribute,
                        attrConfig.minValue,
                        attrConfig.maxValue,
                        attrConfig.defaultValue,
                        1 // 每点消耗1点属性点
                );
                
                // 注册属性点定义
                registerAttributePointDefinition(definition);
                
                loadedAttributes++;
                
                if (config.debug) {
                    JustDying.LOGGER.debug("已加载属性: {} (类型: {})", 
                            attributeName, attrConfig.isDefault() ? "默认" : "自定义");
                }
            } catch (Exception e) {
                failedAttributes++;
                JustDying.LOGGER.error("加载属性失败 {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        JustDying.LOGGER.info("已加载 {}/{} 个属性，失败 {} 个", 
                loadedAttributes, totalAttributes, failedAttributes);
    }
    
    /**
     * 注册原版属性
     */
    private EntityAttribute registerVanillaAttribute(String name, double min, double max, double defaultValue) {
        Identifier id = new Identifier(JustDying.MOD_ID, name);
        
        // 创建原版属性实例
        ClampedEntityAttribute attribute = new ClampedEntityAttribute(
                "attribute.justdying." + name, // 翻译键
                defaultValue,
                min,
                max
        );
        
        // 注册属性
        return Registry.register(Registries.ATTRIBUTE, id, attribute);
    }
    
    /**
     * 注册属性点定义
     */
    private void registerAttributePointDefinition(AttributePointDefinition definition) {
        Identifier id = Registries.ATTRIBUTE.getId(definition.getAttribute());
        
        // 添加到列表和映射
        REGISTERED_ATTRIBUTES.add(definition);
        ID_TO_DEFINITION.put(id, definition);
        ATTRIBUTE_TO_DEFINITION.put(definition.getAttribute(), definition);
    }
    
    /**
     * 清除所有已注册的属性
     */
    private void clearAttributes() {
        REGISTERED_ATTRIBUTES.clear();
        ID_TO_DEFINITION.clear();
        ATTRIBUTE_TO_DEFINITION.clear();
    }
    
    /**
     * 获取所有已注册的属性点定义
     */
    public List<AttributePointDefinition> getRegisteredAttributes() {
        return new ArrayList<>(REGISTERED_ATTRIBUTES);
    }
    
    /**
     * 通过ID获取属性点定义
     */
    public Optional<AttributePointDefinition> getDefinitionById(Identifier id) {
        return Optional.ofNullable(ID_TO_DEFINITION.get(id));
    }
    
    /**
     * 通过EntityAttribute获取属性点定义
     */
    public Optional<AttributePointDefinition> getDefinitionByAttribute(EntityAttribute attribute) {
        return Optional.ofNullable(ATTRIBUTE_TO_DEFINITION.get(attribute));
    }
    
    /**
     * 属性点定义类
     * 定义一个可分配点数的属性
     */
    public static class AttributePointDefinition {
        private final EntityAttribute attribute;
        private final double minValue;
        private final double maxValue;
        private final double defaultValue;
        private final int costPerPoint;
        
        public AttributePointDefinition(
                EntityAttribute attribute,
                double minValue,
                double maxValue,
                double defaultValue,
                int costPerPoint) {
            this.attribute = attribute;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.defaultValue = defaultValue;
            this.costPerPoint = costPerPoint;
        }
        
        public EntityAttribute getAttribute() {
            return attribute;
        }
        
        public double getMinValue() {
            return minValue;
        }
        
        public double getMaxValue() {
            return maxValue;
        }
        
        public double getDefaultValue() {
            return defaultValue;
        }
        
        public int getCostPerPoint() {
            return costPerPoint;
        }
    }
} 