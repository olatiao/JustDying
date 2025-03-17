package com.justdie.boss.client.renderer;

import com.justdie.JustDying;
import com.justdie.boss.client.model.SorcererBossModel;
import com.justdie.boss.entity.SorcererBossEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 法师BOSS的GeckoLib渲染器
 */
public class SorcererBossRenderer extends GeoEntityRenderer<SorcererBossEntity> {
    private static final Identifier TEXTURE = new Identifier(JustDying.MOD_ID, "textures/entity/sorcerer_boss.png");

    public SorcererBossRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SorcererBossModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public Identifier getTextureLocation(SorcererBossEntity entity) {
        return TEXTURE;
    }
} 