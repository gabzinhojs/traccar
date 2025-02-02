package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

public class IterProtocolEncoder extends BaseProtocolEncoder {

    private static final int PID_INSTRUCTION = 0x80;

    public IterProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeCommand(String command) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(0x2828); // marker
        buf.writeByte(PID_INSTRUCTION); // PID
        
        byte[] content = command.getBytes();
        buf.writeShort(7 + content.length); // size
        buf.writeShort(0); // sequence
        buf.writeByte(0x01); // type
        buf.writeInt(0); // instruction UID
        buf.writeBytes(content);
        
        return buf;
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {
        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeCommand("RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeCommand("RELAY,0#");
            case Command.TYPE_GET_VERSION:
                return encodeCommand("VERSION?");
            case Command.TYPE_REBOOT_DEVICE:
                return encodeCommand("RESET#");
            case Command.TYPE_FACTORY_RESET:
                return encodeCommand("FACTORY#");
            case Command.TYPE_GET_DEVICE_STATUS:
                return encodeCommand("STATUS?");
            case Command.TYPE_POSITION_SINGLE:
                return encodeCommand("WHERE?");
            default:
                return null;
        }
    }
}
