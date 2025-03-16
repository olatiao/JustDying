package com.justdie.affix;

import com.justdie.JustDying;
import com.justdie.config.JustDyingConfig;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 词缀注册表，用于管理所有可用的词缀
 */
public class AffixRegistry {
    // 常量定义，减少字符串字面量
    private static final String ERROR_REGISTER_AFFIX = "注册词缀 {} 时发生错误: {}";
    private static final String ERROR_INVALID_TRIGGER = "无效的词缀效果触发器: {}";
    
    // 存储所有注册的词缀
    private static final Map<Identifier, Affix> AFFIXES = new HashMap<>();
    
    // 使用ThreadLocalRandom代替Random，提高并发性能
    private static boolean initialized = false;
    
    // 缓存词缀ID，减少对象创建
    private static final Map<String, Identifier> ID_CACHE = new HashMap<>();

    /**
     * 注册一个词缀
     * 
     * @param affix 要注册的词缀
     */
    public static void register(Affix affix) {
        if (affix == null) {
            return;
        }
        AFFIXES.put(affix.getId(), affix);
        JustDying.AFFIX_LOGGER.debug("注册词缀: {}", affix.getId());
    }

    /**
     * 获取一个词缀
     * 
     * @param id 词缀ID
     * @return 词缀实例，如果不存在则返回null
     */
    public static Affix getAffix(Identifier id) {
        return id != null ? AFFIXES.get(id) : null;
    }

    /**
     * 获取所有词缀
     * 
     * @return 所有词缀的列表副本
     */
    public static List<Affix> getAllAffixes() {
        return AFFIXES.isEmpty() ? Collections.emptyList() : new ArrayList<>(AFFIXES.values());
    }

    /**
     * 随机获取一个词缀
     * 
     * @return 随机词缀，如果没有可用词缀则返回null
     */
    public static Affix getRandomAffix() {
        List<Affix> affixes = getAllAffixes();
        if (affixes.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(affixes.size());
        return affixes.get(index);
    }

    /**
     * 从配置中加载词缀
     */
    public static void loadFromConfig() {
        if (initialized) {
            return;
        }

        try {
            registerPresetAffixes();
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("无法从配置中加载词缀", e);
        }

        initialized = true;
    }

    /**
     * 注册预设词缀
     */
    public static void registerPresetAffixes() {
        JustDyingConfig config = JustDying.getConfig();
        if (!config.affixes.enableAffixes) {
            JustDying.LOGGER.info("词缀系统已禁用，跳过注册预设词缀");
            return;
        }
        
        try {
            // 使用反射获取PresetAffixes类中的所有字段
            Field[] fields = JustDyingConfig.PresetAffixes.class.getDeclaredFields();
            int count = 0;
            boolean isDebugEnabled = config.debug;
            
            for (Field field : fields) {
                if (!field.getType().equals(JustDyingConfig.AffixEntry.class)) {
                    continue;
                }
                
                field.setAccessible(true);
                JustDyingConfig.AffixEntry entry = (JustDyingConfig.AffixEntry) field.get(config.affixes.presetAffixes);
                
                if (entry != null && entry.enabled) {
                    String affixId = field.getName().toLowerCase();
                    if (registerAffix(affixId, entry, isDebugEnabled)) {
                        count++;
                    }
                }
            }
            
            JustDying.LOGGER.info("成功注册了 {} 个预设词缀", count);
        } catch (Exception e) {
            JustDying.LOGGER.error("注册预设词缀时发生错误: {}", e.getMessage());
            if (config.debug) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 注册单个词缀
     * 
     * @param id 词缀ID
     * @param entry 词缀配置条目
     * @param isDebugEnabled 是否启用调试日志
     * @return 是否成功注册
     */
    private static boolean registerAffix(String id, JustDyingConfig.AffixEntry entry, boolean isDebugEnabled) {
        try {
            // 使用缓存获取词缀ID对象
            Identifier affixId = ID_CACHE.computeIfAbsent(id, 
                key -> new Identifier(JustDying.MOD_ID, key));
                
            Affix affix = new Affix(affixId, entry.name, entry.formatting);
            
            // 添加属性
            if (entry.attribute != null) {
                addAffixAttribute(affix, entry.attribute);
            }
            
            // 添加第二个属性
            if (entry.secondaryAttribute != null) {
                addAffixAttribute(affix, entry.secondaryAttribute);
            }
            
            // 添加效果
            if (entry.effect != null) {
                addAffixEffect(affix, entry.effect);
            }
            
            // 设置物品类型
            affix.setItemType(entry.itemType.name());
            
            // 注册词缀
            register(affix);
            
            if (isDebugEnabled) {
                JustDying.LOGGER.debug("注册词缀: {}", affixId);
            }
            
            return true;
        } catch (Exception e) {
            JustDying.LOGGER.error(ERROR_REGISTER_AFFIX, id, e.getMessage());
            return false;
        }
    }
    
    /**
     * 添加词缀属性
     * 
     * @param affix 要添加到的词缀
     * @param attrEntry 属性条目配置
     */
    private static void addAffixAttribute(Affix affix, JustDyingConfig.AffixAttributeEntry attrEntry) {
        // 提前检查参数
        if (affix == null || attrEntry == null || attrEntry.attributeId == null || attrEntry.attributeId.isEmpty()) {
            return;
        }
        
        try {
            EntityAttribute attribute = Registries.ATTRIBUTE.get(new Identifier(attrEntry.attributeId));
            if (attribute != null) {
                AffixAttribute affixAttr = new AffixAttribute(
                    new Identifier(attrEntry.attributeId),
                    attrEntry.operation,
                    attrEntry.amount
                );
                affix.addAttribute(affixAttr);
            }
        } catch (Exception e) {
            JustDying.LOGGER.error("添加词缀属性时发生错误: {}", e.getMessage());
        }
    }
    
    /**
     * 添加词缀效果
     * 
     * @param affix 要添加到的词缀
     * @param effectEntry 效果条目配置
     */
    private static void addAffixEffect(Affix affix, JustDyingConfig.AffixEffectEntry effectEntry) {
        // 提前检查参数
        if (affix == null || effectEntry == null || effectEntry.effectId == null || effectEntry.effectId.isEmpty()) {
            return;
        }
        
        try {
            AffixEffectTrigger trigger = AffixEffectTrigger.valueOf(effectEntry.trigger);
            AffixEffect affixEffect = new AffixEffect(
                new Identifier(effectEntry.effectId),
                effectEntry.level,
                effectEntry.duration,
                effectEntry.chance,
                trigger
            );
            affix.addEffect(affixEffect);
        } catch (IllegalArgumentException e) {
            JustDying.LOGGER.error(ERROR_INVALID_TRIGGER, effectEntry.trigger);
        } catch (Exception e) {
            JustDying.LOGGER.error("添加词缀效果时发生错误: {}", e.getMessage());
        }
    }

    /**
     * 重新加载词缀配置
     */
    public static void reload() {
        // 清空词缀集合
        AFFIXES.clear();
        initialized = false;
        loadFromConfig();
    }
}