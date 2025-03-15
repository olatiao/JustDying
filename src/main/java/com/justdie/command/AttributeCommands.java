package com.justdie.command;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeHelper;
import com.justdie.attribute.AttributeManager;
import com.justdie.attribute.JustDyingAttribute;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;

/**
 * 属性命令类
 * 提供获取和设置玩家属性的命令
 */
public class AttributeCommands {

    /**
     * 注册所有属性命令
     * 
     * @param dispatcher 命令调度器
     * @param registryAccess 注册表访问
     * @param environment 环境
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // 获取玩家属性命令
        dispatcher.register(CommandManager.literal("attribute")
            .requires(source -> source.hasPermissionLevel(0)) // 所有玩家都可以使用
            .then(CommandManager.literal("get")
                .executes(context -> getOwnAttributes(context))
                .then(CommandManager.argument("attribute", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        AttributeManager.getAllAttributes().forEach(attr -> 
                            builder.suggest(attr.getId().getPath()));
                        return builder.buildFuture();
                    })
                    .executes(context -> getOwnAttribute(context, StringArgumentType.getString(context, "attribute")))
                )
            )
            // 管理员命令 - 获取其他玩家属性
            .then(CommandManager.literal("getplayer")
                .requires(source -> source.hasPermissionLevel(2)) // 需要管理员权限
                .then(CommandManager.argument("player", EntityArgumentType.players())
                    .executes(context -> getPlayerAttributes(context, EntityArgumentType.getPlayers(context, "player")))
                    .then(CommandManager.argument("attribute", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            AttributeManager.getAllAttributes().forEach(attr -> 
                                builder.suggest(attr.getId().getPath()));
                            return builder.buildFuture();
                        })
                        .executes(context -> getPlayerAttribute(
                            context, 
                            EntityArgumentType.getPlayers(context, "player"),
                            StringArgumentType.getString(context, "attribute")
                        ))
                    )
                )
            )
            // 管理员命令 - 设置玩家属性
            .then(CommandManager.literal("set")
                .requires(source -> source.hasPermissionLevel(2)) // 需要管理员权限
                .then(CommandManager.argument("player", EntityArgumentType.players())
                    .then(CommandManager.argument("attribute", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            AttributeManager.getAllAttributes().forEach(attr -> 
                                builder.suggest(attr.getId().getPath()));
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> setPlayerAttribute(
                                context, 
                                EntityArgumentType.getPlayers(context, "player"),
                                StringArgumentType.getString(context, "attribute"),
                                IntegerArgumentType.getInteger(context, "value")
                            ))
                        )
                    )
                )
            )
            // 管理员命令 - 设置玩家可用属性点
            .then(CommandManager.literal("points")
                .requires(source -> source.hasPermissionLevel(2)) // 需要管理员权限
                .then(CommandManager.argument("player", EntityArgumentType.players())
                    .then(CommandManager.argument("points", IntegerArgumentType.integer(0))
                        .executes(context -> setPlayerPoints(
                            context, 
                            EntityArgumentType.getPlayers(context, "player"),
                            IntegerArgumentType.getInteger(context, "points")
                        ))
                    )
                )
            )
            // 管理员命令 - 添加玩家可用属性点
            .then(CommandManager.literal("addpoints")
                .requires(source -> source.hasPermissionLevel(2)) // 需要管理员权限
                .then(CommandManager.argument("player", EntityArgumentType.players())
                    .then(CommandManager.argument("points", IntegerArgumentType.integer(1))
                        .executes(context -> addPlayerPoints(
                            context, 
                            EntityArgumentType.getPlayers(context, "player"),
                            IntegerArgumentType.getInteger(context, "points")
                        ))
                    )
                )
            )
            // 管理员命令 - 设置属性上限
            .then(CommandManager.literal("maxvalue")
                .requires(source -> source.hasPermissionLevel(2)) // 需要管理员权限
                .then(CommandManager.argument("attribute", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        AttributeManager.getAllAttributes().forEach(attr -> 
                            builder.suggest(attr.getId().getPath()));
                        return builder.buildFuture();
                    })
                    .then(CommandManager.argument("maxvalue", IntegerArgumentType.integer(1, 10000))
                        .executes(context -> setAttributeMaxValue(
                            context,
                            StringArgumentType.getString(context, "attribute"),
                            IntegerArgumentType.getInteger(context, "maxvalue")
                        ))
                    )
                )
            )
        );
    }

    /**
     * 获取自己的所有属性
     */
    private static int getOwnAttributes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        return getPlayerAttributes(context, java.util.Collections.singleton(player));
    }

    /**
     * 获取自己的指定属性
     */
    private static int getOwnAttribute(CommandContext<ServerCommandSource> context, String attributeName) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        return getPlayerAttribute(context, java.util.Collections.singleton(player), attributeName);
    }

    /**
     * 获取玩家的所有属性
     */
    private static int getPlayerAttributes(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) {
        int count = 0;
        for (ServerPlayerEntity player : players) {
            context.getSource().sendFeedback(() -> Text.literal("===== ").append(player.getDisplayName()).append(" =====").formatted(Formatting.GOLD), false);
            
            // 显示可用点数
            int points = AttributeHelper.getAvailablePoints(player);
            context.getSource().sendFeedback(() -> Text.translatable("gui." + JustDying.MOD_ID + ".available_points", points).formatted(Formatting.GREEN), false);
            
            // 显示所有属性
            for (JustDyingAttribute attribute : AttributeManager.getAllAttributes()) {
                int value = AttributeHelper.getAttributeValue(player, attribute.getId());
                
                // 使用final变量捕获
                final int attrValue = value;
                final JustDyingAttribute attr = attribute;
                
                context.getSource().sendFeedback(() -> {
                    String effectText = "";
                    if (attr.getVanillaAttribute() != null) {
                        double bonus = attr.calculateAttributeBonus(attrValue);
                        effectText = String.format(" (+%.1f %s)", bonus, attr.getVanillaAttribute().getTranslationKey());
                    }
                    
                    return Text.literal("- ").append(attr.getName())
                        .append(": " + attrValue + "/" + attr.getMaxValue())
                        .append(Text.literal(effectText).formatted(Formatting.AQUA))
                        .formatted(Formatting.YELLOW);
                }, false);
            }
            count++;
        }
        return count;
    }

    /**
     * 获取玩家的指定属性
     */
    private static int getPlayerAttribute(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, String attributeName) {
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
        Optional<JustDyingAttribute> attributeOpt = AttributeManager.getAttribute(attributeId);
        
        if (attributeOpt.isEmpty()) {
            context.getSource().sendError(Text.literal("属性不存在: " + attributeName));
            return 0;
        }
        
        JustDyingAttribute attribute = attributeOpt.get();
        int count = 0;
        
        for (ServerPlayerEntity player : players) {
            int value = AttributeHelper.getAttributeValue(player, attributeId);
            
            // 使用final变量捕获
            final int playerValue = value;
            
            context.getSource().sendFeedback(() -> {
                String effectText = "";
                if (attribute.getVanillaAttribute() != null) {
                    double bonus = attribute.calculateAttributeBonus(playerValue);
                    effectText = String.format(" (+%.1f %s)", bonus, attribute.getVanillaAttribute().getTranslationKey());
                }
                
                return Text.literal(player.getDisplayName().getString() + " - ")
                    .append(attribute.getName())
                    .append(": " + playerValue + "/" + attribute.getMaxValue())
                    .append(Text.literal(effectText).formatted(Formatting.AQUA))
                    .formatted(Formatting.YELLOW);
            }, false);
            
            count++;
        }
        
        return count;
    }

    /**
     * 设置玩家的指定属性
     */
    private static int setPlayerAttribute(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, String attributeName, int value) {
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
        Optional<JustDyingAttribute> attributeOpt = AttributeManager.getAttribute(attributeId);
        
        if (attributeOpt.isEmpty()) {
            context.getSource().sendError(Text.literal("属性不存在: " + attributeName));
            return 0;
        }
        
        JustDyingAttribute attribute = attributeOpt.get();
        int count = 0;
        
        // 确保值在有效范围内
        int validValue = Math.max(attribute.getMinValue(), Math.min(value, attribute.getMaxValue()));
        
        for (ServerPlayerEntity player : players) {
            AttributeHelper.setAttributeValue(player, attributeId, validValue);
            
            context.getSource().sendFeedback(() -> 
                Text.literal("已将 ")
                    .append(player.getDisplayName())
                    .append(" 的 ")
                    .append(attribute.getName())
                    .append(" 设置为 " + validValue)
                    .formatted(Formatting.GREEN), 
                true);
            count++;
        }
        
        return count;
    }

    /**
     * 设置玩家的可用属性点
     */
    private static int setPlayerPoints(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, int points) {
        int count = 0;
        
        for (ServerPlayerEntity player : players) {
            AttributeHelper.setAvailablePoints(player, points);
            
            context.getSource().sendFeedback(() -> 
                Text.literal("已将 ")
                    .append(player.getDisplayName())
                    .append(" 的可用属性点设置为 " + points)
                    .formatted(Formatting.GREEN), 
                true);
            count++;
        }
        
        return count;
    }

    /**
     * 添加玩家的可用属性点
     */
    private static int addPlayerPoints(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, int points) {
        int count = 0;
        
        for (ServerPlayerEntity player : players) {
            AttributeHelper.addPoints(player, points);
            int newPoints = AttributeHelper.getAvailablePoints(player);
            
            context.getSource().sendFeedback(() -> 
                Text.literal("已为 ")
                    .append(player.getDisplayName())
                    .append(" 添加 " + points + " 点属性点，当前可用点数: " + newPoints)
                    .formatted(Formatting.GREEN), 
                true);
            count++;
        }
        
        return count;
    }

    /**
     * 设置属性的最大值
     * 
     * @param context 命令上下文
     * @param attributePath 属性路径
     * @param maxValue 新的最大值
     * @return 命令结果
     */
    private static int setAttributeMaxValue(CommandContext<ServerCommandSource> context, String attributePath, int maxValue) {
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributePath);
        
        boolean success = AttributeManager.updateAttributeMaxValue(attributeId, maxValue);
        
        if (success) {
            // 通知所有玩家属性上限已更新 - 移除通知，只调整超过上限的属性值
            context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
                // 获取当前属性值
                int currentValue = AttributeHelper.getAttributeValue(player, attributeId);
                
                // 如果当前值超过新的上限，则调整为新上限
                if (currentValue > maxValue) {
                    AttributeHelper.setAttributeValue(player, attributeId, maxValue);
                }
            });
            
            // 只向命令执行者发送反馈，不广播给所有玩家
            context.getSource().sendFeedback(
                () -> Text.translatable("command.justdying.attribute.maxvalue.success", 
                        attributePath, maxValue)
                        .formatted(Formatting.GREEN),
                false // 改为false，不广播
            );
            
            return 1;
        } else {
            context.getSource().sendError(
                Text.translatable("command.justdying.attribute.maxvalue.failed", 
                        attributePath)
            );
            
            return 0;
        }
    }
} 