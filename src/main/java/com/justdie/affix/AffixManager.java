package com.justdie.affix;

import com.justdie.JustDying;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.EquipmentSlot;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.NbtCompound;

/**
 * 词缀管理器，用于管理词缀的应用和处理
 * 提供添加、删除和管理物品词缀的方法以及处理词缀效果
 */
public class AffixManager {
    // 使用ThreadLocalRandom替代单一Random实例以提高多线程性能
    
    // 物品类型常量
    private static final String ITEM_TYPE_WEAPON = Affix.ITEM_TYPE_WEAPON;
    private static final String ITEM_TYPE_ARMOR = Affix.ITEM_TYPE_ARMOR;
    private static final String ITEM_TYPE_TOOL = Affix.ITEM_TYPE_TOOL;
    private static final String ITEM_TYPE_ANY = Affix.ITEM_TYPE_ANY;
    
    /**
     * 初始化词缀系统
     */
    public static void init() {
        try {
            // 从配置中加载词缀
            AffixRegistry.loadFromConfig();
            JustDying.LOGGER.info("词缀系统初始化完成，已加载 {} 个词缀", AffixRegistry.getAllAffixes().size());
        } catch (Exception e) {
            JustDying.LOGGER.error("词缀系统初始化失败: {}", e.getMessage());
        }
    }
    
    /**
     * 为物品添加随机词缀
     * 
     * @param stack 物品堆
     * @param count 要添加的词缀数量
     * @return 添加词缀后的物品堆
     */
    public static ItemStack addRandomAffixes(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty() || count <= 0) {
            return stack;
        }
        
        // 获取物品类型
        String itemType = getItemType(stack);
        if (itemType == null) {
            return stack; // 不适合添加词缀的物品
        }
        
        int maxAffixes = JustDying.getConfig().affixes.maxAffixesPerItem;
        List<Affix> existingAffixes = getAffixes(stack);
        int availableSlots = maxAffixes - existingAffixes.size();
        
        if (availableSlots <= 0) {
            return stack;
        }
        
        int affixesToAdd = Math.min(count, availableSlots);
        int added = 0;
        
        for (int i = 0; i < affixesToAdd; i++) {
            Affix affix = getRandomAffixForType(itemType);
            if (affix != null) {
                affix.applyToItem(stack);
                added++;
            }
        }
        
        if (added > 0) {
            JustDying.LOGGER.debug("已添加 {} 个随机词缀到物品", added);
        }
        
        return stack;
    }
    
    /**
     * 获取物品的类型
     * 
     * @param stack 物品堆
     * @return 物品类型，如果不适合添加词缀则返回null
     */
    private static String getItemType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        
        // 检查是否为武器
        if (stack.getItem() instanceof net.minecraft.item.SwordItem ||
            stack.getItem() instanceof net.minecraft.item.AxeItem ||
            stack.getItem() == net.minecraft.item.Items.BOW ||
            stack.getItem() == net.minecraft.item.Items.CROSSBOW ||
            stack.getItem() == net.minecraft.item.Items.TRIDENT) {
            return ITEM_TYPE_WEAPON;
        }
        
        // 检查是否为防具
        if (stack.getItem() instanceof net.minecraft.item.ArmorItem) {
            return ITEM_TYPE_ARMOR;
        }
        
        // 检查是否为工具
        if (stack.getItem() instanceof net.minecraft.item.PickaxeItem ||
            stack.getItem() instanceof net.minecraft.item.ShovelItem ||
            stack.getItem() instanceof net.minecraft.item.HoeItem) {
            return ITEM_TYPE_TOOL;
        }
        
        // 特殊物品
        if (stack.getItem() == net.minecraft.item.Items.SHIELD) {
            return ITEM_TYPE_ARMOR; // 盾牌视为防具
        }
        
        return null; // 不适合添加词缀
    }
    
    /**
     * 获取适合指定物品类型的随机词缀
     * 
     * @param itemType 物品类型
     * @return 随机词缀，如果没有适合的词缀则返回null
     */
    private static Affix getRandomAffixForType(String itemType) {
        if (itemType == null) {
            return null;
        }
        
        List<Affix> suitableAffixes = new ArrayList<>();
        
        for (Affix affix : AffixRegistry.getAllAffixes()) {
            if (affix != null && (affix.isApplicableTo(itemType) || affix.isApplicableTo(ITEM_TYPE_ANY))) {
                suitableAffixes.add(affix);
            }
        }
        
        if (suitableAffixes.isEmpty()) {
            JustDying.LOGGER.debug("没有找到适合物品类型 {} 的词缀", itemType);
            return null;
        }
        
        // 使用ThreadLocalRandom替代静态Random
        return suitableAffixes.get(ThreadLocalRandom.current().nextInt(suitableAffixes.size()));
    }
    
    /**
     * 为物品添加指定词缀
     * 
     * @param stack 物品堆
     * @param affixId 词缀ID
     * @return 添加词缀后的物品堆
     */
    public static ItemStack addAffix(ItemStack stack, Identifier affixId) {
        if (stack == null || stack.isEmpty() || affixId == null) {
            return stack;
        }
        
        // 获取物品类型
        String itemType = getItemType(stack);
        if (itemType == null) {
            return stack; // 不适合添加词缀的物品
        }
        
        int maxAffixes = JustDying.getConfig().affixes.maxAffixesPerItem;
        List<Affix> existingAffixes = getAffixes(stack);
        
        if (existingAffixes.size() >= maxAffixes) {
            return stack;
        }
        
        Affix affix = AffixRegistry.getAffix(affixId);
        if (affix != null && (affix.isApplicableTo(itemType) || affix.isApplicableTo("ANY"))) {
            affix.applyToItem(stack);
        }
        
        return stack;
    }
    
    /**
     * 从物品中移除指定词缀
     */
    public static boolean removeAffix(ItemStack stack, Identifier affixId) {
        if (stack.isEmpty()) {
            return false;
        }
        
        Affix affix = AffixRegistry.getAffix(affixId);
        if (affix == null) {
            return false;
        }
        
        return affix.removeFromItem(stack);
    }
    
    /**
     * 清除物品上的所有词缀
     */
    public static void clearAllAffixes(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        List<Affix> affixes = getAffixes(stack);
        for (Affix affix : affixes) {
            affix.removeFromItem(stack);
        }
    }
    
    /**
     * 获取物品的所有词缀
     */
    public static List<Affix> getAffixes(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptyList();
        }
        return Affix.getAffixesFromItem(stack);
    }
    
    /**
     * 获取物品的词缀提示文本
     */
    public static List<Text> getAffixTooltips(ItemStack stack) {
        // 检查配置是否允许显示词缀提示
        if (!JustDying.getConfig().affixes.showAffixTooltips) {
            return Collections.emptyList();
        }
        
        List<Affix> affixes = getAffixes(stack);
        
        if (affixes.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Text> tooltips = new ArrayList<>();
        for (Affix affix : affixes) {
            tooltips.addAll(affix.getTooltip());
        }
        
        return tooltips;
    }
    
    /**
     * 处理攻击事件
     */
    public static void handleAttack(LivingEntity attacker, LivingEntity target, ItemStack weapon) {
        if (attacker == null || target == null || weapon.isEmpty()) {
            return;
        }
        
        try {
            List<Affix> affixes = getAffixes(weapon);
            
            for (Affix affix : affixes) {
                if (affix == null) continue;
                
                // 处理攻击时触发的效果
                for (AffixEffect effect : affix.getEffects()) {
                    if (effect == null) continue;
                    
                    if (effect.getTrigger() == AffixEffectTrigger.ON_HIT) {
                        // 根据几率触发效果
                        if (ThreadLocalRandom.current().nextFloat() < effect.getChance()) {
                            StatusEffectInstance statusEffect = effect.createEffectInstance();
                            if (statusEffect != null) {
                                target.addStatusEffect(statusEffect);
                                
                                JustDying.AFFIX_LOGGER.debug("Applied effect {} to target from affix {}", 
                                        statusEffect.getEffectType().getName().getString(), affix.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("处理攻击事件时出错", e);
        }
    }
    
    /**
     * 处理受伤事件
     */
    public static void handleHurt(LivingEntity entity, ItemStack armor) {
        if (entity == null || armor.isEmpty()) {
            return;
        }
        
        try {
            List<Affix> affixes = getAffixes(armor);
            
            for (Affix affix : affixes) {
                if (affix == null) continue;
                
                // 处理受伤时触发的效果
                for (AffixEffect effect : affix.getEffects()) {
                    if (effect == null) continue;
                    
                    if (effect.getTrigger() == AffixEffectTrigger.ON_HURT) {
                        // 根据几率触发效果
                        if (ThreadLocalRandom.current().nextFloat() < effect.getChance()) {
                            StatusEffectInstance statusEffect = effect.createEffectInstance();
                            if (statusEffect != null) {
                                entity.addStatusEffect(statusEffect);
                                
                                JustDying.AFFIX_LOGGER.debug("Applied effect {} to self from affix {}", 
                                        statusEffect.getEffectType().getName().getString(), affix.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("处理受伤事件时出错", e);
        }
    }
    
    /**
     * 应用被动效果
     */
    public static void applyPassiveEffects(LivingEntity entity, ItemStack stack) {
        if (entity == null || stack.isEmpty()) {
            return;
        }
        
        try {
            List<Affix> affixes = getAffixes(stack);
            
            for (Affix affix : affixes) {
                if (affix == null) continue;
                
                // 处理被动效果
                for (AffixEffect effect : affix.getEffects()) {
                    if (effect == null) continue;
                    
                    if (effect.getTrigger() == AffixEffectTrigger.PASSIVE) {
                        StatusEffectInstance statusEffect = effect.createEffectInstance();
                        if (statusEffect != null) {
                            entity.addStatusEffect(statusEffect);
                        }
                    }
                }
            }
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("应用被动效果时出错", e);
        }
    }
    
    /**
     * 检查是否应该掉落带有词缀的物品
     */
    public static boolean shouldDropWithAffix() {
        float chance = JustDying.getConfig().affixes.affixDropChance;
        return ThreadLocalRandom.current().nextFloat() < (chance / 100.0f);
    }
    
    /**
     * 重新加载词缀配置
     */
    public static void reload() {
        try {
            AffixRegistry.reload();
        } catch (Exception e) {
            JustDying.AFFIX_LOGGER.error("无法重新加载词缀配置", e);
        }
    }
    
    /**
     * 测试方法：打印物品属性
     * 
     * @param stack 物品堆
     * @param slot 装备槽位
     */
    public static void printItemAttributes(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || slot == null) {
            JustDying.LOGGER.info("物品为空或槽位为null，无法打印属性");
            return;
        }
        
        JustDying.LOGGER.info("========== 物品属性信息 ==========");
        JustDying.LOGGER.info("物品: {}", stack.getItem().getName().getString());
        JustDying.LOGGER.info("槽位: {}", slot.getName());
        
        Multimap<EntityAttribute, EntityAttributeModifier> attributes = stack.getAttributeModifiers(slot);
        JustDying.LOGGER.info("属性数量: {}", attributes.size());
        
        for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : attributes.entries()) {
            JustDying.LOGGER.info("  属性: {} = {} ({})", 
                    entry.getKey().getTranslationKey(), 
                    entry.getValue().getValue(), 
                    entry.getValue().getOperation());
        }
        
        JustDying.LOGGER.info("===================================");
    }

    public static void updateItemAttributes(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        // 确定装备槽位
        EquipmentSlot slot = Affix.getAppropriateSlot(stack);
        if (slot == null) {
            return;
        }
        
        // 打印原始物品属性
        JustDying.LOGGER.info("原始物品属性:");
        // printItemAttributes(stack, slot);
        
        // 保存原始NBT数据
        NbtCompound originalNbt = null;
        if (stack.hasNbt()) {
            originalNbt = stack.getNbt().copy();
            JustDying.LOGGER.info("保存了原始NBT数据: {}", originalNbt.toString());
        }
        
        // 创建一个新的物品副本，保留原始物品的所有数据（如附魔等）
        ItemStack cleanStack = stack.copy();
        
        // 移除词缀相关的NBT数据，但保留其他数据
        if (cleanStack.hasNbt()) {
            NbtCompound nbt = cleanStack.getNbt();
            // 只移除词缀列表，保留其他NBT数据
            if (nbt.contains(Affix.AFFIX_NBT_KEY)) {
                nbt.remove(Affix.AFFIX_NBT_KEY);
                JustDying.LOGGER.info("从副本中移除了词缀NBT数据");
            }
            // 移除属性修饰符，稍后会重新添加
            nbt.remove("AttributeModifiers");
            JustDying.LOGGER.info("从副本中移除了属性修饰符NBT数据");
        }
        
        // 获取基础物品的属性修饰符（包含附魔等效果，但不包含词缀）
        Multimap<EntityAttribute, EntityAttributeModifier> baseModifiers = cleanStack.getAttributeModifiers(slot);
        
        // 打印清理后的物品属性
        JustDying.LOGGER.info("清理后的基础物品属性:");
        // printItemAttributes(cleanStack, slot);
        
        // 记录基础属性
        JustDying.AFFIX_LOGGER.debug("Base attributes for item {}:", stack.getItem().getName().getString());
        for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : baseModifiers.entries()) {
            JustDying.AFFIX_LOGGER.debug("  {} = {} ({})", 
                    entry.getKey().getTranslationKey(), 
                    entry.getValue().getValue(), 
                    entry.getValue().getOperation());
        }
        
        // 创建一个新的Map来存储合并后的修饰符
        Map<EntityAttribute, Map<EntityAttributeModifier.Operation, Double>> mergedModifiers = new HashMap<>();
        
        // 添加基础属性到合并Map
        for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : baseModifiers.entries()) {
            EntityAttribute attribute = entry.getKey();
            EntityAttributeModifier modifier = entry.getValue();
            
            // 获取或创建该属性的操作类型映射
            Map<EntityAttributeModifier.Operation, Double> operationMap = mergedModifiers.computeIfAbsent(
                attribute, k -> new HashMap<>());
            
            // 获取该操作类型的当前值，如果不存在则为0
            double currentValue = operationMap.getOrDefault(modifier.getOperation(), 0.0);
            
            // 合并值
            operationMap.put(modifier.getOperation(), currentValue + modifier.getValue());
        }
        
        // 恢复原始NBT数据
        if (originalNbt != null) {
            stack.setNbt(originalNbt);
            JustDying.LOGGER.info("恢复了原始NBT数据");
        }
        
        // 获取物品的所有词缀
        List<Affix> affixes = Affix.getAffixesFromItem(stack);
        if (!affixes.isEmpty()) {
            JustDying.LOGGER.info("找到 {} 个词缀", affixes.size());
            JustDying.AFFIX_LOGGER.debug("Found {} affixes on item {}", affixes.size(), stack.getItem().getName().getString());
            
            // 收集所有词缀的属性修饰符
            for (Affix affix : affixes) {
                JustDying.LOGGER.info("处理词缀: {}", affix.getName());
                JustDying.AFFIX_LOGGER.debug("Processing affix: {}", affix.getName());
                
                for (AffixAttribute attribute : affix.getAttributes()) {
                    if (attribute == null) continue;
                    
                    // 只处理攻击伤害、护甲等直接显示在物品上的属性
                    // Identifier attributeId = attribute.getAttributeId();
                    EntityAttribute entityAttribute = attribute.getAttribute();
                    
                    if (entityAttribute != null) {
                        // 获取或创建该属性的操作类型映射
                        Map<EntityAttributeModifier.Operation, Double> operationMap = mergedModifiers.computeIfAbsent(
                            entityAttribute, k -> new HashMap<>());
                        
                        // 获取该操作类型的当前值，如果不存在则为0
                        double currentValue = operationMap.getOrDefault(attribute.getOperation(), 0.0);
                        
                        // 合并值
                        operationMap.put(attribute.getOperation(), currentValue + attribute.getAmount());
                        
                        JustDying.LOGGER.info("  添加属性 {} = {} ({}) 来自词缀 {}", 
                                entityAttribute.getTranslationKey(), 
                                attribute.getAmount(), 
                                attribute.getOperation(),
                                affix.getName());
                        
                        JustDying.AFFIX_LOGGER.debug("  Added attribute {} = {} ({}) from affix {}", 
                                entityAttribute.getTranslationKey(), 
                                attribute.getAmount(), 
                                attribute.getOperation(),
                                affix.getName());
                    }
                }
            }
        } else {
            JustDying.LOGGER.info("物品上没有词缀");
            JustDying.AFFIX_LOGGER.debug("No affixes found on item {}", stack.getItem().getName().getString());
        }
        
        // 完全清除物品的NBT属性数据
        if (stack.hasNbt()) {
            stack.getNbt().remove("AttributeModifiers");
            JustDying.LOGGER.info("清除了物品的属性修饰符NBT数据");
        }
        
        // 创建一个新的Multimap来存储最终的修饰符
        Multimap<EntityAttribute, EntityAttributeModifier> finalModifiers = HashMultimap.create();
        
        // 应用合并后的修饰符
        int totalModifiers = 0;
        for (Map.Entry<EntityAttribute, Map<EntityAttributeModifier.Operation, Double>> attributeEntry : mergedModifiers.entrySet()) {
            EntityAttribute attribute = attributeEntry.getKey();
            
            for (Map.Entry<EntityAttributeModifier.Operation, Double> operationEntry : attributeEntry.getValue().entrySet()) {
                EntityAttributeModifier.Operation operation = operationEntry.getKey();
                double value = operationEntry.getValue();
                
                // 创建唯一的UUID
                UUID uuid = UUID.randomUUID();
                
                // 创建属性修饰符名称
                String modifierName = "Merged_" + attribute.getTranslationKey() + "_" + operation.name();
                
                // 创建合并后的修饰符
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    uuid,
                    modifierName,
                    value,
                    operation
                );
                
                // 添加到最终修饰符集合
                finalModifiers.put(attribute, modifier);
                
                // 应用到物品
                stack.addAttributeModifier(attribute, modifier, slot);
                
                totalModifiers++;
                
                JustDying.LOGGER.info("应用合并后的属性: {} = {} ({})", 
                        attribute.getTranslationKey(), 
                        value, 
                        operation);
            }
        }
        
        JustDying.LOGGER.info("应用了 {} 个合并后的属性修饰符到物品", totalModifiers);
        
        // 记录最终属性
        JustDying.LOGGER.info("最终物品属性:");
        printItemAttributes(stack, slot);
        
        JustDying.AFFIX_LOGGER.debug("Final attributes for item {}:", stack.getItem().getName().getString());
        for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : finalModifiers.entries()) {
            JustDying.AFFIX_LOGGER.debug("  {} = {} ({})", 
                    entry.getKey().getTranslationKey(), 
                    entry.getValue().getValue(), 
                    entry.getValue().getOperation());
        }
    }
    
    /**
     * 移除物品上所有词缀相关的属性修饰符
     * 
     * @param stack 物品堆
     */
    public static void removeAllAffixModifiers(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        // 确定装备槽位
        EquipmentSlot slot = Affix.getAppropriateSlot(stack);
        if (slot == null) {
            return;
        }
        
        // 打印原始物品属性
        JustDying.LOGGER.info("移除词缀前的物品属性:");
        printItemAttributes(stack, slot);
        
        // 保存原始NBT数据
        NbtCompound originalNbt = null;
        if (stack.hasNbt()) {
            originalNbt = stack.getNbt().copy();
            JustDying.LOGGER.info("保存了原始NBT数据: {}", originalNbt.toString());
        }
        
        // 创建一个新的物品副本，保留原始物品的所有数据（如附魔等）
        ItemStack cleanStack = stack.copy();
        
        // 移除词缀相关的NBT数据，但保留其他数据
        if (cleanStack.hasNbt()) {
            NbtCompound nbt = cleanStack.getNbt();
            // 只移除词缀列表，保留其他NBT数据
            if (nbt.contains(Affix.AFFIX_NBT_KEY)) {
                nbt.remove(Affix.AFFIX_NBT_KEY);
                JustDying.LOGGER.info("从副本中移除了词缀NBT数据");
            }
            // 移除属性修饰符，稍后会重新添加
            nbt.remove("AttributeModifiers");
            JustDying.LOGGER.info("从副本中移除了属性修饰符NBT数据");
        }
        
        // 获取基础物品的属性修饰符（包含附魔等效果，但不包含词缀）
        Multimap<EntityAttribute, EntityAttributeModifier> baseModifiers = cleanStack.getAttributeModifiers(slot);
        
        // 打印清理后的物品属性
        JustDying.LOGGER.info("清理后的基础物品属性:");
        printItemAttributes(cleanStack, slot);
        
        // 记录基础属性
        JustDying.AFFIX_LOGGER.debug("Restoring base attributes for item {}:", stack.getItem().getName().getString());
        for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : baseModifiers.entries()) {
            JustDying.AFFIX_LOGGER.debug("  {} = {} ({})", 
                    entry.getKey().getTranslationKey(), 
                    entry.getValue().getValue(), 
                    entry.getValue().getOperation());
        }
        
        // 创建一个新的Map来存储合并后的修饰符
        Map<EntityAttribute, Map<EntityAttributeModifier.Operation, Double>> mergedModifiers = new HashMap<>();
        
        // 添加基础属性到合并Map
        for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : baseModifiers.entries()) {
            EntityAttribute attribute = entry.getKey();
            EntityAttributeModifier modifier = entry.getValue();
            
            // 获取或创建该属性的操作类型映射
            Map<EntityAttributeModifier.Operation, Double> operationMap = mergedModifiers.computeIfAbsent(
                attribute, k -> new HashMap<>());
            
            // 获取该操作类型的当前值，如果不存在则为0
            double currentValue = operationMap.getOrDefault(modifier.getOperation(), 0.0);
            
            // 合并值
            operationMap.put(modifier.getOperation(), currentValue + modifier.getValue());
        }
        
        // 完全清除物品的NBT属性数据
        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();
            nbt.remove("AttributeModifiers");
            JustDying.LOGGER.info("清除了物品的属性修饰符NBT数据");
            
            // 同时移除词缀列表
            if (nbt.contains(Affix.AFFIX_NBT_KEY)) {
                nbt.remove(Affix.AFFIX_NBT_KEY);
                JustDying.LOGGER.info("移除了物品的词缀列表");
                JustDying.AFFIX_LOGGER.debug("Removed affix list from item {}", stack.getItem().getName().getString());
            }
        }
        
        // 应用合并后的修饰符
        int totalModifiers = 0;
        for (Map.Entry<EntityAttribute, Map<EntityAttributeModifier.Operation, Double>> attributeEntry : mergedModifiers.entrySet()) {
            EntityAttribute attribute = attributeEntry.getKey();
            
            for (Map.Entry<EntityAttributeModifier.Operation, Double> operationEntry : attributeEntry.getValue().entrySet()) {
                EntityAttributeModifier.Operation operation = operationEntry.getKey();
                double value = operationEntry.getValue();
                
                // 创建唯一的UUID
                UUID uuid = UUID.randomUUID();
                
                // 创建属性修饰符名称
                String modifierName = "Merged_" + attribute.getTranslationKey() + "_" + operation.name();
                
                // 创建合并后的修饰符
                EntityAttributeModifier modifier = new EntityAttributeModifier(
                    uuid,
                    modifierName,
                    value,
                    operation
                );
                
                // 应用到物品
                stack.addAttributeModifier(attribute, modifier, slot);
                
                totalModifiers++;
                
                JustDying.LOGGER.info("应用合并后的属性: {} = {} ({})", 
                        attribute.getTranslationKey(), 
                        value, 
                        operation);
            }
        }
        
        JustDying.LOGGER.info("应用了 {} 个合并后的属性修饰符到物品", totalModifiers);
        
        // 打印最终物品属性
        JustDying.LOGGER.info("移除词缀后的最终物品属性:");
        printItemAttributes(stack, slot);
        
        JustDying.AFFIX_LOGGER.debug("Removed all affix modifiers from item {}, restored {} base modifiers", 
                stack.getItem().getName().getString(), baseModifiers.size());
    }
} 