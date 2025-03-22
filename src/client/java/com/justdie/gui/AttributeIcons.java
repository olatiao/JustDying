package com.justdie.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import com.justdie.JustDying;
import com.justdie.attribute.JustDyingAttributeType;

/**
 * 属性图标渲染器
 * 用于在GUI中渲染不同属性的图标
 */
public class AttributeIcons {
    // 使用Minecraft原版纹理
    public static final Identifier ICONS_TEXTURE = new Identifier("textures/gui/icons.png");
    
    // 图标尺寸
    public static final int ICON_SIZE = 16;

    /**
     * 渲染属性图标（通过属性ID）
     * 
     * @param context   绘制上下文
     * @param attributeId 属性ID
     * @param x         X坐标
     * @param y         Y坐标
     */
    public static void renderIcon(DrawContext context, String attributeId, int x, int y) {
        // 使用物品图标替代纹理图标
        ItemStack itemStack = getIconItemForAttribute(attributeId);
        context.drawItem(itemStack, x, y);
    }
    
    /**
     * 渲染属性图标（通过属性类型枚举）
     * 
     * @param context      绘制上下文
     * @param attributeType 属性类型
     * @param x            X坐标
     * @param y            Y坐标
     */
    public static void renderIcon(DrawContext context, JustDyingAttributeType attributeType, int x, int y) {
        // 使用物品图标替代纹理图标
        ItemStack itemStack = getIconItemForAttribute(attributeType);
        context.drawItem(itemStack, x, y);
    }

    /**
     * 获取属性对应的物品图标（通过属性ID）
     * 
     * @param attributeId 属性ID
     * @return 代表该属性的物品堆栈
     */
    private static ItemStack getIconItemForAttribute(String attributeId) {
        // 使用哈希值来确定图标物品，确保相同的属性ID总是使用相同的图标
        int iconIndex = Math.abs(attributeId.hashCode() % 8);
        return getIconItemByIndex(iconIndex);
    }
    
    /**
     * 获取属性对应的物品图标（通过属性类型枚举）
     * 
     * @param attributeType 属性类型
     * @return 代表该属性的物品堆栈
     */
    private static ItemStack getIconItemForAttribute(JustDyingAttributeType attributeType) {
        switch (attributeType) {
            case CONSTITUTION:
                return new ItemStack(Items.APPLE);
            case STRENGTH:
                return new ItemStack(Items.IRON_SWORD);
            case DEFENSE:
                return new ItemStack(Items.SHIELD);
            case SPEED:
                return new ItemStack(Items.FEATHER);
            case LUCK:
                return new ItemStack(Items.GOLD_INGOT);
            default:
                // 对于未知的属性类型，返回一个默认物品
                return new ItemStack(Items.BOOK);
        }
    }
    
    /**
     * 根据索引获取默认的物品图标
     * 
     * @param index 索引值 (0-7)
     * @return 物品堆栈
     */
    private static ItemStack getIconItemByIndex(int index) {
        switch (index) {
            case 0:
                return new ItemStack(Items.APPLE);
            case 1:
                return new ItemStack(Items.IRON_SWORD);
            case 2:
                return new ItemStack(Items.SHIELD);
            case 3:
                return new ItemStack(Items.FEATHER);
            case 4:
                return new ItemStack(Items.GOLD_INGOT);
            case 5:
                return new ItemStack(Items.POTION);
            case 6:
                return new ItemStack(Items.ENDER_PEARL);
            case 7:
                return new ItemStack(Items.EXPERIENCE_BOTTLE);
            default:
                return new ItemStack(Items.BOOK);
        }
    }
    
    /**
     * 获取等级兑换图标
     * 
     * @return 代表等级兑换的物品堆栈
     */
    public static ItemStack getLevelExchangeIcon() {
        return new ItemStack(Items.EXPERIENCE_BOTTLE);
    }
}