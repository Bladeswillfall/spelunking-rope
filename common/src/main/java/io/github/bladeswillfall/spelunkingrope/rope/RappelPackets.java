package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public final class RappelPackets {
    public static final ResourceLocation INPUT_CHANNEL = new ResourceLocation(SpelunkingRope.MOD_ID, "rappel_input");
    public static final ResourceLocation STATE_CHANNEL = new ResourceLocation(SpelunkingRope.MOD_ID, "rappel_state");

    private RappelPackets() {
    }

    public record Input(byte vertical, boolean detach, boolean push) {
        public Input {
            if (vertical < -1 || vertical > 1) {
                throw new IllegalArgumentException("vertical must be -1, 0, or 1");
            }
        }
    }

    public record State(
            boolean active,
            UUID spanId,
            double anchorX,
            double anchorY,
            double anchorZ,
            double currentLength,
            double maxLength
    ) {
        public State {
            if (active) {
                Objects.requireNonNull(spanId, "spanId");
                if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY) || !Double.isFinite(anchorZ)
                        || !Double.isFinite(currentLength) || !Double.isFinite(maxLength)
                        || currentLength <= 0.0 || maxLength <= 0.0 || currentLength > maxLength) {
                    throw new IllegalArgumentException("active rappel state must be finite and length-bounded");
                }
            }
        }

        public static State detached() {
            return new State(false, null, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void encodeInput(Input input, FriendlyByteBuf buffer) {
        buffer.writeByte(input.vertical());
        buffer.writeBoolean(input.detach());
        buffer.writeBoolean(input.push());
    }

    public static Input decodeInput(FriendlyByteBuf buffer) {
        return new Input(buffer.readByte(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void encodeState(State state, FriendlyByteBuf buffer) {
        buffer.writeBoolean(state.active());
        if (!state.active()) {
            return;
        }
        buffer.writeUUID(state.spanId());
        buffer.writeDouble(state.anchorX());
        buffer.writeDouble(state.anchorY());
        buffer.writeDouble(state.anchorZ());
        buffer.writeDouble(state.currentLength());
        buffer.writeDouble(state.maxLength());
    }

    public static State decodeState(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return State.detached();
        }
        return new State(
                true,
                buffer.readUUID(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }
}
