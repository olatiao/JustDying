package com.justdie.affix;

import com.justdie.JustDying;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ArmorItem;
import net.minecraft.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/**
 * 表示物品词缀的基类
 */
public class Affix {
    // NBT标签常量
    public static final String AFFIX_NBT_KEY = "JustDyingAffixes";
    public static final String AFFIX_ID_KEY = "id";
    public static final String AFFIX_TYPE_KEY = "type";
    public static final String AFFIX_ITEM_TYPE_KEY = "itemType";
    
    private final Identifier id;
    private final String name;
    private final Formatting formatting;
    private final List<AffixAttribute> attributes;
    private final List<AffixEffect> effects;
    private UUID uuid;
    private String itemType = "ANY"; // 默认为任何类型
    
    /**
     * 创建一个新的词缀
     * 
     * @param id 词缀ID
     * @param name 词缀名称
     * @param formatting 词缀格式化（颜色）
     */
    public Affix(Identifier id, String name, Formatting formatting) {
        this.id = id;
        this.name = name;
        this.formatting = formatting;
        this.attributes = new ArrayList<>();
        this.effects = new ArrayList<>();
        this.uuid = UUID.randomUUID();
    }
    
    /**
     * 从NBT数据创建词缀
     * 
     * @param nbt NBT数据
     * @return 词缀实例
     */
    public static Affix fromNbt(NbtCompound nbt) {
        Identifier id = new Identifier(nbt.getString("id"));
        String name = nbt.getString("name");
        Formatting formatting = Formatting.byName(nbt.getString("formatting"));
        
        Affix affix = new Affix(id, name, formatting);
        
        // 读取UUID
        if (nbt.contains("uuid")) {
            affix.uuid = UUID.fromString(nbt.getString("uuid"));
        }
        
        // 读取物品类型
        if (nbt.contains("itemType")) {
            affix.itemType = nbt.getString("itemType");
        }
        
        // 读取属性列表
        if (nbt.contains("attributes", NbtElement.LIST_TYPE)) {
            NbtList attributesList = nbt.getList("attributes", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < attributesList.size(); i++) {
                NbtCompound attributeNbt = attributesList.getCompound(i);
                AffixAttribute attribute = AffixAttribute.fromNbt(attributeNbt);
                affix.attributes.add(attribute);
            }
        }
        
        // 读取效果列表
        if (nbt.contains("effects", NbtElement.LIST_TYPE)) {
            NbtList effectsList = nbt.getList("effects", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < effectsList.size(); i++) {
                NbtCompound effectNbt = effectsList.getCompound(i);
                AffixEffect effect = AffixEffect.fromNbt(effectNbt);
                affix.effects.add(effect);
            }
        }
        
        return affix;
    }
    
    /**
     * 将词缀转换为NBT数据
     * 
     * @return NBT数据
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", id.toString());
        nbt.putString("name", name);
        nbt.putString("formatting", formatting.getName());
        nbt.putString("uuid", uuid.toString());
        nbt.putString("itemType", itemType);
        
        // 保存属性列表
        NbtList attributesList = new NbtList();
        for (AffixAttribute attribute : attributes) {
            attributesList.add(attribute.toNbt());
        }
        nbt.put("attributes", attributesList);
        
        // 保存效果列表
        NbtList effectsList = new NbtList();
        for (AffixEffect effect : effects) {
            effectsList.add(effect.toNbt());
        }
        nbt.put("effects", effectsList);
        
        return nbt;
    }
    
    /**
     * 设置词缀适用的物品类型
     * 
     * @param itemType 物品类型
     * @return 当前词缀实例
     */
    public Affix setItemType(String itemType) {
        this.itemType = itemType;
        return this;
    }
    
    /**
     * 获取词缀适用的物品类型
     * 
     * @return 物品类型
     */
    public String getItemType() {
        return itemType;
    }
    
    /**
     * 检查词缀是否适用于指定的物品类型
     * 
     * @param type 物品类型
     * @return 是否适用
     */
    public boolean isApplicableTo(String type) {
        return "ANY".equals(itemType) || itemType.equals(type);
    }
    
    /**
     * 添加属性到词缀
     * 
     * @param attribute 属性
     * @return 当前词缀实例
     */
    public Affix addAttribute(AffixAttribute attribute) {
        this.attributes.add(attribute);
        return this;
    }
    
    /**
     * 添加效果到词缀
     * 
     * @param effect 效果
     * @return 当前词缀实例
     */
    public Affix addEffect(AffixEffect effect) {
        this.effects.add(effect);
        return this;
    }
    
    /**
     * 获取词缀ID
     * 
     * @return 词缀ID
     */
    public Identifier getId() {
        return id;
    }
    
    /**
     * 获取词缀名称
     * 
     * @return 词缀名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取词缀格式化（颜色）
     * 
     * @return 词缀格式化
     */
    public Formatting getFormatting() {
        return formatting;
    }
    
    /**
     * 获取词缀UUID
     * 
     * @return 词缀UUID
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * 获取词缀属性列表
     * 
     * @return 属性列表
     */
    public List<AffixAttribute> getAttributes() {
        return attributes;
    }
    
    /**
     * 获取词缀效果列表
     * 
     * @return 效果列表
     */
    public List<AffixEffect> getEffects() {
        return effects;
    }
    
    /**
     * 应用词缀到物品
     * 
     * @param stack 物品堆
     */
    public void applyToItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        // 获取或创建物品的NBT数据
        NbtCompound nbt = stack.getOrCreateNbt();
        
        // 获取或创建词缀列表
        NbtList affixList;
        if (nbt.contains(AFFIX_NBT_KEY, NbtElement.LIST_TYPE)) {
            affixList = nbt.getList(AFFIX_NBT_KEY, NbtElement.COMPOUND_TYPE);
        } else {
            affixList = new NbtList();
            nbt.put(AFFIX_NBT_KEY, affixList);
        }
        
        // 检查是否已经存在相同的词缀
        for (int i = 0; i < affixList.size(); i++) {
            NbtCompound affixNbt = affixList.getCompound(i);
            if (affixNbt.getString("id").equals(id.toString())) {
                // 已存在相同词缀，不重复添加
                return;
            }
        }
        
        // 添加词缀到列表
        affixList.add(toNbt());
        
        // 更新物品属性
        AffixManager.updateItemAttributes(stack);
        
        JustDying.AFFIX_LOGGER.debug("Applied affix {} to item {}", 
                id, stack.getItem().getName().getString());
    }
    
    /**
     * 从物品中移除词缀
     * 
     * @param stack 物品堆
     * @return 是否成功移除
     */
    public boolean removeFromItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasNbt()) {
            return false;
        }
        
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(AFFIX_NBT_KEY, NbtElement.LIST_TYPE)) {
            return false;
        }
        
        NbtList affixList = nbt.getList(AFFIX_NBT_KEY, NbtElement.COMPOUND_TYPE);
        boolean removed = false;
        
        // 查找并移除词缀
        for (int i = 0; i < affixList.size(); i++) {
            NbtCompound affixNbt = affixList.getCompound(i);
            if (affixNbt.getString("id").equals(id.toString())) {
                affixList.remove(i);
                removed = true;
                break;
            }
        }
        
        // 如果词缀列表为空，移除整个标签
        if (affixList.isEmpty()) {
            nbt.remove(AFFIX_NBT_KEY);
        }
        
        // 如果成功移除词缀，更新物品属性
        if (removed) {
            // 重新应用所有剩余词缀的属性
            AffixManager.updateItemAttributes(stack);
            JustDying.AFFIX_LOGGER.debug("Removed affix {} from item {}", 
                    id, stack.getItem().getName().getString());
        }
        
        return removed;
    }
    
    /**
     * 获取物品上的所有词缀
     * 
     * @param stack 物品堆
     * @return 词缀列表
     */
    public static List<Affix> getAffixesFromItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasNbt()) {
            return Collections.emptyList();
        }
        
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(AFFIX_NBT_KEY, NbtElement.LIST_TYPE)) {
            return Collections.emptyList();
        }
        
        NbtList affixList = nbt.getList(AFFIX_NBT_KEY, NbtElement.COMPOUND_TYPE);
        if (affixList.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Affix> affixes = new ArrayList<>();
        for (int i = 0; i < affixList.size(); i++) {
            NbtCompound affixNbt = affixList.getCompound(i);
            Affix affix = fromNbt(affixNbt);
            affixes.add(affix);
        }
        
        return affixes;
    }
    
    /**
     * 获取词缀的显示文本
     * 
     * @return 显示文本
     */
    public MutableText getDisplayText() {
        return Text.literal(name).formatted(formatting);
    }
    
    /**
     * 获取词缀的提示文本列表
     * 
     * @return 提示文本列表
     */
    public List<Text> getTooltip() {
        List<Text> tooltip = new ArrayList<>();
        
        // 添加词缀名称
        tooltip.add(getDisplayText());
        
        // 添加空行，使提示更清晰
        tooltip.add(Text.empty());
        
        // 创建一个集合来跟踪已经添加的属性ID，避免重复显示
        Set<String> addedAttributeIds = new HashSet<>();
        
        // 添加属性提示
        for (AffixAttribute attribute : attributes) {
            // 检查是否已经添加过相同类型的属性
            String attributeId = attribute.getAttributeId().toString();
            if (!addedAttributeIds.contains(attributeId)) {
                tooltip.add(Text.literal("  ").append(attribute.getTooltip()));
                addedAttributeIds.add(attributeId);
            }
        }
        
        // 添加效果提示
        for (AffixEffect effect : effects) {
            tooltip.add(Text.literal("  ").append(effect.getTooltip()));
        }
        
        return tooltip;
    }
    
    /**
     * 根据物品类型确定适当的装备槽位
     * 
     * @param stack 物品堆
     * @return 装备槽位，如果不适用则返回null
     */
    public static EquipmentSlot getAppropriateSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        
        Item item = stack.getItem();
        
        // 检查是否为武器或工具
        if (item instanceof SwordItem || 
            item instanceof AxeItem || 
            item instanceof PickaxeItem || 
            item instanceof ShovelItem || 
            item instanceof HoeItem ||
            item == Items.BOW ||
            item == Items.CROSSBOW ||
            item == Items.TRIDENT) {
            return EquipmentSlot.MAINHAND;
        }
        
        // 检查是否为盾牌
        if (item == Items.SHIELD) {
            return EquipmentSlot.OFFHAND;
        }
        
        // 检查是否为防具
        if (item instanceof ArmorItem) {
            return ((ArmorItem) item).getSlotType();
        }
        
        return null;
    }
} 