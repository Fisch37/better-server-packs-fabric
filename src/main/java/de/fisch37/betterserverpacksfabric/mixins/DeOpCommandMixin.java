package de.fisch37.betterserverpacksfabric.mixins;

import com.mojang.authlib.GameProfile;
import de.fisch37.betterserverpacksfabric.networking.Networking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.DeOpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(DeOpCommand.class)
public class DeOpCommandMixin {
    @Inject(method = "deop", at = @At("TAIL"))
    private static void deopInject(
            ServerCommandSource source,
            Collection<GameProfile> targets,
            CallbackInfoReturnable<Integer> cir
    ) {
        Networking.sendAccessUpdate(source.getServer(), targets);
    }
}
