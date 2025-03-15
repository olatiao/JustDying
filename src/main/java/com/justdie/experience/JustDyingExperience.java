package com.justdie.experience;

import net.minecraft.util.math.MathHelper;
import java.util.Arrays;
import java.util.Objects;

public class JustDyingExperience {
    private static JustDyingExperience INSTANCE;

    public static JustDyingExperience getInstance() {
        return Objects.requireNonNull(INSTANCE, "JustDyingExperience instance is not initialized");
    }

    public static void setInstance(JustDyingExperience instance) {
        JustDyingExperience.INSTANCE = instance;
    }

    private final long baseCost; // 基础成本
    private final double multiplier; // 倍率
    private final double levelExponent; // 等级指数
    private final int maxLevel; // 最大等级

    private int entryCount = 1_000; // 入口数量
    private int highestCalculated = 0; // 最高计算等级
    private long[] levelExperienceArr; // 等级经验数组
    private long[] totalExperienceArr; // 总经验数组

    /**
     * 
     * @param baseCost      基础成本
     * @param multiplier    倍率
     * @param levelExponent 等级指数
     * @param maxLevel      最大等级
     */
    public JustDyingExperience(long baseCost, double multiplier, double levelExponent, int maxLevel) {

        // 检查参数
        this.baseCost = baseCost;
        this.multiplier = multiplier;
        this.levelExponent = levelExponent;
        this.maxLevel = maxLevel;

        // 初始化数组
        this.levelExperienceArr = new long[entryCount];
        this.totalExperienceArr = new long[entryCount];

        // 检查指数是否在有效范围内
        if (levelExponent < 0 || levelExponent > 4) {
            throw new IllegalStateException(
                    "Level exponent is too low or too high. It should be between 0 (exclusive) and 4 (inclusive), but it is "
                            + levelExponent);
        }

        // 计算初始成本
        this.levelExperienceArr[0] = baseCost;
        this.totalExperienceArr[0] = baseCost;

        // 计算初始成本
        calculateLevelExperienceUpToLevel(maxLevel);
    }

    /**
     * 获取基础成本
     * 
     * @return 基础成本
     */
    public long getBaseCost() {
        return baseCost;
    }

    /**
     * 获取倍率
     * 
     * @return 倍率
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * 获取等级指数
     * 
     * @return 等级指数
     */
    public double getLevelExponent() {
        return levelExponent;
    }

    /**
     * 获取最大等级
     * 
     * @return 最大等级
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    public long getLovelExperience(int level) {
        calculateLevelExperienceUpToLevel(level);
        return levelExperienceArr[level];
    }

    /**
     * 计算指定等级的经验
     * 
     * @param level 等级
     */
    public void calculateLevelExperienceUpToLevel(int level) {
        int target = MathHelper.clamp(level, 0, maxLevel);

        if (target <= highestCalculated) {
            return;
        }

        if (target >= entryCount) {
            while (entryCount <= target) {
                entryCount *= 2;
            }
            levelExperienceArr = Arrays.copyOf(levelExperienceArr, entryCount);
            totalExperienceArr = Arrays.copyOf(totalExperienceArr, entryCount);
        }

        for (int i = highestCalculated + 1; i < entryCount; i++) {
            long experienceRequiredForLevel = (long) (baseCost * Math.pow(i - 1, levelExponent));
            this.levelExperienceArr[i] = experienceRequiredForLevel;
            this.totalExperienceArr[i] = this.totalExperienceArr[i - 1] + experienceRequiredForLevel;
            if (this.totalExperienceArr[i] < this.totalExperienceArr[i - 1]) {
                throw new IllegalStateException(
                        "Experience level equation grew too fast. Level %d has a higher exp cost than level %d. Try reducing your multiplier and/or level exponent to ensure the number doesn't overflow."
                                .formatted(i - 1, i));
            }
        }

        highestCalculated = entryCount - 1;
    }
}
