package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;
import org.traccar.session.DeviceSession;

public class ITRProtocolEncoder extends BaseProtocolEncoder {

    private static final int PID_COMMAND = 0x80;

    public ITRProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeCommandFrame(Command command, String payload) {
        DeviceSession deviceSession = getDeviceSession(command);
        if (deviceSession == null) {
            throw new IllegalArgumentException("Device session not available");
        }

        // Converter IMEI para formato binário (8 bytes)
        String imei = deviceSession.getImei();
        String imeiPadded = "0" + imei; // Padding para 16 caracteres hex
        byte[] imeiBytes = ByteBufUtil.decodeHexDump(imeiPadded);

        // Construir conteúdo completo
        ByteBuf content = Unpooled.buffer();
        content.writeBytes(imeiBytes);
        content.writeBytes(payload.getBytes(StandardCharsets.US_ASCII));

        // Calcular tamanho total do frame
        int frameSize = 7 + content.readableBytes(); // 7 = seq(2) + type(1) + id(4)

        // Construir frame
        ByteBuf frame = Unpooled.buffer();
        frame.writeByte(0x28); // Header
        frame.writeByte(0x28); // Header
        frame.writeByte(PID_COMMAND);
        frame.writeShort(frameSize);

        int sequence = ThreadLocalRandom.current().nextInt(65536);
        frame.writeShort(sequence); // Número de sequência
        frame.writeByte(0x01); // Tipo de comando
        frame.writeInt(sequence); // ID de correlação
        frame.writeBytes(content);

        return frame;
    }

    @Override
    protected Object encodeCommand(Command command) {
        switch (command.getType()) {
            case Command.TYPE_ENGINE_STOP:
                return encodeCommandFrame(command, "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME:
                return encodeCommandFrame(command, "RELAY,0#");
            case Command.TYPE_CUSTOM:
                String data = command.getString(Command.KEY_DATA);
                if (data != null) {
                    return encodeCommandFrame(command, data);
                }
                break;
        }
        return null;
    }
}
