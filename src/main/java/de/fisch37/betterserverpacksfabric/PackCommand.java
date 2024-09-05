package de.fisch37.betterserverpacksfabric;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.*;

public class PackCommand {
    public final static Text MSG_PREFIX = Text.literal("")
            .append(Text.literal("[").formatted(Formatting.YELLOW))
            .append(Text.literal("BSP").formatted(Formatting.AQUA))
            .append(Text.literal("] ").formatted(Formatting.YELLOW));

    private final static LiteralArgumentBuilder<ServerCommandSource> COMMAND = literal("pack")
            .requires(required -> required.hasPermissionLevel(4))
            .then(literal("set")
                    .executes(PackCommand::disablePack)
                    .then(argument("url", string())
                            .executes(context -> PackCommand.setPack(context, false))
                            .then(literal("push")
                                    .executes(context -> PackCommand.setPack(context, true))
                            )
                    )
            )
            .then(literal("reload")
                    .executes(context -> PackCommand.reloadPack(context, false))
                    .then(literal("push")
                            .executes(context -> PackCommand.reloadPack(context, true))
                    )
            )
            .then(literal("push")
                    .executes(context -> ResourcePackHandler.pushTo(context.getSource().getServer()))
                    .then(argument("players", EntityArgumentType.players())
                            .executes(context -> ResourcePackHandler.pushTo(
                                    EntityArgumentType.getPlayers(context, "players")
                            ))
                    )
            )
            .then(literal("required")
                    .executes(PackCommand::getRequired)
                    .then(argument("required", BoolArgumentType.bool())
                            .executes(PackCommand::setRequired)
                    )
            );

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(COMMAND)
        );
    }

    private static int setPack(CommandContext<ServerCommandSource> context, boolean pushAfterSet) {
        final Supplier<Text> INVALID_URL_ERROR = (
                () -> MSG_PREFIX.copy()
                .append(Text.literal("The text supplied is not a valid URL")
                        .formatted(Formatting.RED))
        );

        ServerCommandSource source = context.getSource();
        String url = StringArgumentType.getString(context, "url");
        URL parsedUrl;

        try { parsedUrl = new URI(url).toURL(); }
        catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            context.getSource().sendFeedback(INVALID_URL_ERROR, false);
            return 0;
        }
        String protocol = parsedUrl.getProtocol();
        if ((!protocol.equals("https")) && (!protocol.equals("http"))) {
            source.sendFeedback(INVALID_URL_ERROR, false);
            return 0;
        }

        Main.config.url.set(url).save();
        source.sendFeedback( () -> MSG_PREFIX.copy()
                .append("Pack URL has been updated. Reloading hash...")
                ,
                true
        );
        Main.updateHash(context.getSource().getServer(), source, pushAfterSet);
        return 1;
    }

    private static int disablePack(CommandContext<ServerCommandSource> context) {
        Main.config.url.set("");
        Main.updateHash(context.getSource().getServer(), context.getSource(), false);
        return 1;
    }

    private static int reloadPack(CommandContext<ServerCommandSource> context, boolean pushAfterReload) {
        Main.updateHash(context.getSource().getServer(), context.getSource(), pushAfterReload);
        return 1;
    }

    private static int getRequired(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        Boolean required = Main.config.required.get();

        source.sendFeedback(() -> MSG_PREFIX.copy()
                .append("Pack is " + (required ? "required" : "optional"))
                ,
                true
        );
        return 1;
    }

    private static int setRequired(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        Boolean required = BoolArgumentType.getBool(context, "required");
        Main.config.required.set(required).save();

        source.sendFeedback( () -> MSG_PREFIX.copy()
                .append("Pack is now " + (required ? "required" : "optional"))
                ,
                true
        );
        return 1;
    }
}
