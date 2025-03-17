package com.justdie.boss.interfaces;

/**
 * 处理实体状态的接口
 */
public interface IStatusHandler {
    /**
     * 处理状态码
     * 
     * @param status 状态码
     */
    void handleStatus(byte status);
} 