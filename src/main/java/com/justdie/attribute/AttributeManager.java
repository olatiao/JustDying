package com.justdie.attribute;

import com.justdie.JustDying;
import com.justdie.config.JustDyingConfig;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性管理器，负责加载和管理所有属性
 */
public class AttributeManager {
    // 按ID存储的属性映射
    private static final Map<Identifier, JustDyingAttribute> ATTRIBUTES = new ConcurrentHashMap<>(32);
    
    // 按属性名称存储的属性映射，用于快速查找
    private static final Map<String, JustDyingAttribute> ATTRIBUTES_BY_NAME = new ConcurrentHashMap<>(32);
    
    /**
     * 从配置加载所有属性
     * 
     * @param config 配置
     */
    public static void loadFromConfig(JustDyingConfig config) {
        // 清除缓存
        clearCaches();
        
        // 检查配置是否有效
        if (!isValidConfiguration(config)) {
            JustDying.LOGGER.warn("属性配置无效，已禁用属性系统");
            return;
        }
        
        // 统计加载结果
        int totalAttributes = 0;
        int loadedAttributes = 0;
        int failedAttributes = 0;
        
        // 统一加载所有配置的属性
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
                JustDyingAttribute attribute = JustDyingAttribute.fromConfig(attributeName, entry.getValue());
                Identifier id = new Identifier(JustDying.MOD_ID, attributeName);
                registerAttribute(id, attribute);
                loadedAttributes++;
                
                if (config.debug) {
                    JustDying.LOGGER.debug("已加载属性: {} (类型: {})", 
                            attributeName, entry.getValue().isDefault() ? "默认" : "自定义");
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
     * 清除所有缓存
     */
    public static void clearCaches() {
        ATTRIBUTES.clear();
        ATTRIBUTES_BY_NAME.clear();
        JustDying.LOGGER.debug("已清除属性缓存");
    }
    
    /**
     * 检查配置是否有效
     * 
     * @param config 配置对象
     * @return 配置是否有效
     */
    private static boolean isValidConfiguration(JustDyingConfig config) {
        // 参数验证
        if (config == null) {
            JustDying.LOGGER.error("配置对象为空");
            return false;
        }
        
        // 检查是否启用了属性系统
        if (!config.attributes.enableAttributeSystem) {
            JustDying.LOGGER.info("属性系统已在配置中禁用");
            return false;
        }
        
        // 检查是否至少有一个有效的属性
        if (config.attributes.attributes == null || config.attributes.attributes.isEmpty()) {
            JustDying.LOGGER.error("配置中没有定义任何属性，已禁用属性系统");
            return false;
        }
        
        // 检查是否至少有一个启用的属性
        long enabledAttributeCount = config.attributes.attributes.values().stream()
                .filter(attr -> attr.enabled && attr.isValid())
                .count();
                
        if (enabledAttributeCount == 0) {
            JustDying.LOGGER.error("配置中没有启用且有效的属性，已禁用属性系统");
            return false;
        }
        
        JustDying.LOGGER.debug("配置有效，发现 {} 个启用的有效属性", enabledAttributeCount);
        return true;
    }
    
    /**
     * 注册一个属性
     * 
     * @param id 属性ID
     * @param attribute 属性
     */
    public static void registerAttribute(Identifier id, JustDyingAttribute attribute) {
        // 参数验证
        if (id == null || attribute == null) {
            JustDying.LOGGER.error("无法注册属性：ID或属性对象为空");
            return;
        }
        
        if (ATTRIBUTES.containsKey(id)) {
            JustDying.LOGGER.debug("属性 {} 已注册，将覆盖", id);
        }
        
        // 同时更新两个缓存
        ATTRIBUTES.put(id, attribute);
        ATTRIBUTES_BY_NAME.put(id.getPath(), attribute);
    }
    
    /**
     * 获取一个属性
     * 
     * @param id 属性ID
     * @return 属性，如果不存在则返回空
     */
    public static Optional<JustDyingAttribute> getAttribute(Identifier id) {
        if (id == null) {
            JustDying.LOGGER.error("尝试获取空ID的属性");
            return Optional.empty();
        }
        return Optional.ofNullable(ATTRIBUTES.get(id));
    }
    
    /**
     * 根据属性名称获取属性
     * 
     * @param name 属性名称
     * @return 属性，如果不存在则返回空
     */
    public static Optional<JustDyingAttribute> getAttributeByName(String name) {
        if (name == null || name.isEmpty()) {
            JustDying.LOGGER.error("尝试获取空名称的属性");
            return Optional.empty();
        }
        return Optional.ofNullable(ATTRIBUTES_BY_NAME.get(name));
    }
    
    /**
     * 获取所有属性
     * 
     * @return 所有属性的集合
     */
    public static Collection<JustDyingAttribute> getAllAttributes() {
        return ATTRIBUTES.values();
    }
    
    /**
     * 更新属性的最大值
     * 
     * @param attributeId 属性ID
     * @param newMaxValue 新的最大值
     * @return 是否更新成功
     */
    public static boolean updateAttributeMaxValue(Identifier attributeId, int newMaxValue) {
        // 参数验证
        if (attributeId == null) {
            JustDying.LOGGER.error("无法更新最大值: 属性ID为空");
            return false;
        }
        
        if (newMaxValue <= 0) {
            JustDying.LOGGER.error("无法更新最大值: 无效的最大值 {}", newMaxValue);
            return false;
        }
        
        Optional<JustDyingAttribute> attributeOpt = getAttribute(attributeId);
        if (attributeOpt.isEmpty()) {
            JustDying.LOGGER.error("无法更新最大值: 未找到属性 {}", attributeId);
            return false;
        }
        
        JustDyingAttribute attribute = attributeOpt.get();
        boolean success = attribute.setMaxValue(newMaxValue);
        
        if (success) {
            // 更新配置文件中的值
            try {
                String path = attributeId.getPath();
                JustDyingConfig config = JustDying.getConfig();
                
                if (config != null && config.attributes.attributes.containsKey(path)) {
                    config.attributes.attributes.get(path).maxValue = newMaxValue;
                    JustDying.LOGGER.info("已在配置中更新属性 {} 的最大值为 {}", attributeId, newMaxValue);
                }
            } catch (Exception e) {
                JustDying.LOGGER.error("更新属性 {} 的配置失败: {}", attributeId, e.getMessage());
                // 配置更新失败不影响内存中的值
            }
        }
        
        return success;
    }
    
    /**
     * 获取所有默认属性
     * 
     * @return 默认属性的集合
     */
    public static Collection<JustDyingAttribute> getDefaultAttributes() {
        return ATTRIBUTES.values().stream()
                .filter(attr -> {
                    // 通过ID检查是否是默认属性
                    Identifier id = attr.getId();
                    String path = id.getPath();
                    JustDyingConfig config = JustDying.getConfig();
                    return config != null 
                           && config.attributes.attributes.containsKey(path) 
                           && config.attributes.attributes.get(path).isDefault();
                })
                .toList();
    }
    
    /**
     * 获取所有自定义属性
     * 
     * @return 自定义属性的集合
     */
    public static Collection<JustDyingAttribute> getCustomAttributes() {
        return ATTRIBUTES.values().stream()
                .filter(attr -> {
                    // 通过ID检查是否是自定义属性
                    Identifier id = attr.getId();
                    String path = id.getPath();
                    JustDyingConfig config = JustDying.getConfig();
                    return config != null 
                           && config.attributes.attributes.containsKey(path) 
                           && !config.attributes.attributes.get(path).isDefault();
                })
                .toList();
    }
} 