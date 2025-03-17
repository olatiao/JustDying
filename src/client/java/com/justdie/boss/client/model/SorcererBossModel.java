package com.justdie.boss.client.model;

import com.justdie.JustDying;
import com.justdie.boss.entity.SorcererBossEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * 法师BOSS的GeckoLib模型
 */
public class SorcererBossModel extends GeoModel<SorcererBossEntity> {
    private static final Identifier MODEL_ID = new Identifier(JustDying.MOD_ID, "geo/entity/sorcerer_boss.geo.json");
    private static final Identifier TEXTURE_ID = new Identifier(JustDying.MOD_ID, "textures/entity/sorcerer_boss.png");
    private static final Identifier ANIMATION_ID = new Identifier(JustDying.MOD_ID, "animations/sorcerer_boss.animation.json");

    @Override
    public Identifier getModelResource(SorcererBossEntity entity) {
        return MODEL_ID;
    }

    @Override
    public Identifier getTextureResource(SorcererBossEntity entity) {
        return TEXTURE_ID;
    }

    @Override
    public Identifier getAnimationResource(SorcererBossEntity entity) {
        return ANIMATION_ID;
    }
} 