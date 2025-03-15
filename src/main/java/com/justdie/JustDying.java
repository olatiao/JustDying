package com.justdie;

import com.justdie.affix.AffixEventHandler;
import com.justdie.affix.AffixManager;
import com.justdie.attribute.AttributeManager;
import com.justdie.attribute.AttributeHelper;
import com.justdie.attribute.AttributeRegistry;
import com.justdie.command.AttributeCommands;
import com.justdie.command.AffixCommand;
import com.justdie.config.DefaultConfig;
import com.justdie.config.JustDyingConfig;
import com.justdie.network.AttributeUpdatePacket;
import com.justdie.item.AttributeCapItems;
import com.justdie.item.AttributeCapItemHandler;
import com.justdie.item.ModItems;
import com.justdie.item.ModItemGroup;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.entity.attribute.EntityAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主类
 */
public class JustDying implements ModInitializer {
	public static final String MOD_ID = "justdying";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Logger AFFIX_LOGGER = LoggerFactory.getLogger(MOD_ID + "_affix");

	// 配置实例
	private static JustDyingConfig CONFIG;

	/**
	 * 获取配置实例
	 * @return 配置实例
	 */
	public static JustDyingConfig getConfig() {
		return CONFIG;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("初始化JustDying模组...");

		// 注册配置
		registerConfig();

		// 初始化属性系统
		AttributeRegistry.getInstance().loadFromConfig(CONFIG);
		
		// 保留旧的AttributeManager以保持兼容性
		AttributeManager.loadFromConfig(CONFIG);
		
		// 初始化属性Cap物品处理器
		AttributeCapItemHandler.initialize();
		
		// 注册网络包处理器
		AttributeUpdatePacket.register();
		
		// 注册命令
		CommandRegistrationCallback.EVENT.register(AttributeCommands::register);
		
		// 注册玩家加入服务器事件，同步所有属性数据
		registerPlayerJoinEvent();
		
		// 初始化词缀系统
		initAffixSystem();
		
		// 注册物品和物品组
		registerItems();

		LOGGER.info("JustDying模组初始化完成");
	}
	
	/**
	 * 注册配置系统
	 */
	private void registerConfig() {
		try {
			// 注册配置序列化器
			AutoConfig.register(JustDyingConfig.class, JanksonConfigSerializer::new);
			
			// 获取配置实例
			CONFIG = AutoConfig.getConfigHolder(JustDyingConfig.class).getConfig();
			
			// 检查配置是否有效
			if (CONFIG.attributes.attributes.isEmpty()) {
				LOGGER.warn("检测到配置文件中没有属性定义，将使用默认配置");
				CONFIG = DefaultConfig.createDefaultConfig();
				AutoConfig.getConfigHolder(JustDyingConfig.class).save();
			}
			
			LOGGER.info("配置加载完成，发现 {} 个属性定义", CONFIG.attributes.attributes.size());
		} catch (Exception e) {
			LOGGER.error("配置加载失败，将使用默认配置: {}", e.getMessage());
			CONFIG = DefaultConfig.createDefaultConfig();
		}
	}
	
	/**
	 * 注册玩家加入事件处理
	 */
	private void registerPlayerJoinEvent() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			
			// 使用旧版本的属性系统更新
			AttributeHelper.updateAllAttributes(player);
			
			// 同步属性到客户端
			syncAllAttributes(player);
			
			// 仅在调试模式下输出详细日志
			if (CONFIG.debug) {
				LOGGER.debug("玩家 {} 加入，同步了所有属性", player.getName().getString());
			}
		});
	}
	
	/**
	 * 注册所有物品和物品组
	 */
	private void registerItems() {
		// 注册模组物品
		ModItems.register();
		
		// 注册自定义物品
		AttributeCapItems.register();
		
		// 注册物品组
		ModItemGroup.register();
		
	// 注册属性Cap物品使用处理器
		AttributeCapItemHandler.register();
	}
	
	/**
	 * 初始化词缀系统
	 */
	private void initAffixSystem() {
		if (!CONFIG.affixes.enableAffixes) {
			AFFIX_LOGGER.info("词缀系统已在配置中禁用");
			return;
		}
		
		AFFIX_LOGGER.info("初始化词缀系统...");
		
		// 初始化词缀管理器
		AffixManager.init();
		
		// 注册词缀命令
		if (CONFIG.affixes.enableAffixCommands) {
			CommandRegistrationCallback.EVENT.register(AffixCommand::register);
		}
		
		// 注册事件处理器
		AffixEventHandler.register();
		
		AFFIX_LOGGER.info("词缀系统初始化完成");
	}
	
	/**
	 * 同步所有属性数据到客户端
	 * 
	 * @param player 玩家
	 */
	private void syncAllAttributes(ServerPlayerEntity player) {
		// 使用批处理方式减少网络开销
		AttributeManager.getAllAttributes().forEach(attribute -> {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeString(attribute.getId().toString());
			buf.writeInt(AttributeHelper.getAttributeValue(player, attribute.getId()));
			buf.writeInt(AttributeHelper.getAvailablePoints(player));
			
			ServerPlayNetworking.send(player, AttributeUpdatePacket.SYNC_ATTRIBUTES_ID, buf);
		});
	}

	/**
	 * 创建一个属于该模组的标识符
	 */
	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}