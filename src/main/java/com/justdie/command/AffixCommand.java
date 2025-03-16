package com.justdie.command;

import com.justdie.affix.Affix;
import com.justdie.affix.AffixManager;
import com.justdie.affix.AffixRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * 词缀命令类，用于添加和管理词缀
 */
public class AffixCommand {
    private static final int MAX_AFFIXES_PER_COMMAND = 5;
    
    /**
     * 注册词缀命令
     * 
     * @param dispatcher 命令调度器
     * @param registryAccess 注册表访问
     * @param environment 环境
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("affix")
            .requires(source -> source.hasPermissionLevel(2)) // 需要权限等级2（OP）
            
            // 添加随机词缀
            .then(CommandManager.literal("random")
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, MAX_AFFIXES_PER_COMMAND))
                    .executes(context -> addRandomAffixes(context, IntegerArgumentType.getInteger(context, "count")))
                )
                .executes(context -> addRandomAffixes(context, 1))
            )
            
            // 添加指定词缀
            .then(CommandManager.literal("add")
                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                    .executes(context -> addAffix(context, IdentifierArgumentType.getIdentifier(context, "id")))
                )
            )
            
            // 列出所有词缀
            .then(CommandManager.literal("list")
                .executes(AffixCommand::listAffixes)
            )
            
            // 清除词缀
            .then(CommandManager.literal("clear")
                .executes(AffixCommand::clearAffixes)
            )
            
            // 为其他玩家添加词缀
            .then(CommandManager.literal("give")
                .then(CommandManager.argument("players", EntityArgumentType.players())
                    .then(CommandManager.literal("random")
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, MAX_AFFIXES_PER_COMMAND))
                            .executes(context -> giveRandomAffixes(
                                context,
                                EntityArgumentType.getPlayers(context, "players"),
                                IntegerArgumentType.getInteger(context, "count")
                            ))
                        )
                        .executes(context -> giveRandomAffixes(
                            context,
                            EntityArgumentType.getPlayers(context, "players"),
                            1
                        ))
                    )
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                            .executes(context -> giveAffix(
                                context,
                                EntityArgumentType.getPlayers(context, "players"),
                                IdentifierArgumentType.getIdentifier(context, "id")
                            ))
                        )
                    )
                )
            )
            
            // 添加测试命令，添加所有词缀到手持物品
            .then(CommandManager.literal("test")
                .executes(AffixCommand::addAllAffixes)
            )
        );
    }
    
    /**
     * 添加所有词缀到手持物品（测试用）
     * 
     * @param context 命令上下文
     * @return 命令结果
     * @throws CommandSyntaxException 命令语法异常
     */
    private static int addAllAffixes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        
        if (stack.isEmpty()) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.error.no_item").formatted(Formatting.RED));
            return 0;
        }
        
        List<Affix> allAffixes = AffixRegistry.getAllAffixes();
        if (allAffixes.isEmpty()) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.list.empty").formatted(Formatting.RED));
            return 0;
        }
        
        // 清除现有词缀
        if (stack.hasNbt() && stack.getNbt().contains(Affix.AFFIX_NBT_KEY)) {
            stack.getNbt().remove(Affix.AFFIX_NBT_KEY);
        }
        
        // 添加所有词缀
        int count = 0;
        for (Affix affix : allAffixes) {
            affix.applyToItem(stack);
            count++;
            
            // 限制最多添加5个词缀，避免过多
            if (count >= 5) break;
        }
        
        // 使用final变量在lambda中
        final int finalCount = count;
        context.getSource().sendFeedback(() -> 
            Text.translatable("command.justdying.affix.test.success", finalCount).formatted(Formatting.GREEN), true);
        
        return count;
    }
    
    /**
     * 添加随机词缀
     * 
     * @param context 命令上下文
     * @param count 词缀数量
     * @return 命令结果
     * @throws CommandSyntaxException 命令语法异常
     */
    private static int addRandomAffixes(CommandContext<ServerCommandSource> context, int count) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        
        if (stack.isEmpty()) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.error.no_item").formatted(Formatting.RED));
            return 0;
        }
        
        AffixManager.addRandomAffixes(stack, count);
        
        // 使用final变量在lambda中
        final int finalCount = count;
        context.getSource().sendFeedback(() -> 
            Text.translatable("command.justdying.affix.random.success", finalCount).formatted(Formatting.GREEN), true);
        return 1;
    }
    
    /**
     * 添加指定词缀
     * 
     * @param context 命令上下文
     * @param id 词缀ID
     * @return 命令结果
     * @throws CommandSyntaxException 命令语法异常
     */
    private static int addAffix(CommandContext<ServerCommandSource> context, Identifier id) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        
        if (stack.isEmpty()) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.error.no_item").formatted(Formatting.RED));
            return 0;
        }
        
        Affix affix = AffixRegistry.getAffix(id);
        if (affix == null) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.add.fail", id).formatted(Formatting.RED));
            return 0;
        }
        
        AffixManager.addAffix(stack, id);
        
        context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.add.success", affix.getDisplayText()).formatted(Formatting.GREEN), true);
        return 1;
    }
    
    /**
     * 列出所有词缀
     * 
     * @param context 命令上下文
     * @return 命令结果
     */
    private static int listAffixes(CommandContext<ServerCommandSource> context) {
        List<Affix> affixes = AffixRegistry.getAllAffixes();
        
        if (affixes.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.list.empty").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.list.title").formatted(Formatting.YELLOW), false);
        
        for (Affix affix : affixes) {
            context.getSource().sendFeedback(() -> Text.literal(" - ")
                .append(affix.getDisplayText())
                .append(" (").append(Text.literal(affix.getId().toString()).formatted(Formatting.GRAY)).append(")"), false);
        }
        
        return affixes.size();
    }
    
    /**
     * 清除词缀
     * 
     * @param context 命令上下文
     * @return 命令结果
     * @throws CommandSyntaxException 命令语法异常
     */
    private static int clearAffixes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        
        if (stack.isEmpty()) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.clear.empty_hand"));
            return 0;
        }
        
        List<Affix> affixes = AffixManager.getAffixes(stack);
        if (affixes.isEmpty()) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.clear.no_affixes"));
            return 0;
        }
        
        // 清除所有词缀
        AffixManager.clearAllAffixes(stack);
        
        context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.clear.success", stack.getName()), true);
        return 1;
    }
    
    /**
     * 为其他玩家添加随机词缀
     * 
     * @param context 命令上下文
     * @param players 玩家列表
     * @param count 词缀数量
     * @return 命令结果
     */
    private static int giveRandomAffixes(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, int count) {
        int successCount = 0;
        
        for (ServerPlayerEntity player : players) {
            ItemStack stack = player.getMainHandStack();
            
            if (!stack.isEmpty()) {
                AffixManager.addRandomAffixes(stack, count);
                // 使用final变量在lambda中
                final int finalCount = count;
                final ServerPlayerEntity finalPlayer = player;
                context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.give.random.success", 
                    finalPlayer.getDisplayName(), finalCount).formatted(Formatting.GREEN), true);
                successCount++;
            } else {
                final ServerPlayerEntity finalPlayer = player;
                context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.give.no_item", 
                    finalPlayer.getDisplayName()).formatted(Formatting.RED), false);
            }
        }
        
        return successCount;
    }
    
    /**
     * 为其他玩家添加指定词缀
     * 
     * @param context 命令上下文
     * @param players 玩家列表
     * @param id 词缀ID
     * @return 命令结果
     */
    private static int giveAffix(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, Identifier id) {
        Affix affix = AffixRegistry.getAffix(id);
        if (affix == null) {
            context.getSource().sendError(Text.translatable("command.justdying.affix.add.fail", id).formatted(Formatting.RED));
            return 0;
        }
        
        int successCount = 0;
        
        for (ServerPlayerEntity player : players) {
            ItemStack stack = player.getMainHandStack();
            
            if (!stack.isEmpty()) {
                AffixManager.addAffix(stack, id);
                // 使用final变量在lambda中
                final Identifier finalId = id;
                final ServerPlayerEntity finalPlayer = player;
                context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.give.add.success", 
                    finalPlayer.getDisplayName(), finalId).formatted(Formatting.GREEN), true);
                successCount++;
            } else {
                final ServerPlayerEntity finalPlayer = player;
                context.getSource().sendFeedback(() -> Text.translatable("command.justdying.affix.give.no_item", 
                    finalPlayer.getDisplayName()).formatted(Formatting.RED), false);
            }
        }
        
        return successCount;
    }
} 