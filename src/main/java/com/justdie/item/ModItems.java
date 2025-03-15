package com.justdie.item;

import com.justdie.JustDying;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 模组物品管理类
 */
public class ModItems {
    
    // 预先创建物品标识符，避免重复创建
    private static final Identifier MOD_ICON_ID = new Identifier(JustDying.MOD_ID, "mod_icon");
    
    // 模组图标物品，仅用于物品组显示，不在游戏中展示
    public static final Item MOD_ICON = new Item(
        new FabricItemSettings()
            .maxCount(1)
            .fireproof()
    );
    
    /**
     * 注册所有模组物品
     */
    public static void register() {
        // 注册模组图标物品（不在创造模式物品栏中显示）
        Registry.register(Registries.ITEM, MOD_ICON_ID, MOD_ICON);
        
        if (JustDying.getConfig().debug) {
            JustDying.LOGGER.info("注册了模组物品");
        }
    }
} 