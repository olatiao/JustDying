package com.justdie.boss.interfaces;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

/**
 * 处理实体伤害逻辑的接口
 */
public interface IDamageHandler {
    /**
     * 在伤害生效前处理
     * 
     * @param entity 实体
     * @param source 伤害源
     * @param amount 伤害量
     * @return 是否继续处理伤害
     */
    boolean beforeDamage(LivingEntity entity, DamageSource source, float amount);

    /**
     * 在伤害生效后处理
     * 
     * @param entity 实体
     * @param source 伤害源
     * @param amount 伤害量
     * @param result 伤害是否成功
     */
    void afterDamage(LivingEntity entity, DamageSource source, float amount, boolean result);
} 