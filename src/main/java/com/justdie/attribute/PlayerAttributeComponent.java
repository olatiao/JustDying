package com.justdie.attribute;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.util.Identifier;

/**
 * 玩家属性组件接口
 */
public interface PlayerAttributeComponent extends ComponentV3 {
    /**
     * 获取属性值
     * 
     * @param attributeId 属性ID
     * @return 属性值
     */
    int getAttributeValue(Identifier attributeId);
    
    /**
     * 设置属性值
     * 
     * @param attributeId 属性ID
     * @param value 新的属性值
     */
    void setAttributeValue(Identifier attributeId, int value);
    
    /**
     * 增加属性值
     * 
     * @param attributeId 属性ID
     * @param amount 增加的数量
     */
    void addAttributeValue(Identifier attributeId, int amount);
    
    /**
     * 更新所有原版属性
     */
    void updateAllVanillaAttributes();
    
    /**
     * 获取玩家属性数据
     * 
     * @return 玩家属性数据
     */
    PlayerAttributeData getAttributeData();
} 