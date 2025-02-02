package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.util.Date;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

public class ITRProtocolDecoder extends BaseProtocolDecoder {

    private static final int PID_LOGIN = 0x01;
    private static final int PID_HBT = 0x03;
    private static final int PID_LOCATION = 0x12;
    private static final int PID_WARNING = 0x14;
    private static final int PID_REPORT = 0x15;
    private static final int PID_MESSAGE = 0x16;

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
            if (
                buf.readableBytes() < 2 ||
                buf.readUnsignedByte() != 0x28 ||
                buf.readUnsignedByte() != 0x28
            ) {
                return null;
            }

            int pid = buf.readUnsignedByte();
            int length = buf.readUnsignedShort();
            int seq = buf.readUnsignedShort();

            if (buf.readableBytes() < length - 2) {
                return null;
            }

            DeviceSession deviceSession = getSessionManager()
                .getDeviceSession(
                    buf
                        .readSlice(8)
                        .toString(java.nio.charset.StandardCharsets.US_ASCII)
                        .substring(1)
                );

            switch (pid) {
                case PID_LOGIN:
                    return decodeLogin(seq, channel, remoteAddress, buf);
                case PID_HBT:
                    return decodeHbt(
                        seq,
                        channel,
                        remoteAddress,
                        deviceSession
                    );
                case PID_LOCATION:
                    return decodeLocation(
                        seq,
                        channel,
                        remoteAddress,
                        buf,
                        deviceSession
                    );
                default:
                    return null;
            }
        } finally {
            buf.release();
        }
    }

    private Position decodeLogin(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf
    ) {
        String imei = buf
            .readSlice(8)
            .toString(java.nio.charset.StandardCharsets.US_ASCII)
            .substring(1);
        DeviceSession deviceSession = getSessionManager()
            .getDeviceSession(imei);

        if (deviceSession != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x28);
            response.writeByte(0x28);
            response.writeByte(PID_LOGIN);
            response.writeShort(9);
            response.writeShort(seq);
            response.writeInt(0);
            response.writeShort(0x01);
            response.writeByte(0x00);
            channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
        }
        return null;
    }

    private Position decodeHbt(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        DeviceSession deviceSession
    ) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(new Date());
        return position;
    }

    private Position decodeLocation(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf,
        DeviceSession deviceSession
    ) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date(buf.readUnsignedInt() * 1000L));
        position.setValid(true);
        position.setLatitude(buf.readInt() / 180.0 / 10000);
        position.setLongitude(buf.readInt() / 180.0 / 10000);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

        return position;
    }
}
