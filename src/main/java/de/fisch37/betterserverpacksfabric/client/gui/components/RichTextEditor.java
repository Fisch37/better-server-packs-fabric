package de.fisch37.betterserverpacksfabric.client.gui.components;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RichTextEditor extends FlowLayout {
    public RichTextEditor(Sizing horizontalSizing, Sizing verticalSizing) {
        super(horizontalSizing, verticalSizing, Algorithm.VERTICAL);
    }

    public void setText(@Nullable Text text) {

    }

    public Optional<Text> getText() {
        // TODO: All of this
        return Optional.of(Text.literal("Placeholder text from RichTextEditor"));
    }
}
