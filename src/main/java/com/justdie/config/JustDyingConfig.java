package com.justdie.config;

import com.justdie.affix.AffixEffectTrigger;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

@Config(name = "justdying")
public class JustDyingConfig implements ConfigData {
        // 配置版本，便于将来升级配置结构
        @ConfigEntry.Gui.Excluded
        public String version = "1.0.0";

        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean debug = false; // 是否启用调试模式

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.TransitiveObject
        public AttributesConfig attributes = new AttributesConfig();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.TransitiveObject
        public LevelExchangeConfig levelExchange = new LevelExchangeConfig();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.TransitiveObject
        public AffixConfig affixes = new AffixConfig();

        public static class AttributesConfig {
                @ConfigEntry.Gui.Tooltip
                public boolean enableAttributeSystem = true;

                @ConfigEntry.Gui.Tooltip
                @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
                public int initialAttributePoints = 5; // 初始赠送的属性点数

                @ConfigEntry.Gui.Tooltip
                public boolean showDecreaseButtons = true; // 是否显示减少属性的按钮

                @ConfigEntry.Gui.CollapsibleObject
                @ConfigEntry.Gui.TransitiveObject
                public AttributeCapItemsConfig attributeCapItems = new AttributeCapItemsConfig();

                @ConfigEntry.Gui.Tooltip
                public Map<String, AttributeConfig> attributes = new HashMap<>();
        }

        public static class LevelExchangeConfig {
                @ConfigEntry.Gui.Tooltip
                public boolean enableLevelExchange = true;

                @ConfigEntry.Gui.Tooltip
                @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
                public int baseLevel = 5;

                @ConfigEntry.Gui.Tooltip
                @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
                public int levelMultiplier = 2;
        }

        public static class AffixConfig {
                @ConfigEntry.Gui.Tooltip
                public boolean enableAffixes = false;

                @ConfigEntry.Gui.Tooltip
                @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
                public int maxAffixesPerItem = 3;

                @ConfigEntry.Gui.Tooltip
                @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
                public int affixDropChance = 10; // 百分比，10表示10%

                @ConfigEntry.Gui.Tooltip
                public boolean enableAffixCommands = true;

                @ConfigEntry.Gui.Tooltip
                public boolean showAffixTooltips = true;

                @ConfigEntry.Gui.CollapsibleObject
                public PresetAffixes presetAffixes = new PresetAffixes();
        }

        public static class AttributeCapItemsConfig {
                @ConfigEntry.Gui.Tooltip
                public boolean enableAttributeCapItems = true;

                @ConfigEntry.Gui.Tooltip
                @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
                public int increaseAmountPerUse = 5;
        }

        public static class AttributeConfig {
                @ConfigEntry.Gui.Tooltip
                public String name = "";

                @ConfigEntry.Gui.Tooltip
                public String description = "";

                @ConfigEntry.Gui.Tooltip
                public String iconItem = "minecraft:stone";

                @ConfigEntry.Gui.Tooltip
                public String vanillaAttribute = "";

                @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
                public int minValue = 0;

                @ConfigEntry.BoundedDiscrete(min = 1, max = 1000)
                public int maxValue = 20;

                @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
                public int initialValue = 0;

                @ConfigEntry.Gui.Tooltip
                public float valueMultiplier = 1.0f;

                @ConfigEntry.Gui.Tooltip
                public boolean enabled = true;

                @ConfigEntry.Gui.Tooltip
                public boolean enableCapItem = false; // 是否启用该属性的上限增加物品

                @ConfigEntry.Gui.Tooltip
                public String capItemId = ""; // 属性上限增加物品的ID

                // 无参构造函数，用于序列化
                public AttributeConfig() {
                }

                public AttributeConfig(String name, String description, String iconItem, String vanillaAttribute,
                                int minValue, int maxValue, int initialValue, float valueMultiplier,
                                boolean enableCapItem, String capItemId) {
                        this.name = name;
                        this.description = description;
                        this.iconItem = iconItem;
                        this.vanillaAttribute = vanillaAttribute;
                        this.minValue = minValue;
                        this.maxValue = maxValue;
                        this.initialValue = initialValue;
                        this.valueMultiplier = valueMultiplier;
                        this.enabled = true;
                        this.enableCapItem = enableCapItem;
                        this.capItemId = capItemId;
                }
                
                /**
                 * 验证属性配置是否有效
                 * @return 配置是否有效
                 */
                public boolean isValid() {
                        return !name.isEmpty() && maxValue > minValue;
                }
        }

        /**
         * 预设词缀配置
         */
        public static class PresetAffixes {
                // ===== 武器词缀 =====
                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry fire_weapon = new AffixEntry(
                                "烈焰之息",
                                "攻击造成额外伤害并点燃目标",
                                Formatting.RED,
                                true,
                                AffixItemType.WEAPON,
                                new AffixAttributeEntry("minecraft:generic.attack_damage",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                1.5),
                                new AffixEffectEntry("minecraft:fire_resistance", 1, 60, 0.3f,
                                                AffixEffectTrigger.ON_HIT.name()));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry ice_weapon = new AffixEntry(
                                "冰霜裁决",
                                "攻击造成额外伤害并使目标减速",
                                Formatting.AQUA,
                                true,
                                AffixItemType.WEAPON,
                                new AffixAttributeEntry("minecraft:generic.attack_damage",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                1.0),
                                new AffixEffectEntry("minecraft:slowness", 2, 80, 0.4f,
                                                AffixEffectTrigger.ON_HIT.name()));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry lightning_weapon = new AffixEntry(
                                "雷霆召唤",
                                "攻击造成额外伤害并召唤闪电击中目标",
                                Formatting.YELLOW,
                                true,
                                AffixItemType.WEAPON,
                                new AffixAttributeEntry("minecraft:generic.attack_damage",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                1.0),
                                new AffixEffectEntry("minecraft:weakness", 1, 40, 0.15f,
                                                AffixEffectTrigger.ON_HIT.name()));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry poison_weapon = new AffixEntry(
                                "剧毒侵蚀",
                                "攻击造成额外伤害并使目标中毒",
                                Formatting.DARK_GREEN,
                                true,
                                AffixItemType.WEAPON,
                                new AffixAttributeEntry("minecraft:generic.attack_damage",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                0.5),
                                new AffixEffectEntry("minecraft:poison", 1, 100, 0.35f,
                                                AffixEffectTrigger.ON_HIT.name()));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry critical_weapon = new AffixEntry(
                                "狂暴突袭",
                                "攻击时有几率造成1.5倍暴击伤害",
                                Formatting.GOLD,
                                true,
                                AffixItemType.WEAPON,
                                new AffixAttributeEntry("minecraft:generic.attack_damage",
                                                EntityAttributeModifier.Operation.MULTIPLY_TOTAL, 0.2));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry lifesteal_weapon = new AffixEntry(
                                "嗜血掠夺",
                                "攻击时恢复造成伤害10%的生命值",
                                Formatting.LIGHT_PURPLE,
                                true,
                                AffixItemType.WEAPON,
                                new AffixAttributeEntry("minecraft:generic.attack_damage",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                0.5),
                                new AffixEffectEntry("minecraft:regeneration", 1, 20, 0.25f,
                                                AffixEffectTrigger.ON_HIT.name()));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry explosive_weapon = new AffixEntry(
                                "爆裂冲击",
                                "攻击时造成AOE爆炸伤害",
                                Formatting.RED,
                                true,
                                AffixItemType.WEAPON,
                                new AffixAttributeEntry("minecraft:generic.attack_damage",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                1.0));

                // ===== 防具词缀 =====
                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry health_armor = new AffixEntry(
                                "不朽之心",
                                "增加最大生命值",
                                Formatting.GREEN,
                                true,
                                AffixItemType.ARMOR,
                                new AffixAttributeEntry("minecraft:generic.max_health",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                4.0));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry damage_reduction_armor = new AffixEntry(
                                "守护壁垒",
                                "按百分比减少受到的伤害",
                                Formatting.BLUE,
                                true,
                                AffixItemType.ARMOR,
                                new AffixAttributeEntry("minecraft:generic.armor",
                                                EntityAttributeModifier.Operation.ADDITION, 3.0),
                                new AffixAttributeEntry("minecraft:generic.armor_toughness",
                                                EntityAttributeModifier.Operation.ADDITION,
                                                1.0));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry speed_armor = new AffixEntry(
                                "迅捷步伐",
                                "增加移动速度",
                                Formatting.AQUA,
                                true,
                                AffixItemType.ARMOR,
                                new AffixAttributeEntry("minecraft:generic.movement_speed",
                                                EntityAttributeModifier.Operation.MULTIPLY_BASE, 0.1));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry knockback_resistance_armor = new AffixEntry(
                                "钢铁意志",
                                "减少被击退的几率",
                                Formatting.GRAY,
                                true,
                                AffixItemType.ARMOR,
                                new AffixAttributeEntry("minecraft:generic.knockback_resistance",
                                                EntityAttributeModifier.Operation.ADDITION, 0.25));

                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry thorns_armor = new AffixEntry(
                                "荆棘护盾",
                                "将部分伤害反弹给攻击者",
                                Formatting.DARK_RED,
                                true,
                                AffixItemType.ARMOR,
                                new AffixAttributeEntry("minecraft:generic.armor",
                                                EntityAttributeModifier.Operation.ADDITION, 1.0),
                                new AffixEffectEntry("minecraft:instant_damage", 1, 1, 0.2f,
                                                AffixEffectTrigger.ON_HURT.name()));

                // ===== 通用词缀 =====
                @ConfigEntry.Gui.CollapsibleObject
                public AffixEntry lucky = new AffixEntry(
                                "幸运",
                                "增加幸运值",
                                Formatting.GOLD,
                                true,
                                AffixItemType.ANY,
                                new AffixAttributeEntry("minecraft:generic.luck",
                                                EntityAttributeModifier.Operation.ADDITION, 1.0));
        }

        /**
         * 词缀条目配置
         */
        public static class AffixEntry {
                @ConfigEntry.Gui.Tooltip
                public String name;

                @ConfigEntry.Gui.Tooltip
                public String description;

                @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
                public Formatting formatting;

                @ConfigEntry.Gui.Tooltip
                public boolean enabled;

                @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
                public AffixItemType itemType;

                @ConfigEntry.Gui.Excluded
                public AffixAttributeEntry attribute;

                @ConfigEntry.Gui.Excluded
                public AffixAttributeEntry secondaryAttribute;

                @ConfigEntry.Gui.Excluded
                public AffixEffectEntry effect;

                public AffixEntry() {
                        this.name = "";
                        this.description = "";
                        this.formatting = Formatting.WHITE;
                        this.enabled = true;
                        this.itemType = AffixItemType.ANY;
                }

                public AffixEntry(String name, String description, Formatting formatting, boolean enabled,
                                AffixItemType itemType,
                                AffixAttributeEntry attribute) {
                        this.name = name;
                        this.description = description;
                        this.formatting = formatting;
                        this.enabled = enabled;
                        this.itemType = itemType;
                        this.attribute = attribute;
                }

                public AffixEntry(String name, String description, Formatting formatting, boolean enabled,
                                AffixItemType itemType,
                                AffixAttributeEntry attribute, AffixEffectEntry effect) {
                        this.name = name;
                        this.description = description;
                        this.formatting = formatting;
                        this.enabled = enabled;
                        this.itemType = itemType;
                        this.attribute = attribute;
                        this.effect = effect;
                }

                public AffixEntry(String name, String description, Formatting formatting, boolean enabled,
                                AffixItemType itemType,
                                AffixAttributeEntry attribute, AffixAttributeEntry secondaryAttribute) {
                        this.name = name;
                        this.description = description;
                        this.formatting = formatting;
                        this.enabled = enabled;
                        this.itemType = itemType;
                        this.attribute = attribute;
                        this.secondaryAttribute = secondaryAttribute;
                }
        }

        /**
         * 词缀物品类型
         */
        public enum AffixItemType {
                WEAPON, // 武器（剑、斧、弓、弩等）
                ARMOR, // 防具（头盔、胸甲、护腿、鞋子）
                TOOL, // 工具（镐、锹、锄等）
                ANY // 任何类型
        }

        /**
         * 词缀属性条目配置
         */
        public static class AffixAttributeEntry {
                public String attributeId;
                public EntityAttributeModifier.Operation operation;
                public double amount;

                public AffixAttributeEntry() {
                        this.attributeId = "";
                        this.operation = EntityAttributeModifier.Operation.ADDITION;
                        this.amount = 0.0;
                }

                public AffixAttributeEntry(String attributeId, EntityAttributeModifier.Operation operation,
                                double amount) {
                        this.attributeId = attributeId;
                        this.operation = operation;
                        this.amount = amount;
                }
        }

        /**
         * 词缀效果条目配置
         */
        public static class AffixEffectEntry {
                public String effectId;
                public int level;
                public int duration;
                public float chance;
                public String trigger;

                public AffixEffectEntry() {
                        this.effectId = "";
                        this.level = 1;
                        this.duration = 0;
                        this.chance = 1.0f;
                        this.trigger = AffixEffectTrigger.ON_HIT.name();
                }

                public AffixEffectEntry(String effectId, int level, int duration, float chance, String trigger) {
                        this.effectId = effectId;
                        this.level = level;
                        this.duration = duration;
                        this.chance = chance;
                        this.trigger = trigger;
                }
        }
}
