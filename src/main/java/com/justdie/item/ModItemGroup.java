package com.justdie.item;

import com.justdie.JustDying;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 模组物品组注册
 */
public class ModItemGroup {
    
    // 创建物品组标识符
    private static final Identifier ITEM_GROUP_ID = new Identifier(JustDying.MOD_ID, "items");
    
    // 创建模组物品组
    private static final RegistryKey<ItemGroup> MOD_ITEM_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            ITEM_GROUP_ID
    );
    
    // 物品组显示名称，只加载一次
    private static final Text ITEM_GROUP_NAME = Text.translatable("itemGroup.justdying.items");
    
    /**
     * 注册物品组
     */
    public static void register() {
        // 注册模组物品组
        Registry.register(Registries.ITEM_GROUP, MOD_ITEM_GROUP, FabricItemGroup.builder()
                .displayName(ITEM_GROUP_NAME)
                .icon(() -> new ItemStack(ModItems.MOD_ICON)) // 使用模组图标作为物品组图标
                .build());
        
        // 将属性强化物品添加到模组物品组
        ItemGroupEvents.modifyEntriesEvent(MOD_ITEM_GROUP).register(content -> {
            // 使用批量添加，减少重复代码
            content.add(AttributeCapItems.CONSTITUTION_CAP_ITEM);
            content.add(AttributeCapItems.STRENGTH_CAP_ITEM);
            content.add(AttributeCapItems.DEFENSE_CAP_ITEM);
            content.add(AttributeCapItems.SPEED_CAP_ITEM);
            content.add(AttributeCapItems.LUCK_CAP_ITEM);
        });
        
        // 只在调试模式下显示详细信息，减少日志输出
        if (JustDying.getConfig().debug) {
            JustDying.LOGGER.info("注册了JustDying模组物品组");
        } else {
            JustDying.LOGGER.debug("注册了物品组"); // 改为debug级别
        }
    }
} 