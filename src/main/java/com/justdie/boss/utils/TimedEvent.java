package com.justdie.boss.utils;

import java.util.function.Supplier;

/**
 * 表示一个计时事件，可用于延迟执行动作或者定期执行动作
 */
public class TimedEvent {
    private final Runnable action;
    private final int startTime;
    private final int duration;
    private final Supplier<Boolean> shouldCancel;
    private int ticksElapsed = 0;
    private int repeatInterval = -1;
    
    /**
     * 创建一个计时事件
     * 
     * @param action 要执行的动作
     * @param startTime 开始执行的时间点（tick）
     * @param duration 持续时间（tick）
     * @param shouldCancel 取消条件
     */
    public TimedEvent(Runnable action, int startTime, int duration, Supplier<Boolean> shouldCancel) {
        this.action = action;
        this.startTime = startTime;
        this.duration = duration;
        this.shouldCancel = shouldCancel;
    }
    
    /**
     * 创建一个计时事件，不带取消条件
     * 
     * @param action 要执行的动作
     * @param startTime 开始执行的时间点（tick）
     * @param duration 持续时间（tick）
     */
    public TimedEvent(Runnable action, int startTime, int duration) {
        this(action, startTime, duration, () -> false);
    }
    
    /**
     * 创建一个即时执行的计时事件
     * 
     * @param action 要执行的动作
     * @param startTime 开始执行的时间点（tick）
     */
    public TimedEvent(Runnable action, int startTime) {
        this(action, startTime, 1);
    }
    
    /**
     * 设置重复间隔
     * 
     * @param interval 重复间隔（tick）
     * @return 当前事件实例
     */
    public TimedEvent repeating(int interval) {
        this.repeatInterval = interval;
        return this;
    }
    
    /**
     * 更新事件状态
     * 
     * @return 如果事件已完成或取消，则返回true
     */
    public boolean update() {
        ticksElapsed++;
        
        if (shouldCancel.get()) {
            return true;
        }
        
        if (ticksElapsed == startTime) {
            action.run();
            if (repeatInterval <= 0) {
                return ticksElapsed >= startTime + duration;
            }
        } else if (repeatInterval > 0 && ticksElapsed > startTime && 
                  (ticksElapsed - startTime) % repeatInterval == 0) {
            action.run();
        }
        
        return ticksElapsed >= startTime + duration;
    }
} 