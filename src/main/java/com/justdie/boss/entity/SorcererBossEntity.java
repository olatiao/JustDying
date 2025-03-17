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
import net.minecraft.util.math.Vec3d;

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
        this.targetSelector.add(3, new TargetGoal<>(this, PlayerEntity.class, 10, true, false, null));
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
        
        // 主动检查并尝试攻击玩家
        tryAttackNearbyPlayers();
    }
    
    /**
     * 主动检查并尝试攻击附近的玩家
     */
    private void tryAttackNearbyPlayers() {
        // 如果没有目标，但周围有玩家，尝试将其设为目标
        if (this.getTarget() == null) {
            // 寻找8-32格范围内的玩家
            PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(
                this.getX(), this.getY(), this.getZ(), 32.0, true);
                
            if (nearestPlayer != null) {
                this.setTarget(nearestPlayer);
            }
        }
        
        // 当有目标但处于闲置状态时，尝试使用法术攻击
        if (this.getTarget() != null && this.attackCooldown <= 0 && this.getCastState() == 0) {
            // 根据与目标的距离选择合适的法术
            double distanceToTarget = this.squaredDistanceTo(this.getTarget());
            
            if (distanceToTarget > 100.0) { // 10格以上距离
                // 使用闪电法术
                this.setCastState(LIGHTNING_CAST_STATE);
                castLightningSpell(this.getTarget());
            } else if (distanceToTarget > 36.0) { // 6格以上距离
                // 使用火球法术
                this.setCastState(FIREBALL_CAST_STATE);
                castFireballSpell(this.getTarget());
            } else if (this.getRandom().nextInt(10) == 0) { // 10%概率召唤
                // 使用召唤法术
                this.setCastState(SUMMON_CAST_STATE);
                castSummonSpell();
            }
            
            // 设置攻击冷却
            this.attackCooldown = 40 + this.getRandom().nextInt(60); // 2-5秒冷却
        }
    }
    
    /**
     * 施放闪电法术
     */
    private void castLightningSpell(LivingEntity target) {
        if (target == null || !target.isAlive() || !(this.getWorld() instanceof ServerWorld)) {
            return;
        }
        
        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        
        // 在目标位置附近创建闪电
        net.minecraft.util.math.BlockPos targetPos = 
            new net.minecraft.util.math.BlockPos((int)target.getX(), (int)target.getY(), (int)target.getZ());
            
        net.minecraft.entity.LightningEntity lightningBolt = 
            net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(serverWorld);
            
        if (lightningBolt != null) {
            lightningBolt.refreshPositionAfterTeleport(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            serverWorld.spawnEntity(lightningBolt);
        }
    }
    
    /**
     * 施放火球法术
     */
    private void castFireballSpell(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        
        // 发射小火球
        double dX = target.getX() - this.getX();
        double dY = target.getBodyY(0.5) - this.getBodyY(0.5);
        double dZ = target.getZ() - this.getZ();
        
        double strength = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        
        // 创建小火球实体
        net.minecraft.entity.projectile.SmallFireballEntity fireball = 
            new net.minecraft.entity.projectile.SmallFireballEntity(
                this.getWorld(), this, 
                dX / strength, dY / strength, dZ / strength);
        
        fireball.setPosition(
            this.getX() + dX / strength, 
            this.getBodyY(0.5) + 0.5, 
            this.getZ() + dZ / strength);
        
        this.getWorld().spawnEntity(fireball);
    }
    
    /**
     * 施放召唤法术
     */
    private void castSummonSpell() {
        if (!(this.getWorld() instanceof ServerWorld)) {
            return;
        }
        
        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        
        // 召唤2-3个随机怪物 (如蜘蛛或僵尸)
        int count = 2 + this.getRandom().nextInt(2);
        
        for (int i = 0; i < count; i++) {
            net.minecraft.entity.mob.MobEntity minion = null;
            
            if (this.getRandom().nextBoolean()) {
                // 召唤蜘蛛
                minion = net.minecraft.entity.EntityType.SPIDER.create(serverWorld);
            } else {
                // 召唤僵尸
                minion = net.minecraft.entity.EntityType.ZOMBIE.create(serverWorld);
            }
            
            if (minion != null) {
                // 在BOSS周围随机位置生成怪物
                double offsetX = this.getRandom().nextDouble() * 3.0 - 1.5;
                double offsetZ = this.getRandom().nextDouble() * 3.0 - 1.5;
                
                minion.refreshPositionAndAngles(
                    this.getX() + offsetX,
                    this.getY(),
                    this.getZ() + offsetZ,
                    this.getRandom().nextFloat() * 360.0F, 0.0F);
                
                // 如果BOSS有目标，让召唤的怪物也攻击同一目标
                if (this.getTarget() != null) {
                    minion.setTarget(this.getTarget());
                }
                
                serverWorld.spawnEntityAndPassengers(minion);
            }
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
                   boss.phaseTransitionTicks <= 0 &&
                   boss.getRandom().nextInt(3) == 0; // 增加施法机会
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
            
            // 执行实际传送逻辑
            this.attemptTeleport();
        }
        
        /**
         * 尝试进行传送
         */
        private void attemptTeleport() {
            if (!(boss.getWorld() instanceof ServerWorld)) {
                return;
            }
            
            ServerWorld world = (ServerWorld) boss.getWorld();
            LivingEntity target = boss.getTarget();
            
            if (target == null) {
                // 如果没有目标，则传送回初始位置
                if (boss.idlePosition != Vec3d.ZERO) {
                    teleportToLocation(boss.idlePosition.x, boss.idlePosition.y, boss.idlePosition.z);
                }
                return;
            }
            
            // 尝试20次找到一个安全的传送位置
            for (int i = 0; i < 20; i++) {
                // 计算传送距离和角度
                double distance = boss.squaredDistanceTo(target) < 9.0 ? 
                    // 如果太近则传送到8-10格远的地方
                    8.0 + boss.getRandom().nextDouble() * 2.0 : 
                    // 如果太远则传送到6-8格远的地方
                    6.0 + boss.getRandom().nextDouble() * 2.0;
                
                // 随机角度
                double angle = boss.getRandom().nextDouble() * Math.PI * 2.0;
                
                // 计算新坐标
                double newX = target.getX() + Math.sin(angle) * distance;
                double newZ = target.getZ() + Math.cos(angle) * distance;
                
                // 找到合适的Y坐标
                int newY = findSuitableY(world, newX, newZ);
                
                if (newY > 0) {
                    // 执行传送
                    if (teleportToLocation(newX, newY, newZ)) {
                        // 传送成功
                        world.sendEntityStatus(boss, TELEPORT_STATE);
                        return;
                    }
                }
            }
        }
        
        /**
         * 在指定X,Z坐标找到合适的Y坐标
         */
        private int findSuitableY(ServerWorld world, double x, double z) {
            // 获取区块
            net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos((int)x, 0, (int)z);
            int maxY = world.getTopY();
            
            // 从上往下找第一个非空气方块
            for (int y = Math.min(maxY, (int)boss.getY() + 15); y >= world.getBottomY(); y--) {
                net.minecraft.util.math.BlockPos currentPos = new net.minecraft.util.math.BlockPos((int)x, y, (int)z);
                net.minecraft.block.BlockState state = world.getBlockState(currentPos);
                
                if (!state.isAir() && state.isFullCube(world, currentPos) && 
                    state.isSolidBlock(world, currentPos) && 
                    world.isAir(currentPos.up()) && world.isAir(currentPos.up(2))) {
                    return y + 1; // 返回方块上方的坐标
                }
            }
            
            return -1; // 未找到合适位置
        }
        
        /**
         * 将实体传送到指定坐标
         */
        private boolean teleportToLocation(double x, double y, double z) {
            // 设置位置并刷新
            boss.refreshPositionAndAngles(x, y, z, boss.getYaw(), boss.getPitch());
            
            // 特效和声音
            if (boss.getWorld() instanceof ServerWorld serverWorld) {
                // 在原位置产生紫色粒子
                serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.PORTAL,
                    boss.getX(), boss.getBodyY(0.5), boss.getZ(),
                    30, 0.2, 0.2, 0.2, 0.0);
                
                // 播放末影人传送声音
                serverWorld.playSound(
                    null, boss.prevX, boss.prevY, boss.prevZ,
                    net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    net.minecraft.sound.SoundCategory.HOSTILE,
                    1.0F, 1.0F);
                    
                // 在新位置产生紫色粒子
                serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.PORTAL,
                    boss.getX(), boss.getBodyY(0.5), boss.getZ(),
                    30, 0.2, 0.2, 0.2, 0.0);
                    
                // 再次播放传送声音
                serverWorld.playSound(
                    null, boss.getX(), boss.getY(), boss.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    net.minecraft.sound.SoundCategory.HOSTILE,
                    1.0F, 1.0F);
            }
            
            return true;
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

    /**
     * 自定义感知目标，增强BOSS主动发现并攻击玩家的能力
     */
    private class TargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {
        public TargetGoal(MobEntity mob, Class<T> targetClass, int reciprocalChance, boolean checkVisibility, boolean checkNavigable, java.util.function.Predicate<LivingEntity> targetPredicate) {
            super(mob, targetClass, reciprocalChance, checkVisibility, checkNavigable, targetPredicate);
        }
        
        @Override
        public boolean canStart() {
            // 确保目标选择不受其他AI影响
            if (SorcererBossEntity.this.phaseTransitionTicks > 0) {
                return false;
            }
            return super.canStart();
        }
        
        @Override
        protected double getFollowRange() {
            // 扩大感知范围
            return super.getFollowRange() * 1.5;
        }
    }
} 