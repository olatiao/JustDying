package com.justdie.affix;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * 词缀效果类，表示词缀提供的状态效果
 */
public class AffixEffect {
    private final Identifier effectId;
    private final int level;
    private final int duration;
    private final float chance;
    private final AffixEffectTrigger trigger;
    
    /**
     * 创建一个新的词缀效果
     * 
     * @param effectId 效果ID
     * @param level 效果等级
     * @param duration 持续时间（单位：tick）
     * @param chance 触发几率
     * @param trigger 触发条件
     */
    public AffixEffect(Identifier effectId, int level, int duration, float chance, AffixEffectTrigger trigger) {
        this.effectId = effectId;
        this.level = level;
        this.duration = duration;
        this.chance = chance;
        this.trigger = trigger;
    }
    
    /**
     * 从NBT数据创建词缀效果
     * 
     * @param nbt NBT数据
     * @return 词缀效果实例
     */
    public static AffixEffect fromNbt(NbtCompound nbt) {
        Identifier effectId = new Identifier(nbt.getString("effectId"));
        int level = nbt.getInt("level");
        int duration = nbt.getInt("duration");
        float chance = nbt.getFloat("chance");
        AffixEffectTrigger trigger = AffixEffectTrigger.valueOf(nbt.getString("trigger"));
        
        return new AffixEffect(effectId, level, duration, chance, trigger);
    }
    
    /**
     * 将词缀效果转换为NBT数据
     * 
     * @return NBT数据
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("effectId", effectId.toString());
        nbt.putInt("level", level);
        nbt.putInt("duration", duration);
        nbt.putFloat("chance", chance);
        nbt.putString("trigger", trigger.name());
        return nbt;
    }
    
    /**
     * 获取效果ID
     * 
     * @return 效果ID
     */
    public Identifier getEffectId() {
        return effectId;
    }
    
    /**
     * 获取效果等级
     * 
     * @return 效果等级
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * 获取持续时间
     * 
     * @return 持续时间
     */
    public int getDuration() {
        return duration;
    }
    
    /**
     * 获取触发几率
     * 
     * @return 触发几率
     */
    public float getChance() {
        return chance;
    }
    
    /**
     * 获取触发条件
     * 
     * @return 触发条件
     */
    public AffixEffectTrigger getTrigger() {
        return trigger;
    }
    
    /**
     * 获取状态效果
     * 
     * @return 状态效果
     */
    public StatusEffect getStatusEffect() {
        return Registries.STATUS_EFFECT.get(effectId);
    }
    
    /**
     * 创建状态效果实例
     * 
     * @return 状态效果实例
     */
    public StatusEffectInstance createEffectInstance() {
        StatusEffect effect = getStatusEffect();
        if (effect == null) {
            return null;
        }
        
        return new StatusEffectInstance(effect, duration, level - 1, false, false);
    }
    
    /**
     * 获取提示信息
     * 
     * @return 提示信息
     */
    public Text getTooltip() {
        StatusEffect effect = getStatusEffect();
        if (effect == null) {
            return Text.literal("未知效果").formatted(Formatting.RED);
        }
        
        String durationText = "";
        if (duration > 0) {
            int seconds = duration / 20;
            durationText = " (" + seconds + "秒)";
        }
        
        String chanceText = "";
        if (chance < 1.0f) {
            chanceText = " " + (int)(chance * 100) + "%几率";
        }
        
        String triggerText = "";
        switch (trigger) {
            case ON_HIT:
                triggerText = "攻击时";
                break;
            case ON_HURT:
                triggerText = "受伤时";
                break;
            case PASSIVE:
                triggerText = "被动";
                break;
        }
        
        String levelText = level > 1 ? " " + level : "";
        
        // 使用MutableText构建更清晰的提示
        MutableText tooltip = Text.literal(triggerText + chanceText + ": ")
                .formatted(Formatting.GRAY);
        
        // 添加效果名称，使用效果的本地化名称
        tooltip.append(effect.getName().copy().formatted(effect.getCategory().getFormatting()));
        
        // 添加等级和持续时间
        if (level > 1 || duration > 0) {
            MutableText details = Text.literal(levelText + durationText).formatted(Formatting.GRAY);
            tooltip.append(details);
        }
        
        return tooltip;
    }
} 