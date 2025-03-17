package com.justdie.boss.utils;

import com.justdie.boss.interfaces.IStatusHandler;
import java.util.Map;

/**
 * 动画状态管理器，用于处理实体的动画状态
 */
public class AnimationHolder implements IStatusHandler {
    private final Map<Byte, Animation> animations;
    private final byte defaultAnimation;
    private byte currentAnimation;
    
    /**
     * 创建动画管理器
     * 
     * @param animations 动画映射表，键为状态码，值为动画信息
     * @param defaultAnimation 默认动画状态码
     */
    public AnimationHolder(Map<Byte, Animation> animations, byte defaultAnimation) {
        this.animations = animations;
        this.defaultAnimation = defaultAnimation;
        this.currentAnimation = defaultAnimation;
    }
    
    @Override
    public void handleStatus(byte status) {
        if (animations.containsKey(status)) {
            this.currentAnimation = status;
        }
    }
    
    /**
     * 获取当前动画状态码
     * 
     * @return 当前动画状态码
     */
    public byte getCurrentAnimation() {
        return currentAnimation;
    }
    
    /**
     * 重置到默认动画
     */
    public void resetAnimation() {
        this.currentAnimation = defaultAnimation;
    }
    
    /**
     * 获取当前动画名称
     * 
     * @return 当前动画名称
     */
    public String getCurrentAnimationName() {
        Animation anim = animations.get(currentAnimation);
        return anim != null ? anim.name : "idle";
    }
    
    /**
     * 表示一个动画配置
     */
    public static class Animation {
        public final String name;
        
        /**
         * 创建一个动画配置
         * 
         * @param name 动画名称
         */
        public Animation(String name) {
            this.name = name;
        }
    }
} 