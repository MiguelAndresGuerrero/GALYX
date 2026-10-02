package com.galyx.client.mixin;

import com.galyx.client.bridge.ActionBarBridge;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiActionBarMixin {

    @Inject(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"))
    private void galyx$onSetOverlayMessage(Component component, boolean isStatusEffect, CallbackInfo ci) {
        ActionBarBridge.fireMessage(component);
    }

}