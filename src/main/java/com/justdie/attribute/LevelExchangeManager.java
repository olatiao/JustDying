package com.justdie.attribute;

import com.justdie.JustDying;
import com.justdie.config.JustDyingConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 等级兑换管理器
 * 负责计算和处理玩家等级兑换为属性点的逻辑
 */
public class LevelExchangeManager {
    // 常量定义
    private static final String DEBUG_PLAYER_EXCHANGE = "玩家 {} 尝试兑换等级 {} 为属性点，初始点数: {}";
    private static final String DEBUG_POINTS_USED = "玩家 {} 属性已使用 {} 点";
    private static final String DEBUG_REQUIRED_LEVEL = "兑换所需等级: {}";
    private static final String DEBUG_EXCHANGE_SUCCESS = "兑换成功，玩家新等级: {}, 新点数: {}";
    private static final String DEBUG_EXCHANGE_FAILED = "兑换失败，玩家等级不足";
    
    // 最大等级上限
    private static final int MAX_LEVEL_REQUIRED = 100;
    
    /**
     * 计算兑换所需的等级
     * 
     * @param player 玩家
     * @return 玩家需要的等级才能兑换一个属性点
     */
    public static int calculateRequiredLevel(PlayerEntity player) {
        JustDyingConfig config = JustDying.getConfig();
        
        // 如果没有启用等级兑换，返回最大整数
        if (!config.levelExchange.enableLevelExchange) {
            return Integer.MAX_VALUE;
        }
        
        // 计算玩家已使用的点数
        int usedPoints = calculateUsedPoints(player);
        
        // 计算所需等级 = 基础等级 + (已使用点数 * 等级乘数)
        int requiredLevel = config.levelExchange.baseLevel + (usedPoints * config.levelExchange.levelMultiplier);
        
        // 确保所需等级不超过最大等级限制
        return Math.min(requiredLevel, MAX_LEVEL_REQUIRED);
    }
    
    /**
     * 计算玩家已使用的属性点数
     * 
     * @param player 玩家
     * @return 玩家已使用的属性点数
     */
    private static int calculateUsedPoints(PlayerEntity player) {
        int totalUsed = 0;
        
        // 遍历所有属性，计算每个属性使用的点数
        for (JustDyingAttribute attribute : AttributeManager.getAllAttributes()) {
            Identifier id = attribute.getId();
            int currentValue = AttributeHelper.getAttributeValue(player, id);
            int initialValue = attribute.getInitialValue();
            
            // 使用的点数 = 当前值 - 初始值
            int used = currentValue - initialValue;
            totalUsed += Math.max(0, used); // 确保不计算负值
        }
        
        return totalUsed;
    }
    
    /**
     * 兑换等级为属性点
     * 
     * @param player 玩家
     * @return 是否成功兑换
     */
    public static boolean exchangeLevelForPoint(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        
        JustDyingConfig config = JustDying.getConfig();
        
        // 如果没有启用等级兑换，直接返回失败
        if (!config.levelExchange.enableLevelExchange) {
            return false;
        }
        
        // 获取玩家当前等级和可用点数
        int currentLevel = player.experienceLevel;
        int availablePoints = AttributeHelper.getAvailablePoints(player);
        
        if (config.debug) {
            JustDying.LOGGER.debug(DEBUG_PLAYER_EXCHANGE, 
                    player.getName().getString(), currentLevel, availablePoints);
            
            int usedPoints = calculateUsedPoints(player);
            JustDying.LOGGER.debug(DEBUG_POINTS_USED, player.getName().getString(), usedPoints);
        }
        
        // 计算兑换所需等级
        int requiredLevel = calculateRequiredLevel(player);
        
        if (config.debug) {
            JustDying.LOGGER.debug(DEBUG_REQUIRED_LEVEL, requiredLevel);
        }
        
        // 检查玩家等级是否足够
        if (currentLevel < requiredLevel) {
            if (config.debug) {
                JustDying.LOGGER.debug(DEBUG_EXCHANGE_FAILED);
            }
            return false;
        }
        
        // 扣除等级，增加属性点
        player.addExperienceLevels(-requiredLevel);
        AttributeHelper.addPoints(player, 1);
        
        if (config.debug) {
            JustDying.LOGGER.debug(DEBUG_EXCHANGE_SUCCESS, 
                    player.experienceLevel, AttributeHelper.getAvailablePoints(player));
        }
        
        return true;
    }
} 