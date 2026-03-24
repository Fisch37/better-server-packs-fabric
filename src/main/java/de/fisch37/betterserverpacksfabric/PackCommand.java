package de.fisch37.betterserverpacksfabric;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.fisch37.betterserverpacksfabric.config_serializers.MaybeInstant;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Supplier;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.*;

public class PackCommand {
    public final static Component MSG_PREFIX = Component.literal("")
            .append(Component.literal("[").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("BSP").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.YELLOW));
    private static final SimpleCommandExceptionType INVALID_URI_EXCEPTION = new SimpleCommandExceptionType(
            Component.translatableWithFallback("bsp.commands.exc.invalid_uri", "The pack URI is malformed. You can try fixing this in the config or just use /pack set <url> again")
    );

    private static LiteralArgumentBuilder<CommandSourceStack> makeCommand(CommandBuildContext registryAccess) {
        return literal("pack")
                .requires(Commands.hasPermission(LEVEL_ADMINS))
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
                        .then(argument("players", EntityArgument.players())
                                .executes(context -> ResourcePackHandler.pushTo(
                                        EntityArgument.getPlayers(context, "players")
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
                        .then(argument("prompt", ComponentArgument.textComponent(registryAccess))
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

    private static void updateHashWithContext(CommandSourceStack source, boolean pushAfterSet) {
        source.sendSuccess(() -> MSG_PREFIX.copy()
                .append("Updating pack hash...")
                ,
                true
        );
        Main.updateHash().ifPresentOrElse(
                // Let's all hope that this doesn't cause threading issues :+1:
                future -> future.whenComplete((packState, exc) -> {
                    if (exc != null) {
                        source.sendSuccess(
                                () -> MSG_PREFIX.copy()
                                        .append(Component.literal(
                                                "Failed to update hash."
                                                        + "Please check the server logs for more information."
                                                )
                                                .withStyle(ChatFormatting.RED)
                                        )
                                ,
                                true
                        );
                    } else {
                        // Hash updated
                        source.sendSuccess(
                                () -> MSG_PREFIX.copy()
                                        .append("Pack Hash has been updated!")
                                ,
                                true);

                        if (pushAfterSet) {
                            source.sendSuccess(
                                    () -> MSG_PREFIX.copy()
                                            .append("Pushing to players...")
                                    ,
                                    true);
                            ResourcePackHandler.pushTo(source.getServer());
                        }
                    }
                }),
                () -> {
                    // Hash removed (no pack selected)
                    source.sendSuccess(
                            () -> MSG_PREFIX.copy()
                                    .append("BetterServerPacks has been disabled. ")
                                    .append("This cannot be pushed to the players :(")
                            ,
                            true
                    );
                }
        );
    }

    private static int setPack(CommandContext<CommandSourceStack> context, boolean pushAfterSet) {
        final Supplier<Component> INVALID_URL_ERROR = (
                () -> MSG_PREFIX.copy()
                .append(Component.literal("The text supplied is not a valid URL")
                        .withStyle(ChatFormatting.RED))
        );

        CommandSourceStack source = context.getSource();
        String url = StringArgumentType.getString(context, "url");
        URL parsedUrl;

        try { parsedUrl = new URI(url).toURL(); }
        catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            context.getSource().sendSuccess(INVALID_URL_ERROR, false);
            return 0;
        }
        String protocol = parsedUrl.getProtocol();
        if ((!protocol.equals("https")) && (!protocol.equals("http"))) {
            source.sendSuccess(INVALID_URL_ERROR, false);
            return 0;
        }

        Main.config.url.set(url).save();
        Main.config.lastPolled.set(MaybeInstant.empty()).save();
        source.sendSuccess( () -> MSG_PREFIX.copy()
                .append("Pack URL has been updated. Reloading hash...")
                ,
                true
        );
        updateHashWithContext(source, pushAfterSet);
        return 1;
    }

    private static int disablePack(CommandContext<CommandSourceStack> context) {
        Main.config.url.set("").save();
        updateHashWithContext(context.getSource(), false);
        return 1;
    }

    private static int reloadPack(CommandContext<CommandSourceStack> context, boolean pushAfterReload) {
        updateHashWithContext(context.getSource(), pushAfterReload);
        return 1;
    }

    private static int getRequired(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        Boolean required = Main.config.required.get();

        source.sendSuccess(() -> MSG_PREFIX.copy()
                .append("Pack is " + (required ? "required" : "optional"))
                ,
                false
        );
        return 1;
    }

    private static int setRequired(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        Boolean required = BoolArgumentType.getBool(context, "required");
        Main.config.required.set(required).save();

        source.sendSuccess( () -> MSG_PREFIX.copy()
                .append("Pack is now " + (required ? "required" : "optional"))
                ,
                true
        );
        return 1;
    }

    private static int showPrompt(CommandContext<CommandSourceStack> context) {
        final Optional<Component> prompt = Main.config.getPrompt(context.getSource().registryAccess());
        context.getSource().sendSuccess(
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

    private static int setPrompt(CommandContext<CommandSourceStack> context) {
        final Component prompt = ComponentArgument.getRawComponent(context, "prompt");
        Main.config.setPrompt(prompt, context.getSource().registryAccess())
                .save();
        context.getSource().sendSuccess(
                () -> MSG_PREFIX.copy()
                        .append("Prompt has been set to: ")
                        .append(prompt)
                ,
                true
        );
        return 1;
    }

    private static int clearPrompt(CommandContext<CommandSourceStack> context) {
        Main.config.setPrompt(null, null);
        context.getSource().sendSuccess(
                () -> MSG_PREFIX.copy()
                        .append("Prompt has been removed")
                ,
                true
        );
        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String url = Main.config.url.get();
        URI uriObj;
        try {
            uriObj = new URI(url);
        } catch (URISyntaxException e) {
            throw INVALID_URI_EXCEPTION.create();
        }
        boolean required = Main.config.required.get();
        Optional<Component> prompt = Main.config.getPrompt(context.getSource().registryAccess());
        if (!url.isEmpty()) {
            context.getSource().sendSuccess(() ->
                            MSG_PREFIX.copy()
                                    .append("Pack URL: ")
                                    .append(Component.literal(url)
                                            .withStyle(Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(uriObj)))
                                            .withStyle(ChatFormatting.GREEN)
                                            .withStyle(ChatFormatting.UNDERLINE)
                                    )
                                    .append("\n")
                                    .append("Pack hash: ")
                                    .append(Optional.ofNullable(Main.getHashString())
                                            .map(s -> Component.literal(s)
                                                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                                                    .withStyle(ChatFormatting.ITALIC)
                                            )
                                            .orElse(Component.literal("undetermined").withStyle(ChatFormatting.GRAY))
                                    )
                                    .append("\n")
                                    .append("Pack is ")
                                    .append(
                                            required
                                                    ? Component.literal("required")
                                                    .withStyle(ChatFormatting.RED)
                                                    : Component.literal("optional")
                                                    .withStyle(ChatFormatting.YELLOW)
                                    )
                                    .append("\n")
                                    .append(prompt
                                            .map(text -> Component.literal("Prompt: \n    ")
                                                    .append(text)
                                            )
                                            .orElseGet(() -> Component.literal("No prompt is set"))
                                    )
                    ,
                    false
            );
        } else {
            context.getSource().sendSuccess(() ->
                    MSG_PREFIX.copy()
                            .append("No resourcepack is set")
                    ,
                    false
            );
        }

        return 1;
    }
}
