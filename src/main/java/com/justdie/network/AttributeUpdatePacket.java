package com.justdie.network;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeHelper;
import com.justdie.attribute.AttributeManager;
import com.justdie.attribute.LevelExchangeManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * 服务端属性更新网络包处理器
 * 用于处理来自客户端的属性更新请求，包括属性增减、等级兑换和属性同步
 */
public class AttributeUpdatePacket {
    // 常量定义
    private static final String PACKET_DEBUG_FORMAT = "{} 玩家 {} 的属性 {} 为 {}";
    private static final String EXCHANGE_DEBUG_FORMAT = "玩家 {} 兑换等级获得属性点";
    private static final String SYNC_DEBUG_FORMAT = "收到玩家 {} 的属性 {} 同步请求";
    private static final String ERROR_PLAYER_NULL = "玩家对象为空，无法处理属性更新";
    private static final String ERROR_ATTRIBUTE_NULL = "属性ID为空，无法处理属性更新";
    private static final String ERROR_PACKET_PROCESSING = "处理网络包时发生错误: {}";
    
    // 网络包标识符
    public static final Identifier INCREASE_ATTRIBUTE_ID = JustDying.id("increase_attribute");
    public static final Identifier DECREASE_ATTRIBUTE_ID = JustDying.id("decrease_attribute");
    public static final Identifier SYNC_ATTRIBUTES_ID = JustDying.id("sync_attributes");
    public static final Identifier EXCHANGE_LEVEL_ID = JustDying.id("exchange_level");
    public static final Identifier SYNC_REQUEST_ID = JustDying.id("sync_request");
    
    /**
     * 注册所有网络包处理器
     */
    public static void register() {
        try {
            // 注册增加属性处理器
            registerIncreaseAttributeHandler();
            
            // 注册减少属性处理器
            registerDecreaseAttributeHandler();
            
            // 注册等级兑换属性点处理器
            registerExchangeLevelHandler();
            
            // 注册同步请求处理器
            registerSyncRequestHandler();
            
            JustDying.LOGGER.info("已注册所有属性更新网络包处理器");
        } catch (Exception e) {
            JustDying.LOGGER.error("注册网络包处理器时发生错误: {}", e.getMessage());
        }
    }
    
    /**
     * 注册增加属性处理器
     */
    private static void registerIncreaseAttributeHandler() {
        ServerPlayNetworking.registerGlobalReceiver(INCREASE_ATTRIBUTE_ID, (server, player, handler, buf, responseSender) -> {
            String attributeId = buf.readString();
            
            // 在服务器线程上执行属性更新
            server.execute(() -> {
                try {
                    // 安全性检查：确保玩家和属性ID有效
                    if (player == null) {
                        JustDying.LOGGER.warn(ERROR_PLAYER_NULL);
                        return;
                    }
                    
                    if (attributeId == null || attributeId.isEmpty()) {
                        JustDying.LOGGER.warn(ERROR_ATTRIBUTE_NULL);
                        return;
                    }
                    
                    // 检查玩家是否有足够的点数
                    int availablePoints = AttributeHelper.getAvailablePoints(player);
                    if (availablePoints <= 0) {
                        JustDying.LOGGER.debug("玩家 {} 没有足够的属性点用于增加属性", player.getName().getString());
                        return;
                    }
                    
                    Identifier id = new Identifier(attributeId);
                    int currentValue = AttributeHelper.getAttributeValue(player, id);
                    int maxValue = AttributeHelper.getAttributeMaxValue(id);
                    
                    // 检查是否可以增加属性
                    if (currentValue < maxValue) {
                        // 增加属性值并减少可用点数
                        AttributeHelper.setAttributeValue(player, id, currentValue + 1);
                        AttributeHelper.usePoints(player, 1);
                        
                        logAttributeChange("增加", player, attributeId, currentValue + 1);
                        
                        // 同步到客户端
                        syncAttributesToClient(player, id);
                    } else {
                        JustDying.LOGGER.debug("玩家 {} 的属性 {} 已达到最大值 {}", 
                                player.getName().getString(), attributeId, maxValue);
                    }
                } catch (Exception e) {
                    JustDying.LOGGER.error(ERROR_PACKET_PROCESSING, e.getMessage());
                }
            });
        });
    }
    
    /**
     * 注册减少属性处理器
     */
    private static void registerDecreaseAttributeHandler() {
        ServerPlayNetworking.registerGlobalReceiver(DECREASE_ATTRIBUTE_ID, (server, player, handler, buf, responseSender) -> {
            String attributeId = buf.readString();
            
            // 在服务器线程上执行属性更新
            server.execute(() -> {
                try {
                    // 安全性检查：确保玩家和属性ID有效
                    if (player == null) {
                        JustDying.LOGGER.warn(ERROR_PLAYER_NULL);
                        return;
                    }
                    
                    if (attributeId == null || attributeId.isEmpty()) {
                        JustDying.LOGGER.warn(ERROR_ATTRIBUTE_NULL);
                        return;
                    }
                    
                    Identifier id = new Identifier(attributeId);
                    int currentValue = AttributeHelper.getAttributeValue(player, id);
                    int minValue = AttributeHelper.getAttributeMinValue(id);
                    
                    // 检查是否可以减少属性
                    if (currentValue > minValue) {
                        // 减少属性值并增加可用点数
                        AttributeHelper.setAttributeValue(player, id, currentValue - 1);
                        AttributeHelper.addPoints(player, 1);
                        
                        logAttributeChange("减少", player, attributeId, currentValue - 1);
                        
                        // 同步到客户端
                        syncAttributesToClient(player, id);
                    } else {
                        JustDying.LOGGER.debug("玩家 {} 的属性 {} 已达到最小值 {}", 
                                player.getName().getString(), attributeId, minValue);
                    }
                } catch (Exception e) {
                    JustDying.LOGGER.error(ERROR_PACKET_PROCESSING, e.getMessage());
                }
            });
        });
    }
    
    /**
     * 注册等级兑换属性点处理器
     */
    private static void registerExchangeLevelHandler() {
        ServerPlayNetworking.registerGlobalReceiver(EXCHANGE_LEVEL_ID, (server, player, handler, buf, responseSender) -> {
            // 在服务器线程上执行兑换
            server.execute(() -> {
                try {
                    // 安全性检查：确保玩家有效
                    if (player == null) {
                        JustDying.LOGGER.warn(ERROR_PLAYER_NULL);
                        return;
                    }
                    
                    // 尝试兑换等级为属性点
                    boolean success = LevelExchangeManager.exchangeLevelForPoint(player);
                    
                    if (success) {
                        if (JustDying.getConfig().debug) {
                            JustDying.LOGGER.debug(EXCHANGE_DEBUG_FORMAT, player.getName().getString());
                        }
                        
                        // 同步所有属性到客户端
                        syncAllAttributesToClient(player);
                    } else {
                        JustDying.LOGGER.debug("玩家 {} 无法兑换等级", player.getName().getString());
                    }
                } catch (Exception e) {
                    JustDying.LOGGER.error(ERROR_PACKET_PROCESSING, e.getMessage());
                }
            });
        });
    }
    
    /**
     * 注册同步请求处理器
     */
    private static void registerSyncRequestHandler() {
        ServerPlayNetworking.registerGlobalReceiver(SYNC_REQUEST_ID, (server, player, handler, buf, responseSender) -> {
            String attributeId = buf.readString();
            
            // 在服务器线程上执行同步
            server.execute(() -> {
                // 安全性检查：确保玩家和属性ID有效
                if (player == null || attributeId == null || attributeId.isEmpty()) {
                    return;
                }
                
                Identifier id = new Identifier(attributeId);
                
                // 同步到客户端
                syncAttributesToClient(player, id);
                
                if (JustDying.getConfig().debug) {
                    JustDying.LOGGER.debug(SYNC_DEBUG_FORMAT, player.getName().getString(), attributeId);
                }
            });
        });
    }
    
    /**
     * 记录属性变更日志
     * 
     * @param action 操作类型（增加/减少）
     * @param player 玩家
     * @param attributeId 属性ID
     * @param newValue 新的属性值
     */
    private static void logAttributeChange(String action, ServerPlayerEntity player, String attributeId, int newValue) {
        if (JustDying.getConfig().debug) {
            JustDying.LOGGER.debug(PACKET_DEBUG_FORMAT, 
                    action, player.getName().getString(), attributeId, newValue);
        }
    }
    
    /**
     * 同步属性到客户端
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     */
    private static void syncAttributesToClient(ServerPlayerEntity player, Identifier attributeId) {
        if (player == null || attributeId == null) {
            return;
        }
        
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(attributeId.toString());
        buf.writeInt(AttributeHelper.getAttributeValue(player, attributeId));
        buf.writeInt(AttributeHelper.getAvailablePoints(player));
        
        ServerPlayNetworking.send(player, SYNC_ATTRIBUTES_ID, buf);
    }
    
    /**
     * 同步所有属性到客户端
     * 
     * @param player 玩家
     */
    private static void syncAllAttributesToClient(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        
        // 同步所有属性
        AttributeManager.getAllAttributes().forEach(attribute -> {
            syncAttributesToClient(player, attribute.getId());
        });
    }
} 