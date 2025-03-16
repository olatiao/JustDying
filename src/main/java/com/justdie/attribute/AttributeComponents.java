package com.justdie.attribute;

import com.justdie.JustDying;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 属性组件注册
 */
public class AttributeComponents implements EntityComponentInitializer {
    /**
     * 玩家属性组件的键
     */
    public static final ComponentKey<PlayerAttributeComponent> PLAYER_ATTRIBUTES = 
            ComponentRegistry.getOrCreate(new Identifier(JustDying.MOD_ID, "player_attributes"), PlayerAttributeComponent.class);
    
    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // 注册玩家属性组件
        registry.registerFor(PlayerEntity.class, PLAYER_ATTRIBUTES, player -> new PlayerAttributeComponentImpl(player));
    }
} 