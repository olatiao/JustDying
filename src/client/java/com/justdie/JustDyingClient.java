package com.justdie;

import com.justdie.affix.AffixEventHandlerClient;
import com.justdie.gui.AttributeKeybinding;
import com.justdie.network.ClientAttributePacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class JustDyingClient implements ClientModInitializer {
	// 决定是否使用轻量级界面的配置项
	// 后期可以将此移至配置系统中
	private static final boolean USE_LIGHTWEIGHT_UI = true;
	
	@Override
	public void onInitializeClient() {
		// 根据配置决定使用哪个界面版本
		if (USE_LIGHTWEIGHT_UI) {
		    // 使用轻量级界面
		    com.justdie.gui.lightweight.AttributeKeybinding.register();
		} else {
		    // 使用原版界面
		    AttributeKeybinding.register();
		}
		
		// 注册客户端网络包处理器
		ClientAttributePacketHandler.register();
		
		// 注册词缀系统客户端事件处理器
		AffixEventHandlerClient.register();
	}
}