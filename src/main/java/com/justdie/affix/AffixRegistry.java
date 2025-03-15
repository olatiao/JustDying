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

/**
 * 词缀注册表，用于管理所有可用的词缀
 */
public class AffixRegistry {
    private static final Map<Identifier, Affix> AFFIXES = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static boolean initialized = false;

    /**
     * 注册一个词缀
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
     */
    public static Affix getAffix(Identifier id) {
        return id != null ? AFFIXES.get(id) : null;
    }

    /**
     * 获取所有词缀
     */
    public static List<Affix> getAllAffixes() {
        return AFFIXES.isEmpty() ? Collections.emptyList() : new ArrayList<>(AFFIXES.values());
    }

    /**
     * 随机获取一个词缀
     */
    public static Affix getRandomAffix() {
        List<Affix> affixes = getAllAffixes();
        if (affixes.isEmpty()) {
            return null;
        }
        return affixes.get(RANDOM.nextInt(affixes.size()));
    }

    /**
     * 从配置中加载词缀
     */
    public static void loadFromConfig() {
        if (initialized) {
            return;
        }

        try {
            registerDefaultAffixes();
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("无法从配置中加载词缀", e);
        }

        initialized = true;
    }

    /**
     * 注册默认词缀
     */
    public static void registerDefaultAffixes() {
        // 清除现有词缀
        AFFIXES.clear();

        try {
            // 使用反射获取DefaultAffixes类中的所有字段
            Field[] fields = JustDyingConfig.DefaultAffixes.class.getDeclaredFields();
            int count = 0;

            for (Field field : fields) {
                if (field.getType() == JustDyingConfig.AffixEntry.class) {
                    field.setAccessible(true);
                    JustDyingConfig.AffixEntry entry = (JustDyingConfig.AffixEntry) field.get(JustDying.getConfig().affixes.defaultAffixes);

                    // 检查词缀是否启用
                    if (!entry.enabled) {
                        JustDying.AFFIX_LOGGER.debug("跳过禁用的词缀: {}", entry.name);
                        continue;
                    }

                    // 创建词缀ID
                    String fieldName = field.getName();
                    Identifier id = new Identifier(JustDying.MOD_ID, fieldName.toLowerCase());

                    // 创建词缀实例
                    Affix affix = new Affix(id, entry.name, entry.formatting);

                    // 添加属性
                    if (entry.attribute != null) {
                        JustDyingConfig.AffixAttributeEntry attrEntry = entry.attribute;
                        EntityAttribute attribute = Registries.ATTRIBUTE.get(new Identifier(attrEntry.attributeId));
                        if (attribute != null) {
                            AffixAttribute affixAttr = new AffixAttribute(
                                    new Identifier(attrEntry.attributeId),
                                    attrEntry.operation,
                                    attrEntry.amount);
                            affix.addAttribute(affixAttr);
                        }
                    }

                    // 添加第二个属性（如果存在）
                    if (entry.secondaryAttribute != null) {
                        JustDyingConfig.AffixAttributeEntry attrEntry = entry.secondaryAttribute;
                        EntityAttribute attribute = Registries.ATTRIBUTE.get(new Identifier(attrEntry.attributeId));
                        if (attribute != null) {
                            AffixAttribute affixAttr = new AffixAttribute(
                                    new Identifier(attrEntry.attributeId),
                                    attrEntry.operation,
                                    attrEntry.amount);
                            affix.addAttribute(affixAttr);
                        }
                    }

                    // 添加效果
                    if (entry.effect != null) {
                        JustDyingConfig.AffixEffectEntry effectEntry = entry.effect;
                        AffixEffectTrigger trigger = AffixEffectTrigger.valueOf(effectEntry.trigger);
                        AffixEffect affixEffect = new AffixEffect(
                                new Identifier(effectEntry.effectId),
                                effectEntry.level,
                                effectEntry.duration,
                                effectEntry.chance,
                                trigger);
                        affix.addEffect(affixEffect);
                    }

                    // 设置物品类型
                    affix.setItemType(entry.itemType.name());

                    // 注册词缀
                    register(affix);
                    count++;
                }
            }

            JustDying.AFFIX_LOGGER.info("已从配置中注册 {} 个词缀", count);
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("注册默认词缀时出错", e);
        }
    }

    /**
     * 重新加载词缀配置
     */
    public static void reload() {
        initialized = false;
        loadFromConfig();
    }
}