package com.justdie.attribute;

import com.justdie.JustDying;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 属性工具类，提供便捷的方法来访问和修改玩家属性
 */
public class AttributeHelper {
    /**
     * 获取玩家的属性值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @return 属性值，如果玩家没有该属性则返回0
     */
    public static int getAttributeValue(PlayerEntity player, Identifier attributeId) {
        PlayerAttributeComponent component = AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        return component.getAttributeValue(attributeId);
    }
    
    /**
     * 获取玩家的属性值
     * 
     * @param player 玩家
     * @param attributePath 属性路径（不包含命名空间）
     * @return 属性值，如果玩家没有该属性则返回0
     */
    public static int getAttributeValue(PlayerEntity player, String attributePath) {
        return getAttributeValue(player, new Identifier(JustDying.MOD_ID, attributePath));
    }
    
    /**
     * 设置玩家的属性值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @param value 新的属性值
     */
    public static void setAttributeValue(PlayerEntity player, Identifier attributeId, int value) {
        PlayerAttributeComponent component = AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        component.setAttributeValue(attributeId, value);
    }
    
    /**
     * 设置玩家的属性值
     * 
     * @param player 玩家
     * @param attributePath 属性路径（不包含命名空间）
     * @param value 新的属性值
     */
    public static void setAttributeValue(PlayerEntity player, String attributePath, int value) {
        setAttributeValue(player, new Identifier(JustDying.MOD_ID, attributePath), value);
    }
    
    /**
     * 增加玩家的属性值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @param amount 增加的数量
     */
    public static void addAttributeValue(PlayerEntity player, Identifier attributeId, int amount) {
        PlayerAttributeComponent component = AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        component.addAttributeValue(attributeId, amount);
    }
    
    /**
     * 增加玩家的属性值
     * 
     * @param player 玩家
     * @param attributePath 属性路径（不包含命名空间）
     * @param amount 增加的数量
     */
    public static void addAttributeValue(PlayerEntity player, String attributePath, int amount) {
        addAttributeValue(player, new Identifier(JustDying.MOD_ID, attributePath), amount);
    }
    
    /**
     * 获取玩家的可用属性点数
     * 
     * @param player 玩家
     * @return 可用属性点数
     */
    public static int getAvailablePoints(PlayerEntity player) {
        PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        return component.getAvailablePoints();
    }
    
    /**
     * 设置玩家的可用属性点数
     * 
     * @param player 玩家
     * @param points 点数
     */
    public static void setAvailablePoints(PlayerEntity player, int points) {
        PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        component.setAvailablePoints(points);
    }
    
    /**
     * 增加玩家的可用属性点数
     * 
     * @param player 玩家
     * @param amount 增加的数量
     */
    public static void addPoints(PlayerEntity player, int amount) {
        PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        component.addPoints(amount);
    }
    
    /**
     * 使用玩家的属性点数
     * 
     * @param player 玩家
     * @param amount 使用的数量
     * @return 是否成功使用
     */
    public static boolean usePoints(PlayerEntity player, int amount) {
        PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        return component.usePoints(amount);
    }
    
    /**
     * 获取属性的最大值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @return 属性最大值
     */
    public static int getAttributeMaxValue(PlayerEntity player, Identifier attributeId) {
        return AttributeManager.getAttribute(attributeId)
                .map(JustDyingAttribute::getMaxValue)
                .orElse(0);
    }
    
    /**
     * 获取属性的最小值
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     * @return 属性最小值
     */
    public static int getAttributeMinValue(PlayerEntity player, Identifier attributeId) {
        return AttributeManager.getAttribute(attributeId)
                .map(JustDyingAttribute::getMinValue)
                .orElse(0);
    }
} 