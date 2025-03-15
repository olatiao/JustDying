package com.justdie.attribute;

import com.justdie.JustDying;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.justdie.attribute.AttributeComponents;
import com.justdie.attribute.PlayerAttributeComponent;
import com.justdie.attribute.PlayerAttributeComponentImpl;

/**
 * 属性辅助类 - 改进版
 * 使用原生属性系统管理玩家属性
 */
public class AttributeHelperV2 {
    // 用于属性修饰符的UUID命名空间，避免冲突
    private static final UUID BASE_UUID = UUID.fromString("37bbcad0-5f5e-11ec-bf63-0242ac130002");
    
    // 修饰符名称前缀
    private static final String MODIFIER_NAME_PREFIX = "justdying.attribute.";
    
    // 缓存属性修饰符UUID
    private static final Map<Identifier, UUID> ATTRIBUTE_MODIFIER_UUIDS = new HashMap<>();
    
    /**
     * 获取属性值
     * 
     * @param player 玩家
     * @param attribute 属性
     * @return 当前值
     */
    public static double getAttributeValue(PlayerEntity player, EntityAttribute attribute) {
        if (player == null || attribute == null) {
            return 0;
        }
        
        try {
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance == null) {
                // 如果实体没有该属性实例，直接从组件获取存储的值
                return getValueFromComponent(player, Registries.ATTRIBUTE.getId(attribute));
            }
            
            // 从修饰符中获取我们的属性值
            double baseValue = 0;
            UUID modifierId = getAttributeModifierUuid(Registries.ATTRIBUTE.getId(attribute));
            for (EntityAttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getId().equals(modifierId)) {
                    baseValue = modifier.getValue();
                    break;
                }
            }
            
            return baseValue;
        } catch (Exception e) {
            JustDying.LOGGER.error("获取属性值时出错: {}", e.getMessage());
            // 尝试从组件获取
            return getValueFromComponent(player, Registries.ATTRIBUTE.getId(attribute));
        }
    }
    
    /**
     * 从组件获取属性值
     */
    private static double getValueFromComponent(PlayerEntity player, Identifier attributeId) {
        try {
            PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            return component.getAttributeValue(attributeId);
        } catch (Exception e) {
            JustDying.LOGGER.error("从组件获取属性值时出错: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取属性实际效果值（考虑基础值和其他修饰符）
     */
    public static double getAttributeEffectiveValue(PlayerEntity player, EntityAttribute attribute) {
        if (player == null || attribute == null) {
            return 0;
        }
        
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return 0;
        }
        
        return instance.getValue();
    }
    
    /**
     * 设置属性值
     * 
     * @param player 玩家
     * @param attribute 属性
     * @param value 新值
     */
    public static void setAttributeValue(PlayerEntity player, EntityAttribute attribute, double value) {
        if (player == null || attribute == null) {
            return;
        }
        
        try {
            // 获取属性定义检查范围
            AttributeRegistry.AttributePointDefinition definition = 
                    AttributeRegistry.getInstance().getDefinitionByAttribute(attribute).orElse(null);
            
            if (definition != null) {
                // 确保值在有效范围内
                value = Math.max(definition.getMinValue(), Math.min(definition.getMaxValue(), value));
            }
            
            // 获取玩家的属性实例
            EntityAttributeInstance instance = player.getAttributeInstance(attribute);
            if (instance != null) {
                // 获取或创建修饰符
                UUID modifierId = getAttributeModifierUuid(Registries.ATTRIBUTE.getId(attribute));
                String modifierName = MODIFIER_NAME_PREFIX + Registries.ATTRIBUTE.getId(attribute).getPath();
                
                // 移除现有修饰符
                instance.removeModifier(modifierId);
                
                // 添加新修饰符
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                        modifierId, 
                        modifierName, 
                        value, 
                        EntityAttributeModifier.Operation.ADDITION);
                
                instance.addPersistentModifier(modifier);
            } else {
                JustDying.LOGGER.warn("玩家 {} 没有属性实例: {}, 只更新组件数据", 
                        player.getName().getString(), Registries.ATTRIBUTE.getId(attribute));
            }
            
            // 始终更新组件中的值，确保数据持久化
            updateComponentValue(player, Registries.ATTRIBUTE.getId(attribute), (int)value);
            
            // 使用调试标志来决定是否记录日志
            if (JustDying.LOGGER.isDebugEnabled()) {
                JustDying.LOGGER.debug("已设置玩家 {} 的属性 {} 为 {}", 
                        player.getName().getString(), 
                        Registries.ATTRIBUTE.getId(attribute), 
                        value);
            }
        } catch (Exception e) {
            JustDying.LOGGER.error("设置属性值时出错: {} - {}", Registries.ATTRIBUTE.getId(attribute), e.getMessage());
            // 尝试只更新组件数据
            updateComponentValue(player, Registries.ATTRIBUTE.getId(attribute), (int)value);
        }
    }
    
    /**
     * 更新组件中的属性值
     */
    private static void updateComponentValue(PlayerEntity player, Identifier attributeId, int value) {
        try {
            PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            component.setAttributeValue(attributeId, value);
        } catch (Exception e) {
            JustDying.LOGGER.error("更新组件属性值时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 增加属性值
     * 
     * @param player 玩家
     * @param attribute 属性
     * @param amount 增加数量
     */
    public static void addAttributeValue(PlayerEntity player, EntityAttribute attribute, double amount) {
        double currentValue = getAttributeValue(player, attribute);
        setAttributeValue(player, attribute, currentValue + amount);
    }
    
    /**
     * 获取属性修饰符的UUID
     * 
     * @param attributeId 属性ID
     * @return UUID
     */
    private static UUID getAttributeModifierUuid(Identifier attributeId) {
        return ATTRIBUTE_MODIFIER_UUIDS.computeIfAbsent(attributeId, 
                id -> {
                    // 使用确定性UUID生成，基于属性ID
                    String seed = BASE_UUID.toString() + id.toString();
                    return UUID.nameUUIDFromBytes(seed.getBytes());
                });
    }
    
    /**
     * 获取玩家可用属性点数
     */
    public static int getAvailablePoints(PlayerEntity player) {
        if (player == null) {
            return 0;
        }
        
        PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        return component.getAvailablePoints();
    }
    
    /**
     * 设置玩家可用属性点数
     */
    public static void setAvailablePoints(PlayerEntity player, int points) {
        if (player == null) {
            return;
        }
        
        PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
        component.setAvailablePoints(Math.max(0, points));
    }
    
    /**
     * 增加玩家可用属性点数
     */
    public static void addPoints(PlayerEntity player, int amount) {
        int currentPoints = getAvailablePoints(player);
        setAvailablePoints(player, currentPoints + amount);
    }
    
    /**
     * 使用属性点
     * 
     * @return 是否成功使用
     */
    public static boolean usePoints(PlayerEntity player, int amount) {
        int currentPoints = getAvailablePoints(player);
        
        if (currentPoints >= amount) {
            setAvailablePoints(player, currentPoints - amount);
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新玩家的所有属性
     */
    public static void updateAllAttributes(PlayerEntity player) {
        if (player == null) {
            return;
        }
        
        try {
            PlayerAttributeComponentImpl component = (PlayerAttributeComponentImpl) AttributeComponents.PLAYER_ATTRIBUTES.get(player);
            
            // 记录日志
            JustDying.LOGGER.info("开始更新玩家 {} 的所有属性", player.getName().getString());
            
            // 计数器
            int updatedCount = 0;
            int missingCount = 0;
            
            for (AttributeRegistry.AttributePointDefinition definition : 
                AttributeRegistry.getInstance().getRegisteredAttributes()) {
                
                try {
                    EntityAttribute attribute = definition.getAttribute();
                    Identifier attributeId = Registries.ATTRIBUTE.getId(attribute);
                    
                    // 从组件获取值
                    double value = component.getAttributeValue(attributeId);
                    
                    // 如果没有值，使用默认值并保存到组件
                    if (value == 0) {
                        value = definition.getDefaultValue();
                        component.setAttributeValue(attributeId, (int)value);
                        JustDying.LOGGER.debug("为玩家 {} 设置默认属性值 {}: {}", 
                                player.getName().getString(), attributeId, value);
                    }
                    
                    // 尝试获取属性实例
                    EntityAttributeInstance instance = player.getAttributeInstance(attribute);
                    if (instance == null) {
                        // 属性实例不存在，记录警告
                        JustDying.LOGGER.warn("玩家 {} 没有属性实例: {}, 无法在实体上应用，只更新组件数据", 
                                player.getName().getString(), attributeId);
                        missingCount++;
                        continue;
                    }
                    
                    // 获取或创建修饰符
                    UUID modifierId = getAttributeModifierUuid(attributeId);
                    String modifierName = MODIFIER_NAME_PREFIX + attributeId.getPath();
                    
                    // 设置基础值
                    instance.setBaseValue(0); // 重置基础值为0
                    
                    // 移除现有修饰符
                    instance.removeModifier(modifierId);
                    
                    // 添加新修饰符
                    EntityAttributeModifier modifier = new EntityAttributeModifier(
                            modifierId, 
                            modifierName, 
                            value, 
                            EntityAttributeModifier.Operation.ADDITION);
                    
                    instance.addPersistentModifier(modifier);
                    
                    JustDying.LOGGER.debug("已更新玩家 {} 的属性实例 {}: {}", 
                            player.getName().getString(), attributeId, value);
                    updatedCount++;
                } catch (Exception e) {
                    JustDying.LOGGER.error("更新属性时出错: {}", e.getMessage());
                    missingCount++;
                }
            }
            
            JustDying.LOGGER.info("玩家 {} 的属性更新完成, 成功: {}, 失败: {}", 
                    player.getName().getString(), updatedCount, missingCount);
        } catch (Exception e) {
            JustDying.LOGGER.error("更新所有属性失败: {}", e.getMessage(), e);
        }
    }
} 