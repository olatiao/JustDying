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
    private static final TrackedData<Integer> CAST_STATE = DataTracker.registerData(SorcererBossEntity.class,
            TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_TELEPORTING = DataTracker.registerData(SorcererBossEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN);

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
    private static final RawAnimation ANIM_CAST_FIREBALL = RawAnimation.begin()
            .thenPlay("animation.sorcerer.cast_fireball");
    private static final RawAnimation ANIM_CAST_LIGHTNING = RawAnimation.begin()
            .thenPlay("animation.sorcerer.cast_lightning");
    private static final RawAnimation ANIM_CAST_SUMMON = RawAnimation.begin()
            .thenPlay("animation.sorcerer.cast_summon");
    private static final RawAnimation ANIM_TELEPORT = RawAnimation.begin().thenPlay("animation.sorcerer.teleport");

    /**
     * 创建法师BOSS实体
     */
    public SorcererBossEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.serverTick = new SorcererServerTick(this);
        this.setStepHeight(1.0F); // 使用setter方法增加台阶高度
        this.noClip = false; // 确保不穿墙
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
        this.goalSelector.add(3, new SorcererKeepDistanceGoal(this, 6.0, 10.0, 16.0, 1.0));
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
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0) // 完全免疫击退
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
            this.getWorld().sendEntityStatus(this, (byte) state);
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
        PHASE_1, // 初始阶段
        PHASE_2, // 血量低于60%
        PHASE_3 // 血量低于30%
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

        // 安全检查 - 检测BOSS是否被卡住或窒息
        boolean isStuck = checkIfStuck(world);
        if (isStuck && this.teleportCooldown <= 0) {
            // 立即执行紧急传送
            this.setTeleporting(true);
            new SorcererTeleportGoal(this).emergencyTeleport();
            this.teleportCooldown = 20; // 短冷却
        }

        // 防止倒地 - 保持实体垂直并限制Y轴速度
        Vec3d velocity = this.getVelocity();
        if (velocity.y < 0 && !this.isOnGround()) {
            // 限制下落速度，防止下落过快
            this.setVelocity(velocity.x, Math.max(velocity.y, -0.1), velocity.z);
        }
        
        // 每10个tick检查一次姿势，确保实体保持直立
        if (this.age % 10 == 0) {
            this.setNoGravity(true); // 禁用重力
            
            // 如果没有目标且在空中，缓慢下降
            if (!this.isOnGround() && this.getTarget() == null) {
                this.setVelocity(0, -0.05, 0);
            }
        }

        // 强制攻击检测 - 确保BOSS始终能主动攻击玩家
        // 每秒主动检查一次附近玩家
        if (this.age % 20 == 0) {
            // 即使已经有目标，也要检查是否有更近的玩家
            forceAttackNearbyPlayers();
        }

        // 增加攻击频率 - 每2秒必然尝试施法一次
        if (this.getTarget() != null && this.getCastState() == 0 && this.age % 40 == 0) {
            // 根据与目标的距离选择合适的法术
            double distanceToTarget = this.squaredDistanceTo(this.getTarget());

            if (distanceToTarget > 100.0) { // 10格以上距离
                // 使用闪电法术
                this.setCastState(LIGHTNING_CAST_STATE);
                executeSpell(LIGHTNING_CAST_STATE);
            } else if (distanceToTarget > 16.0) { // 4格以上距离
                // 使用火球法术
                this.setCastState(FIREBALL_CAST_STATE);
                executeSpell(FIREBALL_CAST_STATE);
            } else if (distanceToTarget < 36.0) { // 6格以内
                // 近距离使用召唤法术
                this.setCastState(SUMMON_CAST_STATE);
                executeSpell(SUMMON_CAST_STATE);

                // 近距离同时也考虑传送
                if (this.getRandom().nextInt(3) == 0 && this.teleportCooldown <= 0) {
                    this.teleportCooldown = 20;
                    this.setTeleporting(true);
                    new SorcererTeleportGoal(this).attemptTeleport();
                }
            }

            // 设置适当的冷却
            this.attackCooldown = 20; // 1秒
        }

        // 确保实体姿势正确，防止倒地问题
        this.setPitch(0.0f);
        this.setBodyYaw(this.getYaw());
    }

    /**
     * 检查BOSS是否被卡住或在窒息方块中
     */
    private boolean checkIfStuck(ServerWorld world) {
        // 获取BOSS所在位置的方块状态
        net.minecraft.util.math.BlockPos entityPos = this.getBlockPos();
        net.minecraft.util.math.BlockPos headPos = entityPos.up();
        
        net.minecraft.block.BlockState blockState = world.getBlockState(entityPos);
        net.minecraft.block.BlockState headBlockState = world.getBlockState(headPos);
        
        // 检查是否在树叶内 (被卡住)
        boolean inLeaves = blockState.getBlock() instanceof net.minecraft.block.LeavesBlock ||
                          headBlockState.getBlock() instanceof net.minecraft.block.LeavesBlock;
        
        // 检查是否在实心方块中 (窒息)
        boolean suffocating = blockState.shouldSuffocate(world, entityPos) || 
                             headBlockState.shouldSuffocate(world, headPos);
        
        // 检查是否无法移动 (速度几乎为0但仍有目标)
        boolean cantMove = this.getVelocity().lengthSquared() < 0.01 && 
                         this.getTarget() != null && 
                         !this.isOnGround();
        
        // 如果有目标而且已经尝试移动但仍被卡住超过2秒
        if (this.getTarget() != null && this.age % 40 == 0) {
            // 检查是否处于碰撞中
            boolean collidingWithBlocks = !world.isSpaceEmpty(this, this.getBoundingBox());
            if (collidingWithBlocks) {
                return true;
            }
        }
        
        return inLeaves || suffocating || (cantMove && this.age % 40 == 0);
    }

    // 覆盖移动相关方法，确保实体总是直立的
    @Override
    public void travel(Vec3d movementInput) {
        // 始终保持垂直姿态
        this.setPitch(0);
        this.prevPitch = 0;
        
        // 仅使用水平移动分量
        Vec3d horizontalMovement = new Vec3d(movementInput.x, 0, movementInput.z);
        super.travel(horizontalMovement);
        
        // 移动后再次重置姿态
        this.setPitch(0);
        this.prevPitch = 0;
    }

    @Override
    public void updateLimbs(float limbDistance) {
        // 完全替换原有实现，不调用super方法
        // 这样可以防止原版动画系统影响模型
    }

    // 使用自定义碰撞箱优化碰撞检测
    // 已删除重复的initDataTracker方法，此处使用的是类顶部已经定义的方法

    // 全面替换默认的移动和位置计算逻辑
    @Override
    public void baseTick() {
        // 在基础tick开始前强制设置姿态
        this.setYaw(this.headYaw);
        
        // 清除所有俯仰角变化和偏移量
        this.setPitch(0);
        this.prevPitch = 0;
        
        super.baseTick();
        
        // 在基础tick结束后再次强制设置姿态
        this.setPitch(0);
        this.prevPitch = 0;
    }
    
    // 防止实体掉落
    @Override
    public boolean hasNoGravity() {
        return true;
    }
    
    // 完全锁定动画姿势
    @Override
    public void tickMovement() {
        // 缓存当前yaw用于计算移动
        float originalYaw = this.getYaw();
        
        // 设置直立姿态
        this.setPitch(0);
        this.prevPitch = 0;
        
        super.tickMovement();
        
        // 移动后重置姿态
        this.setPitch(0);
        this.prevPitch = 0;
        
        // 确保实体不会进入飞行状态
        if (!this.isOnGround() && this.getVelocity().y < 0) {
            // 如果在空中且下落中，尝试找到地面
            this.setVelocity(this.getVelocity().multiply(1, 0, 1));
        }
    }
    
    // 替换默认的旋转行为
    @Override
    public void setHeadYaw(float headYaw) {
        // 同步头部和身体朝向
        super.setHeadYaw(headYaw);
        this.bodyYaw = this.headYaw;
        this.prevBodyYaw = this.bodyYaw;
    }
    
    // 锁定俯仰角
    @Override
    public void setRotation(float yaw, float pitch) {
        // 忽略传入的pitch，始终保持为0
        super.setRotation(yaw, 0);
    }

    // 重写碰撞检测方法
    @Override
    public boolean collidesWith(net.minecraft.entity.Entity other) {
        // 只与玩家和重要实体碰撞，忽略小型实体
        if (other instanceof net.minecraft.entity.mob.SlimeEntity) {
            return false;
        }
        return super.collidesWith(other);
    }
    
    // 处理掉落相关方法和动作
    @Override
    protected void fall(double heightDifference, boolean onGround, net.minecraft.block.BlockState landedState, net.minecraft.util.math.BlockPos landedPosition) {
        // 完全禁用掉落伤害和动画
    }
    
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, net.minecraft.entity.damage.DamageSource damageSource) {
        // 完全禁用掉落伤害
        return false;
    }

    // GeckoLib方法实现
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, this::handleAnimations));
    }

    private PlayState handleAnimations(AnimationState<SorcererBossEntity> state) {
        // 强制重置实体的俯仰角，防止倒地
        this.setPitch(0.0f);
        this.prevPitch = 0.0f;
        this.setBodyYaw(this.getYaw());
        this.prevBodyYaw = this.bodyYaw;
        
        // 强制停止所有垂直移动
        Vec3d velocity = this.getVelocity();
        if (velocity.y != 0) {
            this.setVelocity(velocity.x, 0, velocity.z);
        }
        
        // 根据当前状态选择动画
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
            // 确保实体始终播放闲置动画
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
     * 保持距离AI
     */
    class SorcererKeepDistanceGoal extends Goal {
        private final SorcererBossEntity boss;
        private final double minDistance;
        private final double optimalDistance;
        private final double maxDistance;
        private final double moveSpeed;
        private final double strafeSpeed;
        private boolean clockwise;
        private int strafeTimer;
        private int positionCheckTimer = 0; // 新增：位置检查计时器

        public SorcererKeepDistanceGoal(SorcererBossEntity boss, double minDistance, double optimalDistance, double maxDistance, double moveSpeed) {
            this.boss = boss;
            this.minDistance = minDistance;
            this.optimalDistance = optimalDistance;
            this.maxDistance = maxDistance;
            this.moveSpeed = moveSpeed;
            this.strafeSpeed = moveSpeed * 0.8;
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return boss.getTarget() != null && 
                   boss.getTarget().isAlive() && 
                   boss.getCastState() == 0 && // 不在施法状态
                   boss.phaseTransitionTicks <= 0; // 不在相变状态
        }

        @Override
        public void start() {
            // 随机初始化绕行方向
            this.clockwise = boss.getRandom().nextBoolean();
            this.strafeTimer = 0;
            this.positionCheckTimer = 0; // 重置位置检查计时器
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target == null)
                return;

            // 始终看向目标
            boss.getLookControl().lookAt(target, 30.0F, 30.0F);

            // 计算与目标的距离
            double distSq = boss.squaredDistanceTo(target);

            // 处理绕行计时器 - 减少时间以增加方向变化频率
            if (this.strafeTimer-- <= 0) {
                this.strafeTimer = 40 + boss.getRandom().nextInt(40); // 2-4秒
                this.clockwise = !this.clockwise; // 切换绕行方向
            }

            // 位置检查 - 每秒检查一次是否被卡住
            if (this.positionCheckTimer++ >= 20) {
                this.positionCheckTimer = 0;
                
                // 如果实体被卡住（速度接近于0但有目标），尝试传送到更好的位置
                if (boss.getVelocity().lengthSquared() < 0.01 && target != null) {
                    tryRepositionIfStuck(target);
                }
            }

            // 根据距离决定行动 - 更积极地保持最佳距离
            if (distSq < minDistance * minDistance) {
                // 太近，需要远离
                moveAwayFromTarget(target);
            } else if (distSq > maxDistance * maxDistance) {
                // 太远，靠近目标
                moveTowardTarget(target);
            } else {
                // 保持在最佳范围内绕行
                strafeAroundTarget(target);
            }
        }

        /**
         * 尝试重新定位如果实体被卡住
         */
        private void tryRepositionIfStuck(LivingEntity target) {
            // 计算目标方向
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            if (dist < 0.1) return;
            
            // 标准化方向
            double normalizedX = dx / dist;
            double normalizedZ = dz / dist;
            
            // 计算理想距离的位置（在最佳距离处）
            double idealX = target.getX() - normalizedX * optimalDistance;
            double idealZ = target.getZ() - normalizedZ * optimalDistance;
            
            // 找到安全的Y坐标
            int safeY = boss.findSafeY(boss.getWorld(), idealX, idealZ);
            if (safeY > 0) {
                // 直接移动到该位置
                boss.refreshPositionAndAngles(idealX, safeY, idealZ, boss.getYaw(), 0);
                boss.getNavigation().stop();
            }
        }

        /**
         * 远离目标
         */
        private void moveAwayFromTarget(LivingEntity target) {
            // 计算远离方向
            double dx = boss.getX() - target.getX();
            double dz = boss.getZ() - target.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist < 0.1)
                return; // 避免除零

            // 标准化并设置目标位置（反方向移动）
            double normalizedX = dx / dist;
            double normalizedZ = dz / dist;

            double targetX = boss.getX() + normalizedX * 10.0; // 远离10格，增加距离
            double targetZ = boss.getZ() + normalizedZ * 10.0;

            // 找到合适的Y坐标
            int targetY = boss.findSafeY(boss.getWorld(), targetX, targetZ);
            if (targetY > 0) {
                // 移动到该位置，增加速度
                boss.getNavigation().startMovingTo(targetX, targetY, targetZ, moveSpeed * 1.5); 
            } else {
                // 随机选择新方向
                double angle = boss.getRandom().nextDouble() * Math.PI * 2;
                targetX = boss.getX() + Math.sin(angle) * 10.0;
                targetZ = boss.getZ() + Math.cos(angle) * 10.0;
                boss.getNavigation().startMovingTo(targetX, boss.getY(), targetZ, moveSpeed * 1.5);
            }
        }

        /**
         * 靠近目标
         */
        private void moveTowardTarget(LivingEntity target) {
            // 靠近但保持在最佳距离
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist < 0.1)
                return; // 避免除零

            // 标准化并设置目标位置（方向移动但不要太近）
            double normalizedX = dx / dist;
            double normalizedZ = dz / dist;

            double moveDistance = dist - optimalDistance; // 仅靠近到最佳距离
            double targetX = boss.getX() + normalizedX * moveDistance;
            double targetZ = boss.getZ() + normalizedZ * moveDistance;

            boss.getNavigation().startMovingTo(targetX, target.getY(), targetZ, moveSpeed);
        }

        /**
         * 绕目标移动
         */
        private void strafeAroundTarget(LivingEntity target) {
            // 计算与目标的向量
            double dx = target.getX() - boss.getX();
            double dz = target.getZ() - boss.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist < 0.1)
                return; // 避免除零

            // 计算绕行方向（垂直于目标方向的向量）
            double strafeX, strafeZ;
            if (clockwise) {
                strafeX = -dz / dist;
                strafeZ = dx / dist;
            } else {
                strafeX = dz / dist;
                strafeZ = -dx / dist;
            }

            // 设置移动目标
            double targetX = boss.getX() + strafeX * 3.0; // 绕行3格
            double targetZ = boss.getZ() + strafeZ * 3.0;

            boss.getNavigation().startMovingTo(targetX, target.getY(), targetZ, strafeSpeed);
        }
    }

    /**
     * 在指定位置找到安全的Y坐标
     */
    public int findSafeY(World world, double x, double z) {
        int maxY = world.getTopY();

        // 从上往下找第一个非空气方块
        for (int y = Math.min(maxY, (int) this.getY() + 15); y >= world.getBottomY(); y--) {
            net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos((int) x, y, (int) z);
            net.minecraft.block.BlockState state = world.getBlockState(pos);

            if (!state.isAir() && state.isFullCube(world, pos) &&
                    world.isAir(pos.up()) && world.isAir(pos.up(2))) {
                return y + 1; // 返回方块上方的坐标
            }
        }

        return -1; // 未找到合适位置
    }

    /**
     * 服务端逻辑处理
     */
    private class SorcererServerTick implements IEntityTick<ServerWorld> {
        private final SorcererBossEntity boss;
        private int attackTick = 0;

        public SorcererServerTick(SorcererBossEntity boss) {
            this.boss = boss;
        }

        @Override
        public void tick(ServerWorld world) {
            // 额外的攻击检查，确保AI行为正常
            attackTick++;
            
            // 如果5秒内没有攻击过，强制尝试攻击
            if (attackTick >= 100) {
                attackTick = 0;
                
                // 检查是否有目标
                if (boss.getTarget() == null || !boss.getTarget().isAlive()) {
                    boss.forceAttackNearbyPlayers();
                } else {
                    // 有目标但没在攻击，强制施法
                    if (boss.getCastState() == 0 && boss.attackCooldown <= 0) {
                        LivingEntity target = boss.getTarget();
                        double distSq = boss.squaredDistanceTo(target);
                        
                        // 根据距离选择法术
                        int spellType;
                        if (distSq < 36) {
                            spellType = LIGHTNING_CAST_STATE;
                        } else {
                            spellType = FIREBALL_CAST_STATE;
                        }
                        
                        boss.setCastState(spellType);
                        boss.executeSpell(spellType);
                        boss.attackCooldown = 20;
                    }
                }
            }
        }
    }

    /**
     * 自定义感知目标，增强BOSS主动发现并攻击玩家的能力
     */
    private class TargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {
        public TargetGoal(MobEntity mob, Class<T> targetClass, int reciprocalChance, boolean checkVisibility,
                boolean checkNavigable, java.util.function.Predicate<LivingEntity> targetPredicate) {
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

    /**
     * 执行指定类型的法术，确保动作有效
     */
    private void executeSpell(int spellType) {
        if (this.getTarget() == null)
            return;

        switch (spellType) {
            case FIREBALL_CAST_STATE:
                castFireballSpell(this.getTarget());
                break;
            case LIGHTNING_CAST_STATE:
                castLightningSpell(this.getTarget());
                break;
            case SUMMON_CAST_STATE:
                castSummonSpell();
                break;
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

        // 在目标位置附近创建闪电 - 直接使用目标坐标
        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();

        // 创建闪电实体，确保它能被正确渲染
        net.minecraft.entity.LightningEntity lightningBolt = (net.minecraft.entity.LightningEntity) net.minecraft.entity.EntityType.LIGHTNING_BOLT
                .create(serverWorld);

        if (lightningBolt != null) {
            // 确保闪电实体位置正确
            lightningBolt.refreshPositionAndAngles(targetX, targetY, targetZ, 0, 0);
            serverWorld.spawnEntity(lightningBolt);

            // 添加伤害效果
            target.damage(this.getDamageSources().indirectMagic(this, this), 12.0f);

            // 播放法师施法声音和效果
            serverWorld.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL,
                    net.minecraft.sound.SoundCategory.HOSTILE,
                    1.0F, 1.0F);

            // 在目标处生成闪电粒子效果
            serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                    targetX, targetY + 1.0, targetZ,
                    50, 1.0, 1.0, 1.0, 0.2);
        }
    }

    /**
     * 施放火球法术
     */
    private void castFireballSpell(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        // 播放施法声音
        this.getWorld().playSound(
                null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sound.SoundEvents.ENTITY_BLAZE_SHOOT,
                net.minecraft.sound.SoundCategory.HOSTILE,
                1.0F, 1.0F);

        // 发射火球 - 确保方向和速度正确
        double dX = target.getX() - this.getX();
        double dY = target.getBodyY(0.5) - this.getBodyY(0.5);
        double dZ = target.getZ() - this.getZ();

        double distance = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        if (distance < 0.1)
            return; // 防止除零错误

        // 标准化向量
        double velocityX = dX / distance;
        double velocityY = dY / distance;
        double velocityZ = dZ / distance;

        // 创建大火球实体 - 使用更具杀伤力的大火球
        net.minecraft.entity.projectile.FireballEntity fireball = new net.minecraft.entity.projectile.FireballEntity(
                this.getWorld(),
                this,
                velocityX,
                velocityY,
                velocityZ,
                2); // 爆炸威力为2

        // 设置火球的发射位置 - 从胸部发射，更加直观
        fireball.setPosition(
                this.getX(),
                this.getBodyY(0.5) + 0.5,
                this.getZ());

        // 设置火球的所有者为BOSS
        fireball.setOwner(this);

        // 直接添加到世界
        this.getWorld().spawnEntity(fireball);

        // 在火球周围生成粒子效果
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.FLAME,
                    this.getX(), this.getBodyY(0.5) + 0.5, this.getZ(),
                    20, 0.2, 0.2, 0.2, 0.05);
        }
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

    // 覆盖碰撞箱计算方法，完全自定义碰撞箱形状
    // getBoundingBox是final方法无法覆盖，改为修改其他相关方法
    
    // 控制实体维度来影响碰撞箱
    @Override
    public void calculateDimensions() {
        // 在计算维度前重置姿势
        this.setPitch(0);
        
        // 调用父类方法保持兼容性
        super.calculateDimensions();
        
        // 确保在计算后实体的俯仰角仍然为0
        this.setPitch(0);
        this.prevPitch = 0;
    }
    
    // 覆盖实体维度计算，确保始终使用直立的碰撞箱
    @Override
    public net.minecraft.entity.EntityDimensions getDimensions(net.minecraft.entity.EntityPose pose) {
        // 忽略传入的姿势参数，始终返回固定的直立尺寸
        return net.minecraft.entity.EntityDimensions.fixed(0.6f, 1.95f);
    }
    
    // 完全控制实体状态
    @Override
    public void checkDespawn() {
        // 防止实体自然消失
        if (this.getWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        
        // 不执行原版的消失逻辑，确保BOSS不会自然消失
    }
    
    // 禁用所有可能导致实体倒地的运动
    @Override
    protected void applyDamage(net.minecraft.entity.damage.DamageSource source, float amount) {
        // 在受伤前保存朝向
        float originalYaw = this.getYaw();
        
        // 执行正常的伤害逻辑
        super.applyDamage(source, amount);
        
        // 恢复朝向，确保受伤不会改变实体朝向
        this.setYaw(originalYaw);
        this.bodyYaw = originalYaw;
        this.prevBodyYaw = originalYaw;
        this.headYaw = originalYaw;
        this.prevHeadYaw = originalYaw;
        
        // 重置俯仰角
        this.setPitch(0);
        this.prevPitch = 0;
    }
    
    // 自定义实体移动方法，而不是覆盖默认实现
    @Override
    public void move(net.minecraft.entity.MovementType movementType, Vec3d movement) {
        // 保留原始yaw值
        float originalYaw = this.getYaw();
        
        // 处理垂直移动
        if (movement.y != 0 && !this.isOnGround()) {
            // 如果是下落（负垂直速度），尝试找到地面
            if (movement.y < 0) {
                Vec3d pos = this.getPos();
                int safeY = findSafeY(this.getWorld(), pos.x, pos.z);
                if (safeY > 0) {
                    // 如果找到地面，直接将实体设置到地面上
                    this.refreshPositionAndAngles(pos.x, safeY, pos.z, originalYaw, 0);
                    this.setVelocity(Vec3d.ZERO);
                    return; // 跳过默认的移动处理
                }
            }
            
            // 如果无法直接回到地面，只使用水平移动分量
            super.move(movementType, new Vec3d(movement.x, 0, movement.z));
        } else {
            // 正常情况下，只使用水平移动分量
            super.move(movementType, new Vec3d(movement.x, 0, movement.z));
        }
        
        // 在移动后强制重置姿势
        this.setPitch(0);
        this.prevPitch = 0;
        this.setYaw(originalYaw);
        this.bodyYaw = this.headYaw;
        this.prevBodyYaw = this.bodyYaw;
    }
    
    // 完全控制实体位置和朝向更新
    @Override
    public void updatePositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        // 更新位置时忽略pitch值，始终使用0
        super.updatePositionAndAngles(x, y, z, yaw, 0);
    }
    
    // 确保实体总是处于非投射状态
    @Override
    public boolean isPushable() {
        return false; // 防止其他实体推动此实体
    }
    
    @Override
    public boolean isPushedByFluids() {
        return false; // 防止流体推动此实体
    }
    
    // 禁用实体的所有默认物理受力逻辑
    @Override
    public void takeKnockback(double strength, double x, double z) {
        // 完全禁用击退效果
    }
    
    // 防止任何形式的旋转
    @Override
    public void changeLookDirection(double x, double z) {
        // 保存当前朝向
        float oldYaw = this.getYaw();
        
        // 应用正常的朝向变化逻辑
        super.changeLookDirection(x, z);
        
        // 确保pitch始终为0
        this.setPitch(0);
        this.prevPitch = 0;
        
        // 同步所有朝向相关的变量
        this.bodyYaw = this.headYaw;
        this.prevBodyYaw = this.bodyYaw;
    }
    
    // 完全禁用转向动画和相关计算
    @Override
    protected float getActiveEyeHeight(net.minecraft.entity.EntityPose pose, net.minecraft.entity.EntityDimensions dimensions) {
        // 忽略传入的姿势，始终返回标准站立时的眼睛高度
        return 1.7f; // 略低于实体总高度
    }
    
    // 提供跟随逻辑的回退方案
    @Override
    public Vec3d getLeashOffset() {
        // 确保缰绳连接点保持在合理位置
        return new Vec3d(0, 1.2, 0);
    }

    /**
     * 强制攻击附近的玩家，确保BOSS主动性
     */
    private void forceAttackNearbyPlayers() {
        // 扩大搜索范围到64格，更主动地发现玩家
        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(
                this.getX(), this.getY(), this.getZ(), 64.0, true);

        if (nearestPlayer != null) {
            // 检查当前目标是否存在且更近
            LivingEntity currentTarget = this.getTarget();
            if (currentTarget != null && currentTarget.isAlive()) {
                double currentDistSq = this.squaredDistanceTo(currentTarget);
                double newDistSq = this.squaredDistanceTo(nearestPlayer);
                
                // 如果新玩家更近，或者原目标超出攻击范围，则更新目标
                if (newDistSq < currentDistSq || currentDistSq > 40*40) {
                    this.setTarget(nearestPlayer);
                }
            } else {
                // 如果没有当前目标，直接设置新目标
                this.setTarget(nearestPlayer);
            }
            
            // 如果玩家太远，立即尝试施放远程攻击
            double distanceToTarget = this.squaredDistanceTo(nearestPlayer);
            if (distanceToTarget > 100.0 && this.getCastState() == 0 && this.attackCooldown <= 0) {
                this.setCastState(LIGHTNING_CAST_STATE);
                executeSpell(LIGHTNING_CAST_STATE);
                this.attackCooldown = 20;
            }
        }
    }

    /**
     * 施法目标AI
     */
    class SorcererCastSpellGoal extends Goal {
        private final SorcererBossEntity boss;
        private int spellCastTime;
        private int spellCooldown;
        private int forcedCastCounter = 0; // 新增：强制施法计数器

        public SorcererCastSpellGoal(SorcererBossEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.boss.getTarget();
            
            // 增加强制施法逻辑
            forcedCastCounter++;
            boolean forcedCast = forcedCastCounter >= 60; // 每3秒强制尝试施法一次
            
            return (target != null &&
                    target.isAlive() &&
                    boss.attackCooldown <= 0 &&
                    boss.getCastState() == 0 &&
                    boss.phaseTransitionTicks <= 0 &&
                    (boss.getRandom().nextInt(3) < 2 || forcedCast)); // 增加施法机会到66%或强制施法
        }

        @Override
        public boolean shouldContinue() {
            return this.spellCastTime > 0 &&
                    this.boss.getTarget() != null &&
                    this.boss.getTarget().isAlive();
        }

        @Override
        public void start() {
            this.spellCastTime = 30; // 减少到1.5秒施法时间，加快节奏
            this.forcedCastCounter = 0; // 重置强制施法计数器
            
            LivingEntity target = boss.getTarget();
            int spellType;
            
            // 根据与目标的距离选择合适的法术
            if (target != null) {
                double distSq = boss.squaredDistanceTo(target);
                
                if (distSq < 36) { // 6格以内
                    // 近距离优先使用闪电法术
                    spellType = LIGHTNING_CAST_STATE;
                } else if (distSq > 100) { // 10格以外
                    // 远距离优先使用火球法术
                    spellType = FIREBALL_CAST_STATE;
                } else {
                    // 中等距离随机选择，但增加召唤的概率
                    if (boss.getRandom().nextInt(10) < 3) { // 30%几率召唤
                        spellType = SUMMON_CAST_STATE;
                    } else {
                        // 其他情况随机选择火球或闪电
                        spellType = boss.getRandom().nextBoolean() ? 
                                    FIREBALL_CAST_STATE : LIGHTNING_CAST_STATE;
                    }
                }
            } else {
                // 没有目标时随机选择
                spellType = boss.getRandom().nextInt(3) + 1; // 1-3
            }
            
            boss.setCastState(spellType);

            // 根据阶段设置冷却时间，减少冷却时间以增加攻击频率
            switch (boss.getCurrentPhase()) {
                case PHASE_1:
                    this.spellCooldown = 40; // 2秒
                    break;
                case PHASE_2:
                    this.spellCooldown = 30; // 1.5秒
                    break;
                case PHASE_3:
                    this.spellCooldown = 20; // 1秒
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

                // 施法期间减速移动，但不要完全停止
                if (this.boss.squaredDistanceTo(target) > 100.0) { // 10格以上
                    this.boss.getNavigation().startMovingTo(target, 0.5);
                } else if (this.boss.squaredDistanceTo(target) < 16.0) { // 4格以内
                    // 太近时后退
                    double dx = this.boss.getX() - target.getX();
                    double dz = this.boss.getZ() - target.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    
                    if (dist > 0.1) {
                        double moveX = this.boss.getX() + (dx / dist) * 2.0;
                        double moveZ = this.boss.getZ() + (dz / dist) * 2.0;
                        this.boss.getNavigation().startMovingTo(moveX, this.boss.getY(), moveZ, 0.5);
                    }
                } else {
                    // 适当距离时停止移动，专注施法
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
            boolean badDistance = boss.squaredDistanceTo(target) < 12.0 || // 3.5格以内
                    boss.squaredDistanceTo(target) > 196.0; // 14格以外

            // 提高传送概率 - 从5%增加到10%
            return (isDangerous || badDistance) && boss.getRandom().nextInt(10) == 0;
        }

        @Override
        public void start() {
            // 开始传送
            boss.setTeleporting(true);
            boss.teleportCooldown = 60; // 减少冷却到3秒

            // 执行实际传送逻辑
            this.attemptTeleport();
        }

        /**
         * 紧急传送 - 无视常规检查，立即传送到安全位置
         */
        private void emergencyTeleport() {
            if (!(boss.getWorld() instanceof ServerWorld)) {
                return;
            }

            ServerWorld world = (ServerWorld) boss.getWorld();
            LivingEntity target = boss.getTarget();
            
            // 尝试传送到目标附近(如果有目标)
            if (target != null) {
                // 先尝试传送到目标附近
                if (teleportNearTarget(target, world)) {
                    return;
                }
            }
            
            // 如果没有目标或无法传送到目标附近，尝试传送到随机安全位置
            for (int i = 0; i < 30; i++) { // 增加尝试次数
                double distance = 8.0 + boss.getRandom().nextDouble() * 8.0; // 8-16格范围
                double angle = boss.getRandom().nextDouble() * Math.PI * 2.0;
                
                // 计算新坐标
                double currentX = boss.getX();
                double currentZ = boss.getZ();
                
                double newX = currentX + Math.sin(angle) * distance;
                double newZ = currentZ + Math.cos(angle) * distance;
                
                // 找到合适的Y坐标，并且确保传送位置安全
                int newY = findSafeLocation(world, newX, newZ);
                
                if (newY > 0) {
                    if (teleportToLocation(newX, newY, newZ)) {
                        world.sendEntityStatus(boss, TELEPORT_STATE);
                        return;
                    }
                }
            }
            
            // 最后的紧急措施：如果还是找不到安全位置，尝试传送到高空
            double newX = boss.getX() + (boss.getRandom().nextDouble() - 0.5) * 16.0;
            double newZ = boss.getZ() + (boss.getRandom().nextDouble() - 0.5) * 16.0;
            int surfaceY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int)newX, (int)newZ);
            
            if (surfaceY > 0) {
                // 传送到地表上方一些距离，避免被卡住
                teleportToLocation(newX, surfaceY + 2, newZ);
                world.sendEntityStatus(boss, TELEPORT_STATE);
            }
        }

        /**
         * 在指定X,Z坐标找到合适的Y坐标，并确保位置安全
         */
        private int findSafeLocation(ServerWorld world, double x, double z) {
            int maxY = world.getTopY();
            int startY = Math.min(maxY, Math.max((int)boss.getY() + 10, 
                    world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int)x, (int)z)));
            
            // 从上往下找安全位置
            for (int y = startY; y >= world.getBottomY(); y--) {
                net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos((int)x, y, (int)z);
                net.minecraft.util.math.BlockPos posAbove = pos.up();
                net.minecraft.util.math.BlockPos posAbove2 = pos.up(2);
                
                net.minecraft.block.BlockState state = world.getBlockState(pos);
                net.minecraft.block.BlockState stateAbove = world.getBlockState(posAbove);
                net.minecraft.block.BlockState stateAbove2 = world.getBlockState(posAbove2);
                
                // 检查是否是安全的方块类型
                boolean solidBase = !state.isAir() && 
                                    state.isFullCube(world, pos) && 
                                    !(state.getBlock() instanceof net.minecraft.block.LeavesBlock) &&
                                    !state.shouldSuffocate(world, pos);
                
                boolean safeSpace = stateAbove.isAir() && stateAbove2.isAir();
                
                if (solidBase && safeSpace) {
                    // 额外检查上方是否有任何固体方块
                    boolean clearPath = true;
                    for (int checkY = 3; checkY <= 5; checkY++) {
                        net.minecraft.block.BlockState checkState = world.getBlockState(pos.up(checkY));
                        if (!checkState.isAir() && !(checkState.getBlock() instanceof net.minecraft.block.LeavesBlock)) {
                            clearPath = false;
                            break;
                        }
                    }
                    
                    if (clearPath) {
                        return y + 1; // 返回方块上方的坐标
                    }
                }
            }
            
            return -1; // 未找到合适位置
        }

        private boolean teleportNearTarget(LivingEntity target, ServerWorld world) {
            // 优先传送到目标背后
            double targetYaw = target.getYaw() * Math.PI / 180.0; // 转换为弧度
            double offsetX = -Math.sin(targetYaw) * 6.0; // 在目标背后6格
            double offsetZ = -Math.cos(targetYaw) * 6.0;
            
            double newX = target.getX() + offsetX;
            double newZ = target.getZ() + offsetZ;
            
            // 使用更严格的安全位置检查
            int newY = findSafeLocation(world, newX, newZ);
            
            if (newY > 0) {
                if (teleportToLocation(newX, newY, newZ)) {
                    world.sendEntityStatus(boss, TELEPORT_STATE);
                    return true;
                }
            }
            
            // 如果无法传送到背后，尝试传送到目标侧面
            double[] angles = {Math.PI/2, -Math.PI/2, 2*Math.PI/3, -2*Math.PI/3}; // 不同的角度
            
            for (double angleOffset : angles) {
                double angle = targetYaw + angleOffset;
                offsetX = -Math.sin(angle) * 5.0; // 距离减少到5格
                offsetZ = -Math.cos(angle) * 5.0;
                
                newX = target.getX() + offsetX;
                newZ = target.getZ() + offsetZ;
                
                // 使用更严格的安全位置检查
                newY = findSafeLocation(world, newX, newZ);
                
                if (newY > 0) {
                    if (teleportToLocation(newX, newY, newZ)) {
                        world.sendEntityStatus(boss, TELEPORT_STATE);
                        return true;
                    }
                }
            }
            
            return false;
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

            // 优先尝试传送到目标附近的位置
            if (teleportNearTarget(target, world)) {
                return;
            }

            // 如果无法传送到目标附近，尝试25次找到一个安全的传送位置
            for (int i = 0; i < 25; i++) {
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

                // 使用更安全的位置查找方法
                int newY = findSafeLocation(world, newX, newZ);

                if (newY > 0) {
                    // 执行传送
                    if (teleportToLocation(newX, newY, newZ)) {
                        // 传送成功
                        world.sendEntityStatus(boss, TELEPORT_STATE);
                        return;
                    }
                }
            }
            
            // 如果所有尝试都失败，尝试紧急传送
            emergencyTeleport();
        }

        /**
         * 将实体传送到指定坐标，并在传送后进行额外的安全检查
         */
        private boolean teleportToLocation(double x, double y, double z) {
            // 保存原始位置，以便在传送不安全时能够回退
            double oldX = boss.getX();
            double oldY = boss.getY();
            double oldZ = boss.getZ();
            
            // 设置位置并刷新
            boss.refreshPositionAndAngles(x, y, z, boss.getYaw(), boss.getPitch());
            
            // 获取所在世界
            ServerWorld serverWorld = null;
            if (boss.getWorld() instanceof ServerWorld) {
                serverWorld = (ServerWorld) boss.getWorld();
                
                // 安全检查：确认新位置不在实体方块内
                net.minecraft.util.math.BlockPos newPos = boss.getBlockPos();
                net.minecraft.util.math.BlockPos headPos = newPos.up();
                
                net.minecraft.block.BlockState blockState = serverWorld.getBlockState(newPos);
                net.minecraft.block.BlockState headBlockState = serverWorld.getBlockState(headPos);
                
                boolean unsafe = (blockState.shouldSuffocate(serverWorld, newPos) || 
                                  headBlockState.shouldSuffocate(serverWorld, headPos) ||
                                  blockState.getBlock() instanceof net.minecraft.block.LeavesBlock ||
                                  headBlockState.getBlock() instanceof net.minecraft.block.LeavesBlock);
                
                // 如果新位置不安全，回退到原始位置
                if (unsafe) {
                    boss.refreshPositionAndAngles(oldX, oldY, oldZ, boss.getYaw(), boss.getPitch());
                    return false;
                }
                
                // 在原位置产生紫色粒子
                serverWorld.spawnParticles(
                        net.minecraft.particle.ParticleTypes.PORTAL,
                        oldX, boss.getBodyY(0.5), oldZ,
                        30, 0.2, 0.2, 0.2, 0.0);

                // 播放末影人传送声音
                serverWorld.playSound(
                        null, oldX, oldY, oldZ,
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
}