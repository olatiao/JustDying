package com.justdie.gui.lightweight;

/**
 * 简单的动画类
 */
public class SimpleAnimation {
    // 动画类型
    public enum AnimationType {
        TRANSLATE_Y, // Y轴平移
        FADE,        // 淡入淡出
        SCALE        // 缩放
    }
    
    private final AnimationType type;
    private final long duration; // 持续时间(毫秒)
    private final float startValue;
    private final float endValue;
    private long startTime;
    private boolean running = false;
    private Runnable onComplete;
    
    public SimpleAnimation(AnimationType type, long duration, float startValue, float endValue) {
        this.type = type;
        this.duration = duration;
        this.startValue = startValue;
        this.endValue = endValue;
    }
    
    // 创建并启动淡入动画
    public static SimpleAnimation fadeIn(long duration) {
        SimpleAnimation animation = new SimpleAnimation(AnimationType.FADE, duration, 0f, 1f);
        animation.start();
        return animation;
    }
    
    // 创建并启动淡出动画
    public static SimpleAnimation fadeOut(long duration) {
        SimpleAnimation animation = new SimpleAnimation(AnimationType.FADE, duration, 1f, 0f);
        animation.start();
        return animation;
    }
    
    // 创建并启动滑入动画
    public static SimpleAnimation slideIn(long duration, float startY, float endY) {
        SimpleAnimation animation = new SimpleAnimation(AnimationType.TRANSLATE_Y, duration, startY, endY);
        animation.start();
        return animation;
    }
    
    // 创建并启动滑出动画
    public static SimpleAnimation slideOut(long duration, float startY, float endY) {
        SimpleAnimation animation = new SimpleAnimation(AnimationType.TRANSLATE_Y, duration, startY, endY);
        animation.start();
        return animation;
    }
    
    public AnimationType getType() {
        return type;
    }
    
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }
    
    public void stop() {
        this.running = false;
    }
    
    public void setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public float getValue() {
        if (!running) {
            return endValue;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        
        if (elapsedTime >= duration) {
            running = false;
            if (onComplete != null) {
                onComplete.run();
            }
            return endValue;
        }
        
        // 使用简单的缓动函数
        float progress = easeInOutQuad((float) elapsedTime / duration);
        return startValue + (endValue - startValue) * progress;
    }
    
    // 简单的缓动函数
    private float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;
    }
} 