package com.galyx.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void initialize() {
        modules.forEach(Module::initialize);
    }

    public void enableAll() {
        modules.forEach(Module::enable);
    }

    public void disableAll() {
        modules.forEach(Module::disable);
    }

    public void tick() {
        modules.stream().filter(Module::isEnabled).forEach(Module::tick);
    }

    public void render() {
        modules.stream().filter(Module::isEnabled).forEach(Module::render);
    }
}