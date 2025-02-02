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

    private DeviceSession getDeviceSession(Command command) {
        return command.getDeviceId() != 0
            ? getSession(command.getDeviceId())
            : null;
    }

    private ByteBuf encodeContent(Command command, String content) {
        DeviceSession deviceSession = getDeviceSession(command);
        if (deviceSession == null) {
            throw new IllegalArgumentException("Session not found");
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
                return encodeContent(command, "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(command, "RELAY,0#");
            case Command.TYPE_CUSTOM:
                return encodeContent(
                    command,
                    command.getString(Command.KEY_DATA)
                );
            default:
                return null;
        }
    }
}
