package io.github.bladeswillfall.spelunkingrope.rope;

import io.github.bladeswillfall.spelunkingrope.SpelunkingRope;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public final class RappelPackets {
    public static final byte MODE_RAPPEL = 1;
    public static final byte MODE_TRAVERSE = 2;
    public static final ResourceLocation INPUT_CHANNEL = new ResourceLocation(SpelunkingRope.MOD_ID, "rappel_input");
    public static final ResourceLocation STATE_CHANNEL = new ResourceLocation(SpelunkingRope.MOD_ID, "rappel_state");

    private RappelPackets() {
    }

    public record Input(byte vertical, boolean detach, boolean push, UUID grabSpanId) {
        public Input(byte vertical, boolean detach, boolean push) {
            this(vertical, detach, push, null);
        }

        public Input {
            if (vertical < -1 || vertical > 1) {
                throw new IllegalArgumentException("vertical must be -1, 0, or 1");
            }
        }
    }

    public record State(
            boolean active,
            byte mode,
            UUID spanId,
            double anchorX,
            double anchorY,
            double anchorZ,
            double currentLength,
            double maxLength,
            double traverseSpeed
    ) {
        public State(
                boolean active,
                UUID spanId,
                double anchorX,
                double anchorY,
                double anchorZ,
                double currentLength,
                double maxLength
        ) {
            this(active, active ? MODE_RAPPEL : 0, spanId, anchorX, anchorY, anchorZ, currentLength, maxLength, 0.0);
        }

        public State {
            if (active) {
                Objects.requireNonNull(spanId, "spanId");
                if (mode != MODE_RAPPEL && mode != MODE_TRAVERSE) {
                    throw new IllegalArgumentException("unknown active rope mode: " + mode);
                }
                if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY) || !Double.isFinite(anchorZ)
                        || !Double.isFinite(currentLength) || !Double.isFinite(maxLength)
                        || !Double.isFinite(traverseSpeed)
                        || maxLength <= 0.0 || currentLength < 0.0 || currentLength > maxLength
                        || (mode == MODE_RAPPEL && currentLength <= 0.0)) {
                    throw new IllegalArgumentException("active rope state must be finite and length-bounded");
                }
            }
        }

        public static State detached() {
            return new State(false, (byte) 0, null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public static State traverse(UUID spanId, double distance, double pathLength, double speed) {
            return new State(true, MODE_TRAVERSE, spanId, 0.0, 0.0, 0.0, distance, pathLength, speed);
        }
    }

    public static void encodeInput(Input input, FriendlyByteBuf buffer) {
        buffer.writeByte(input.vertical());
        buffer.writeBoolean(input.detach());
        buffer.writeBoolean(input.push());
        buffer.writeBoolean(input.grabSpanId() != null);
        if (input.grabSpanId() != null) {
            buffer.writeUUID(input.grabSpanId());
        }
    }

    public static Input decodeInput(FriendlyByteBuf buffer) {
        byte vertical = buffer.readByte();
        boolean detach = buffer.readBoolean();
        boolean push = buffer.readBoolean();
        UUID grabSpanId = buffer.readBoolean() ? buffer.readUUID() : null;
        return new Input(vertical, detach, push, grabSpanId);
    }

    public static void encodeState(State state, FriendlyByteBuf buffer) {
        buffer.writeBoolean(state.active());
        if (!state.active()) {
            return;
        }
        buffer.writeByte(state.mode());
        buffer.writeUUID(state.spanId());
        buffer.writeDouble(state.anchorX());
        buffer.writeDouble(state.anchorY());
        buffer.writeDouble(state.anchorZ());
        buffer.writeDouble(state.currentLength());
        buffer.writeDouble(state.maxLength());
        buffer.writeDouble(state.traverseSpeed());
    }

    public static State decodeState(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return State.detached();
        }
        return new State(
                true,
                buffer.readByte(),
                buffer.readUUID(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }
}
