package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;
import org.traccar.session.DeviceSession;

public class ITRProtocolEncoder extends BaseProtocolEncoder {

    public ITRProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeCommand(Command command, String content) {
        DeviceSession deviceSession = getSessionManager()
            .getDeviceSession(command.getDeviceId());
        if (deviceSession == null) {
            throw new IllegalArgumentException("Device session not found");
        }

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x28);
        buf.writeByte(0x28);
        buf.writeByte(0x80);
        buf.writeShort(7 + content.length());
        buf.writeShort(0); // placeholder para sequence
        buf.writeByte(0x01);
        buf.writeInt(0); // placeholder para ID
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII));
        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {
        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeCommand(command, "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeCommand(command, "RELAY,0#");
            case Command.TYPE_CUSTOM:
                return encodeCommand(
                    command,
                    command.getString(Command.KEY_DATA)
                );
            default:
                return null;
        }
    }
}
