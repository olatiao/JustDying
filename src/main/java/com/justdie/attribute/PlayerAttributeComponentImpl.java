package com.justdie.attribute;

import com.justdie.JustDying;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * 玩家属性组件的实现
 */
public class PlayerAttributeComponentImpl implements PlayerAttributeComponent {
    private final PlayerAttributeData attributeData;
    private int availablePoints; // 可用属性点
    private static final String POINTS_KEY = "AttributePoints";
    
    public PlayerAttributeComponentImpl(PlayerEntity player) {
        this.attributeData = new PlayerAttributeData(player);
        
        // 从配置文件中获取初始属性点
        this.availablePoints = JustDying.getConfig().attributes.initialAttributePoints;
        JustDying.LOGGER.info("Initialized player with {} attribute points from config", this.availablePoints);
    }
    
    @Override
    public int getAttributeValue(Identifier attributeId) {
        return attributeData.getAttributeValue(attributeId);
    }
    
    @Override
    public void setAttributeValue(Identifier attributeId, int value) {
        attributeData.setAttributeValue(attributeId, value);
    }
    
    @Override
    public void addAttributeValue(Identifier attributeId, int amount) {
        attributeData.addAttributeValue(attributeId, amount);
    }
    
    @Override
    public void updateAllVanillaAttributes() {
        attributeData.updateAllVanillaAttributes();
    }
    
    @Override
    public PlayerAttributeData getAttributeData() {
        return attributeData;
    }
    
    /**
     * 获取可用属性点数
     * 
     * @return 可用属性点数
     */
    public int getAvailablePoints() {
        return availablePoints;
    }
    
    /**
     * 设置可用属性点数
     * 
     * @param points 点数
     */
    public void setAvailablePoints(int points) {
        this.availablePoints = Math.max(0, points);
    }
    
    /**
     * 增加可用属性点数
     * 
     * @param amount 增加的数量
     */
    public void addPoints(int amount) {
        this.availablePoints += amount;
    }
    
    /**
     * 使用属性点数
     * 
     * @param amount 使用的数量
     * @return 是否成功使用
     */
    public boolean usePoints(int amount) {
        if (availablePoints >= amount) {
            availablePoints -= amount;
            return true;
        }
        return false;
    }
    
    @Override
    public void readFromNbt(NbtCompound tag) {
        attributeData.readFromNbt(tag);
        if (tag.contains(POINTS_KEY)) {
            availablePoints = tag.getInt(POINTS_KEY);
        }
    }
    
    @Override
    public void writeToNbt(NbtCompound tag) {
        attributeData.writeToNbt(tag);
        tag.putInt(POINTS_KEY, availablePoints);
    }
} 