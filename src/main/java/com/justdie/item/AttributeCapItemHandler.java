package com.justdie.item;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeManager;
import com.justdie.attribute.JustDyingAttribute;
import com.justdie.config.JustDyingConfig;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理属性上限增加物品的逻辑
 */
public class AttributeCapItemHandler {
    
    // 物品ID到属性ID的映射
    private static final Map<String, String> ITEM_TO_ATTRIBUTE_MAP = new ConcurrentHashMap<>(16);
    
    // 缓存属性标识符，减少对象创建
    private static final Map<String, Identifier> ATTRIBUTE_ID_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 初始化属性Cap物品映射
     */
    public static void initialize() {
        ITEM_TO_ATTRIBUTE_MAP.clear();
        ATTRIBUTE_ID_CACHE.clear();
        
        JustDyingConfig config = JustDying.getConfig();
        
        // 只有当启用属性上限增加功能时才继续
        if (!config.attributes.attributeCapItems.enableAttributeCapItems) {
            JustDying.LOGGER.info("属性上限增加功能已禁用");
            return;
        }
        
        // 遍历所有属性查找支持上限增加的属性
        for (Map.Entry<String, JustDyingConfig.AttributeConfig> entry : 
                config.attributes.attributes.entrySet()) {
            
            JustDyingConfig.AttributeConfig attrConfig = entry.getValue();
            
            // 检查属性是否启用了上限增加功能
            if (!attrConfig.enabled || !attrConfig.enableCapItem || attrConfig.capItemId.isEmpty()) {
                continue;
            }
            
            // 创建属性ID
            String attributeKey = entry.getKey();
            String capItemIdStr = attrConfig.capItemId;
            
            // 确保物品ID是合法的
            if (!capItemIdStr.contains(":")) {
                capItemIdStr = JustDying.MOD_ID + ":" + capItemIdStr;
            }
            
            // 检查物品是否有效
            try {
                Identifier itemId = new Identifier(capItemIdStr);
                Item item = Registries.ITEM.get(itemId);
                
                if (item != Items.AIR || capItemIdStr.equals("minecraft:air")) {
                    // 添加到映射
                    ITEM_TO_ATTRIBUTE_MAP.put(capItemIdStr, attributeKey);
                    
                    // 预缓存属性ID，减少后续查找次数
                    ATTRIBUTE_ID_CACHE.put(attributeKey, new Identifier(JustDying.MOD_ID, attributeKey));
                    
                    if (config.debug) {
                        JustDying.LOGGER.debug("已注册属性上限增加物品: {} -> {}", capItemIdStr, attributeKey);
                    }
                } else {
                    JustDying.LOGGER.warn("未找到属性上限增加物品: {}，该属性将不支持通过物品增加上限", capItemIdStr);
                }
            } catch (Exception e) {
                JustDying.LOGGER.error("注册属性上限增加物品失败: {} -> {}: {}", 
                    attributeKey, capItemIdStr, e.getMessage());
            }
        }
        
        JustDying.LOGGER.info("已注册 {} 个属性上限增加物品", ITEM_TO_ATTRIBUTE_MAP.size());
    }
    
    /**
     * 获取缓存的属性标识符
     */
    private static Identifier getAttributeId(String attributePath) {
        return ATTRIBUTE_ID_CACHE.computeIfAbsent(attributePath, 
            path -> new Identifier(JustDying.MOD_ID, path));
    }
    
    /**
     * 注册物品使用事件处理器
     */
    public static void register() {
        if (!JustDying.getConfig().attributes.attributeCapItems.enableAttributeCapItems) {
            JustDying.LOGGER.info("属性上限增加物品功能已禁用");
            return;
        }
        
        // 初始化属性映射
        initialize();
        
        // 如果没有找到有效的属性上限增加物品，则无需注册
        if (ITEM_TO_ATTRIBUTE_MAP.isEmpty()) {
            JustDying.LOGGER.warn("没有找到有效的属性上限增加物品，跳过注册物品使用事件处理器");
            return;
        }
        
        // 注册物品使用事件处理器
        UseItemCallback.EVENT.register((player, world, hand) -> {
            // 只处理服务器端和主手使用的物品
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }
            
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty()) {
                return TypedActionResult.pass(stack);
            }
            
            // 获取物品ID
            Item item = stack.getItem();
            if (item == null) {
                return TypedActionResult.pass(stack);
            }
            
            Identifier itemId = Registries.ITEM.getId(item);
            String itemIdStr = itemId.toString();
            
            // 检查是否属于属性上限增加物品（使用高效的映射查找）
            String attributePath = ITEM_TO_ATTRIBUTE_MAP.get(itemIdStr);
            if (attributePath != null) {
                // 使用缓存获取属性ID，提高性能
                Identifier attributeId = getAttributeId(attributePath);
                return handleAttributeCapItem(player, stack, attributeId);
            }
            
            // 不是属性上限增加物品，继续正常使用逻辑
            return TypedActionResult.pass(stack);
        });
        
        JustDying.LOGGER.info("属性上限增加物品使用事件处理器已注册");
    }
    
    /**
     * 处理属性上限增加物品
     * 
     * @param player 玩家
     * @param stack 物品堆
     * @param attributeId 属性ID
     * @return 操作结果
     */
    private static TypedActionResult<ItemStack> handleAttributeCapItem(PlayerEntity player, ItemStack stack, Identifier attributeId) {
        // 参数验证
        if (player == null || stack == null || attributeId == null) {
            JustDying.LOGGER.error("无效的属性上限增加物品处理参数");
            return TypedActionResult.fail(stack);
        }
    
        // 获取属性
        Optional<JustDyingAttribute> attributeOpt = AttributeManager.getAttribute(attributeId);
        if (attributeOpt.isEmpty()) {
            if (JustDying.getConfig().debug) {
                JustDying.LOGGER.error("找不到属性: {}", attributeId);
            }
            return TypedActionResult.fail(stack);
        }
        
        JustDyingAttribute attribute = attributeOpt.get();
        
        // 获取当前上限和增加量
        int currentMaxValue = attribute.getMaxValue();
        int increaseAmount = JustDying.getConfig().attributes.attributeCapItems.increaseAmountPerUse;
        
        // 增加上限
        boolean success = AttributeManager.updateAttributeMaxValue(attributeId, currentMaxValue + increaseAmount);
        
        if (success) {
            // 消耗物品
            stack.decrement(1);
            
            // 发送消息给玩家
            player.sendMessage(
                Text.translatable("message.justdying.attribute_cap_increased", 
                    attribute.getName().getString(), 
                    increaseAmount, 
                    currentMaxValue + increaseAmount)
                .formatted(Formatting.GREEN), 
                true
            );
            
            if (JustDying.getConfig().debug) {
                JustDying.LOGGER.debug("玩家 {} 使用物品增加了属性 {} 的上限，新上限: {}", 
                    player.getName().getString(), attributeId, currentMaxValue + increaseAmount);
            }
            
            return TypedActionResult.success(stack);
        } else {
            // 发送失败消息
            player.sendMessage(
                Text.translatable("message.justdying.attribute_cap_increase_failed", 
                    attribute.getName().getString())
                .formatted(Formatting.RED), 
                true
            );
            
            JustDying.LOGGER.error("玩家 {} 尝试增加属性 {} 的上限失败", 
                player.getName().getString(), attributeId);
            
            return TypedActionResult.fail(stack);
        }
    }
} 