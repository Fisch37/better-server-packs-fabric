package de.fisch37.betterserverpacksfabric.client.gui;

import de.fisch37.betterserverpacksfabric.client.ToastHelper;
import de.fisch37.betterserverpacksfabric.client.gui.components.RichTextEditor;
import de.fisch37.betterserverpacksfabric.networking.Networking;
import de.fisch37.betterserverpacksfabric.networking.PackState;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.Observable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public class ServerConfigScreen extends BaseOwoScreen<FlowLayout> {
    private final Observable<PackState> stateWatcher;
    private PackState state;

    private FlowLayout rootComponent;
    private TextBoxComponent urlComponent;
    private CheckboxComponent requiredComponent, rehashComponent;
    private RichTextEditor promptComponent;
    private LabelComponent desyncWarningLabel;
    private ButtonComponent reloadButton;
    private ParentComponent desyncWarnOverlay;
    private boolean desynced = false;  // Technically could be reloadButton.active, but no.

    public ServerConfigScreen(Observable<PackState> stateWatcher) {
        this.stateWatcher = stateWatcher;
        state = stateWatcher.get();
        stateWatcher.observe(this::onStateChange);
    }

    private void onStateChange(PackState packState) {
        desyncWarningLabel.text(Text.translatable("bsp.config_ui.warn_desync")
                .formatted(Formatting.RED)
        );
        reloadButton.active(true);
        desynced = true;
    }

    private void reload() {
        state = stateWatcher.get();
        urlComponent.text(state.url());
        requiredComponent.checked(state.required());
        rehashComponent.checked(state.rehashNextStart());
        promptComponent.setText(state.prompt().orElse(null));
        desyncWarningLabel.text(Text.empty());
        reloadButton.active(false);
        desynced = false;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent;
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);

        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.padding(Insets.of(30, 0, 20, 20));

        rootComponent.child(Containers.verticalFlow(Sizing.fill(), Sizing.content())
                .child(
                        (urlComponent = Components.textBox(Sizing.fill(), state.url()))
                                .margins(Insets.bottom(20))
                ).child(Containers.grid(Sizing.fill(), Sizing.content(), 1, 2)
                        .child(
                                (requiredComponent = Components.checkbox(
                                        Text.translatable("bsp.config_ui.required")
                                )),
                                0, 0
                        )
                        .child(
                                rehashComponent = Components.checkbox(Text.translatable("bsp.config_ui.reshash")),
                                0, 1
                        )
                ).child(promptComponent = new RichTextEditor(Sizing.fill(), Sizing.content()))
        )
        .child(Containers.verticalFlow(Sizing.content(), Sizing.expand())
                .child(Containers.horizontalFlow(Sizing.fill(), Sizing.content())
                        .child((desyncWarningLabel = Components.label(Text.empty()))
                                .horizontalTextAlignment(HorizontalAlignment.RIGHT)
                                .verticalTextAlignment(VerticalAlignment.CENTER)
                                .horizontalSizing(Sizing.expand())
                        )
                        .child(reloadButton = Components.button(Text.translatable("bsp.config_ui.reload"), b ->
                                reload()
                        ).active(false))
                        .child(Components.button(Text.translatable("bsp.config_ui.save"), this::saveButtonTrigger)
                                .margins(Insets.horizontal(5))
                        )
                        .child(Components.button(Text.translatable("bsp.config_ui.cancel"), b -> close()))
                        .horizontalAlignment(HorizontalAlignment.RIGHT)
                        .id("button-row")
                ).verticalAlignment(VerticalAlignment.BOTTOM)
                .margins(Insets.bottom(10))
                .id("vertical-buffer")
        );

        desyncWarnOverlay = Containers.overlay(
                Containers.verticalFlow(Sizing.fill(), Sizing.content())
                        .child(Components.label(Text.translatable("bsp.config_ui.save_warn_desync.title")
                                .formatted(Formatting.RED)
                                .styled( style -> style.withBold(true) )
                        ))
                        .child(Components.label(Text.translatable("bsp.config_ui.save_warn_desync")
                                .formatted(Formatting.RED)
                            )
                            .horizontalTextAlignment(HorizontalAlignment.CENTER)
                            .horizontalSizing(Sizing.fill())
                        )
                        .child(Containers.grid(Sizing.content(), Sizing.content(), 1, 2)
                                .child(Components.button(
                                        Text.translatable("bsp.config_ui.save_warn_desync.save"),
                                        b -> save()
                                        ).margins(Insets.horizontal(5)),
                                        0, 0
                                )
                                .child(Components.button(
                                        Text.translatable("bsp.config_ui.save_warn_desync.cancel"),
                                        b -> rootComponent.removeChild(desyncWarnOverlay)
                                        ).margins(Insets.horizontal(5)),
                                        0, 1
                                )
                                .horizontalAlignment(HorizontalAlignment.RIGHT)
                                .id("overlay-button-grid")
                        )
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                        .verticalAlignment(VerticalAlignment.CENTER)
        );
        desyncWarnOverlay
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER);

        urlComponent.setMaxLength(4096);
    }

    private void saveButtonTrigger(ButtonComponent saveButton) {
        if (desynced) {
            rootComponent.child(desyncWarnOverlay);
        } else {
            save();
        }
    }

    private void save() {
        ToastHelper.showToast(
                client,
                Text.literal("Saved!"),
                Text.literal("This is a debug message to show that someone pressed the save button!")
        );
        close();
        PackState newState = new PackState(
                urlComponent.getText(),
                requiredComponent.isChecked(),
                rehashComponent.isChecked(),
                promptComponent.getText()
        );
        Networking.CHANNEL.clientHandle().send(newState);
    }
}
