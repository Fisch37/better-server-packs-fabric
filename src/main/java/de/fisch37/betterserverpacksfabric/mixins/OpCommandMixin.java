package de.fisch37.betterserverpacksfabric.mixins;

import com.mojang.authlib.GameProfile;
import de.fisch37.betterserverpacksfabric.networking.Networking;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(net.minecraft.server.dedicated.command.OpCommand.class)
public class OpCommandMixin {
    @Inject(method = "op", at = @At("TAIL"))
    private static void opInject(
            ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir
    ) {
        Networking.sendAccessUpdate(source.getServer(), targets);
    }
}
