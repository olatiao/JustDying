package com.justdie;

import com.justdie.affix.AffixEventHandler;
import com.justdie.affix.AffixManager;
import com.justdie.attribute.AttributeManager;
import com.justdie.attribute.AttributeHelper;
import com.justdie.boss.registry.BossRegistry;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主类
 * 负责初始化模组的各个系统和组件
 */
public class JustDying implements ModInitializer {
	// 模组ID常量
	public static final String MOD_ID = "justdying";

	// 日志常量
	private static final String LOG_INITIALIZING = "初始化JustDying模组...";
	private static final String LOG_INITIALIZED = "JustDying模组初始化完成，用时: {}ms";
	private static final String LOG_CONFIG_LOADED = "配置加载完成，发现 {} 个属性定义";
	private static final String LOG_CONFIG_FAILED = "配置加载失败，将使用默认配置: {}";
	private static final String LOG_CONFIG_EMPTY = "检测到配置文件中没有属性定义，将使用默认配置";
	private static final String LOG_PLAYER_JOINED = "玩家 {} 加入，同步所有属性数据";
	private static final String LOG_AFFIX_DISABLED = "词缀系统已在配置中禁用";
	private static final String LOG_AFFIX_INITIALIZING = "初始化词缀系统...";
	private static final String LOG_AFFIX_INITIALIZED = "词缀系统初始化完成";
	private static final String LOG_BOSS_INITIALIZING = "初始化BOSS系统...";
	private static final String LOG_BOSS_INITIALIZED = "BOSS系统初始化完成";
	
	// 日志记录器
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Logger AFFIX_LOGGER = LoggerFactory.getLogger(MOD_ID + "_affix");
	public static final Logger BOSS_LOGGER = LoggerFactory.getLogger(MOD_ID + "_boss");

	// 配置实例
	private static JustDyingConfig CONFIG;

	@Override
	public void onInitialize() {
		LOGGER.info(LOG_INITIALIZING);
		long startTime = System.currentTimeMillis();
		
		try {
			// 初始化核心系统
			initializeCore();
			
			// 初始化网络和事件
			initializeNetworkAndEvents();
			
			// 初始化命令系统
			initializeCommands();
			
			// 初始化物品系统
			initializeItems();
			
			// 初始化词缀系统
			initializeAffixSystem();
			
			// 初始化BOSS系统
			initializeBossSystem();
			
			// 记录初始化完成
			long duration = System.currentTimeMillis() - startTime;
			LOGGER.info(LOG_INITIALIZED, duration);
		} catch (Exception e) {
			LOGGER.error("模组初始化过程中发生错误", e);
		}
	}
	
	/**
	 * 初始化核心系统
	 */
	private void initializeCore() {
		// 注册并加载配置
		registerConfig();

		// 初始化属性系统
		AttributeManager.loadFromConfig(CONFIG);
	}
	
	/**
	 * 初始化网络和事件系统
	 */
	private void initializeNetworkAndEvents() {
		// 注册网络包处理器
		AttributeUpdatePacket.register();
		
		// 注册玩家加入服务器事件，同步所有属性数据
		registerPlayerJoinEvent();
	}
	
	/**
	 * 初始化命令系统
	 */
	private void initializeCommands() {
		// 注册属性命令
		AttributeCommands.register();
		
		// 注册词缀命令（如果启用）
		if (CONFIG.affixes.enableAffixCommands) {
			CommandRegistrationCallback.EVENT.register(AffixCommand::register);
		}
	}
	
	/**
	 * 初始化BOSS系统
	 */
	private void initializeBossSystem() {
		BOSS_LOGGER.info(LOG_BOSS_INITIALIZING);
		
		try {
			// 注册BOSS实体
			BossRegistry.registerEntities();
			
			// 注册BOSS属性
			BossRegistry.registerAttributes();
			
			// 注册BOSS命令
			com.justdie.boss.command.BossCommands.register();
			
			BOSS_LOGGER.info(LOG_BOSS_INITIALIZED);
		} catch (Exception e) {
			BOSS_LOGGER.error("BOSS系统初始化失败", e);
		}
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
			validateConfig();
			
			LOGGER.info(LOG_CONFIG_LOADED, CONFIG.attributes.attributes.size());
		} catch (Exception e) {
			LOGGER.error(LOG_CONFIG_FAILED, e.getMessage());
			CONFIG = DefaultConfig.createDefaultConfig();
		}
	}
	
	/**
	 * 验证配置并处理无效配置
	 */
	private void validateConfig() {
		if (CONFIG.attributes.attributes == null || CONFIG.attributes.attributes.isEmpty()) {
			LOGGER.warn(LOG_CONFIG_EMPTY);
			CONFIG = DefaultConfig.createDefaultConfig();
			
			// 确保配置被保存到磁盘
			saveConfig();
			
			// 再次验证
			if (CONFIG.attributes.attributes.isEmpty()) {
				LOGGER.error("无法加载默认属性，属性列表仍然为空！");
			} else {
				LOGGER.info("已使用预设配置，包含 {} 个属性", CONFIG.attributes.attributes.size());
			}
		}
	}
	
	/**
	 * 保存当前配置
	 */
	private void saveConfig() {
		try {
			// 获取配置持有者
			var configHolder = AutoConfig.getConfigHolder(JustDyingConfig.class);
			
			// 确保最新配置已经应用
			configHolder.setConfig(CONFIG);
			
			// 保存配置
			configHolder.save();
			
			LOGGER.info("配置已成功保存到磁盘");
		} catch (Exception e) {
			LOGGER.error("保存配置失败: {}", e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * 注册玩家加入事件处理
	 */
	private void registerPlayerJoinEvent() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			syncAllAttributes(player);
			
			// 仅在调试模式下输出详细日志
			if (CONFIG.debug) {
				LOGGER.debug(LOG_PLAYER_JOINED, player.getName().getString());
			}
		});
	}
	
	/**
	 * 初始化物品系统
	 */
	private void initializeItems() {
		// 初始化属性Cap物品处理器
		AttributeCapItemHandler.initialize();
		
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
	private void initializeAffixSystem() {
		if (!CONFIG.affixes.enableAffixes) {
			AFFIX_LOGGER.info(LOG_AFFIX_DISABLED);
			return;
		}
		
		AFFIX_LOGGER.info(LOG_AFFIX_INITIALIZING);
		
		try {
		// 初始化词缀管理器
		AffixManager.init();
		
		// 注册事件处理器
		AffixEventHandler.register();
		
			AFFIX_LOGGER.info(LOG_AFFIX_INITIALIZED);
		} catch (Exception e) {
			AFFIX_LOGGER.error("词缀系统初始化失败", e);
		}
	}
	
	/**
	 * 同步所有属性数据到客户端
	 * 
	 * @param player 玩家
	 */
	private void syncAllAttributes(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		
		// 使用批处理方式减少网络开销
		AttributeManager.getAllAttributes().forEach(attribute -> {
			Identifier attributeId = attribute.getId();
			
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeString(attributeId.toString());
			buf.writeInt(AttributeHelper.getAttributeValue(player, attributeId));
			buf.writeInt(AttributeHelper.getAvailablePoints(player));
			
			ServerPlayNetworking.send(player, AttributeUpdatePacket.SYNC_ATTRIBUTES_ID, buf);
		});
	}

	/**
	 * 获取配置实例
	 * 
	 * @return 配置实例
	 */
	public static JustDyingConfig getConfig() {
		return CONFIG;
	}
	
	/**
	 * 创建一个属于该模组的标识符
	 * 
	 * @param path 路径
	 * @return 模组命名空间的标识符
	 */
	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}