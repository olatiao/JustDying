package com.justdie.attribute;

/**
 * 属性类型枚举
 */
public enum JustDyingAttributeType {
    CONSTITUTION("constitution"),
    STRENGTH("strength"),
    DEFENSE("defense"),
    SPEED("speed"),
    LUCK("luck");

    private final String id;

    JustDyingAttributeType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
} 