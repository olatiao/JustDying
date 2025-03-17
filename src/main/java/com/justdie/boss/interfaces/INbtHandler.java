package com.justdie.boss.interfaces;

import net.minecraft.nbt.NbtCompound;

/**
 * 处理实体NBT数据的接口
 */
public interface INbtHandler {
    /**
     * 将实体数据写入NBT
     * 
     * @param tag NBT数据
     * @return 修改后的NBT数据
     */
    NbtCompound toTag(NbtCompound tag);

    /**
     * 从NBT读取实体数据
     * 
     * @param tag NBT数据
     */
    void fromTag(NbtCompound tag);
} 