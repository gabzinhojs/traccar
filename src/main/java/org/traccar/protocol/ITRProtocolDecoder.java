package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.net.SocketAddress;
import java.util.Date;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
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
        response.writeShort(2);
        response.writeShort(seq);
        channel.writeAndFlush(new NetworkMessage(response, remoteAddress));
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
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(new Date(buf.readUnsignedInt() * 1000L));
        position.setValid(true);
        position.setLatitude(buf.readInt() / 180.0 / 10000);
        position.setLongitude(buf.readInt() / 180.0 / 10000);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

        sendResponse(channel, remoteAddress, PID_LOCATION, seq);
        return position;
    }

    private Object decodeReport(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf,
        DeviceSession deviceSession
    ) {
        // Implementar lógica específica do relatório
        sendResponse(channel, remoteAddress, PID_REPORT, seq);
        return null;
    }

    private Object decodeMessage(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf,
        DeviceSession deviceSession
    ) {
        // Implementar lógica específica de mensagem
        sendResponse(channel, remoteAddress, PID_MESSAGE, seq);
        return null;
    }

    private Position decodeWarning(
        int seq,
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf,
        DeviceSession deviceSession
    ) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.set(Position.KEY_ALARM, Position.ALARM_GENERAL);
        sendResponse(channel, remoteAddress, PID_WARNING, seq);
        return position;
    }

    @Override
    protected Object decode(
        ChannelHandlerContext ctx,
        Channel channel,
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
                    return decodeLogin(
                        seq,
                        channel,
                        ctx.channel().remoteAddress(),
                        buf
                    );
                case PID_HBT:
                    return decodeHbt(
                        seq,
                        channel,
                        ctx.channel().remoteAddress(),
                        deviceSession
                    );
                case PID_LOCATION:
                    return decodeLocation(
                        seq,
                        channel,
                        ctx.channel().remoteAddress(),
                        buf,
                        deviceSession
                    );
                case PID_REPORT:
                    return decodeReport(
                        seq,
                        channel,
                        ctx.channel().remoteAddress(),
                        buf,
                        deviceSession
                    );
                case PID_MESSAGE:
                    return decodeMessage(
                        seq,
                        channel,
                        ctx.channel().remoteAddress(),
                        buf,
                        deviceSession
                    );
                case PID_WARNING:
                    return decodeWarning(
                        seq,
                        channel,
                        ctx.channel().remoteAddress(),
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
}
