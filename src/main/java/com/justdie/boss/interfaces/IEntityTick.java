package com.justdie.boss.interfaces;

import net.minecraft.world.World;

/**
 * 处理实体tick逻辑的接口
 */
public interface IEntityTick<T extends World> {
    /**
     * 执行每tick处理
     * 
     * @param world 当前世界
     */
    void tick(T world);
} 