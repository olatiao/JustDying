package com.justdie.boss.entity;

import com.justdie.boss.interfaces.*;
import com.justdie.boss.utils.EventScheduler;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * BOSS实体的基础类，提供通用功能
 */
public abstract class BaseBossEntity extends PathAwareEntity {
    // 事件调度器
    protected final EventScheduler preTickEvents = new EventScheduler();
    protected final EventScheduler postTickEvents = new EventScheduler();
    
    // 组件接口
    protected IDamageHandler damageHandler;
    protected IStatusHandler statusHandler;
    protected IEntityTick<ServerWorld> serverTick;
    protected IMoveHandler moveHandler;
    protected INbtHandler nbtHandler;
    protected IEntityTick<ServerWorld> deathServerTick;
    
    // BOSS血条
    protected ServerBossBar bossBar;
    
    // 初始位置记录（用于重置行为）
    protected Vec3d idlePosition = Vec3d.ZERO;
    
    public BaseBossEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.ignoreCameraFrustum = true; // 确保BOSS总是被渲染
        
        if (!world.isClient()) {
            this.bossBar = new ServerBossBar(
                this.getDisplayName(), 
                BossBar.Color.RED, 
                BossBar.Style.NOTCHED_6
            );
        }
    }
    
    @Override
    public void tick() {
        preTickEvents.updateEvents();
        
        if (idlePosition == Vec3d.ZERO) {
            idlePosition = this.getPos();
        }
        
        // 客户端和服务端特定处理
        if (this.getWorld().isClient()) {
            clientTick();
        } else if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverTick(serverWorld);
            if (serverTick != null) {
                serverTick.tick(serverWorld);
            }
        }
        
        super.tick();
        postTickEvents.updateEvents();
    }
    
    // 客户端和服务端特定的tick方法
    protected void clientTick() {}
    protected void serverTick(ServerWorld world) {}
    
    @Override
    public void updatePostDeath() {
        if (this.getWorld().isClient()) {
            super.updatePostDeath();
        } else if (this.getWorld() instanceof ServerWorld serverWorld && deathServerTick != null) {
            deathServerTick.tick(serverWorld);
        } else {
            super.updatePostDeath();
        }
    }
    
    @Override
    public void mobTick() {
        super.mobTick();
        if (bossBar != null) {
            bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        }
    }
    
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }
    
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (damageHandler != null) {
            boolean result = damageHandler.beforeDamage(this, source, amount);
            if (!result) return false;
        }
        
        boolean damaged = super.damage(source, amount);
        
        if (damageHandler != null) {
            damageHandler.afterDamage(this, source, amount, damaged);
        }
        
        return damaged;
    }
    
    @Override
    public void handleStatus(byte status) {
        super.handleStatus(status);
        if (statusHandler != null) {
            statusHandler.handleStatus(status);
        }
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbtHandler != null) {
            nbtHandler.fromTag(nbt);
        }
    }
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (nbtHandler != null) {
            nbtHandler.toTag(nbt);
        }
    }
    
    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if (moveHandler != null && !moveHandler.canMove(movementType, movement)) {
            return;
        }
        super.move(movementType, movement);
    }
} 