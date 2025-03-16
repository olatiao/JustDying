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
    // 常量定义
    private static final String DEBUG_SKIP_MESSAGE = "跳过属性 {} 的上限增加物品: {}"; 
    private static final String ALREADY_REGISTERED_MESSAGE = "ID {} 已被注册";
    
    // 共享的物品设置，减少对象创建
    private static final FabricItemSettings SHARED_SETTINGS = new FabricItemSettings().maxCount(64);
            
    // 属性上限增加物品映射表
    private static final Map<Identifier, AttributeCapItem> ATTRIBUTE_CAP_ITEMS = new ConcurrentHashMap<>(16);
    
    // 缓存已注册的物品ID，防止重复注册
    private static final Map<String, Identifier> REGISTERED_ITEM_IDS = new ConcurrentHashMap<>(16);

    /**
     * 注册所有属性上限增加物品
     */
    public static void register() {
        // 判断是否启用属性上限增加物品功能
        JustDyingConfig config = JustDying.getConfig();
        if (!config.attributes.attributeCapItems.enableAttributeCapItems) {
            JustDying.LOGGER.info("属性上限增加物品功能已禁用");
            return;
        }
    
        // 清空注册缓存
        REGISTERED_ITEM_IDS.clear();
        ATTRIBUTE_CAP_ITEMS.clear();
        
        // 注册所有属性的上限增加物品
        long startTime = System.currentTimeMillis();
        int registeredCount = registerAttributeCapItems();
        long duration = System.currentTimeMillis() - startTime;
        
        // 只记录注册数量和时间，避免不必要的字符串连接
        JustDying.LOGGER.info("注册了 {} 个属性上限增加物品，耗时: {}ms", registeredCount, duration);
    }
    
    /**
     * 注册属性的上限增加物品
     * 
     * @return 注册的物品数量
     */
    private static int registerAttributeCapItems() {
        JustDyingConfig config = JustDying.getConfig();
        int registeredCount = 0;
        boolean isDebugEnabled = config.debug;
        
        // 检查所有属性
        for (Map.Entry<String, JustDyingConfig.AttributeConfig> entry : config.attributes.attributes.entrySet()) {
            String attributeName = entry.getKey();
            JustDyingConfig.AttributeConfig attributeConfig = entry.getValue();
            
            // 检查是否启用了该属性及其上限增加物品
            if (!attributeConfig.enabled || !attributeConfig.enableCapItem || attributeConfig.capItemId.isEmpty()) {
                if (isDebugEnabled) {
                    JustDying.LOGGER.debug(DEBUG_SKIP_MESSAGE, attributeName, "未启用或未配置物品ID");
                }
                continue;
            }
            
            // 处理物品ID
            String capItemIdStr = processItemId(attributeConfig.capItemId);
            
            // 检查是否已经注册过
            if (REGISTERED_ITEM_IDS.containsKey(capItemIdStr)) {
                JustDying.LOGGER.warn(DEBUG_SKIP_MESSAGE, attributeName, 
                    String.format(ALREADY_REGISTERED_MESSAGE, capItemIdStr));
                continue;
            }
            
            try {
                // 注册物品并添加到映射表
                Identifier capItemId = new Identifier(capItemIdStr);
                AttributeCapItem capItem = registerCapItem(capItemId, attributeName, attributeConfig.name);
                
                // 添加到映射表中
                ATTRIBUTE_CAP_ITEMS.put(capItemId, capItem);
                REGISTERED_ITEM_IDS.put(capItemIdStr, capItemId);
                
                if (isDebugEnabled) {
                    JustDying.LOGGER.debug("注册了属性 {} 的上限增加物品：{}", attributeName, capItemId);
                }
                
                registeredCount++;
            } catch (Exception e) {
                JustDying.LOGGER.error("注册属性 {} 的上限增加物品失败: {}", attributeName, e.getMessage());
            }
        }
        
        return registeredCount;
    }
    
    /**
     * 处理物品ID，确保格式正确
     */
    private static String processItemId(String itemId) {
        if (!itemId.contains(":")) {
            return JustDying.MOD_ID + ":" + itemId;
        }
        return itemId;
    }
    
    /**
     * 注册单个属性上限增加物品
     */
    private static AttributeCapItem registerCapItem(Identifier itemId, String attributeName, String displayName) {
        // 获取合适的格式化，基于属性类型
        Formatting formatting = getFormattingForAttributeType(attributeName);
        
        // 创建属性上限增加物品
        AttributeCapItem capItem = new AttributeCapItem(
            SHARED_SETTINGS,
            attributeName,
            displayName,
            formatting
        );
        
        // 注册物品
        Registry.register(Registries.ITEM, itemId, capItem);
        
        return capItem;
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