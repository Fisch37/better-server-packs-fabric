package de.fisch37.betterserverpacksfabric.client.gui;

import de.fisch37.betterserverpacksfabric.client.gui.components.RichTextEditor;
import de.fisch37.betterserverpacksfabric.networking.PackState;
import io.wispforest.owo.ui.base.BaseOwoScreen;
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
        rootComponent.padding(Insets.top(10));

        rootComponent.child(
                Components.textBox(Sizing.fill(), state.url())
        ).child(Containers.horizontalFlow(Sizing.fill(), Sizing.content())
                .child(Components.smallCheckbox(Text.translatable("bsp.config_ui.required")))
                .child(Components.smallCheckbox(Text.translatable("bsp.config_ui.reshash")))
        ).child(new RichTextEditor(Sizing.fill(), Sizing.content())
        );
    }
}
