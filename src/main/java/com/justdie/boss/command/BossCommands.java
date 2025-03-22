package com.justdie.boss.command;

import com.justdie.JustDying;
import com.justdie.boss.registry.BossRegistry;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * BOSS系统相关命令
 */
public class BossCommands {
    private static final String MSG_SPAWNED = "已在指定位置生成法师BOSS";
    private static final String MSG_NO_POSITION = "请指定生成位置";
    private static final String MSG_ERROR = "生成BOSS时发生错误";
    
    /**
     * 注册命令
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // 注册召唤BOSS命令
            dispatcher.register(
                CommandManager.literal("summonboss")
                    .requires(source -> source.hasPermissionLevel(2)) // 需要2级权限（OP）
                    .executes(context -> {
                        // 在玩家位置生成BOSS
                        return summonBossAtPlayer(context.getSource());
                    })
                    .then(CommandManager.argument("position", Vec3ArgumentType.vec3())
                        .executes(context -> {
                            // 在指定位置生成BOSS
                            Vec3d pos = Vec3ArgumentType.getVec3(context, "position");
                            return summonBossAtPosition(context.getSource(), pos);
                        }))
            );
        });
        
        JustDying.BOSS_LOGGER.info("BOSS命令系统已注册");
    }
    
    /**
     * 在玩家位置生成BOSS
     * 
     * @param source 命令源
     * @return 命令执行结果
     */
    private static int summonBossAtPlayer(ServerCommandSource source) {
        try {
            // 获取玩家
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.of(MSG_NO_POSITION));
                return 0;
            }
            
            // 获取玩家位置
            Vec3d pos = player.getPos();
            
            // 生成BOSS
            return summonBossAtPosition(source, pos);
        } catch (Exception e) {
            // 处理错误
            source.sendError(Text.of(MSG_ERROR + ": " + e.getMessage()));
            JustDying.BOSS_LOGGER.error("生成BOSS失败", e);
            return 0;
        }
    }
    
    /**
     * 在指定位置生成BOSS
     * 
     * @param source 命令源
     * @param pos 位置
     * @return 命令执行结果
     */
    private static int summonBossAtPosition(ServerCommandSource source, Vec3d pos) {
        try {
            // 创建BOSS实体
            var boss = BossRegistry.SORCERER_BOSS.create(source.getWorld());
            if (boss == null) {
                source.sendError(Text.of(MSG_ERROR));
                return 0;
            }
            
            // 设置位置
            boss.setPosition(pos.x, pos.y, pos.z);
            
            // 添加到世界
            source.getWorld().spawnEntity(boss);
            
            // 发送成功消息
            source.sendFeedback(() -> Text.of(MSG_SPAWNED), true);
            return 1;
        } catch (Exception e) {
            // 处理错误
            source.sendError(Text.of(MSG_ERROR + ": " + e.getMessage()));
            JustDying.BOSS_LOGGER.error("生成BOSS失败", e);
            return 0;
        }
    }
} 