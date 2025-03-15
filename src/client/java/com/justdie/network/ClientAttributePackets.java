package com.justdie.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * 客户端属性更新网络包
 * 用于客户端向服务器发送属性更新请求
 */
public class ClientAttributePackets {
    /**
     * 发送增加属性请求
     * 
     * @param attributeId 属性ID
     */
    public static void sendIncreasePacket(Identifier attributeId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(attributeId.toString());
        ClientPlayNetworking.send(AttributeUpdatePacket.INCREASE_ATTRIBUTE_ID, buf);
    }
    
    /**
     * 发送减少属性请求
     * 
     * @param attributeId 属性ID
     */
    public static void sendDecreasePacket(Identifier attributeId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(attributeId.toString());
        ClientPlayNetworking.send(AttributeUpdatePacket.DECREASE_ATTRIBUTE_ID, buf);
    }

    /**
     * 发送等级兑换属性点请求
     */
    public static void sendExchangeLevelPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(AttributeUpdatePacket.EXCHANGE_LEVEL_ID, buf);
    }

    /**
     * 发送同步请求
     * 
     * @param attributeId 属性ID
     */
    public static void sendSyncRequestPacket(Identifier attributeId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(attributeId.toString());
        ClientPlayNetworking.send(AttributeUpdatePacket.SYNC_REQUEST_ID, buf);
    }
} 