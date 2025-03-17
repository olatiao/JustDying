package com.justdie.boss.interfaces;

import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

/**
 * 处理实体移动的接口
 */
public interface IMoveHandler {
    /**
     * 检查实体是否可以移动
     * 
     * @param type 移动类型
     * @param movement 移动向量
     * @return 是否允许移动
     */
    boolean canMove(MovementType type, Vec3d movement);
} 