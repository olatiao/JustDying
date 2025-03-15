package com.justdie.attribute;

import com.justdie.JustDying;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 等级兑换属性点管理器
 * 负责处理玩家等级兑换属性点的逻辑
 */
public class LevelExchangeManager {
    /**
     * 计算兑换一点属性点所需的等级
     * 公式: 所需等级 = 基础等级 × (已使用属性点数 + 1) × 等级乘数
     * 其中，已使用属性点数 = 所有属性已使用的点数
     * 
     * @param player 玩家
     * @return 所需等级
     */
    public static int calculateRequiredLevel(PlayerEntity player) {
        int baseLevel = JustDying.getConfig().levelExchange.baseLevel;
        int levelMultiplier = JustDying.getConfig().levelExchange.levelMultiplier;
        
        // 计算总属性点 = 当前可用点数 + 所有属性已使用的点数
        int availablePoints = AttributeHelper.getAvailablePoints(player);
        int usedPoints = calculateTotalUsedPoints(player);
        int totalPoints = availablePoints + usedPoints;
        
        JustDying.LOGGER.debug("Calculating required level. Available points: {}, Used points: {}, Total points: {}", 
                availablePoints, usedPoints, totalPoints);
        
        // 修改公式: 基础等级 × (已使用属性点数 + 1) × 等级乘数
        // 这样初始状态下只需要基础等级×等级乘数就可以兑换，而不考虑初始赠送的点数
        double requiredLevel = baseLevel * (usedPoints + 1) * levelMultiplier;
        
        // 向上取整
        return Math.max(1, (int) Math.ceil(requiredLevel));
    }
    
    /**
     * 计算玩家所有属性已使用的点数总和
     * 
     * @param player 玩家
     * @return 已使用的点数总和
     */
    private static int calculateTotalUsedPoints(PlayerEntity player) {
        int totalUsedPoints = 0;
        
        // 获取所有属性
        for (JustDyingAttribute attribute : AttributeManager.getAllAttributes()) {
            // 获取当前属性值
            int currentValue = AttributeHelper.getAttributeValue(player, attribute.getId());
            // 获取默认属性值
            int defaultValue = attribute.getDefaultValue();
            // 计算已使用的点数 = 当前值 - 默认值
            int usedPoints = Math.max(0, currentValue - defaultValue);
            
            totalUsedPoints += usedPoints;
            
            JustDying.LOGGER.debug("Attribute: {}, Current: {}, Default: {}, Used: {}", 
                    attribute.getId(), currentValue, defaultValue, usedPoints);
        }
        
        return totalUsedPoints;
    }
    
    /**
     * 尝试使用玩家等级兑换属性点
     * 
     * @param player 玩家
     * @return 是否成功兑换
     */
    public static boolean exchangeLevelForPoint(PlayerEntity player) {
        // 检查是否启用等级兑换功能
        if (!JustDying.getConfig().levelExchange.enableLevelExchange) {
            return false;
        }
        
        // 计算所需等级
        int requiredLevel = calculateRequiredLevel(player);
        int availablePoints = AttributeHelper.getAvailablePoints(player);
        int usedPoints = calculateTotalUsedPoints(player);
        
        JustDying.LOGGER.info("Player {} attempting to exchange level. Available points: {}, Used points: {}, Required level: {}, Current level: {}", 
                player.getName().getString(), availablePoints, usedPoints, requiredLevel, player.experienceLevel);
        
        // 检查玩家等级是否足够
        if (player.experienceLevel >= requiredLevel) {
            // 扣除玩家等级
            player.addExperienceLevels(-requiredLevel);
            
            // 增加属性点
            AttributeHelper.addPoints(player, 1);
            
            // 记录日志
            JustDying.LOGGER.info("Player {} exchanged {} levels for 1 attribute point. New available points: {}", 
                    player.getName().getString(), requiredLevel, AttributeHelper.getAvailablePoints(player));
            
            return true;
        }
        
        return false;
    }
} 