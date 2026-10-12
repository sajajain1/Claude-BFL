package com.modernizeai.model;

public enum Category {
    JAKARTA_NAMESPACE("javax.* to jakarta.*"),
    SPRING_BOOT_UPGRADE("Spring Boot upgrade"),
    JAVA_VERSION("Java version"),
    SPRING_API("Deprecated or removed Spring API"),
    DEPENDENCY("Dependencies"),
    CONFIGURATION("Configuration properties"),
    JDK_API("Removed or internal JDK API");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
