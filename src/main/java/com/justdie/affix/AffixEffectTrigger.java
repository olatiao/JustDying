package com.justdie.affix;

/**
 * 词缀效果触发条件枚举
 */
public enum AffixEffectTrigger {
    /**
     * 攻击时触发
     */
    ON_HIT,
    
    /**
     * 受伤时触发
     */
    ON_HURT,
    
    /**
     * 被动触发（持续生效）
     */
    PASSIVE
} 