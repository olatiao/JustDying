package com.justdie.attribute;

import com.justdie.JustDying;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家属性数据，存储玩家的所有属性值
 */
public class PlayerAttributeData {
    // 用于属性修饰符的UUID，每个属性使用不同的UUID
    private static final Map<Identifier, UUID> ATTRIBUTE_MODIFIER_UUIDS = new HashMap<>();
    
    // 属性修饰符的名称前缀
    private static final String ATTRIBUTE_MODIFIER_NAME_PREFIX = "justdying.attribute.";
    
    // 玩家的属性值
    private final Map<Identifier, Integer> attributeValues = new HashMap<>();
    
    // 玩家引用
    private final PlayerEntity player;
    
    /**
     * 创建玩家属性数据
     * 
     * @param player 玩家
     */
    public PlayerAttributeData(PlayerEntity player) {
        this.player = player;
        
        // 初始化所有属性为初始值
        for (JustDyingAttribute attribute : AttributeManager.getAllAttributes()) {
            attributeValues.put(attribute.getId(), attribute.getInitialValue());
        }
    }
    
    /**
     * 获取属性值
     * 
     * @param attributeId 属性ID
     * @return 属性值，如果属性不存在则返回0
     */
    public int getAttributeValue(Identifier attributeId) {
        return attributeValues.getOrDefault(attributeId, 0);
    }
    
    /**
     * 设置属性值
     * 
     * @param attributeId 属性ID
     * @param value 新的属性值
     */
    public void setAttributeValue(Identifier attributeId, int value) {
        AttributeManager.getAttribute(attributeId).ifPresent(attribute -> {
            // 确保值在有效范围内
            int clampedValue = Math.max(attribute.getMinValue(), Math.min(attribute.getMaxValue(), value));
            
            // 更新属性值
            attributeValues.put(attributeId, clampedValue);
            
            // 更新玩家的原版属性
            updateVanillaAttribute(attribute, clampedValue);
        });
    }
    
    /**
     * 增加属性值
     * 
     * @param attributeId 属性ID
     * @param amount 增加的数量
     */
    public void addAttributeValue(Identifier attributeId, int amount) {
        int currentValue = getAttributeValue(attributeId);
        setAttributeValue(attributeId, currentValue + amount);
    }
    
    /**
     * 更新玩家的原版属性
     * 
     * @param attribute 属性
     * @param value 属性值
     */
    public void updateVanillaAttribute(JustDyingAttribute attribute, int value) {
        if (attribute.getVanillaAttribute() == null || player == null) {
            JustDying.LOGGER.debug("Cannot update vanilla attribute for {}: attribute or player is null", 
                    attribute != null ? attribute.getId() : "null");
            return;
        }
        
        try {
            // 获取属性实例
            EntityAttributeInstance instance = player.getAttributeInstance(attribute.getVanillaAttribute());
            if (instance == null) {
                JustDying.LOGGER.warn("Cannot update vanilla attribute for {}: player does not have attribute instance", 
                        attribute.getId());
                return;
            }
            
            // 获取或创建属性修饰符的UUID
            UUID modifierId = getAttributeModifierUuid(attribute.getId());
            
            // 移除现有的修饰符
            EntityAttributeModifier existingModifier = instance.getModifier(modifierId);
            if (existingModifier != null) {
                instance.removeModifier(modifierId);
            }
            
            // 计算属性加成值
            double bonus = attribute.calculateAttributeBonus(value);
            
            // 特殊处理防御属性
            boolean isDefenseAttribute = attribute.getId().getPath().equals("defense");
            
            // 创建新的修饰符并应用
            if (bonus > 0) {
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                        modifierId,
                        ATTRIBUTE_MODIFIER_NAME_PREFIX + attribute.getId().getPath(),
                        bonus,
                        EntityAttributeModifier.Operation.ADDITION
                );
                
                // 先移除再添加，确保修饰符被正确应用
                instance.removeModifier(modifierId);
                instance.addPersistentModifier(modifier);
                
                if (isDefenseAttribute) {
                    JustDying.LOGGER.debug("Applied defense modifier: {} with value {}", 
                            modifier.getName(), modifier.getValue());
                }
            }
        } catch (Exception e) {
            JustDying.LOGGER.error("Failed to update vanilla attribute for {}: {}", attribute.getId(), e.getMessage());
        }
    }
    
    /**
     * 获取属性修饰符的UUID
     * 
     * @param attributeId 属性ID
     * @return UUID
     */
    private static UUID getAttributeModifierUuid(Identifier attributeId) {
        return ATTRIBUTE_MODIFIER_UUIDS.computeIfAbsent(attributeId, id -> UUID.randomUUID());
    }
    
    /**
     * 更新所有原版属性
     */
    public void updateAllVanillaAttributes() {
        for (Map.Entry<Identifier, Integer> entry : attributeValues.entrySet()) {
            AttributeManager.getAttribute(entry.getKey()).ifPresent(attribute -> {
                updateVanillaAttribute(attribute, entry.getValue());
            });
        }
    }
    
    /**
     * 从NBT加载属性数据
     * 
     * @param nbt NBT数据
     */
    public void readFromNbt(NbtCompound nbt) {
        if (!nbt.contains("Attributes", NbtElement.COMPOUND_TYPE)) {
            return;
        }
        
        NbtCompound attributesNbt = nbt.getCompound("Attributes");
        
        for (JustDyingAttribute attribute : AttributeManager.getAllAttributes()) {
            String key = attribute.getId().toString();
            if (attributesNbt.contains(key, NbtElement.INT_TYPE)) {
                int value = attributesNbt.getInt(key);
                attributeValues.put(attribute.getId(), value);
            }
        }
        
        // 更新所有原版属性
        updateAllVanillaAttributes();
    }
    
    /**
     * 将属性数据保存到NBT
     * 
     * @param nbt NBT数据
     */
    public void writeToNbt(NbtCompound nbt) {
        NbtCompound attributesNbt = new NbtCompound();
        
        for (Map.Entry<Identifier, Integer> entry : attributeValues.entrySet()) {
            attributesNbt.putInt(entry.getKey().toString(), entry.getValue());
        }
        
        nbt.put("Attributes", attributesNbt);
    }

    /**
     * 初始化属性数据
     * 
     * @param attribute 要初始化的属性
     */
    public void initAttribute(JustDyingAttribute attribute) {
        if (!attributeValues.containsKey(attribute.getId())) {
            attributeValues.put(attribute.getId(), attribute.getInitialValue());
        }
    }
} 