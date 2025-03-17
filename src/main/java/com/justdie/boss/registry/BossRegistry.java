package com.justdie.boss.registry;

import com.justdie.boss.entity.SorcererBossEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 注册BOSS实体和相关内容
 */
public class BossRegistry {
    // 法师BOSS实体类型
    public static final EntityType<SorcererBossEntity> SORCERER_BOSS = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier("justdie", "sorcerer_boss"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SorcererBossEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(1)
            .forceTrackedVelocityUpdates(true)
            .build()
    );
    
    /**
     * 注册所有实体
     */
    public static void registerEntities() {
        // 实体类型已在静态字段初始化时注册
    }
    
    /**
     * 注册所有实体属性
     */
    public static void registerAttributes() {
        // 注册法师BOSS属性
        FabricDefaultAttributeRegistry.register(SORCERER_BOSS, SorcererBossEntity.createBossAttributes());
    }
} 