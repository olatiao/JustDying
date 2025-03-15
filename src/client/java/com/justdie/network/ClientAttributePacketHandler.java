package com.justdie.network;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeHelper;
import com.justdie.gui.AttributeScreen;
import com.justdie.gui.lightweight.LightweightAttributeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * 客户端属性同步包处理器
 * 用于接收服务器发送的属性更新
 */
@Environment(EnvType.CLIENT)
public class ClientAttributePacketHandler {
    
    /**
     * 注册客户端网络包处理器
     */
    public static void register() {
        // 注册属性同步处理器
        ClientPlayNetworking.registerGlobalReceiver(AttributeUpdatePacket.SYNC_ATTRIBUTES_ID, (client, handler, buf, responseSender) -> {
            String attributeId = buf.readString();
            int attributeValue = buf.readInt();
            int availablePoints = buf.readInt();
            
            // 在客户端线程上执行属性更新
            client.execute(() -> {
                if (client.player != null) {
                    try {
                        Identifier id = new Identifier(attributeId);
                        
                        // 添加调试日志
                        JustDying.LOGGER.debug("从服务器接收属性同步: {} = {}, 可用点数 = {}", 
                                attributeId, attributeValue, availablePoints);
                        
                        // 更新属性值和可用点数
                        AttributeHelper.setAttributeValue(client.player, id, attributeValue);
                        AttributeHelper.setAvailablePoints(client.player, availablePoints);
                        
                        // 刷新UI
                        if (client.currentScreen instanceof LightweightAttributeScreen) {
                            ((LightweightAttributeScreen) client.currentScreen).refreshData();
                        } else if (client.currentScreen instanceof AttributeScreen) {
                            ((AttributeScreen) client.currentScreen).refreshScreen();
                        }
                    } catch (Exception e) {
                        JustDying.LOGGER.error("处理属性同步包时出错: {}", e.getMessage());
                    }
                }
            });
        });
    }
} 