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
 * 这个类表示可以增加玩家属性上限的物品
 */
public class AttributeCapItem extends Item {
    // 属性相关数据
    private final Identifier attributeId;
    private final String attributeName;
    private final Formatting formatting;
    
    // 缓存实例级别的工具提示文本
    private volatile Text cachedTooltip; 

    // 全局工具提示文本缓存，使用属性名和增加量作为键
    // 改用ConcurrentHashMap提高并发性能
    private static final Map<CacheKey, Text> TOOLTIP_CACHE = new ConcurrentHashMap<>(16);

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
        this.attributeId = new Identifier(JustDying.MOD_ID, attributePath != null ? attributePath : "unknown");
        this.attributeName = attributeName != null ? attributeName : "未知属性";
        this.formatting = formatting != null ? formatting : Formatting.WHITE;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        // 延迟初始化缓存的工具提示，使用双重检查锁定模式确保线程安全
        Text localTooltip = cachedTooltip;
        if (localTooltip == null) {
            synchronized (this) {
                localTooltip = cachedTooltip;
                if (localTooltip == null) {
                    int increaseAmount = JustDying.getConfig() != null ? 
                        JustDying.getConfig().attributes.attributeCapItems.increaseAmountPerUse : 5;
                    cachedTooltip = localTooltip = getTooltipText(attributeName, increaseAmount);
                }
            }
        }

        tooltip.add(localTooltip);
    }

    /**
     * 获取缓存的工具提示文本
     * 
     * @param attrName 属性名称
     * @param amount 增加数量
     * @return 格式化的工具提示文本
     */
    private Text getTooltipText(String attrName, int amount) {
        CacheKey key = new CacheKey(attrName, amount);
        return TOOLTIP_CACHE.computeIfAbsent(key,
                k -> {
                    try {
                        return Text.translatable("item.justdying.attribute_cap_item.tooltip", k.attributeName, k.amount)
                                .formatted(formatting);
                    } catch (Exception e) {
                        JustDying.LOGGER.error("创建属性上限物品工具提示失败: {}", e.getMessage());
                        // 提供一个默认值，避免返回null
                        return Text.literal(String.format("增加 %s 属性上限 %d 点", k.attributeName, k.amount))
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
     * 清除所有缓存的工具提示
     * 当配置更新时可以调用此方法
     */
    public static void clearTooltipCache() {
        TOOLTIP_CACHE.clear();
    }
}