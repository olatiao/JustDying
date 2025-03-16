package com.justdie.attribute;

import com.justdie.JustDying;
import com.justdie.config.JustDyingConfig;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 属性管理器
 * 负责加载、注册和管理所有自定义属性，提供属性的查询和缓存功能
 */
public class AttributeManager {
    // 常量定义
    private static final int INITIAL_CAPACITY = 32;
    private static final String LOG_ATTRIBUTE_LOADED = "已加载属性: {}";
    private static final String LOG_ATTRIBUTE_DISABLED = "跳过已禁用的属性: {}";
    private static final String LOG_ATTRIBUTE_LOAD_FAILED = "加载属性失败 {}: {}";
    private static final String LOG_ATTRIBUTES_SUMMARY = "已加载 {}/{} 个属性，失败 {} 个";
    private static final String LOG_CACHE_CLEARED = "已清除属性缓存";
    private static final String LOG_CONFIG_INVALID = "配置对象为空";
    private static final String LOG_SYSTEM_DISABLED = "属性系统已在配置中禁用";
    private static final String LOG_NO_ATTRIBUTES = "配置中没有定义任何属性，已禁用属性系统";
    private static final String LOG_NO_ENABLED_ATTRIBUTES = "配置中没有启用且有效的属性，已禁用属性系统";
    private static final String LOG_CONFIG_VALID = "配置有效，发现 {} 个启用的有效属性";
    private static final String LOG_REGISTER_ERROR = "无法注册属性：ID或属性对象为空";
    private static final String LOG_ATTRIBUTE_EXISTS = "属性 {} 已注册，将覆盖";
    private static final String LOG_ATTRIBUTE_NOT_FOUND = "未找到属性：{}";
    
    // 按ID存储的属性映射
    private static final Map<Identifier, JustDyingAttribute> ATTRIBUTES = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    
    // 按属性名称存储的属性映射，用于快速查找
    private static final Map<String, JustDyingAttribute> ATTRIBUTES_BY_NAME = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    
    /**
     * 从配置加载所有属性
     * 
     * @param config 配置对象
     */
    public static void loadFromConfig(JustDyingConfig config) {
        // 清除缓存以防旧数据影响
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
        
        // 加载所有属性
        for (Map.Entry<String, JustDyingConfig.AttributeConfig> entry : 
                config.attributes.attributes.entrySet()) {
            
            totalAttributes++;
            String attributeName = entry.getKey();
            JustDyingConfig.AttributeConfig attributeConfig = entry.getValue();
            
            // 跳过已禁用的属性
            if (!attributeConfig.enabled) {
                if (config.debug) {
                    JustDying.LOGGER.debug(LOG_ATTRIBUTE_DISABLED, attributeName);
                }
                continue;
            }
            
            try {
                // 创建属性ID
                Identifier id = new Identifier(JustDying.MOD_ID, attributeName);
                
                // 查找匹配的原版属性
                EntityAttribute vanillaAttribute = null;
                if (attributeConfig.vanillaAttribute != null && !attributeConfig.vanillaAttribute.isEmpty()) {
                    vanillaAttribute = Registries.ATTRIBUTE.get(new Identifier(attributeConfig.vanillaAttribute));
                }
                
                // 从配置创建属性并注册
                JustDyingAttribute attribute = JustDyingAttribute.fromConfig(id, attributeConfig, vanillaAttribute);
                registerAttribute(id, attribute);
                loadedAttributes++;
                
                if (config.debug) {
                    JustDying.LOGGER.debug(LOG_ATTRIBUTE_LOADED, attributeName);
                }
            } catch (Exception e) {
                failedAttributes++;
                JustDying.LOGGER.error(LOG_ATTRIBUTE_LOAD_FAILED, attributeName, e.getMessage());
            }
        }
        
        JustDying.LOGGER.info(LOG_ATTRIBUTES_SUMMARY, 
                loadedAttributes, totalAttributes, failedAttributes);
    }
    
    /**
     * 清除所有属性缓存
     */
    public static void clearCaches() {
        ATTRIBUTES.clear();
        ATTRIBUTES_BY_NAME.clear();
        JustDying.LOGGER.debug(LOG_CACHE_CLEARED);
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
            JustDying.LOGGER.error(LOG_CONFIG_INVALID);
            return false;
        }
        
        // 检查属性系统是否启用
        if (config.attributes == null || !config.attributes.enableAttributeSystem) {
            JustDying.LOGGER.info(LOG_SYSTEM_DISABLED);
            return false;
        }
        
        // 检查是否至少有一个有效的属性
        if (config.attributes.attributes == null || config.attributes.attributes.isEmpty()) {
            JustDying.LOGGER.error(LOG_NO_ATTRIBUTES);
            return false;
        }
        
        // 检查是否至少有一个启用的属性
        long enabledAttributeCount = config.attributes.attributes.values().stream()
                .filter(attr -> attr != null && attr.enabled && attr.isValid())
                .count();
                
        if (enabledAttributeCount == 0) {
            JustDying.LOGGER.error(LOG_NO_ENABLED_ATTRIBUTES);
            return false;
        }
        
        JustDying.LOGGER.debug(LOG_CONFIG_VALID, enabledAttributeCount);
        return true;
    }
    
    /**
     * 注册一个属性
     * 
     * @param id 属性ID
     * @param attribute 属性对象
     */
    public static void registerAttribute(Identifier id, JustDyingAttribute attribute) {
        // 参数验证
        if (id == null || attribute == null) {
            JustDying.LOGGER.error(LOG_REGISTER_ERROR);
            return;
        }
        
        // 记录冲突属性
        if (ATTRIBUTES.containsKey(id)) {
            JustDying.LOGGER.debug(LOG_ATTRIBUTE_EXISTS, id);
        }
        
        // 注册到两个映射表中
        ATTRIBUTES.put(id, attribute);
        ATTRIBUTES_BY_NAME.put(id.getPath(), attribute);
    }
    
    /**
     * 通过ID获取属性
     * 
     * @param id 属性ID
     * @return 属性（可能为空）
     */
    public static Optional<JustDyingAttribute> getAttribute(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        
        JustDyingAttribute attribute = ATTRIBUTES.get(id);
        
        if (attribute == null) {
            JustDying.LOGGER.debug(LOG_ATTRIBUTE_NOT_FOUND, id);
        }
        
        return Optional.ofNullable(attribute);
    }
    
    /**
     * 通过属性路径获取属性
     * 
     * @param path 属性路径
     * @return 属性（可能为空）
     */
    public static Optional<JustDyingAttribute> getAttribute(String path) {
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        
        // 如果路径包含冒号，尝试解析为完整标识符
        if (path.contains(":")) {
            return getAttribute(new Identifier(path));
        }
        
        // 否则仅使用路径部分
        JustDyingAttribute attribute = ATTRIBUTES_BY_NAME.get(path);
        
        if (attribute == null) {
            JustDying.LOGGER.debug(LOG_ATTRIBUTE_NOT_FOUND, path);
        }
        
        return Optional.ofNullable(attribute);
    }
    
    /**
     * 获取所有已注册的属性
     * 
     * @return 所有属性的集合
     */
    public static Collection<JustDyingAttribute> getAllAttributes() {
        return Collections.unmodifiableCollection(ATTRIBUTES.values());
    }
    
    /**
     * 获取所有已注册的属性ID
     * 
     * @return 所有属性ID的集合
     */
    public static Collection<Identifier> getAllAttributeIds() {
        return Collections.unmodifiableCollection(ATTRIBUTES.keySet());
    }
    
    /**
     * 获取所有属性的映射表
     * 
     * @return 属性映射表的副本
     */
    public static Map<Identifier, JustDyingAttribute> getAttributesMap() {
        return new HashMap<>(ATTRIBUTES);
    }
    
    /**
     * 获取启用的属性数量
     * 
     * @return 启用的属性数量
     */
    public static int getEnabledAttributeCount() {
        return ATTRIBUTES.size();
    }
    
    /**
     * 检查属性是否存在
     * 
     * @param id 属性ID
     * @return 属性是否存在
     */
    public static boolean hasAttribute(Identifier id) {
        return id != null && ATTRIBUTES.containsKey(id);
    }
    
    /**
     * 查找原版属性
     * 
     * @param id 原版属性ID
     * @return 原版属性，如果不存在则返回null
     */
    public static EntityAttribute findVanillaAttribute(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        
        try {
            return Registries.ATTRIBUTE.get(new Identifier(id));
        } catch (Exception e) {
            JustDying.LOGGER.error("查找原版属性失败: " + id, e);
            return null;
        }
    }
    
    /**
     * 按名称查找属性，可能在模组命名空间或原版命名空间中
     * 
     * @param name 属性名称，可以是完整路径或仅路径部分
     * @return 属性（可能为空）
     */
    public static Optional<JustDyingAttribute> findAttributeByName(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        
        // 先尝试直接查找路径
        JustDyingAttribute attribute = ATTRIBUTES_BY_NAME.get(name);
        if (attribute != null) {
            return Optional.of(attribute);
        }
        
        // 如果不成功，尝试解析为标识符
        try {
            Identifier id;
            if (name.contains(":")) {
                id = new Identifier(name);
            } else {
                id = new Identifier(JustDying.MOD_ID, name);
            }
            return getAttribute(id);
        } catch (Exception e) {
            JustDying.LOGGER.error("解析属性名称失败: " + name, e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取属性的未加工数据，用于调试
     * 
     * @return 属性数据的字符串表示
     */
    public static String dumpAttributeData() {
        return ATTRIBUTES.entrySet().stream()
            .map(entry -> entry.getKey() + " -> " + entry.getValue().toString())
            .collect(Collectors.joining("\n"));
    }
    
    /**
     * 更新属性的最大值
     * 
     * @param attributeId 属性ID
     * @param newMaxValue 新的最大值
     * @return 是否更新成功
     */
    public static boolean updateAttributeMaxValue(Identifier attributeId, int newMaxValue) {
        if (attributeId == null) {
            JustDying.LOGGER.error("更新属性最大值失败：属性ID为空");
            return false;
        }
        
        Optional<JustDyingAttribute> attributeOpt = getAttribute(attributeId);
        if (attributeOpt.isEmpty()) {
            JustDying.LOGGER.error("更新属性最大值失败：找不到属性 {}", attributeId);
            return false;
        }
        
        JustDyingAttribute attribute = attributeOpt.get();
        boolean success = attribute.setMaxValue(newMaxValue);
        
        if (success && JustDying.getConfig().debug) {
            JustDying.LOGGER.debug("已将属性 {} 的最大值更新为 {}", attributeId, newMaxValue);
        }
        
        return success;
    }
} 