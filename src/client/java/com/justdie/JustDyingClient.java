package com.justdie;

import com.justdie.affix.AffixEventHandlerClient;
import com.justdie.gui.AttributeKeybinding;
import com.justdie.network.ClientAttributePacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class JustDyingClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		JustDying.LOGGER.info("初始化JustDying客户端...");
		long startTime = System.currentTimeMillis();

		try {
			// 注册按键绑定
			AttributeKeybinding.register();

			// 注册客户端网络包处理器
			ClientAttributePacketHandler.register();

			// 注册词缀系统客户端事件处理器
			AffixEventHandlerClient.register();

			long endTime = System.currentTimeMillis();
			JustDying.LOGGER.info("JustDying客户端初始化完成，用时：{}ms", (endTime - startTime));
		} catch (Exception e) {
			JustDying.LOGGER.error("JustDying客户端初始化失败: {}", e.getMessage());
			e.printStackTrace();
		}
	}
}