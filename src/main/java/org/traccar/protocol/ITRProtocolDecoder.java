package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Device;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;

public class ITRProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        ITRProtocolDecoder.class
    );

    private static final int PID_LOGIN = 0x01;
    private static final int PID_HBT = 0x03;
    private static final int PID_LOCATION = 0x12;
    private static final int PID_WARNING = 0x14;
    private static final int PID_REPORT = 0x15;
    private static final int PID_MESSAGE = 0x16;
    private static final int PID_COMMAND = 0x80;

    public ITRProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
        Channel channel,
        SocketAddress remoteAddress,
        Object msg
    ) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        try {
            // Verificar cabeçalho básico (0x28 0x28)
            if (
                buf.readableBytes() < 2 ||
                buf.readUnsignedByte() != 0x28 ||
                buf.readUnsignedByte() != 0x28
            ) {
                return null;
            }

            // Verificar tamanho mínimo do frame
            if (buf.readableBytes() < 5) { // PID(1) + Length(2) + Seq(2)
                return null;
            }

            int pid = buf.readUnsignedByte();
            int length = buf.readUnsignedShort();
            int seq = buf.readUnsignedShort();

            // Verificar dados restantes
            if (buf.readableBytes() < length - 2) { // Seq já foi lido
                return null;
            }

            DeviceSession deviceSession = getDeviceSession(
                channel,
                remoteAddress
            );
            ByteBuf content = buf.readSlice(length - 2);

            switch (pid) {
                case PID_LOGIN:
                    return decodeLogin(
                        seq,
                        channel,
                        remoteAddress,
                        content,
                        deviceSession
                    );
                case PID_HBT:
                    return decodeHBT(
                        seq,
                        channel,
                        remoteAddress,
                        content,
                        deviceSession
                    );
                case PID_LOCATION:
                    return decodeLocation(
                        seq,
                        channel,
                        remoteAddress,
                        content,
                        deviceSession
                    );
                case PID_REPORT:
                    return decodeReport(
                        seq,
                        channel,
                        remoteAddress,
                        content,
                        deviceSession
                    );
                case PID_MESSAGE:
                    return decodeMessage(
                        seq,
                        channel,
                        remoteAddress,
                        content,
                        deviceSession
                    );
                case PID_WARNING:
                    return decodeWarning(
                        seq,
                        channel,
                        remoteAddress,
                        content,
                        deviceSession
                    );
                default:
                    LOGGER.warn("Unknown PID: {}", pid);
                    return null;
            }
        } finally {
            buf.release();
        }
    }

    private void sendResponse(
        Channel channel,
        SocketAddress remoteAddress,
        int pid,
        int seq
    ) {
        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(pid);
        response.writeShort(2); // Length (seq only)
        response.writeShort(seq);
        sendReply(
            channel,
            remoteAddress,
            new NetworkMessage(response, remoteAddress)
        );
    }

    private Object decodeLogin(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf,
        DeviceSession deviceSession
    ) {
        if (buf.readableBytes() < 8) {
            return null;
        }

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
        deviceSession = getDeviceSession(channel, remoteAddress, imei);

        if (deviceSession != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x28);
            response.writeByte(0x28);
            response.writeByte(PID_LOGIN);
            response.writeShort(9); // Length
            response.writeShort(seq);
            response.writeInt(0);
            response.writeShort(0x01);
            response.writeByte(0x00);
            sendReply(
                channel,
                remoteAddress,
                new NetworkMessage(response, remoteAddress)
            );
        }
        return null;
    }

    private Position decodeHBT(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf,
        DeviceSession deviceSession
    ) {
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        getLastLocation(position, null);
        position.setTime(new Date());

        sendResponse(channel, remoteAddress, PID_HBT, seq);
        return position;
    }

    private Position decodeLocation(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf,
        DeviceSession deviceSession
    ) {
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        // Decodificação simplificada para exemplo
        position.setTime(new Date(buf.readUnsignedInt() * 1000L));
        position.setValid(true);
        position.setLatitude(buf.readInt() / 180.0 / 10000);
        position.setLongitude(buf.readInt() / 180.0 / 10000);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

        sendResponse(channel, remoteAddress, PID_LOCATION, seq);
        return position;
    }
    // Métodos restantes (decodeReport, decodeMessage, decodeWarning) seguindo o mesmo padrão
}
