package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class ITRProtocolEncoder extends BaseProtocolEncoder {

    public ITRProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(long deviceId, String content) {
        ByteBuf response = Unpooled.buffer();

        // Start of frame
        response.writeByte(0x28);
        response.writeByte(0x28);

        // Protocol identifier for the command
        response.writeByte(0x80);

        // Calculate total size of the content
        int contentSize = 7 + content.length();
        response.writeShort(contentSize);

        // Generate a random sequence number
        Random random = new Random();
        int sequence = random.nextInt(65535);
        response.writeShort(sequence); // seq

        // Command type (1 for relay control)
        response.writeByte(0x01); 

        // Some custom ID for the request (can be used for correlation)
        response.writeInt(sequence); 

        // Adding content in ASCII encoding
        response.writeBytes(content.getBytes(StandardCharsets.US_ASCII));

        return response;
    }

    @Override
    protected Object encodeCommand(Command command) {
        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeContent(command.getDeviceId(), "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeContent(command.getDeviceId(), "RELAY,0#");
            case Command.TYPE_CUSTOM:
                return encodeContent(command.getDeviceId(), command.getString(Command.KEY_DATA));
            default:
                return null;
        }
    }
}
