package com.justdie.boss.entity;

import com.justdie.boss.interfaces.IEntityTick;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * 法师BOSS实体
 */
public class SorcererBossEntity extends BaseBossEntity implements GeoAnimatable {
    // 追踪数据
    private static final TrackedData<Integer> CAST_STATE = DataTracker.registerData(SorcererBossEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_TELEPORTING = DataTracker.registerData(SorcererBossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 攻击状态常量
    public static final byte IDLE_STATE = 0;
    public static final byte FIREBALL_CAST_STATE = 1;
    public static final byte LIGHTNING_CAST_STATE = 2;
    public static final byte SUMMON_CAST_STATE = 3;
    public static final byte TELEPORT_STATE = 4;
    
    // 攻击冷却
    private int attackCooldown = 0;
    private int teleportCooldown = 0;
    
    // 阶段管理
    private BossPhase currentPhase = BossPhase.PHASE_1;
    private int phaseTransitionTicks = 0;
    
    // GeckoLib动画缓存
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    
    // 预定义的动画
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.sorcerer.idle");
    private static final RawAnimation ANIM_CAST_FIREBALL = RawAnimation.begin().thenPlay("animation.sorcerer.cast_fireball");
    private static final RawAnimation ANIM_CAST_LIGHTNING = RawAnimation.begin().thenPlay("animation.sorcerer.cast_lightning");
    private static final RawAnimation ANIM_CAST_SUMMON = RawAnimation.begin().thenPlay("animation.sorcerer.cast_summon");
    private static final RawAnimation ANIM_TELEPORT = RawAnimation.begin().thenPlay("animation.sorcerer.teleport");
    
    /**
     * 创建法师BOSS实体
     */
    public SorcererBossEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.serverTick = new SorcererServerTick(this);
    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CAST_STATE, 0);
        this.dataTracker.startTracking(IS_TELEPORTING, false);
    }
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SorcererCastSpellGoal(this));
        this.goalSelector.add(2, new SorcererTeleportGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    /**
     * 设置实体属性
     */
    public static DefaultAttributeContainer.Builder createBossAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
            .add(EntityAttributes.GENERIC_ARMOR, 8.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }
    
    /**
     * 获取当前施法状态
     */
    public int getCastState() {
        return this.dataTracker.get(CAST_STATE);
    }
    
    /**
     * 设置施法状态
     */
    public void setCastState(int state) {
        this.dataTracker.set(CAST_STATE, state);
        
        // 同步动画状态
        if (state >= 0 && state <= 4) {
            this.getWorld().sendEntityStatus(this, (byte)state);
        }
    }
    
    /**
     * 获取是否正在传送
     */
    public boolean isTeleporting() {
        return this.dataTracker.get(IS_TELEPORTING);
    }
    
    /**
     * 设置传送状态
     */
    public void setTeleporting(boolean teleporting) {
        this.dataTracker.set(IS_TELEPORTING, teleporting);
        
        if (teleporting) {
            this.getWorld().sendEntityStatus(this, TELEPORT_STATE);
        }
    }
    
    /**
     * BOSS阶段枚举
     */
    public enum BossPhase {
        PHASE_1,  // 初始阶段
        PHASE_2,  // 血量低于60%
        PHASE_3   // 血量低于30%
    }
    
    /**
     * 获取当前阶段
     */
    public BossPhase getCurrentPhase() {
        return this.currentPhase;
    }
    
    /**
     * 更新BOSS阶段
     */
    private void updatePhase() {
        BossPhase newPhase;
        float healthRatio = this.getHealth() / this.getMaxHealth();
        
        if (healthRatio < 0.3f) {
            newPhase = BossPhase.PHASE_3;
        } else if (healthRatio < 0.6f) {
            newPhase = BossPhase.PHASE_2;
        } else {
            newPhase = BossPhase.PHASE_1;
        }
        
        if (newPhase != this.currentPhase) {
            this.currentPhase = newPhase;
            this.phaseTransitionTicks = 60; // 3秒过渡期
        }
    }
    
    /**
     * 处理服务端tick逻辑
     */
    @Override
    protected void serverTick(ServerWorld world) {
        // 更新冷却
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }
        
        // 更新阶段
        updatePhase();
        
        // 阶段过渡期间的处理
        if (this.phaseTransitionTicks > 0) {
            this.phaseTransitionTicks--;
        }
    }
    
    // GeckoLib方法实现
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::handleAnimations));
    }
    
    private PlayState handleAnimations(AnimationState<SorcererBossEntity> state) {
        int castState = this.getCastState();
        
        // 根据施法状态选择动画
        if (this.isTeleporting()) {
            return state.setAndContinue(ANIM_TELEPORT);
        } else if (castState == FIREBALL_CAST_STATE) {
            return state.setAndContinue(ANIM_CAST_FIREBALL);
        } else if (castState == LIGHTNING_CAST_STATE) {
            return state.setAndContinue(ANIM_CAST_LIGHTNING);
        } else if (castState == SUMMON_CAST_STATE) {
            return state.setAndContinue(ANIM_CAST_SUMMON);
        } else {
            // 默认闲置动画
            return state.setAndContinue(ANIM_IDLE);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
    
    @Override
    public double getTick(Object o) {
        return this.age;
    }
    
    /**
     * 施法目标AI
     */
    class SorcererCastSpellGoal extends Goal {
        private final SorcererBossEntity boss;
        private int spellCastTime;
        private int spellCooldown;
        
        public SorcererCastSpellGoal(SorcererBossEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            LivingEntity target = this.boss.getTarget();
            return target != null && 
                   target.isAlive() && 
                   boss.attackCooldown <= 0 && 
                   boss.getCastState() == 0 &&
                   boss.phaseTransitionTicks <= 0;
        }
        
        @Override
        public boolean shouldContinue() {
            return this.spellCastTime > 0 && 
                   this.boss.getTarget() != null && 
                   this.boss.getTarget().isAlive();
        }
        
        @Override
        public void start() {
            this.spellCastTime = 60; // 3秒施法时间
            
            // 随机选择一种施法类型
            int spellType = boss.getRandom().nextInt(3) + 1; // 1-3
            boss.setCastState(spellType);
            
            // 根据阶段设置冷却时间
            switch (boss.getCurrentPhase()) {
                case PHASE_1:
                    this.spellCooldown = 100; // 5秒
                    break;
                case PHASE_2:
                    this.spellCooldown = 80; // 4秒
                    break;
                case PHASE_3:
                    this.spellCooldown = 60; // 3秒
                    break;
            }
        }
        
        @Override
        public void tick() {
            this.spellCastTime--;
            
            LivingEntity target = this.boss.getTarget();
            if (target != null) {
                // 施法期间看向目标
                this.boss.getLookControl().lookAt(target, 30.0F, 30.0F);
                
                // 施法期间减速移动
                if (this.boss.squaredDistanceTo(target) > 64.0) { // 8格以上
                    this.boss.getNavigation().startMovingTo(target, 0.5);
                } else {
                    this.boss.getNavigation().stop();
                }
            }
        }
        
        @Override
        public void stop() {
            this.boss.setCastState(0);
            this.boss.attackCooldown = this.spellCooldown;
        }
    }
    
    /**
     * 传送目标AI
     */
    class SorcererTeleportGoal extends Goal {
        private final SorcererBossEntity boss;
        
        public SorcererTeleportGoal(SorcererBossEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            LivingEntity target = this.boss.getTarget();
            if (target == null || !target.isAlive() || boss.teleportCooldown > 0) {
                return false;
            }
            
            // 当处于危险时传送（血量低或被多个实体包围）
            boolean isDangerous = boss.getHealth() < boss.getMaxHealth() * 0.3f ||
                                 boss.getWorld().getOtherEntities(boss, boss.getBoundingBox().expand(3.0), 
                                                                 e -> e instanceof PlayerEntity).size() >= 2;
            
            // 当目标太近或太远时传送
            boolean badDistance = boss.squaredDistanceTo(target) < 9.0 || // 3格以内
                                 boss.squaredDistanceTo(target) > 256.0;  // 16格以外
            
            return (isDangerous || badDistance) && boss.getRandom().nextInt(30) == 0;
        }
        
        @Override
        public void start() {
            // 开始传送
            boss.setTeleporting(true);
            boss.teleportCooldown = 100; // 5秒冷却
        }
    }
    
    /**
     * 服务端逻辑处理
     */
    private class SorcererServerTick implements IEntityTick<ServerWorld> {
        private final SorcererBossEntity boss;
        
        public SorcererServerTick(SorcererBossEntity boss) {
            this.boss = boss;
        }
        
        @Override
        public void tick(ServerWorld world) {
            // 实现服务端逻辑
        }
    }
} 