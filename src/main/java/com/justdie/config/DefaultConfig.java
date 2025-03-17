package com.justdie.config;

import com.justdie.JustDying;

import java.util.Map;
import java.util.HashMap;

/**
 * 负责生成预设配置
 * 这个类提供了初始化模组配置的方法，确保首次运行时有合理的预设值
 * 所有默认值都集中在此处以便于维护
 */
public class DefaultConfig {
    // 配置版本常量
    private static final String DEFAULT_VERSION = "1.0.0";
    
    // 属性系统常量
    private static final int DEFAULT_INITIAL_POINTS = 5;
    private static final int DEFAULT_CAP_ITEM_INCREASE = 5;
    
    // 等级兑换常量
    private static final int DEFAULT_BASE_LEVEL = 5;
    private static final int DEFAULT_LEVEL_MULTIPLIER = 2;
    
    // 词缀系统常量
    private static final int DEFAULT_MAX_AFFIXES = 3;
    private static final int DEFAULT_AFFIX_DROP_CHANCE = 10; // 10%的掉落几率
    
    // 预设属性常量
    private static final String CONSTITUTION_ID = "constitution";
    private static final String STRENGTH_ID = "strength";
    private static final String DEFENSE_ID = "defense";
    private static final String SPEED_ID = "speed";
    private static final String LUCK_ID = "luck";
    
    // 错误消息
    private static final String ERROR_NULL_ATTRIBUTES = "添加预设属性失败：属性映射为null";
    private static final String ERROR_CREATING_ATTRIBUTE = "创建预设属性时出错: {}";
    
    /**
     * 创建预设配置
     * 此方法初始化完整的配置对象，为所有设置提供合理的预设值
     * 
     * @return 包含预设值的配置对象
     */
    public static JustDyingConfig createDefaultConfig() {
        JustDying.LOGGER.info("创建预设配置...");
        JustDyingConfig config = new JustDyingConfig();
        
        // 设置配置版本
        config.version = DEFAULT_VERSION;
        
        // 启用调试模式（可根据需要修改）
        config.debug = false;
        
        // 初始化各配置部分
        try {
            initAttributes(config);
            initLevelExchange(config);
            initAffixes(config);
            
            JustDying.LOGGER.info("预设配置创建完成");
        } catch (Exception e) {
            JustDying.LOGGER.error("创建预设配置时出错: {}", e.getMessage());
            // 确保即使出错也返回有效配置
            if (config.attributes == null) {
                config.attributes = new JustDyingConfig.AttributesConfig();
            }
            if (config.levelExchange == null) {
                config.levelExchange = new JustDyingConfig.LevelExchangeConfig();
            }
            if (config.affixes == null) {
                config.affixes = new JustDyingConfig.AffixConfig();
            }
        }
        
        return config;
    }
    
    /**
     * 初始化属性相关配置
     * 
     * @param config 配置对象
     */
    private static void initAttributes(JustDyingConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置对象不能为null");
        }
        
        // 确保attributes对象存在
        if (config.attributes == null) {
            config.attributes = new JustDyingConfig.AttributesConfig();
        }
        
        // 确保attributes.attributes映射存在
        if (config.attributes.attributes == null) {
            config.attributes.attributes = new HashMap<>();
        }
        
        // 初始化属性配置的预设值
        config.attributes.enableAttributeSystem = true;
        config.attributes.initialAttributePoints = DEFAULT_INITIAL_POINTS;
        config.attributes.showDecreaseButtons = true;
        
        // 确保attributeCapItems对象存在
        if (config.attributes.attributeCapItems == null) {
            config.attributes.attributeCapItems = new JustDyingConfig.AttributeCapItemsConfig();
        }
        
        // 初始化属性上限增加物品配置
        config.attributes.attributeCapItems.enableAttributeCapItems = true;
        config.attributes.attributeCapItems.increaseAmountPerUse = DEFAULT_CAP_ITEM_INCREASE;
        
        // 添加预设属性
        addPresetAttributes(config.attributes.attributes);
    }
    
    /**
     * 初始化等级兑换相关配置
     * 
     * @param config 配置对象
     */
    private static void initLevelExchange(JustDyingConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置对象不能为null");
        }
        
        // 确保levelExchange对象存在
        if (config.levelExchange == null) {
            config.levelExchange = new JustDyingConfig.LevelExchangeConfig();
        }
        
        config.levelExchange.enableLevelExchange = true;
        config.levelExchange.baseLevel = DEFAULT_BASE_LEVEL;
        config.levelExchange.levelMultiplier = DEFAULT_LEVEL_MULTIPLIER;
    }
    
    /**
     * 初始化词缀系统相关配置
     * 
     * @param config 配置对象
     */
    private static void initAffixes(JustDyingConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置对象不能为null");
        }
        
        // 确保affixes对象存在
        if (config.affixes == null) {
            config.affixes = new JustDyingConfig.AffixConfig();
        }
        
        config.affixes.enableAffixes = false; // 默认关闭词缀系统
        config.affixes.maxAffixesPerItem = DEFAULT_MAX_AFFIXES;
        config.affixes.affixDropChance = DEFAULT_AFFIX_DROP_CHANCE; // 10%的掉落几率
        config.affixes.enableAffixCommands = true;
        config.affixes.showAffixTooltips = true;
    }
    
    /**
     * 添加预设属性到配置
     * 
     * @param attributes 属性映射
     */
    private static void addPresetAttributes(Map<String, JustDyingConfig.AttributeConfig> attributes) {
        if (attributes == null) {
            JustDying.LOGGER.error(ERROR_NULL_ATTRIBUTES);
            return;
        }
        
        JustDying.LOGGER.info("开始添加预设属性到配置...");
        int attributesAdded = 0;
        
        try {
            // 添加体质属性
            if (addAttribute(attributes, 
                CONSTITUTION_ID, "体质", "影响生命值和防御力",
                "minecraft:apple", "minecraft:generic.max_health",
                0, 100, 0, 0.5f, "justdying:constitution_cap_item")) {
                attributesAdded++;
            }
            
            // 添加力量属性
            if (addAttribute(attributes, 
                STRENGTH_ID, "力量", "影响攻击力和负重",
                "minecraft:iron_sword", "minecraft:generic.attack_damage",
                0, 100, 0, 0.5f, "justdying:strength_cap_item")) {
                attributesAdded++;
            }
                
            // 添加防御属性
            if (addAttribute(attributes, 
                DEFENSE_ID, "防御", "影响防御力和生命值",
                "minecraft:iron_chestplate", "minecraft:generic.armor",
                0, 100, 0, 0.5f, "justdying:defense_cap_item")) {
                attributesAdded++;
            }
                
            // 添加速度属性
            if (addAttribute(attributes, 
                SPEED_ID, "速度", "影响移动速度",
                "minecraft:feather", "minecraft:generic.movement_speed",
                0, 100, 0, 0.01f, "justdying:speed_cap_item")) {
                attributesAdded++;
            }
                
            // 添加幸运属性
            if (addAttribute(attributes, 
                LUCK_ID, "幸运", "影响掉落和运气",
                "minecraft:emerald", "minecraft:generic.luck",
                0, 100, 0, 0.1f, "justdying:luck_cap_item")) {
                attributesAdded++;
            }
            
            JustDying.LOGGER.info("成功添加 {} 个预设属性", attributesAdded);
            if (attributes.size() > 0) {
                JustDying.LOGGER.info("预设属性列表: {}", String.join(", ", attributes.keySet()));
            }
        } catch (Exception e) {
            JustDying.LOGGER.error(ERROR_CREATING_ATTRIBUTE, e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 添加单个属性到配置
     * 
     * @param attributes 属性映射
     * @param id 属性ID
     * @param name 属性名称
     * @param description 属性描述
     * @param iconItem 图标物品
     * @param vanillaAttribute 原版属性
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param initialValue 初始值
     * @param valueMultiplier 值乘数
     * @param capItemId 上限物品ID
     * @return 是否成功添加属性
     */
    private static boolean addAttribute(
            Map<String, JustDyingConfig.AttributeConfig> attributes,
            String id, String name, String description, 
            String iconItem, String vanillaAttribute,
            int minValue, int maxValue, int initialValue, 
            float valueMultiplier, String capItemId) {
        
        if (id == null || id.isEmpty()) {
            JustDying.LOGGER.warn("跳过添加无效ID的属性");
            return false;
        }
        
        try {
            JustDyingConfig.AttributeConfig attribute = new JustDyingConfig.AttributeConfig(
                name, description, iconItem, vanillaAttribute,
                minValue, maxValue, initialValue, valueMultiplier,
                true, capItemId
            );
            
            attributes.put(id, attribute);
            JustDying.LOGGER.debug("添加属性: {}", id);
            return true;
        } catch (Exception e) {
            JustDying.LOGGER.error("创建属性 {} 时发生错误: {}", id, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 