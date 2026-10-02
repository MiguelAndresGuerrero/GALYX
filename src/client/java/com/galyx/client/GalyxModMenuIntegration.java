package com.galyx.client;

import com.galyx.client.gui.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class GalyxModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> new ConfigScreen(
                GALYXClient.getSharedConfig(),
                GALYXClient.getSharedConfigManager(),
                parentScreen
        );
    }

}