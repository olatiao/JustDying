package com.justdie.gui.lightweight;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.util.Identifier;

/**
 * 属性条目数据类
 */
public class AttributeEntry {
    // 静态映射表，存储不同属性类型的效果计算逻辑
    private static final Map<String, Function<Integer, Double>> EFFECT_CALCULATORS = new HashMap<>();
    private static final Map<String, String> EFFECT_FORMATS = new HashMap<>();
    
    // 静态初始化块，设置默认效果计算器和格式
    static {
        // 注册效果计算逻辑
        EFFECT_CALCULATORS.put("strength", value -> value * 0.5);     // 每点增加0.5%伤害
        EFFECT_CALCULATORS.put("agility", value -> value * 0.75);     // 每点增加0.75%速度
        EFFECT_CALCULATORS.put("intelligence", value -> value * 1.0); // 每点增加1.0%魔法效果
        EFFECT_CALCULATORS.put("health", value -> value * 0.25);      // 每点增加0.25颗心
        EFFECT_CALCULATORS.put("defense", value -> value * 0.5);      // 每点增加0.5%防御
        
        // 注册效果格式
        EFFECT_FORMATS.put("strength", "+%.1f%% 伤害");
        EFFECT_FORMATS.put("agility", "+%.1f%% 速度");
        EFFECT_FORMATS.put("intelligence", "+%.1f%% 魔法效果");
        EFFECT_FORMATS.put("health", "+%.1f 生命值");
        EFFECT_FORMATS.put("defense", "+%.1f%% 防御");
    }
    
    public final Identifier id;
    public final String name;
    public final String description;
    public final int minValue;
    public final int maxValue;
    public final int costPerPoint;
    public final String effectFormat;
    
    public int currentValue;
    public SimpleAnimation highlightAnimation;
    
    public AttributeEntry(Identifier id, String name, String description,
                       int minValue, int maxValue, int currentValue, 
                       int costPerPoint, String effectFormat) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
        this.costPerPoint = costPerPoint;
        this.effectFormat = effectFormat;
    }
    
    /**
     * 获取属性效果文本
     */
    public String getEffectText() {
        // 根据属性类型计算效果值
        double effectValue = getEffectValue();
        
        // 使用格式化字符串
        return String.format(effectFormat, effectValue);
    }
    
    /**
     * 计算属性效果值
     */
    public double getEffectValue() {
        String path = id.getPath();
        // 使用映射表获取计算逻辑，如果没有找到则返回默认值
        Function<Integer, Double> calculator = EFFECT_CALCULATORS.getOrDefault(
            path, value -> (double)value);
        return calculator.apply(currentValue);
    }
    
    /**
     * 获取属性默认效果格式
     */
    public static String getDefaultEffectFormat(String attributePath) {
        return EFFECT_FORMATS.getOrDefault(attributePath, "+%.1f");
    }
    
    /**
     * 启动升级动画
     */
    public void startUpgradeAnimation() {
        highlightAnimation = SimpleAnimation.fadeOut(500); // 使用新的工厂方法
    }
} 