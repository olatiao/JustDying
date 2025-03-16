package com.justdie.attribute;

import com.justdie.JustDying;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * 属性工具类
 * 提供便捷的方法来访问和修改玩家属性，封装底层实现细节
 */
public class AttributeHelper {
    // 日志消息常量
    private static final String LOG_NULL_PLAYER = "尝试访问空玩家的属性";
    private static final String LOG_NULL_ATTRIBUTE = "尝试访问空属性ID";
    private static final String LOG_ATTRIBUTE_NOT_FOUND = "属性不存在: {}";
    private static final String LOG_SET_VALUE = "设置玩家 {} 的属性 {} 为 {}";
    private static final String LOG_ADD_VALUE = "增加玩家 {} 的属性 {} 值 {}，新值: {}";
    private static final String LOG_SET_POINTS = "设置玩家 {} 的可用点数为 {}";
    private static final String LOG_ADD_POINTS = "增加玩家 {} 的可用点数 {}，新值: {}";
    private static final String LOG_USE_POINTS = "玩家 {} 使用点数 {}，剩余: {}";
    private static final String LOG_NOT_ENOUGH_POINTS = "玩家 {} 点数不足，需要 {}，当前: {}";
    
    /**
     * 获取玩家的属性值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @return 属性值，如果玩家或属性无效则返回0
     */
    public static int getAttributeValue(PlayerEntity player, Identifier attributeId) {
        if (player == null) {
            JustDying.LOGGER.warn(LOG_NULL_PLAYER);
            return 0;
        }
        
        if (attributeId == null) {
            JustDying.LOGGER.warn(LOG_NULL_ATTRIBUTE);
            return 0;
        }
        
        try {
            PlayerAttributeComponent component = AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            return component.getAttributeValue(attributeId);
        } catch (Exception e) {
            JustDying.LOGGER.error("获取属性值时出错: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取玩家的属性值
     * 
     * @param player 玩家
     * @param attributePath 属性路径（不包含命名空间）
     * @return 属性值，如果玩家或属性无效则返回0
     */
    public static int getAttributeValue(PlayerEntity player, String attributePath) {
        if (attributePath == null || attributePath.isEmpty()) {
            JustDying.LOGGER.warn(LOG_NULL_ATTRIBUTE);
            return 0;
        }
        
        return getAttributeValue(player, new Identifier(JustDying.MOD_ID, attributePath));
    }
    
    /**
     * 设置玩家的属性值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @param value 新的属性值
     * @return 是否成功设置
     */
    public static boolean setAttributeValue(PlayerEntity player, Identifier attributeId, int value) {
        if (player == null) {
            JustDying.LOGGER.warn(LOG_NULL_PLAYER);
            return false;
        }
        
        if (attributeId == null) {
            JustDying.LOGGER.warn(LOG_NULL_ATTRIBUTE);
            return false;
        }
        
        // 检查属性是否存在
        if (!AttributeManager.hasAttribute(attributeId)) {
            JustDying.LOGGER.warn(LOG_ATTRIBUTE_NOT_FOUND, attributeId);
            return false;
        }
        
        try {
            // 获取属性的最大值和最小值
            int minValue = getAttributeMinValue(attributeId);
            int maxValue = getAttributeMaxValue(attributeId);
            
            // 确保值在有效范围内
            int clampedValue = Math.max(minValue, Math.min(value, maxValue));
            
            // 设置属性值
            PlayerAttributeComponent component = AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            component.setAttributeValue(attributeId, clampedValue);
            
            if (JustDying.getConfig().debug) {
                JustDying.LOGGER.debug(LOG_SET_VALUE, 
                        player.getName().getString(), attributeId, clampedValue);
            }
            
            return true;
        } catch (Exception e) {
            JustDying.LOGGER.error("设置属性值时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 设置玩家的属性值
     * 
     * @param player 玩家
     * @param attributePath 属性路径（不包含命名空间）
     * @param value 新的属性值
     * @return 是否成功设置
     */
    public static boolean setAttributeValue(PlayerEntity player, String attributePath, int value) {
        if (attributePath == null || attributePath.isEmpty()) {
            JustDying.LOGGER.warn(LOG_NULL_ATTRIBUTE);
            return false;
        }
        
        return setAttributeValue(player, new Identifier(JustDying.MOD_ID, attributePath), value);
    }
    
    /**
     * 增加玩家的属性值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @param amount 增加的数量
     * @return 是否成功增加
     */
    public static boolean addAttributeValue(PlayerEntity player, Identifier attributeId, int amount) {
        if (player == null) {
            JustDying.LOGGER.warn(LOG_NULL_PLAYER);
            return false;
        }
        
        if (attributeId == null || amount == 0) {
            return false;
        }
        
        try {
            // 获取当前值
            int currentValue = getAttributeValue(player, attributeId);
            
            // 设置新值
            boolean success = setAttributeValue(player, attributeId, currentValue + amount);
            
            if (success && JustDying.getConfig().debug) {
                JustDying.LOGGER.debug(LOG_ADD_VALUE, 
                        player.getName().getString(), attributeId, amount, 
                        getAttributeValue(player, attributeId));
            }
            
            return success;
        } catch (Exception e) {
            JustDying.LOGGER.error("增加属性值时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 增加玩家的属性值
     * 
     * @param player 玩家
     * @param attributePath 属性路径（不包含命名空间）
     * @param amount 增加的数量
     * @return 是否成功增加
     */
    public static boolean addAttributeValue(PlayerEntity player, String attributePath, int amount) {
        if (attributePath == null || attributePath.isEmpty()) {
            JustDying.LOGGER.warn(LOG_NULL_ATTRIBUTE);
            return false;
        }
        
        return addAttributeValue(player, new Identifier(JustDying.MOD_ID, attributePath), amount);
    }
    
    /**
     * 获取玩家的可用属性点数
     * 
     * @param player 玩家
     * @return 可用属性点数，如果玩家无效则返回0
     */
    public static int getAvailablePoints(PlayerEntity player) {
        if (player == null) {
            JustDying.LOGGER.warn(LOG_NULL_PLAYER);
            return 0;
        }
        
        try {
            PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            return component.getAvailablePoints();
        } catch (Exception e) {
            JustDying.LOGGER.error("获取可用点数时出错: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 设置玩家的可用属性点数
     * 
     * @param player 玩家
     * @param points 点数
     * @return 是否成功设置
     */
    public static boolean setAvailablePoints(PlayerEntity player, int points) {
        if (player == null) {
            JustDying.LOGGER.warn(LOG_NULL_PLAYER);
            return false;
        }
        
        try {
            PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            component.setAvailablePoints(Math.max(0, points));
            
            if (JustDying.getConfig().debug) {
                JustDying.LOGGER.debug(LOG_SET_POINTS, 
                        player.getName().getString(), points);
            }
            
            return true;
        } catch (Exception e) {
            JustDying.LOGGER.error("设置可用点数时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 增加玩家的可用属性点数
     * 
     * @param player 玩家
     * @param amount 增加的数量
     * @return 是否成功增加
     */
    public static boolean addPoints(PlayerEntity player, int amount) {
        if (player == null) {
            JustDying.LOGGER.warn(LOG_NULL_PLAYER);
            return false;
        }
        
        if (amount <= 0) {
            return false;
        }
        
        try {
            PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            int currentPoints = component.getAvailablePoints();
            component.addPoints(amount);
            
            if (JustDying.getConfig().debug) {
                JustDying.LOGGER.debug(LOG_ADD_POINTS, 
                        player.getName().getString(), amount, component.getAvailablePoints());
            }
            
            return true;
        } catch (Exception e) {
            JustDying.LOGGER.error("增加点数时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用玩家的属性点数
     * 
     * @param player 玩家
     * @param amount 使用的数量
     * @return 是否成功使用
     */
    public static boolean usePoints(PlayerEntity player, int amount) {
        if (player == null) {
            JustDying.LOGGER.warn(LOG_NULL_PLAYER);
            return false;
        }
        
        if (amount <= 0) {
            return false;
        }
        
        try {
            PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            int currentPoints = component.getAvailablePoints();
            
            if (currentPoints < amount) {
                if (JustDying.getConfig().debug) {
                    JustDying.LOGGER.debug(LOG_NOT_ENOUGH_POINTS, 
                            player.getName().getString(), amount, currentPoints);
                }
                return false;
            }
            
            boolean success = component.usePoints(amount);
            
            if (success && JustDying.getConfig().debug) {
                JustDying.LOGGER.debug(LOG_USE_POINTS, 
                        player.getName().getString(), amount, component.getAvailablePoints());
            }
            
            return success;
        } catch (Exception e) {
            JustDying.LOGGER.error("使用点数时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取属性的最大值或最小值
     * 
     * @param attributeId 属性ID
     * @param isMax 是否获取最大值（false表示获取最小值）
     * @return 属性的最大值或最小值，如果属性不存在则返回0
     */
    private static int getAttributeBoundValue(Identifier attributeId, boolean isMax) {
        if (attributeId == null) {
            return 0;
        }
        
        return AttributeManager.getAttribute(attributeId)
                .map(attr -> isMax ? attr.getMaxValue() : attr.getMinValue())
                .orElse(0);
    }
    
    /**
     * 获取属性的最大值
     * 
     * @param attributeId 属性ID
     * @return 属性最大值，如果属性不存在则返回0
     */
    public static int getAttributeMaxValue(Identifier attributeId) {
        return getAttributeBoundValue(attributeId, true);
    }
    
    /**
     * 获取属性的最小值
     * 
     * @param attributeId 属性ID
     * @return 属性最小值，如果属性不存在则返回0
     */
    public static int getAttributeMinValue(Identifier attributeId) {
        return getAttributeBoundValue(attributeId, false);
    }
    
    /**
     * 获取属性的最大值
     * 
     * @param attributePath 属性路径（不包含命名空间）
     * @return 属性最大值，如果属性不存在则返回0
     */
    public static int getAttributeMaxValue(String attributePath) {
        if (attributePath == null || attributePath.isEmpty()) {
            return 0;
        }
        
        return getAttributeMaxValue(new Identifier(JustDying.MOD_ID, attributePath));
    }
    
    /**
     * 获取属性的最小值
     * 
     * @param attributePath 属性路径（不包含命名空间）
     * @return 属性最小值，如果属性不存在则返回0
     */
    public static int getAttributeMinValue(String attributePath) {
        if (attributePath == null || attributePath.isEmpty()) {
            return 0;
        }
        
        return getAttributeMinValue(new Identifier(JustDying.MOD_ID, attributePath));
    }
} 