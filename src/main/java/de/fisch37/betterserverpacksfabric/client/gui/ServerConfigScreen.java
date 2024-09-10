package de.fisch37.betterserverpacksfabric.client.gui;

import de.fisch37.betterserverpacksfabric.client.ToastHelper;
import de.fisch37.betterserverpacksfabric.client.gui.components.RichTextEditor;
import de.fisch37.betterserverpacksfabric.networking.PackState;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.Observable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ServerConfigScreen extends BaseOwoScreen<FlowLayout> {
    private final Observable<PackState> stateWatcher;
    private PackState state;

    public ServerConfigScreen(Observable<PackState> stateWatcher) {
        this.stateWatcher = stateWatcher;
        state = stateWatcher.get();
        stateWatcher.observe(this::onStateChange);
    }

    private void onStateChange(PackState packState) {

    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);

        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.padding(Insets.of(30, 0, 20, 20));

        rootComponent.child(Containers.verticalFlow(Sizing.fill(), Sizing.content())
                .child(
                        Components.textBox(Sizing.fill(), state.url())
                                .margins(Insets.bottom(20))
                ).child(Containers.grid(Sizing.fill(), Sizing.content(), 1, 2)
                        .child(Components.checkbox(Text.translatable("bsp.config_ui.required")), 0, 0)
                        .child(Components.checkbox(Text.translatable("bsp.config_ui.reshash")), 0, 1)
                ).child(new RichTextEditor(Sizing.fill(), Sizing.content()))
        ).child(Containers.verticalFlow(Sizing.content(), Sizing.expand())
                .child(Containers.horizontalFlow(Sizing.fill(), Sizing.content())
                        .child(Components.button(Text.translatable("bsp.config_ui.save"), this::save)
                                .margins(Insets.horizontal(5))
                        )
                        .child(Components.button(Text.translatable("bsp.config_ui.cancel"), b -> close()))
                        .horizontalAlignment(HorizontalAlignment.RIGHT)
                        .id("button-row")
                ).verticalAlignment(VerticalAlignment.BOTTOM)
                .margins(Insets.bottom(10))
                .id("vertical-buffer")
        );
    }

    private void save(ButtonComponent saveButton) {
        ToastHelper.showToast(
                client,
                Text.literal("Saved!"),
                Text.literal("This is a debug message to show that someone pressed the save button!")
        );
        close();
    }
}
