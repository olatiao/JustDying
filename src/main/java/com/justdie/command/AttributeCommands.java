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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;

/**
 * 属性命令处理类
 * 注册并处理与属性相关的命令，提供属性查询、修改和管理功能
 */
public class AttributeCommands {
    // 命令常量定义
    private static final String CMD_ROOT = "attributes";
    private static final String CMD_GET = "get";
    private static final String CMD_SET = "set";
    private static final String CMD_POINTS = "points";
    private static final String CMD_POINTS_SET = "set";
    private static final String CMD_POINTS_INCREASE = "increase";
    private static final String CMD_POINTS_DECREASE = "decrease";
    private static final String CMD_ATTR_INCREASE = "increase";
    private static final String CMD_ATTR_DECREASE = "decrease";
    private static final String CMD_MAX = "max";
    private static final String CMD_MAX_INCREASE = "increase";
    private static final String CMD_MAX_DECREASE = "decrease";
    private static final String CMD_MAX_SET = "set";
    private static final String CMD_LIST = "list";
    private static final String CMD_HELP = "help";
    
    // 参数常量定义
    private static final String ARG_ATTRIBUTE = "attribute";
    private static final String ARG_VALUE = "value";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_POINTS = "points";
    
    // 消息常量定义
    private static final String MSG_CURRENT_VALUE = "当前属性 %s: %d";
    private static final String MSG_AVAILABLE_POINTS = "当前可用点数: %d";
    private static final String MSG_ATTRIBUTE_SET = "已设置 %s 的 %s 属性为 %d";
    private static final String MSG_ATTRIBUTE_INCREASED = "已增加 %s 的 %s 属性 %d 点，现在为 %d";
    private static final String MSG_ATTRIBUTE_DECREASED = "已减少 %s 的 %s 属性 %d 点，现在为 %d";
    private static final String MSG_POINTS_SET = "已设置 %s 的可用点数为 %d";
    private static final String MSG_POINTS_ADDED = "已为 %s 添加 %d 点数";
    private static final String MSG_POINTS_DECREASED = "已为 %s 减少 %d 点数";
    private static final String MSG_MAX_VALUE_SET = "已设置 %s 的最大值为 %d";
    private static final String MSG_MAX_VALUE_INCREASED = "已增加 %s 的最大值 %d 点，现在为 %d";
    private static final String MSG_MAX_VALUE_DECREASED = "已减少 %s 的最大值 %d 点，现在为 %d";
    private static final String MSG_ATTRIBUTE_NOT_FOUND = "找不到属性: %s";
    private static final String MSG_ERROR_GETTING_ATTRIBUTE = "获取属性 %s 时出错";
    private static final String MSG_PLAYER_NOT_FOUND = "找不到玩家: %s";
    private static final String MSG_INVALID_VALUE = "无效的值: %d";
    private static final String MSG_COMMAND_ERROR = "执行命令时出错: %s";
    private static final String MSG_OPERATION_FAILED = "操作失败，请检查日志获取详细信息";
    private static final String MSG_LIST_HEADER = "可用属性:";
    private static final String MSG_LIST_ITEM = " - %s (%s): %d/%d";
    private static final String MSG_HELP_HEADER = "属性命令帮助:";
    private static final String MSG_HELP_GET = " - /attributes get <属性>: 查看属性值";
    private static final String MSG_HELP_ATTR_INCREASE = " - /attributes increase <属性> <值> [玩家]: 增加属性值";
    private static final String MSG_HELP_ATTR_DECREASE = " - /attributes decrease <属性> <值> [玩家]: 减少属性值";
    private static final String MSG_HELP_POINTS = " - /attributes points: 查看可用点数";
    private static final String MSG_HELP_POINTS_SET = " - /attributes points set <点数> [玩家]: 设置可用点数";
    private static final String MSG_HELP_POINTS_INCREASE = " - /attributes points increase <点数> [玩家]: 增加可用点数";
    private static final String MSG_HELP_POINTS_DECREASE = " - /attributes points decrease <点数> [玩家]: 减少可用点数";
    private static final String MSG_HELP_MAX = " - /attributes max set <属性> <值>: 设置属性最大值";
    private static final String MSG_HELP_MAX_INCREASE = " - /attributes max increase <属性> <值>: 增加属性最大值";
    private static final String MSG_HELP_MAX_DECREASE = " - /attributes max decrease <属性> <值>: 减少属性最大值";
    private static final String MSG_HELP_LIST = " - /attributes list: 列出所有属性";
    
    // 权限等级
    private static final int PERMISSION_LEVEL_ADMIN = 2;
    
    /**
     * 注册所有属性相关命令
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register(AttributeCommands::registerCommands);
        JustDying.LOGGER.info("已注册属性命令");
    }
    
    /**
     * 注册命令到命令分发器
     */
    private static void registerCommands(
            CommandDispatcher<ServerCommandSource> dispatcher, 
            CommandRegistryAccess registryAccess, 
            CommandManager.RegistrationEnvironment environment) {
        
        try {
            // 注册获取属性命令 (权限等级 0)
            registerGetCommand(dispatcher);
            
            // 注册属性增加命令 (权限等级 2)
            registerAttributeIncreaseCommand(dispatcher);
            
            // 注册属性减少命令 (权限等级 2)
            registerAttributeDecreaseCommand(dispatcher);
            
            // 注册获取可用点数命令 (权限等级 0)
            registerPointsCommand(dispatcher);
            
            // 注册设置最大值命令 (权限等级 2)
            registerMaxCommand(dispatcher);
            
            // 注册列出属性命令 (权限等级 0)
            registerListCommand(dispatcher);
            
            // 注册帮助命令 (权限等级 0)
            registerHelpCommand(dispatcher);
            
            JustDying.LOGGER.debug("已成功注册所有属性命令");
        } catch (Exception e) {
            JustDying.LOGGER.error("注册属性命令时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 注册获取属性命令
     */
    private static void registerGetCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal(CMD_ROOT)
                .then(CommandManager.literal(CMD_GET)
                    .then(CommandManager.argument(ARG_ATTRIBUTE, StringArgumentType.word())
                        .suggests((context, builder) -> {
                            AttributeManager.getAllAttributes().forEach(attr -> 
                                builder.suggest(attr.getId().getPath()));
                            return builder.buildFuture();
                        })
                        .executes(context -> getAttributeCommand(context, getPlayerFromContext(context)))
                    )
                )
        );
    }
    
    /**
     * 注册属性值增加命令
     */
    private static void registerAttributeIncreaseCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal(CMD_ROOT)
                .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL_ADMIN))
                .then(CommandManager.literal(CMD_ATTR_INCREASE)
                    .then(CommandManager.argument(ARG_ATTRIBUTE, StringArgumentType.word())
                        .suggests((context, builder) -> {
                            AttributeManager.getAllAttributes().forEach(attr -> 
                                builder.suggest(attr.getId().getPath()));
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument(ARG_VALUE, IntegerArgumentType.integer(1))
                            .then(CommandManager.argument(ARG_PLAYER, StringArgumentType.word())
                                .executes(context -> {
                                    try {
                                        ServerPlayerEntity player = getPlayerByName(
                                                context, 
                                                StringArgumentType.getString(context, ARG_PLAYER));
                                        if (player == null) {
                                            sendErrorMessage(context, MSG_PLAYER_NOT_FOUND, 
                                                    StringArgumentType.getString(context, ARG_PLAYER));
                                            return 0;
                                        }
                                        return increaseAttributeCommand(
                                                context, 
                                                player, 
                                                StringArgumentType.getString(context, ARG_ATTRIBUTE),
                                                IntegerArgumentType.getInteger(context, ARG_VALUE));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                            .executes(context -> {
                                try {
                                    return increaseAttributeCommand(
                                            context,
                                            getPlayerFromContext(context),
                                            StringArgumentType.getString(context, ARG_ATTRIBUTE),
                                            IntegerArgumentType.getInteger(context, ARG_VALUE));
                                } catch (Exception e) {
                                    handleCommandException(context, e);
                                    return 0;
                                }
                            })
                        )
                    )
                )
        );
    }
    
    /**
     * 注册属性值减少命令
     */
    private static void registerAttributeDecreaseCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal(CMD_ROOT)
                .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL_ADMIN))
                .then(CommandManager.literal(CMD_ATTR_DECREASE)
                    .then(CommandManager.argument(ARG_ATTRIBUTE, StringArgumentType.word())
                        .suggests((context, builder) -> {
                            AttributeManager.getAllAttributes().forEach(attr -> 
                                builder.suggest(attr.getId().getPath()));
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument(ARG_VALUE, IntegerArgumentType.integer(1))
                            .then(CommandManager.argument(ARG_PLAYER, StringArgumentType.word())
                                .executes(context -> {
                                    try {
                                        ServerPlayerEntity player = getPlayerByName(
                                                context, 
                                                StringArgumentType.getString(context, ARG_PLAYER));
                                        if (player == null) {
                                            sendErrorMessage(context, MSG_PLAYER_NOT_FOUND, 
                                                    StringArgumentType.getString(context, ARG_PLAYER));
                                            return 0;
                                        }
                                        return decreaseAttributeCommand(
                                                context, 
                                                player, 
                                                StringArgumentType.getString(context, ARG_ATTRIBUTE),
                                                IntegerArgumentType.getInteger(context, ARG_VALUE));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                            .executes(context -> {
                                try {
                                    return decreaseAttributeCommand(
                                            context,
                                            getPlayerFromContext(context),
                                            StringArgumentType.getString(context, ARG_ATTRIBUTE),
                                            IntegerArgumentType.getInteger(context, ARG_VALUE));
                                } catch (Exception e) {
                                    handleCommandException(context, e);
                                    return 0;
                                }
                            })
                        )
                    )
                )
        );
    }
    
    /**
     * 注册获取可用点数命令
     */
    private static void registerPointsCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal(CMD_ROOT)
                .then(CommandManager.literal(CMD_POINTS)
                    .executes(context -> getPointsCommand(context, getPlayerFromContext(context)))
                    .then(CommandManager.literal(CMD_POINTS_SET)
                        .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL_ADMIN))
                        .then(CommandManager.argument(ARG_POINTS, IntegerArgumentType.integer(0))
                            .then(CommandManager.argument(ARG_PLAYER, StringArgumentType.word())
                                .executes(context -> {
                                    try {
                                        ServerPlayerEntity player = getPlayerByName(
                                                context, 
                                                StringArgumentType.getString(context, ARG_PLAYER));
                                        if (player == null) {
                                            sendErrorMessage(context, MSG_PLAYER_NOT_FOUND, 
                                                    StringArgumentType.getString(context, ARG_PLAYER));
                                            return 0;
                                        }
                                        return setPointsCommand(
                                                context, 
                                                player,
                                                IntegerArgumentType.getInteger(context, ARG_POINTS));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                            .executes(context -> {
                                try {
                                    return setPointsCommand(
                                            context,
                                            getPlayerFromContext(context),
                                            IntegerArgumentType.getInteger(context, ARG_POINTS));
                                } catch (Exception e) {
                                    handleCommandException(context, e);
                                    return 0;
                                }
                            })
                        )
                    )
                    .then(CommandManager.literal(CMD_POINTS_INCREASE)
                        .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL_ADMIN))
                        .then(CommandManager.argument(ARG_POINTS, IntegerArgumentType.integer(1))
                            .then(CommandManager.argument(ARG_PLAYER, StringArgumentType.word())
                                .executes(context -> {
                                    try {
                                        ServerPlayerEntity player = getPlayerByName(
                                                context, 
                                                StringArgumentType.getString(context, ARG_PLAYER));
                                        if (player == null) {
                                            sendErrorMessage(context, MSG_PLAYER_NOT_FOUND, 
                                                    StringArgumentType.getString(context, ARG_PLAYER));
                                            return 0;
                                        }
                                        return increasePointsCommand(
                                                context, 
                                                player,
                                                IntegerArgumentType.getInteger(context, ARG_POINTS));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                            .executes(context -> {
                                try {
                                    return increasePointsCommand(
                                            context,
                                            getPlayerFromContext(context),
                                            IntegerArgumentType.getInteger(context, ARG_POINTS));
                                } catch (Exception e) {
                                    handleCommandException(context, e);
                                    return 0;
                                }
                            })
                        )
                    )
                    .then(CommandManager.literal(CMD_POINTS_DECREASE)
                        .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL_ADMIN))
                        .then(CommandManager.argument(ARG_POINTS, IntegerArgumentType.integer(1))
                            .then(CommandManager.argument(ARG_PLAYER, StringArgumentType.word())
                                .executes(context -> {
                                    try {
                                        ServerPlayerEntity player = getPlayerByName(
                                                context, 
                                                StringArgumentType.getString(context, ARG_PLAYER));
                                        if (player == null) {
                                            sendErrorMessage(context, MSG_PLAYER_NOT_FOUND, 
                                                    StringArgumentType.getString(context, ARG_PLAYER));
                                            return 0;
                                        }
                                        return decreasePointsCommand(
                                                context, 
                                                player,
                                                IntegerArgumentType.getInteger(context, ARG_POINTS));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                            .executes(context -> {
                                try {
                                    return decreasePointsCommand(
                                            context,
                                            getPlayerFromContext(context),
                                            IntegerArgumentType.getInteger(context, ARG_POINTS));
                                } catch (Exception e) {
                                    handleCommandException(context, e);
                                    return 0;
                                }
                            })
                        )
                    )
                )
        );
    }
    
    /**
     * 注册设置最大值命令
     */
    private static void registerMaxCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal(CMD_ROOT)
                .requires(source -> source.hasPermissionLevel(PERMISSION_LEVEL_ADMIN))
                .then(CommandManager.literal(CMD_MAX)
                    .then(CommandManager.literal(CMD_MAX_SET)
                        .then(CommandManager.argument(ARG_ATTRIBUTE, StringArgumentType.word())
                            .suggests((context, builder) -> {
                                AttributeManager.getAllAttributes().forEach(attr -> 
                                    builder.suggest(attr.getId().getPath()));
                                return builder.buildFuture();
                            })
                            .then(CommandManager.argument(ARG_VALUE, IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    try {
                                        return setMaxValueCommand(
                                                context,
                                                StringArgumentType.getString(context, ARG_ATTRIBUTE),
                                                IntegerArgumentType.getInteger(context, ARG_VALUE));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                        )
                    )
                    .then(CommandManager.literal(CMD_MAX_INCREASE)
                        .then(CommandManager.argument(ARG_ATTRIBUTE, StringArgumentType.word())
                            .suggests((context, builder) -> {
                                AttributeManager.getAllAttributes().forEach(attr -> 
                                    builder.suggest(attr.getId().getPath()));
                                return builder.buildFuture();
                            })
                            .then(CommandManager.argument(ARG_VALUE, IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    try {
                                        return increaseMaxValueCommand(
                                                context,
                                                StringArgumentType.getString(context, ARG_ATTRIBUTE),
                                                IntegerArgumentType.getInteger(context, ARG_VALUE));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                        )
                    )
                    .then(CommandManager.literal(CMD_MAX_DECREASE)
                        .then(CommandManager.argument(ARG_ATTRIBUTE, StringArgumentType.word())
                            .suggests((context, builder) -> {
                                AttributeManager.getAllAttributes().forEach(attr -> 
                                    builder.suggest(attr.getId().getPath()));
                                return builder.buildFuture();
                            })
                            .then(CommandManager.argument(ARG_VALUE, IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    try {
                                        return decreaseMaxValueCommand(
                                                context,
                                                StringArgumentType.getString(context, ARG_ATTRIBUTE),
                                                IntegerArgumentType.getInteger(context, ARG_VALUE));
                                    } catch (Exception e) {
                                        handleCommandException(context, e);
                                        return 0;
                                    }
                                })
                            )
                        )
                    )
                )
        );
    }
    
    /**
     * 注册列出属性命令
     */
    private static void registerListCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal(CMD_ROOT)
                .then(CommandManager.literal(CMD_LIST)
                    .executes(context -> {
                        try {
                            return listAttributesCommand(context, getPlayerFromContext(context));
                        } catch (Exception e) {
                            handleCommandException(context, e);
                            return 0;
                        }
                    })
                )
        );
    }
    
    /**
     * 注册帮助命令
     */
    private static void registerHelpCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal(CMD_ROOT)
                .then(CommandManager.literal(CMD_HELP)
                    .executes(context -> {
                        try {
                            return showHelpCommand(context);
                        } catch (Exception e) {
                            handleCommandException(context, e);
                            return 0;
                        }
                    })
                )
                .executes(context -> {
                    try {
                        return showHelpCommand(context);
                    } catch (Exception e) {
                        handleCommandException(context, e);
                        return 0;
                    }
                })
        );
    }
    
    /**
     * 处理获取属性命令
     */
    private static int getAttributeCommand(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        try {
            String attributeName = StringArgumentType.getString(context, ARG_ATTRIBUTE);
            
            Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
            
            // 验证属性存在
            if (!validateAttribute(context, attributeId)) {
                return 0;
            }
            
            int value = AttributeHelper.getAttributeValue(player, attributeId);
            
            // 发送结果消息
            sendSuccessMessage(context, MSG_CURRENT_VALUE, attributeName, value);
            
            return 1;
        } catch (Exception e) {
            handleCommandException(context, e);
            return 0;
        }
    }
    
    /**
     * 处理增加属性值命令
     */
    private static int increaseAttributeCommand(
            CommandContext<ServerCommandSource> context, 
            ServerPlayerEntity player, 
            String attributeName, 
            int value) {
        
        if (value <= 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, value);
            return 0;
        }
        
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
        
        // 验证属性存在
        if (!validateAttribute(context, attributeId)) {
            return 0;
        }
        
        // 获取当前值并验证
        int currentValue = AttributeHelper.getAttributeValue(player, attributeId);
        int maxValue = AttributeHelper.getAttributeMaxValue(attributeId);
        
        if (currentValue + value > maxValue) {
            sendErrorMessage(context, "无法增加属性，最大值为 %d，当前值为 %d，尝试增加 %d", 
                    maxValue, currentValue, value);
            return 0;
        }
        
        // 增加属性值
        boolean success = AttributeHelper.addAttributeValue(player, attributeId, value);
        
        if (success) {
            int newValue = AttributeHelper.getAttributeValue(player, attributeId);
            // 发送成功消息
            sendSuccessMessage(context, MSG_ATTRIBUTE_INCREASED, 
                    player.getName().getString(), attributeName, value, newValue);
            return 1;
        } else {
            sendErrorMessage(context, MSG_OPERATION_FAILED);
            return 0;
        }
    }
    
    /**
     * 处理减少属性值命令
     */
    private static int decreaseAttributeCommand(
            CommandContext<ServerCommandSource> context, 
            ServerPlayerEntity player, 
            String attributeName, 
            int value) {
        
        if (value <= 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, value);
            return 0;
        }
        
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
        
        // 验证属性存在
        if (!validateAttribute(context, attributeId)) {
            return 0;
        }
        
        // 获取当前值并验证
        int currentValue = AttributeHelper.getAttributeValue(player, attributeId);
        int minValue = AttributeHelper.getAttributeMinValue(attributeId);
        
        if (currentValue - value < minValue) {
            sendErrorMessage(context, "无法减少属性，最小值为 %d，当前值为 %d，尝试减少 %d", 
                    minValue, currentValue, value);
            return 0;
        }
        
        // 减少属性值
        boolean success = AttributeHelper.addAttributeValue(player, attributeId, -value);
        
        if (success) {
            int newValue = AttributeHelper.getAttributeValue(player, attributeId);
            // 发送成功消息
            sendSuccessMessage(context, MSG_ATTRIBUTE_DECREASED, 
                    player.getName().getString(), attributeName, value, newValue);
            return 1;
        } else {
            sendErrorMessage(context, MSG_OPERATION_FAILED);
            return 0;
        }
    }
    
    /**
     * 处理获取点数命令
     */
    private static int getPointsCommand(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        try {
            int points = AttributeHelper.getAvailablePoints(player);
            
            // 发送结果消息
            sendSuccessMessage(context, MSG_AVAILABLE_POINTS, points);
            
            return 1;
        } catch (Exception e) {
            handleCommandException(context, e);
            return 0;
        }
    }
    
    /**
     * 处理设置点数命令
     */
    private static int setPointsCommand(
            CommandContext<ServerCommandSource> context, 
            ServerPlayerEntity player, 
            int points) {
        
        if (points < 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, points);
            return 0;
        }
        
        boolean success = AttributeHelper.setAvailablePoints(player, points);
        
        if (success) {
            // 发送成功消息
            sendSuccessMessage(context, MSG_POINTS_SET, player.getName().getString(), points);
            return 1;
        } else {
            sendErrorMessage(context, MSG_OPERATION_FAILED);
            return 0;
        }
    }
    
    /**
     * 处理设置最大值命令
     */
    private static int setMaxValueCommand(
            CommandContext<ServerCommandSource> context, 
            String attributeName, 
            int maxValue) {
        
        if (maxValue <= 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, maxValue);
            return 0;
        }
        
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
        
        // 验证属性存在
        if (!validateAttribute(context, attributeId)) {
            return 0;
        }
        
        Optional<JustDyingAttribute> attribute = AttributeManager.getAttribute(attributeId);
        if (attribute.isPresent()) {
            attribute.get().setMaxValue(maxValue);
            
            // 发送成功消息
            sendSuccessMessage(context, MSG_MAX_VALUE_SET, attributeName, maxValue);
            return 1;
        } else {
            sendErrorMessage(context, MSG_ATTRIBUTE_NOT_FOUND, attributeName);
            return 0;
        }
    }
    
    /**
     * 处理增加最大值命令
     */
    private static int increaseMaxValueCommand(
            CommandContext<ServerCommandSource> context, 
            String attributeName, 
            int increaseAmount) {
        
        if (increaseAmount <= 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, increaseAmount);
            return 0;
        }
        
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
        
        // 验证属性存在
        if (!validateAttribute(context, attributeId)) {
            return 0;
        }
        
        Optional<JustDyingAttribute> attribute = AttributeManager.getAttribute(attributeId);
        if (attribute.isPresent()) {
            JustDyingAttribute attr = attribute.get();
            int currentMax = attr.getMaxValue();
            int newMax = currentMax + increaseAmount;
            attr.setMaxValue(newMax);
            
            // 发送成功消息
            sendSuccessMessage(context, MSG_MAX_VALUE_INCREASED, attributeName, increaseAmount, newMax);
            return 1;
        } else {
            sendErrorMessage(context, MSG_ATTRIBUTE_NOT_FOUND, attributeName);
            return 0;
        }
    }
    
    /**
     * 处理减少最大值命令
     */
    private static int decreaseMaxValueCommand(
            CommandContext<ServerCommandSource> context, 
            String attributeName, 
            int decreaseAmount) {
        
        if (decreaseAmount <= 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, decreaseAmount);
            return 0;
        }
        
        Identifier attributeId = new Identifier(JustDying.MOD_ID, attributeName);
        
        // 验证属性存在
        if (!validateAttribute(context, attributeId)) {
            return 0;
        }
        
        Optional<JustDyingAttribute> attribute = AttributeManager.getAttribute(attributeId);
        if (attribute.isPresent()) {
            JustDyingAttribute attr = attribute.get();
            int currentMax = attr.getMaxValue();
            int newMax = Math.max(1, currentMax - decreaseAmount); // 确保最大值至少为1
            attr.setMaxValue(newMax);
            
            // 发送成功消息
            sendSuccessMessage(context, MSG_MAX_VALUE_DECREASED, attributeName, decreaseAmount, newMax);
            return 1;
        } else {
            sendErrorMessage(context, MSG_ATTRIBUTE_NOT_FOUND, attributeName);
            return 0;
        }
    }
    
    /**
     * 处理列出属性命令
     */
    private static int listAttributesCommand(
            CommandContext<ServerCommandSource> context, 
            ServerPlayerEntity player) {
        
        // 获取所有属性
        Collection<JustDyingAttribute> attributes = AttributeManager.getAllAttributes();
        
        if (attributes.isEmpty()) {
            sendErrorMessage(context, "没有可用的属性");
            return 0;
        }
        
        // 发送属性列表
        sendInfoMessage(context, MSG_LIST_HEADER);
        
        for (JustDyingAttribute attribute : attributes) {
            Identifier id = attribute.getId();
            int value = AttributeHelper.getAttributeValue(player, id);
            int maxValue = attribute.getMaxValue();
            
            sendInfoMessage(context, MSG_LIST_ITEM, 
                    attribute.getName(), id.getPath(), value, maxValue);
        }
        
        return attributes.size();
    }
    
    /**
     * 处理帮助命令
     */
    private static int showHelpCommand(CommandContext<ServerCommandSource> context) {
        sendInfoMessage(context, MSG_HELP_HEADER);
        sendInfoMessage(context, MSG_HELP_GET);
        sendInfoMessage(context, MSG_HELP_ATTR_INCREASE);
        sendInfoMessage(context, MSG_HELP_ATTR_DECREASE);
        sendInfoMessage(context, MSG_HELP_POINTS);
        sendInfoMessage(context, MSG_HELP_POINTS_SET);
        sendInfoMessage(context, MSG_HELP_POINTS_INCREASE);
        sendInfoMessage(context, MSG_HELP_POINTS_DECREASE);
        sendInfoMessage(context, MSG_HELP_MAX);
        sendInfoMessage(context, MSG_HELP_MAX_INCREASE);
        sendInfoMessage(context, MSG_HELP_MAX_DECREASE);
        sendInfoMessage(context, MSG_HELP_LIST);
        
        return 1;
    }
    
    /**
     * 从命令上下文获取玩家
     */
    private static ServerPlayerEntity getPlayerFromContext(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrThrow();
    }
    
    /**
     * 根据名称获取玩家
     */
    private static ServerPlayerEntity getPlayerByName(CommandContext<ServerCommandSource> context, String name) {
        try {
            ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(name);
            
            if (player == null) {
                JustDying.LOGGER.warn("找不到玩家: {}", name);
            }
            
            return player;
        } catch (Exception e) {
            JustDying.LOGGER.error("获取玩家时出错: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证属性存在
     */
    private static boolean validateAttribute(CommandContext<ServerCommandSource> context, Identifier attributeId) {
        try {
            Optional<JustDyingAttribute> attribute = AttributeManager.getAttribute(attributeId);
            
            if (attribute.isEmpty()) {
                sendErrorMessage(context, MSG_ATTRIBUTE_NOT_FOUND, attributeId.toString());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            sendErrorMessage(context, MSG_ERROR_GETTING_ATTRIBUTE, attributeId.toString());
            JustDying.LOGGER.error("验证属性时出错: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送成功消息
     */
    private static void sendSuccessMessage(CommandContext<ServerCommandSource> context, String format, Object... args) {
        // 不向玩家发送消息
        if (JustDying.getConfig().debug) {
            JustDying.LOGGER.debug("成功: " + String.format(format, args));
        }
    }
    
    /**
     * 发送错误消息
     */
    private static void sendErrorMessage(CommandContext<ServerCommandSource> context, String format, Object... args) {
        // 不向玩家发送消息
        if (JustDying.getConfig().debug) {
            JustDying.LOGGER.debug("错误: " + String.format(format, args));
        }
    }
    
    /**
     * 发送信息消息
     */
    private static void sendInfoMessage(CommandContext<ServerCommandSource> context, String format, Object... args) {
        // 不向玩家发送消息
        if (JustDying.getConfig().debug) {
            JustDying.LOGGER.debug("信息: " + String.format(format, args));
        }
    }
    
    /**
     * 处理命令异常
     */
    private static void handleCommandException(CommandContext<ServerCommandSource> context, Exception e) {
        JustDying.LOGGER.error("执行命令时出错: {}", e.getMessage());
        sendErrorMessage(context, MSG_COMMAND_ERROR, e.getMessage());
    }
    
    /**
     * 处理增加点数命令
     */
    private static int increasePointsCommand(
            CommandContext<ServerCommandSource> context, 
            ServerPlayerEntity player, 
            int points) {
        
        if (points <= 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, points);
            return 0;
        }
        
        boolean success = AttributeHelper.addPoints(player, points);
        
        if (success) {
            // 发送成功消息
            sendSuccessMessage(context, MSG_POINTS_ADDED, player.getName().getString(), points);
            return 1;
        } else {
            sendErrorMessage(context, MSG_OPERATION_FAILED);
            return 0;
        }
    }
    
    /**
     * 处理减少点数命令
     */
    private static int decreasePointsCommand(
            CommandContext<ServerCommandSource> context, 
            ServerPlayerEntity player, 
            int points) {
        
        if (points <= 0) {
            sendErrorMessage(context, MSG_INVALID_VALUE, points);
            return 0;
        }
        
        int currentPoints = AttributeHelper.getAvailablePoints(player);
        if (currentPoints < points) {
            sendErrorMessage(context, "点数不足，当前点数: %d, 需要减少: %d", currentPoints, points);
            return 0;
        }
        
        boolean success = AttributeHelper.setAvailablePoints(player, currentPoints - points);
        
        if (success) {
            // 发送成功消息
            sendSuccessMessage(context, MSG_POINTS_DECREASED, player.getName().getString(), points);
            return 1;
        } else {
            sendErrorMessage(context, MSG_OPERATION_FAILED);
            return 0;
        }
    }
} 