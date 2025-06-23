package de.fisch37.betterserverpacksfabric;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.fisch37.betterserverpacksfabric.networking.Networking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.*;

public class PackCommand {
    public final static Text MSG_PREFIX = Text.literal("")
            .append(Text.literal("[").formatted(Formatting.YELLOW))
            .append(Text.literal("BSP").formatted(Formatting.AQUA))
            .append(Text.literal("] ").formatted(Formatting.YELLOW));

    private static LiteralArgumentBuilder<ServerCommandSource> makeCommand(CommandRegistryAccess registryAccess) {
        return literal("pack")
                .requires(ServerMain::hasConfigAccess)
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
                ).then(literal("prompt")
                        .executes(PackCommand::showPrompt)
                        .then(argument("prompt", TextArgumentType.text(registryAccess))
                                .executes(PackCommand::setPrompt)
                        )
                        .then(literal("clear")
                                .executes(PackCommand::clearPrompt)
                        )
                ).then(literal("info")
                        .executes(PackCommand::showInfo));
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    dispatcher.register(makeCommand(registryAccess));
                }
        );
    }

    private static void updateHashWithContext(ServerCommandSource source, boolean pushAfterSet) {
        source.sendFeedback(() -> MSG_PREFIX.copy()
                .append("Updating pack hash...")
                ,
                true
        );
        ServerMain.updateHash().thenAccept(result -> {
            // Let's all hope that this doesn't cause threading issues :+1:
            if (result == null) {
                // Error
                source.sendFeedback(() -> MSG_PREFIX.copy()
                                .append(Text.literal(
                                        "Failed to update hash."
                                                + "Please check the server logs for more information."
                                        )
                                        .formatted(Formatting.RED)
                                )
                        ,
                        true
                );
            } else if (result) {
                // Hash updated
                source.sendFeedback(() -> MSG_PREFIX.copy()
                                .append("Pack Hash has been updated!")
                        ,
                        true);

                if (pushAfterSet) {
                    source.sendFeedback(() -> MSG_PREFIX.copy()
                                    .append("Pushing to players...")
                            ,
                            true);
                    ResourcePackHandler.pushTo(source.getServer());
                }
            } else {
                // Hash removed (no pack selected)
                source.sendFeedback(() -> MSG_PREFIX.copy()
                                .append("BetterServerPacks has been disabled. ")
                                .append("This cannot be pushed to the players :(")
                        ,
                        true
                );
            }
        });
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

        ServerMain.config.url.set(url).save();
        sendUpdate(context);
        source.sendFeedback( () -> MSG_PREFIX.copy()
                .append("Pack URL has been updated. Reloading hash...")
                ,
                true
        );
        updateHashWithContext(source, pushAfterSet);
        return 1;
    }

    private static int disablePack(CommandContext<ServerCommandSource> context) {
        ServerMain.config.url.set("").save();
        sendUpdate(context);
        updateHashWithContext(context.getSource(), false);
        return 1;
    }

    private static int reloadPack(CommandContext<ServerCommandSource> context, boolean pushAfterReload) {
        updateHashWithContext(context.getSource(), pushAfterReload);
        return 1;
    }

    private static int getRequired(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        Boolean required = ServerMain.config.required.get();

        source.sendFeedback(() -> MSG_PREFIX.copy()
                .append("Pack is " + (required ? "required" : "optional"))
                ,
                false
        );
        return 1;
    }

    private static int setRequired(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        Boolean required = BoolArgumentType.getBool(context, "required");
        ServerMain.config.required.set(required).save();
        sendUpdate(context);

        source.sendFeedback( () -> MSG_PREFIX.copy()
                .append("Pack is now " + (required ? "required" : "optional"))
                ,
                true
        );
        return 1;
    }

    private static int showPrompt(CommandContext<ServerCommandSource> context) {
        final Optional<Text> prompt = ServerMain.config.getPrompt(context.getSource().getRegistryManager());
        context.getSource().sendFeedback(
                () -> prompt.map(
                        text -> MSG_PREFIX.copy()
                                .append("Current Prompt is: ")
                                .append(text)
                ).orElseGet(
                        () -> MSG_PREFIX.copy()
                                .append("No prompt is set")
                ),
                false
        );
        return 1;
    }

    private static int setPrompt(CommandContext<ServerCommandSource> context) {
        final Text prompt = TextArgumentType.getTextArgument(context, "prompt");
        ServerMain.config.setPrompt(prompt, context.getSource().getRegistryManager())
                .save();
        sendUpdate(context);
        context.getSource().sendFeedback(
                () -> MSG_PREFIX.copy()
                        .append("Prompt has been set to: ")
                        .append(prompt)
                ,
                true
        );
        return 1;
    }

    private static int clearPrompt(CommandContext<ServerCommandSource> context) {
        ServerMain.config.setPrompt(null, null);
        sendUpdate(context);
        context.getSource().sendFeedback(
                () -> MSG_PREFIX.copy()
                        .append("Prompt has been removed")
                ,
                true
        );
        return 1;
    }

    private static int showInfo(CommandContext<ServerCommandSource> context) {
        String url = ServerMain.config.url.get();
        boolean required = ServerMain.config.required.get();
        Optional<Text> prompt = ServerMain.config.getPrompt(context.getSource().getRegistryManager());
        if (!url.isEmpty()) {
            context.getSource().sendFeedback(() ->
                            MSG_PREFIX.copy()
                                    .append("Pack URL: ")
                                    .append(Text.literal(url)
                                            .fillStyle(Style.EMPTY
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                            )
                                            .formatted(Formatting.GREEN)
                                            .formatted(Formatting.UNDERLINE)
                                    )
                                    .append("\n")
                                    .append("Pack hash: ")
                                    .append(Optional.ofNullable(ServerMain.getHashString())
                                            .map(s -> Text.literal(s)
                                                    .formatted(Formatting.LIGHT_PURPLE)
                                                    .formatted(Formatting.ITALIC)
                                            )
                                            .orElse(Text.literal("undetermined").formatted(Formatting.GRAY))
                                    )
                                    .append("\n")
                                    .append("Pack is ")
                                    .append(
                                            required
                                                    ? Text.literal("required")
                                                    .formatted(Formatting.RED)
                                                    : Text.literal("optional")
                                                    .formatted(Formatting.YELLOW)
                                    )
                                    .append("\n")
                                    .append(prompt
                                            .map(text -> Text.literal("Prompt: \n    ")
                                                    .append(text)
                                            )
                                            .orElseGet(() -> Text.literal("No prompt is set"))
                                    )
                    ,
                    false
            );
        } else {
            context.getSource().sendFeedback(() ->
                    MSG_PREFIX.copy()
                            .append("No resourcepack is set")
                    ,
                    false
            );
        }

        return 1;
    }

    private static void sendUpdate(CommandContext<ServerCommandSource> context) {
        Networking.sendConfigUpdate(context.getSource().getServer());
    }
}
