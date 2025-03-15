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

/**
 * 服务端属性更新网络包处理器
 * 用于处理来自客户端的属性更新请求
 */
public class AttributeUpdatePacket {
    // 网络包标识符
    public static final Identifier INCREASE_ATTRIBUTE_ID = new Identifier(JustDying.MOD_ID, "increase_attribute");
    public static final Identifier DECREASE_ATTRIBUTE_ID = new Identifier(JustDying.MOD_ID, "decrease_attribute");
    public static final Identifier SYNC_ATTRIBUTES_ID = new Identifier(JustDying.MOD_ID, "sync_attributes");
    public static final Identifier EXCHANGE_LEVEL_ID = new Identifier(JustDying.MOD_ID, "exchange_level");
    public static final Identifier SYNC_REQUEST_ID = new Identifier(JustDying.MOD_ID, "sync_request");
    
    /**
     * 注册网络包处理器
     */
    public static void register() {
        // 注册增加属性处理器
        ServerPlayNetworking.registerGlobalReceiver(INCREASE_ATTRIBUTE_ID, (server, player, handler, buf, responseSender) -> {
            String attributeId = buf.readString();
            
            // 在服务器线程上执行属性更新
            server.execute(() -> {
                // 检查玩家是否有足够的点数
                int availablePoints = AttributeHelper.getAvailablePoints(player);
                if (availablePoints > 0) {
                    Identifier id = new Identifier(attributeId);
                    int currentValue = AttributeHelper.getAttributeValue(player, id);
                    int maxValue = AttributeHelper.getAttributeMaxValue(player, id);
                    
                    // 检查是否可以增加属性
                    if (currentValue < maxValue) {
                        // 增加属性值并减少可用点数
                        AttributeHelper.setAttributeValue(player, id, currentValue + 1);
                        AttributeHelper.usePoints(player, 1);
                        
                        JustDying.LOGGER.debug("Player {} increased attribute {} to {}", 
                                player.getName().getString(), attributeId, currentValue + 1);
                        
                        // 同步到客户端
                        syncAttributesToClient(player, id);
                    }
                }
            });
        });
        
        // 注册减少属性处理器
        ServerPlayNetworking.registerGlobalReceiver(DECREASE_ATTRIBUTE_ID, (server, player, handler, buf, responseSender) -> {
            String attributeId = buf.readString();
            
            // 在服务器线程上执行属性更新
            server.execute(() -> {
                Identifier id = new Identifier(attributeId);
                int currentValue = AttributeHelper.getAttributeValue(player, id);
                int minValue = AttributeHelper.getAttributeMinValue(player, id);
                
                // 检查是否可以减少属性
                if (currentValue > minValue) {
                    // 减少属性值并增加可用点数
                    AttributeHelper.setAttributeValue(player, id, currentValue - 1);
                    AttributeHelper.addPoints(player, 1);
                    
                    JustDying.LOGGER.debug("Player {} decreased attribute {} to {}", 
                            player.getName().getString(), attributeId, currentValue - 1);
                    
                    // 同步到客户端
                    syncAttributesToClient(player, id);
                }
            });
        });
        
        // 注册等级兑换属性点处理器
        ServerPlayNetworking.registerGlobalReceiver(EXCHANGE_LEVEL_ID, (server, player, handler, buf, responseSender) -> {
            // 在服务器线程上执行兑换
            server.execute(() -> {
                // 尝试兑换等级为属性点
                boolean success = LevelExchangeManager.exchangeLevelForPoint(player);
                
                if (success) {
                    JustDying.LOGGER.debug("Player {} exchanged level for attribute point", 
                            player.getName().getString());
                    
                    // 同步所有属性到客户端
                    syncAllAttributesToClient(player);
                }
            });
        });
        
        // 注册同步请求处理器
        ServerPlayNetworking.registerGlobalReceiver(SYNC_REQUEST_ID, (server, player, handler, buf, responseSender) -> {
            String attributeId = buf.readString();
            
            // 在服务器线程上执行同步
            server.execute(() -> {
                Identifier id = new Identifier(attributeId);
                
                // 同步到客户端
                syncAttributesToClient(player, id);
                
                JustDying.LOGGER.debug("Received sync request for attribute {} from player {}", 
                        attributeId, player.getName().getString());
            });
        });
    }
    
    /**
     * 同步属性到客户端
     * 
     * @param player 玩家
     * @param attributeId 属性ID
     */
    private static void syncAttributesToClient(ServerPlayerEntity player, Identifier attributeId) {
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
        // 同步所有属性
        AttributeManager.getAllAttributes().forEach(attribute -> {
            syncAttributesToClient(player, attribute.getId());
        });
    }
} 