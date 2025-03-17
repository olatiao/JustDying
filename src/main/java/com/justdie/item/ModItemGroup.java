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

import java.util.Collection;

/**
 * 模组物品组注册
 */
public class ModItemGroup {
    // 物品组相关常量
    private static final Identifier MOD_GROUP_ID = JustDying.id("items");
    private static final RegistryKey<ItemGroup> MOD_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, MOD_GROUP_ID);
    private static final Text MOD_GROUP_NAME = Text.translatable("itemGroup.justdying.items");
    
    // 预计算的物品组图标，避免每次请求时创建新对象
    private static final ItemStack GROUP_ICON = new ItemStack(ModItems.MOD_ICON);
    
    /**
     * 注册物品组
     */
    public static void register() {
        // 注册模组物品组
        Registry.register(
            Registries.ITEM_GROUP, 
            MOD_GROUP_KEY, 
            FabricItemGroup.builder()
                .displayName(MOD_GROUP_NAME)
                .icon(() -> GROUP_ICON) // 使用预计算的物品组图标
                .build()
        );
        
        // 添加物品组内容
        registerGroupItems();
    }
    
    /**
     * 注册物品组内容
     */
    private static void registerGroupItems() {
        ItemGroupEvents.modifyEntriesEvent(MOD_GROUP_KEY).register(content -> {
            // 获取所有已注册的属性上限增加物品
            Collection<AttributeCapItem> items = AttributeCapItems.getRegisteredItems().values();
            
            // 添加所有属性上限增加物品到物品组
            if (!items.isEmpty()) {
                items.forEach(content::add);
                
                // 记录日志
                logItemGroupRegistration(items.size());
            }
        });
    }
    
    /**
     * 记录物品组注册日志
     */
    private static void logItemGroupRegistration(int itemCount) {
        boolean isDebugEnabled = JustDying.getConfig().debug;
        
        if (itemCount > 0) {
            if (isDebugEnabled) {
                JustDying.LOGGER.info("注册了JustDying模组物品组，包含 {} 个属性物品", itemCount);
            } else {
                JustDying.LOGGER.debug("注册了物品组，包含 {} 个物品", itemCount);
            }
        } else {
            JustDying.LOGGER.debug("注册了物品组（无物品）");
        }
    }
} 