package com.justdie.item;

import com.justdie.JustDying;
import com.justdie.config.JustDyingConfig;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性上限增加物品注册
 * 负责创建和注册用于增加属性上限的物品
 */
public class AttributeCapItems {
    // 预先创建标识符，避免重复创建
    private static final Identifier CONSTITUTION_ID = new Identifier(JustDying.MOD_ID, "constitution_cap_item");
    private static final Identifier STRENGTH_ID = new Identifier(JustDying.MOD_ID, "strength_cap_item");
    private static final Identifier DEFENSE_ID = new Identifier(JustDying.MOD_ID, "defense_cap_item");
    private static final Identifier SPEED_ID = new Identifier(JustDying.MOD_ID, "speed_cap_item");
    private static final Identifier LUCK_ID = new Identifier(JustDying.MOD_ID, "luck_cap_item");
    
    // 共享的物品设置，减少对象创建
    private static final FabricItemSettings SHARED_SETTINGS = new FabricItemSettings().maxCount(64);
    
    // 体质上限增加物品
    public static final AttributeCapItem CONSTITUTION_CAP_ITEM = new AttributeCapItem(
            SHARED_SETTINGS,
            "constitution",
            "体质",
            Formatting.RED);

    // 力量上限增加物品
    public static final AttributeCapItem STRENGTH_CAP_ITEM = new AttributeCapItem(
            SHARED_SETTINGS,
            "strength",
            "力量",
            Formatting.DARK_RED);

    // 防御上限增加物品
    public static final AttributeCapItem DEFENSE_CAP_ITEM = new AttributeCapItem(
            SHARED_SETTINGS,
            "defense",
            "防御",
            Formatting.BLUE);

    // 速度上限增加物品
    public static final AttributeCapItem SPEED_CAP_ITEM = new AttributeCapItem(
            SHARED_SETTINGS,
            "speed",
            "速度",
            Formatting.AQUA);

    // 幸运上限增加物品
    public static final AttributeCapItem LUCK_CAP_ITEM = new AttributeCapItem(
            SHARED_SETTINGS,
            "luck",
            "幸运",
            Formatting.GOLD);
            
    // 属性上限增加物品映射表，用于批量注册，使用静态初始化避免重复创建
    private static final Map<Identifier, AttributeCapItem> ATTRIBUTE_CAP_ITEMS = new ConcurrentHashMap<>(16);
    
    // 缓存已注册的物品ID，防止重复注册
    private static final Map<String, Identifier> REGISTERED_ITEM_IDS = new ConcurrentHashMap<>(16);
    
    // 使用静态初始化块，确保映射表在类加载时就初始化
    static {
        ATTRIBUTE_CAP_ITEMS.put(CONSTITUTION_ID, CONSTITUTION_CAP_ITEM);
        ATTRIBUTE_CAP_ITEMS.put(STRENGTH_ID, STRENGTH_CAP_ITEM);
        ATTRIBUTE_CAP_ITEMS.put(DEFENSE_ID, DEFENSE_CAP_ITEM);
        ATTRIBUTE_CAP_ITEMS.put(SPEED_ID, SPEED_CAP_ITEM);
        ATTRIBUTE_CAP_ITEMS.put(LUCK_ID, LUCK_CAP_ITEM);
    }

    /**
     * 注册所有属性上限增加物品
     */
    public static void register() {
        long startTime = System.currentTimeMillis();
        
        // 判断是否启用属性上限增加物品功能
        if (!JustDying.getConfig().attributes.attributeCapItems.enableAttributeCapItems) {
            JustDying.LOGGER.info("属性上限增加物品功能已禁用");
            return;
        }
    
        // 清空注册缓存
        REGISTERED_ITEM_IDS.clear();
        
        // 统计注册结果
        int defaultItemsRegistered = 0;
        int customItemsRegistered = 0;
        
        // 注册默认属性的上限增加物品
        for (Map.Entry<Identifier, AttributeCapItem> entry : ATTRIBUTE_CAP_ITEMS.entrySet()) {
            try {
                Registry.register(Registries.ITEM, entry.getKey(), entry.getValue());
                // 记录已注册的ID
                REGISTERED_ITEM_IDS.put(entry.getKey().toString(), entry.getKey());
                defaultItemsRegistered++;
                
                if (JustDying.getConfig().debug) {
                    JustDying.LOGGER.debug("注册了默认属性上限增加物品: {}", entry.getKey());
                }
            } catch (Exception e) {
                JustDying.LOGGER.error("注册默认属性上限增加物品失败: {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        // 注册自定义属性的上限增加物品
        customItemsRegistered = registerCustomAttributeCapItems();

        long endTime = System.currentTimeMillis();
        
        JustDying.LOGGER.info("注册了 {} 个属性上限增加物品 (默认: {}, 自定义: {}) 耗时: {}ms", 
            defaultItemsRegistered + customItemsRegistered, 
            defaultItemsRegistered, 
            customItemsRegistered,
            endTime - startTime);
    }
    
    /**
     * 注册自定义属性的上限增加物品
     * 
     * @return 成功注册的物品数量
     */
    private static int registerCustomAttributeCapItems() {
        JustDyingConfig config = JustDying.getConfig();
        int registeredCount = 0;
        
        // 检查所有属性
        for (Map.Entry<String, JustDyingConfig.AttributeConfig> entry : config.attributes.attributes.entrySet()) {
            JustDyingConfig.AttributeConfig attributeConfig = entry.getValue();
            
            // 跳过默认属性（这些已经在静态初始化中处理）
            if (attributeConfig.isDefault()) {
                continue;
            }
            
            // 检查是否启用了该属性及其上限增加物品
            if (!attributeConfig.enabled || !attributeConfig.enableCapItem || attributeConfig.capItemId.isEmpty()) {
                if (config.debug) {
                    JustDying.LOGGER.debug("跳过自定义属性 {} 的上限增加物品: 未启用或未配置物品ID", entry.getKey());
                }
                continue;
            }
            
            try {
                // 创建物品ID
                String attributeName = entry.getKey();
                String capItemIdStr = attributeConfig.capItemId;
                
                // 确保物品ID是合法的
                if (!capItemIdStr.contains(":")) {
                    capItemIdStr = JustDying.MOD_ID + ":" + capItemIdStr;
                }
                
                // 检查是否已经注册过
                if (REGISTERED_ITEM_IDS.containsKey(capItemIdStr)) {
                    JustDying.LOGGER.warn("跳过自定义属性 {} 的上限增加物品: ID {} 已被注册", 
                        attributeName, capItemIdStr);
                    continue;
                }
                
                Identifier capItemId = new Identifier(capItemIdStr);
                
                // 获取合适的格式化，基于属性类型
                Formatting formatting = getFormattingForAttributeType(attributeName);
                
                // 创建属性上限增加物品
                AttributeCapItem capItem = new AttributeCapItem(
                    SHARED_SETTINGS,
                    attributeName,
                    attributeConfig.name,
                    formatting
                );
                
                // 注册物品
                Registry.register(Registries.ITEM, capItemId, capItem);
                
                // 添加到映射表中
                ATTRIBUTE_CAP_ITEMS.put(capItemId, capItem);
                REGISTERED_ITEM_IDS.put(capItemIdStr, capItemId);
                
                JustDying.LOGGER.debug("注册了自定义属性 {} 的上限增加物品：{}", attributeName, capItemId);
                registeredCount++;
            } catch (Exception e) {
                JustDying.LOGGER.error("注册自定义属性 {} 的上限增加物品失败: {}", entry.getKey(), e.getMessage());
            }
        }
        
        return registeredCount;
    }
    
    /**
     * 根据属性类型获取适合的格式化
     * 
     * @param attributeName 属性名称
     * @return 格式化颜色
     */
    private static Formatting getFormattingForAttributeType(String attributeName) {
        // 基于属性名称选择合适的颜色
        return switch (attributeName.toLowerCase()) {
            case "health", "constitution", "vitality" -> Formatting.RED;
            case "strength", "power", "damage" -> Formatting.DARK_RED;
            case "defense", "armor", "protection" -> Formatting.BLUE;
            case "speed", "agility", "movement" -> Formatting.AQUA;
            case "luck", "fortune" -> Formatting.GOLD;
            case "intelligence", "wisdom", "mana" -> Formatting.DARK_PURPLE;
            default -> Formatting.WHITE;
        };
    }
    
    /**
     * 获取所有注册的属性上限增加物品
     * 
     * @return 物品映射
     */
    public static Map<Identifier, AttributeCapItem> getRegisteredItems() {
        return ATTRIBUTE_CAP_ITEMS;
    }
} 