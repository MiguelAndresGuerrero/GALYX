package com.galyx.core;

public interface Module extends Lifecycle {
    String getId();
    String getName();
    boolean isEnabled();
    void tick();
    void render();
}