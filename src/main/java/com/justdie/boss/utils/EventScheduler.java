package com.justdie.boss.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 事件调度系统，用于处理延迟和计划任务
 */
public class EventScheduler {
    private final List<TimedEvent> events = new ArrayList<>();
    private final List<TimedEvent> eventsToAdd = new ArrayList<>();
    
    /**
     * 添加一个计时事件
     * 
     * @param event 要添加的事件
     */
    public void addEvent(TimedEvent event) {
        eventsToAdd.add(event);
    }
    
    /**
     * 更新所有事件，应该在每tick调用
     */
    public void updateEvents() {
        events.addAll(eventsToAdd);
        eventsToAdd.clear();
        
        Iterator<TimedEvent> iterator = events.iterator();
        while (iterator.hasNext()) {
            TimedEvent event = iterator.next();
            if (event.update()) {
                iterator.remove();
            }
        }
    }
} 