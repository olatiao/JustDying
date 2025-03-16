package com.justdie.network;

import com.justdie.JustDying;
import com.justdie.attribute.AttributeHelper;
import com.justdie.gui.AttributeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
                    Identifier id = new Identifier(attributeId);
                    
                    // 添加调试日志
                    JustDying.LOGGER.debug("Received attribute sync from server: {} = {}, available points = {}", 
                            attributeId, attributeValue, availablePoints);
                    
                    // 检查是否是防御属性
                    boolean isDefenseAttribute = id.getPath().equals("defense");
                    if (isDefenseAttribute) {
                        JustDying.LOGGER.debug("Special handling for defense attribute on client");
                    }
                    
                    // 更新客户端的属性值和可用点数
                    AttributeHelper.setAttributeValue(client.player, id, attributeValue);
                    AttributeHelper.setAvailablePoints(client.player, availablePoints);
                    
                    // 如果当前屏幕是属性屏幕，刷新它
                    if (client.currentScreen instanceof AttributeScreen) {
                        ((AttributeScreen) client.currentScreen).refreshScreen();
                    }
                }
            });
        });
    }
} 