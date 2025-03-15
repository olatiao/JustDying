package com.justdie.affix;

import com.justdie.JustDying;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 词缀事件处理器客户端类，用于处理客户端事件
 */
@Environment(EnvType.CLIENT)
public class AffixEventHandlerClient {
    
    /**
     * 注册客户端事件处理器
     */
    public static void register() {
        // 注册物品提示事件处理器
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            // 检查配置是否允许显示词缀提示
            if (!JustDying.getConfig().affixes.showAffixTooltips) {
                return;
            }
            
            // 获取物品的词缀提示
            List<Text> affixTooltips = AffixManager.getAffixTooltips(stack);
            if (!affixTooltips.isEmpty()) {
                // 添加空行
                lines.add(Text.empty());
                
                // 在末尾添加词缀提示
                lines.addAll(affixTooltips);
                
                JustDying.AFFIX_LOGGER.debug("Added {} affix tooltips to item {}", 
                        affixTooltips.size(), stack.getItem().getName().getString());
            }
        });
    }
} 