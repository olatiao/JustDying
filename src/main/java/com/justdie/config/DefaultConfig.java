package com.justdie.config;

import com.justdie.JustDying;

import java.util.Map;

/**
 * 负责生成默认配置
 * 这个类提供了初始化模组配置的方法，确保首次运行时有合理的默认值
 */
public class DefaultConfig {
    // 默认配置版本
    private static final String DEFAULT_VERSION = "1.0.0";
    
    /**
     * 创建默认配置
     * 此方法初始化完整的配置对象，为所有设置提供合理的默认值
     * 
     * @return 包含默认值的配置对象
     */
    public static JustDyingConfig createDefaultConfig() {
        JustDying.LOGGER.info("创建默认配置...");
        JustDyingConfig config = new JustDyingConfig();
        
        // 设置配置版本
        config.version = DEFAULT_VERSION;
        
        // 初始化各配置部分
        initAttributes(config);
        initLevelExchange(config);
        initAffixes(config);
        
        return config;
    }
    
    /**
     * 初始化属性相关配置
     * 
     * @param config 配置对象
     */
    private static void initAttributes(JustDyingConfig config) {
        // 初始化属性配置的默认值
        config.attributes.enableAttributeSystem = true;
        config.attributes.initialAttributePoints = 5;
        config.attributes.showDecreaseButtons = true;
        
        // 初始化属性上限增加物品配置
        config.attributes.attributeCapItems.enableAttributeCapItems = true;
        config.attributes.attributeCapItems.increaseAmountPerUse = 5;
        
        // 添加预设属性
        addDefaultAttributes(config.attributes.attributes);
    }
    
    /**
     * 初始化等级兑换相关配置
     * 
     * @param config 配置对象
     */
    private static void initLevelExchange(JustDyingConfig config) {
        config.levelExchange.enableLevelExchange = true;
        config.levelExchange.baseLevel = 5;
        config.levelExchange.levelMultiplier = 2;
    }
    
    /**
     * 初始化词缀系统相关配置
     * 
     * @param config 配置对象
     */
    private static void initAffixes(JustDyingConfig config) {
        config.affixes.enableAffixes = false; // 默认关闭词缀系统
        config.affixes.maxAffixesPerItem = 3;
        config.affixes.affixDropChance = 10; // 10%的掉落几率
        config.affixes.enableAffixCommands = true;
        config.affixes.showAffixTooltips = true;
    }
    
    /**
     * 添加默认属性到配置
     * 
     * @param attributes 属性映射
     */
    private static void addDefaultAttributes(Map<String, JustDyingConfig.AttributeConfig> attributes) {
        if (attributes == null) {
            JustDying.LOGGER.error("添加默认属性失败：属性映射为null");
            return;
        }
        
        try {
            // 使用通用方法添加预设属性，减少代码重复
            addDefaultAttribute(attributes, 
                "constitution", "体质", "增加生命值上限",
                "minecraft:apple", "minecraft:generic.max_health",
                0, 100, 0, 0.5f, "justdying:constitution_cap_item");
            
            addDefaultAttribute(attributes, 
                "strength", "力量", "增加攻击伤害",
                "minecraft:iron_sword", "minecraft:generic.attack_damage",
                0, 100, 0, 0.5f, "justdying:strength_cap_item");
            
            addDefaultAttribute(attributes, 
                "defense", "防御", "增加护甲值",
                "minecraft:iron_chestplate", "minecraft:generic.armor",
                0, 100, 0, 0.5f, "justdying:defense_cap_item");
            
            addDefaultAttribute(attributes, 
                "speed", "速度", "增加移动速度",
                "minecraft:feather", "minecraft:generic.movement_speed",
                0, 100, 0, 0.005f, "justdying:speed_cap_item");
            
            addDefaultAttribute(attributes, 
                "luck", "幸运", "增加幸运值",
                "minecraft:rabbit_foot", "minecraft:generic.luck",
                0, 100, 0, 0.1f, "justdying:luck_cap_item");
            
            JustDying.LOGGER.info("已添加 {} 个预设属性", attributes.size());
        } catch (Exception e) {
            JustDying.LOGGER.error("添加默认属性时发生错误: {}", e.getMessage());
        }
    }
    
    /**
     * 添加单个默认属性
     * 
     * @param attributes 属性映射
     * @param id 属性ID
     * @param name 属性名称
     * @param description 属性描述
     * @param iconItem 图标物品
     * @param vanillaAttribute 原版属性
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param defaultValue 默认值
     * @param valueMultiplier 数值乘数
     * @param capItemId 上限增加物品ID
     */
    private static void addDefaultAttribute(
            Map<String, JustDyingConfig.AttributeConfig> attributes,
            String id, String name, String description, 
            String iconItem, String vanillaAttribute,
            int minValue, int maxValue, int defaultValue, 
            float valueMultiplier, String capItemId) {
        
        if (id == null || id.isEmpty()) {
            JustDying.LOGGER.warn("跳过添加无效ID的默认属性");
            return;
        }
        
        try {
            JustDyingConfig.AttributeConfig attribute = new JustDyingConfig.AttributeConfig(
                name, description, iconItem, vanillaAttribute,
                minValue, maxValue, defaultValue, valueMultiplier,
                true, true, capItemId
            );
            
            attributes.put(id, attribute);
            
            // 如果需要调试，可以添加日志
            if (JustDying.getConfig() != null && JustDying.getConfig().debug) {
                JustDying.LOGGER.debug("添加默认属性: {}", id);
            }
        } catch (Exception e) {
            JustDying.LOGGER.error("创建默认属性 {} 时发生错误: {}", id, e.getMessage());
        }
    }
} 