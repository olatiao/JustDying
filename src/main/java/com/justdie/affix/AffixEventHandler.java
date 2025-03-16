package com.justdie.affix;

import com.justdie.JustDying;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 词缀事件处理器，用于处理游戏事件
 */
public class AffixEventHandler {
    // 存储玩家的装备词缀属性修饰符
    private static final Map<UUID, Map<EquipmentSlot, Map<Identifier, EntityAttributeModifier>>> PLAYER_AFFIX_MODIFIERS = new HashMap<>();

    /**
     * 注册事件处理器
     */
    public static void register() {
        // 注册攻击事件处理器
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            try {
                if (entity instanceof PlayerEntity player && player.isAlive()) {
                    ItemStack mainHandStack = player.getMainHandStack();
                    
                    if (!mainHandStack.isEmpty()) {
                        AffixManager.handleAttack(player, killedEntity, mainHandStack);
                    }
                    
                    // 处理怪物掉落物品的词缀添加
                    if (killedEntity instanceof LivingEntity && !world.isClient) {
                        // 获取怪物掉落的物品
                        for (ItemStack drop : killedEntity.getHandItems()) {
                            if (!drop.isEmpty() && isValidItemForAffix(drop)) {
                                // 根据配置的几率添加词缀
                                if (AffixManager.shouldDropWithAffix()) {
                                    // 随机添加1-2个词缀
                                    int affixCount = world.getRandom().nextBetween(1, 2);
                                    AffixManager.addRandomAffixes(drop, affixCount);
                                    
                                    JustDying.AFFIX_LOGGER.debug("Added {} random affixes to dropped item {}", 
                                            affixCount, drop.getItem().getName().getString());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                JustDying.AFFIX_LOGGER.error("处理攻击事件时出错", e);
            }
        });
        
        // 注册玩家复制事件处理器（用于处理玩家死亡和重生）
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            try {
                // 清除旧玩家的词缀修饰符
                clearPlayerAffixModifiers(oldPlayer);
                
                // 为新玩家应用装备词缀修饰符
                updatePlayerAffixModifiers(newPlayer);
            } catch (Exception e) {
                JustDying.AFFIX_LOGGER.error("处理玩家复制事件时出错", e);
            }
        });
        
        // 注册服务器tick事件处理器（用于定期更新被动效果）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                if (server != null && server.getPlayerManager() != null) {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        if (player != null && player.isAlive()) {
                            updatePlayerPassiveEffects(player);
                        }
                    }
                }
            } catch (Exception e) {
                JustDying.AFFIX_LOGGER.error("更新玩家被动效果时出错", e);
            }
        });
        
        // 注册装备变更事件处理器
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            try {
                if (server != null && server.getPlayerManager() != null) {
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        if (player != null && player.isAlive()) {
                            // 检查玩家装备是否变更，如果变更则更新词缀修饰符
                            updatePlayerAffixModifiers(player);
                        }
                    }
                }
            } catch (Exception e) {
                JustDying.AFFIX_LOGGER.error("更新玩家词缀修饰符时出错", e);
            }
        });
        
        // 注册战利品表事件处理器
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            try {
                // 只处理怪物和箱子的战利品表
                if (id.getPath().startsWith("entities/") || id.getPath().startsWith("chests/")) {
                    // 添加一个新的战利品池，用于处理词缀添加
                    // 注意：这里我们不直接修改现有的池，而是在物品生成后通过事件处理
                    // 这里只是为了示例，实际上我们主要通过怪物掉落和命令来添加词缀
                }
            } catch (Exception e) {
                JustDying.AFFIX_LOGGER.error("处理战利品表事件时出错: {}", id, e);
            }
        });
    }

    /**
     * 检查物品是否适合添加词缀
     */
    private static boolean isValidItemForAffix(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // 检查物品类型
        String itemType = getItemType(stack);
        
        // 根据物品类型判断是否适合添加词缀
        return itemType != null;
    }
    
    /**
     * 获取物品的类型
     * 
     * @param stack 物品堆
     * @return 物品类型，如果不适合添加词缀则返回null
     */
    private static String getItemType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        
        // 检查是否为武器
        if (stack.getItem() instanceof net.minecraft.item.SwordItem ||
            stack.getItem() instanceof net.minecraft.item.AxeItem ||
            stack.getItem() == Items.BOW ||
            stack.getItem() == Items.CROSSBOW ||
            stack.getItem() == Items.TRIDENT) {
            return "WEAPON";
        }
        
        // 检查是否为防具
        if (stack.getItem() instanceof net.minecraft.item.ArmorItem) {
            return "ARMOR";
        }
        
        // 检查是否为工具
        if (stack.getItem() instanceof net.minecraft.item.PickaxeItem ||
            stack.getItem() instanceof net.minecraft.item.ShovelItem ||
            stack.getItem() instanceof net.minecraft.item.HoeItem) {
            return "TOOL";
        }
        
        // 特殊物品
        if (stack.getItem() == Items.SHIELD) {
            return "ARMOR"; // 盾牌视为防具
        }
        
        return null; // 不适合添加词缀
    }

    /**
     * 更新玩家的被动效果
     */
    private static void updatePlayerPassiveEffects(ServerPlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return;
        }
        
        try {
            // 处理玩家装备的被动效果
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                // 只处理实际装备在玩家身上的物品
                if (isEquipmentSlot(slot)) {
                    ItemStack stack = player.getEquippedStack(slot);
                    if (!stack.isEmpty()) {
                        AffixManager.applyPassiveEffects(player, stack);
                    }
                }
            }
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("应用被动效果时出错: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * 检查是否为装备槽位（而非物品栏槽位）
     */
    private static boolean isEquipmentSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || 
               slot == EquipmentSlot.CHEST || 
               slot == EquipmentSlot.LEGS || 
               slot == EquipmentSlot.FEET || 
               slot == EquipmentSlot.MAINHAND || 
               slot == EquipmentSlot.OFFHAND;
    }

    /**
     * 更新玩家的词缀属性修饰符
     */
    public static void updatePlayerAffixModifiers(PlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return;
        }
        
        try {
            // 清除旧的词缀修饰符
            clearPlayerAffixModifiers(player);
            
            // 为每个装备槽应用词缀属性修饰符
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                // 只处理实际装备在玩家身上的物品
                if (isEquipmentSlot(slot)) {
                    ItemStack stack = player.getEquippedStack(slot);
                    if (!stack.isEmpty()) {
                        applyAffixModifiers(player, stack, slot);
                    }
                }
            }
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("更新词缀属性修饰符时出错: {}", player.getName().getString(), e);
        }
    }

    /**
     * 清除玩家的词缀属性修饰符
     */
    private static void clearPlayerAffixModifiers(PlayerEntity player) {
        if (player == null) {
            return;
        }
        
        try {
            UUID playerUuid = player.getUuid();
            Map<EquipmentSlot, Map<Identifier, EntityAttributeModifier>> slotModifiers = PLAYER_AFFIX_MODIFIERS.remove(playerUuid);
            
            if (slotModifiers == null) {
                return;
            }
            
            for (Map.Entry<EquipmentSlot, Map<Identifier, EntityAttributeModifier>> slotEntry : slotModifiers.entrySet()) {
                for (Map.Entry<Identifier, EntityAttributeModifier> entry : slotEntry.getValue().entrySet()) {
                    Identifier attributeId = entry.getKey();
                    EntityAttributeModifier modifier = entry.getValue();
                    
                    // 从玩家属性中移除修饰符
                    EntityAttribute attribute = Registries.ATTRIBUTE.get(attributeId);
                    if (attribute != null) {
                        EntityAttributeInstance attributeInstance = player.getAttributeInstance(attribute);
                        if (attributeInstance != null) {
                            attributeInstance.removeModifier(modifier.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("清除词缀属性修饰符时出错: {}", player.getName().getString(), e);
        }
    }

    /**
     * 应用词缀属性修饰符
     */
    private static void applyAffixModifiers(PlayerEntity player, ItemStack stack, EquipmentSlot slot) {
        if (player == null || stack.isEmpty() || slot == null) {
            return;
        }
        
        try {
            List<Affix> affixes = AffixManager.getAffixes(stack);
            
            if (affixes.isEmpty()) {
                return;
            }
            
            UUID playerUuid = player.getUuid();
            
            // 获取或创建玩家的词缀修饰符记录
            Map<EquipmentSlot, Map<Identifier, EntityAttributeModifier>> slotModifiers = PLAYER_AFFIX_MODIFIERS.computeIfAbsent(
                    playerUuid, k -> new HashMap<>());
            
            // 获取或创建装备槽的词缀修饰符记录
            Map<Identifier, EntityAttributeModifier> modifiers = slotModifiers.computeIfAbsent(
                    slot, k -> new HashMap<>());
            
            // 清除该槽位的旧修饰符
            for (Map.Entry<Identifier, EntityAttributeModifier> entry : new HashMap<>(modifiers).entrySet()) {
                Identifier attributeId = entry.getKey();
                EntityAttributeModifier oldModifier = entry.getValue();
                
                EntityAttribute attribute = Registries.ATTRIBUTE.get(attributeId);
                if (attribute != null) {
                    EntityAttributeInstance attributeInstance = player.getAttributeInstance(attribute);
                    if (attributeInstance != null && oldModifier != null) {
                        attributeInstance.removeModifier(oldModifier.getId());
                        JustDying.AFFIX_LOGGER.debug("Removed old modifier {} from attribute {} for player {}", 
                                oldModifier.getId(), attribute.getTranslationKey(), player.getName().getString());
                    }
                }
            }
            
            // 清空该槽位的修饰符记录
            modifiers.clear();
            
            // 应用每个词缀的属性修饰符
            for (Affix affix : affixes) {
                if (affix == null) continue;
                
                JustDying.AFFIX_LOGGER.debug("Applying affix {} to player {} in slot {}", 
                        affix.getName(), player.getName().getString(), slot.getName());
                
                for (AffixAttribute attribute : affix.getAttributes()) {
                    if (attribute == null) continue;
                    
                    EntityAttribute entityAttribute = attribute.getAttribute();
                    
                    if (entityAttribute != null) {
                        // 获取玩家的属性实例
                        EntityAttributeInstance attributeInstance = player.getAttributeInstance(entityAttribute);
                        if (attributeInstance == null) {
                            JustDying.AFFIX_LOGGER.warn("Player {} does not have attribute {}", 
                                    player.getName().getString(), entityAttribute.getTranslationKey());
                            continue;
                        }
                        
                        // 创建唯一的UUID，避免冲突
                        UUID modifierUuid = UUID.randomUUID();
                        
                        // 创建属性修饰符
                        String modifierName = "Affix_" + affix.getId() + "_" + slot.getName();
                        EntityAttributeModifier modifier = new EntityAttributeModifier(
                            modifierUuid,
                            modifierName,
                            attribute.getAmount(),
                            attribute.getOperation()
                        );
                        
                        try {
                            // 添加到玩家属性
                            attributeInstance.addPersistentModifier(modifier);
                            
                            // 记录修饰符
                            modifiers.put(attribute.getAttributeId(), modifier);
                            
                            JustDying.AFFIX_LOGGER.debug("Added modifier {} with value {} to attribute {} for player {}", 
                                    modifierName, attribute.getAmount(), entityAttribute.getTranslationKey(), player.getName().getString());
                        } catch (IllegalArgumentException e) {
                            // 如果出现错误，尝试先移除所有相同名称的修饰符，然后重新添加
                            JustDying.AFFIX_LOGGER.warn("Error adding modifier to attribute {}: {}. Attempting to remove existing modifiers first.", 
                                    entityAttribute.getTranslationKey(), e.getMessage());
                            
                            // 移除所有相同名称的修饰符
                            attributeInstance.getModifiers().forEach(existingModifier -> {
                                if (existingModifier.getName().equals(modifierName)) {
                                    attributeInstance.removeModifier(existingModifier.getId());
                                    JustDying.AFFIX_LOGGER.debug("Removed existing modifier {} from attribute {}", 
                                            existingModifier.getId(), entityAttribute.getTranslationKey());
                                }
                            });
                            
                            // 重新尝试添加修饰符
                            try {
                                attributeInstance.addPersistentModifier(modifier);
                                
                                // 记录修饰符
                                modifiers.put(attribute.getAttributeId(), modifier);
                                
                                JustDying.AFFIX_LOGGER.debug("Successfully added modifier after cleanup: {} to attribute {}", 
                                        modifierName, entityAttribute.getTranslationKey());
                            } catch (IllegalArgumentException ex) {
                                // 如果仍然出现错误，记录详细信息但不中断处理
                                JustDying.AFFIX_LOGGER.error("Still unable to add modifier {} to attribute {}: {}", 
                                        modifier.getId(), entityAttribute.getTranslationKey(), ex.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("应用词缀属性修饰符时出错: {} 在槽位 {}", 
                    player.getName().getString(), slot.getName(), e);
        }
    }
}