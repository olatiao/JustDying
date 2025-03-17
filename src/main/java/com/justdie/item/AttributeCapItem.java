package com.justdie.item;

import com.justdie.JustDying;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性上限增加物品
 * 此类表示可以增加玩家属性上限的物品，玩家可以使用它来提高自身的属性上限。
 * 每个AttributeCapItem实例关联一个特定的属性ID和名称。
 */
public class AttributeCapItem extends Item {
    // 常量定义
    private static final String DEFAULT_ATTRIBUTE_PATH = "unknown";
    private static final String DEFAULT_ATTRIBUTE_NAME = "未知属性";
    private static final int DEFAULT_INCREASE_AMOUNT = 5;
    private static final int CACHE_INITIAL_CAPACITY = 16;
    
    // 错误消息
    private static final String ERROR_TOOLTIP_CREATION = "创建属性上限物品工具提示失败: {}";
    private static final String DEFAULT_TOOLTIP_FORMAT = "增加 %s 属性上限 %d 点";
    private static final String TOOLTIP_TRANSLATION_KEY = "item.justdying.attribute_cap_item.tooltip";
    
    // 属性相关数据
    private final Identifier attributeId;
    private final String attributeName;
    private final Formatting formatting;
    
    // 缓存实例级别的工具提示文本
    private volatile Text cachedTooltip; 

    // 全局工具提示文本缓存，使用属性名和增加量作为键
    // 使用ConcurrentHashMap提高并发性能
    private static final Map<CacheKey, Text> TOOLTIP_CACHE = new ConcurrentHashMap<>(CACHE_INITIAL_CAPACITY);

    /**
     * 缓存键，使用属性名和增加量作为组合键
     * 这个内部类用于作为工具提示缓存的键
     */
    private static class CacheKey {
        final String attributeName;
        final int amount;
        // 缓存hashCode，提高性能
        private final int hashCode;

        CacheKey(String attributeName, int amount) {
            this.attributeName = attributeName != null ? attributeName : "";
            this.amount = amount;
            this.hashCode = Objects.hash(this.attributeName, amount);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CacheKey cacheKey = (CacheKey) o;
            return amount == cacheKey.amount &&
                    Objects.equals(attributeName, cacheKey.attributeName);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * 构造属性上限增加物品
     * 
     * @param settings 物品设置
     * @param attributePath 属性路径
     * @param attributeName 属性名称
     * @param formatting 文本格式化
     */
    public AttributeCapItem(Settings settings, String attributePath, String attributeName, Formatting formatting) {
        super(settings);
        this.attributeId = new Identifier(JustDying.MOD_ID, 
                attributePath != null ? attributePath : DEFAULT_ATTRIBUTE_PATH);
        this.attributeName = attributeName != null ? attributeName : DEFAULT_ATTRIBUTE_NAME;
        this.formatting = formatting != null ? formatting : Formatting.WHITE;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        if (tooltip == null) {
            return;
        }

        // 延迟初始化缓存的工具提示，使用双重检查锁定模式确保线程安全
        Text localTooltip = cachedTooltip;
        if (localTooltip == null) {
            synchronized (this) {
                localTooltip = cachedTooltip;
                if (localTooltip == null) {
                    int increaseAmount = getIncreaseAmount();
                    cachedTooltip = localTooltip = getTooltipText(attributeName, increaseAmount);
                }
            }
        }

        tooltip.add(localTooltip);
    }

    /**
     * 获取从配置中读取的增加数量，如果配置不可用则使用默认值
     * 
     * @return 属性增加数量
     */
    private int getIncreaseAmount() {
        try {
            if (JustDying.getConfig() != null && JustDying.getConfig().attributes != null 
                    && JustDying.getConfig().attributes.attributeCapItems != null) {
                return JustDying.getConfig().attributes.attributeCapItems.increaseAmountPerUse;
            }
        } catch (Exception e) {
            JustDying.LOGGER.warn("读取属性增加数量配置失败，使用默认值: {}", e.getMessage());
        }
        return DEFAULT_INCREASE_AMOUNT;
    }

    /**
     * 获取缓存的工具提示文本
     * 
     * @param attrName 属性名称
     * @param amount 增加数量
     * @return 格式化的工具提示文本
     */
    private Text getTooltipText(String attrName, int amount) {
        if (attrName == null || attrName.isEmpty()) {
            attrName = DEFAULT_ATTRIBUTE_NAME;
        }
        
        final String finalAttrName = attrName;
        CacheKey key = new CacheKey(finalAttrName, amount);
        
        return TOOLTIP_CACHE.computeIfAbsent(key,
                k -> {
                    try {
                        return Text.translatable(TOOLTIP_TRANSLATION_KEY, k.attributeName, k.amount)
                                .formatted(formatting);
                    } catch (Exception e) {
                        JustDying.LOGGER.error(ERROR_TOOLTIP_CREATION, e.getMessage());
                        // 提供一个默认值，避免返回null
                        return Text.literal(String.format(DEFAULT_TOOLTIP_FORMAT, k.attributeName, k.amount))
                                .formatted(formatting);
                    }
                });
    }

    /**
     * 获取属性ID
     * 
     * @return 属性ID
     */
    public Identifier getAttributeId() {
        return attributeId;
    }

    /**
     * 获取属性名称
     * 
     * @return 属性名称
     */
    public String getAttributeName() {
        return attributeName;
    }
    
    /**
     * 获取属性格式化样式
     * 
     * @return 格式化样式
     */
    public Formatting getFormatting() {
        return formatting;
    }

    /**
     * 清除工具提示缓存
     * 当配置更新时应调用此方法
     */
    public static void clearTooltipCache() {
        TOOLTIP_CACHE.clear();
        JustDying.LOGGER.debug("已清除AttributeCapItem工具提示缓存");
    }
}