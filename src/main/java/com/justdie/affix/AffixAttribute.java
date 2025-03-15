package com.justdie.affix;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * 词缀属性类，表示词缀提供的属性修饰符
 */
public class AffixAttribute {
    private final Identifier attributeId;
    private final EntityAttributeModifier.Operation operation;
    private final double amount;
    private UUID uuid;
    
    /**
     * 创建一个新的词缀属性
     * 
     * @param attributeId 属性ID
     * @param operation 操作类型
     * @param amount 数值
     */
    public AffixAttribute(Identifier attributeId, EntityAttributeModifier.Operation operation, double amount) {
        this.attributeId = attributeId;
        this.operation = operation;
        this.amount = amount;
        this.uuid = UUID.randomUUID();
    }
    
    /**
     * 从NBT数据创建词缀属性
     * 
     * @param nbt NBT数据
     * @return 词缀属性实例
     */
    public static AffixAttribute fromNbt(NbtCompound nbt) {
        Identifier attributeId = new Identifier(nbt.getString("attributeId"));
        EntityAttributeModifier.Operation operation = EntityAttributeModifier.Operation.fromId(nbt.getInt("operation"));
        double amount = nbt.getDouble("amount");
        
        AffixAttribute attribute = new AffixAttribute(attributeId, operation, amount);
        
        if (nbt.contains("uuid")) {
            attribute.uuid = UUID.fromString(nbt.getString("uuid"));
        }
        
        return attribute;
    }
    
    /**
     * 将词缀属性转换为NBT数据
     * 
     * @return NBT数据
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("attributeId", attributeId.toString());
        nbt.putInt("operation", operation.getId());
        nbt.putDouble("amount", amount);
        nbt.putString("uuid", uuid.toString());
        return nbt;
    }
    
    /**
     * 创建属性修饰符
     * 
     * @param name 修饰符名称
     * @return 属性修饰符
     */
    public EntityAttributeModifier createModifier(String name) {
        return new EntityAttributeModifier(UUID.randomUUID(), name, amount, operation);
    }
    
    /**
     * 获取属性ID
     * 
     * @return 属性ID
     */
    public Identifier getAttributeId() {
        return attributeId;
    }
    
    /**
     * 获取操作类型
     * 
     * @return 操作类型
     */
    public EntityAttributeModifier.Operation getOperation() {
        return operation;
    }
    
    /**
     * 获取数值
     * 
     * @return 数值
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * 获取UUID
     * 
     * @return UUID
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * 获取属性
     * 
     * @return 属性
     */
    public EntityAttribute getAttribute() {
        return Registries.ATTRIBUTE.get(attributeId);
    }
    
    /**
     * 获取提示信息
     * 
     * @return 提示信息
     */
    public Text getTooltip() {
        EntityAttribute attribute = getAttribute();
        if (attribute == null) {
            return Text.literal("未知属性").formatted(Formatting.RED);
        }
        
        String operationText;
        String sign = amount >= 0 ? "+" : "";
        
        switch (operation) {
            case ADDITION:
                if (amount == (int) amount) {
                    operationText = sign + (int) amount;
                } else {
                    operationText = sign + String.format("%.1f", amount);
                }
                break;
            case MULTIPLY_BASE:
                operationText = sign + (int) (amount * 100) + "%";
                break;
            case MULTIPLY_TOTAL:
                operationText = sign + (int) (amount * 100) + "%";
                break;
            default:
                if (amount == (int) amount) {
                    operationText = String.valueOf((int) amount);
                } else {
                    operationText = String.format("%.1f", amount);
                }
        }
        
        return Text.literal(operationText + " ")
                .formatted(amount >= 0 ? Formatting.BLUE : Formatting.RED)
                .append(Text.translatable(attribute.getTranslationKey()));
    }
} 