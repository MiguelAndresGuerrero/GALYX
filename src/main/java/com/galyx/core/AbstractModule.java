package com.galyx.core;

public abstract class AbstractModule implements Module {

    private boolean enabled;

    @Override
    public void initialize() {
    }

    @Override
    public void enable() {
        enabled = true;
    }

    @Override
    public void disable() {
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void tick() {
    }

    @Override
    public void render() {
    }
}