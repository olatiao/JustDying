package com.justdie.boss.utils;

import com.justdie.boss.interfaces.IStatusHandler;
import java.util.Arrays;
import java.util.List;

/**
 * 合成状态处理器，将多个状态处理器组合在一起
 */
public class CompositeStatusHandler implements IStatusHandler {
    private final List<IStatusHandler> handlers;
    
    /**
     * 创建合成状态处理器
     * 
     * @param handlers 要组合的状态处理器
     */
    public CompositeStatusHandler(IStatusHandler... handlers) {
        this.handlers = Arrays.asList(handlers);
    }
    
    @Override
    public void handleStatus(byte status) {
        // 将状态传递给所有处理器
        for (IStatusHandler handler : handlers) {
            handler.handleStatus(status);
        }
    }
} 