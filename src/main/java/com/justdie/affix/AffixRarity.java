package com.justdie.affix;

import net.minecraft.util.Formatting;

/**
 * 词缀稀有度枚举
 */
public enum AffixRarity {
    /**
     * 普通稀有度，白色
     */
    COMMON(Formatting.WHITE),
    
    /**
     * 魔法稀有度，蓝色
     */
    MAGIC(Formatting.BLUE),
    
    /**
     * 稀有稀有度，黄色
     */
    RARE(Formatting.YELLOW),
    
    /**
     * 史诗稀有度，紫色
     */
    EPIC(Formatting.LIGHT_PURPLE),
    
    /**
     * 传说稀有度，金色
     */
    LEGENDARY(Formatting.GOLD);
    
    private final Formatting color;
    
    AffixRarity(Formatting color) {
        this.color = color;
    }
    
    /**
     * 获取稀有度对应的颜色
     * 
     * @return 颜色
     */
    public Formatting getColor() {
        return color;
    }
} 