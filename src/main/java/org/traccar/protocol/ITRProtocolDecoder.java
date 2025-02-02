package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.DeviceSession;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.Device;
import org.traccar.model.WifiAccessPoint;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ITRProtocolDecoder extends BaseProtocolDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ITRProtocolDecoder.class);

    private static final int PID_LOGIN = 0x01;
    private static final int PID_HBT = 0x03;
    private static final int PID_LOCATION = 0x12;
    private static final int PID_WARNING = 0x14;
    private static final int PID_REPORT = 0x15;
    private static final int PID_MESSAGE = 0x16;
    private static final int PID_COMMAND = 0x80;

    private final Map<Integer, ByteBuf> photos = new HashMap<>();

    public ITRProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Object decodeLogin(int seq, Channel channel, SocketAddress remoteAddress, ByteBuf buf, DeviceSession deviceSession) throws Exception {
        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);

        deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        } else {
            ByteBuf response = Unpooled.buffer();
            response.writeByte(0x28);
            response.writeByte(0x28);
            response.writeByte(PID_LOGIN);
            response.writeShort(9); //size
            response.writeShort(seq); //seq
            response.writeInt(0);
            response.writeShort(0x01);
            response.writeByte(0x00);

            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
            return null;
        }
    }

    private Object decodeHBT(int seq, Channel channel, SocketAddress remoteAddress, ByteBuf buf, DeviceSession deviceSession) throws Exception {
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        getLastLocation(position, null);

        position.setTime(position.getServerTime());

        // Decode status
        decodeStatus(position, buf);

        // Response HBT
        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(PID_HBT);
        response.writeShort(2); //size
        response.writeShort(seq); //seq

        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        return position;
    }

    private Object decodeStatus(Position position, ByteBuf buf) {
        int mask = buf.readUnsignedShort();
        position.set("status", mask);

        position.setValid(BitUtil.check(mask, 0));
        position.set("designedToCar", BitUtil.check(mask, 1));
        if (BitUtil.check(mask, 1)) {
            position.set(Position.KEY_IGNITION, BitUtil.check(mask, 2));
        }

        if (BitUtil.check(mask, 3)) {
            position.set(Position.KEY_MOTION, BitUtil.check(mask, 4));
        }

        if (BitUtil.check(mask, 5)) {
            position.set(Position.KEY_BLOCKED, !BitUtil.check(mask, 6));
        }

        if (BitUtil.check(mask, 7)) {
            position.set(Position.KEY_CHARGE, BitUtil.check(mask, 8));
        }

        position.set("gpsEnabled", BitUtil.check(mask, 10));
        position.set("DIN0", BitUtil.check(mask, 12));

        return position;
    }

    private Object decodePosition(Position position, ByteBuf buf) {
        long time = buf.readUnsignedInt();
        position.setTime(new Date(time * 1000));

        int mask = buf.readUnsignedByte();

        if (BitUtil.check(mask, 0)) { // gps data
            double latitude = (buf.readInt() / 180.0) / 10000;
            double longitude = (buf.readInt() / 180.0) / 10000;

            position.setLatitude(latitude);
            position.setLongitude(longitude);
            position.setAltitude(buf.readShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());

            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if (BitUtil.check(mask, 1)) { // BSID data
            int mcc = buf.readUnsignedShort();
            int mnc = buf.readUnsignedShort();
            int lac = buf.readUnsignedShort();
            long cid = buf.readUnsignedInt();

            int rssi = -110 + buf.readUnsignedByte();

            position.set(Position.KEY_RSSI, rssi);
            position.setNetwork(new Network(CellTower.from(mcc, mnc, lac, cid)));
        }

        return position;
    }

    private Object decodeLocation(int seq, Channel channel, SocketAddress remoteAddress, ByteBuf buf, DeviceSession deviceSession) throws Exception {
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        decodePosition(position, buf);
        decodeStatus(position, buf);

        position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
        position.set(Position.KEY_POWER, buf.readUnsignedShort() / 100.0);
        position.set("AIN1", buf.readUnsignedShort() / 1000.0);
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

        buf.skipBytes(18);
        buf.readUnsignedInt(); // Accumulated time (seconds) with ignition on

        // Response Location
        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(PID_LOCATION);
        response.writeShort(2); //size
        response.writeShort(seq); //seq

        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        return position;
    }

    private Object decodeMessage(int seq, Channel channel, SocketAddress remoteAddress, ByteBuf buf, DeviceSession deviceSession) throws Exception {
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(position.getServerTime());

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(PID_MESSAGE);
        response.writeShort(2); //size
        response.writeShort(seq); //seq

        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        return null;
    }

    private Object decodeReport(int seq, Channel channel, SocketAddress remoteAddress, ByteBuf buf, DeviceSession deviceSession) throws Exception {
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        decodePosition(position, buf);

        int report = buf.readUnsignedByte();

        switch (report) {
            case 0x01:
                position.set(Position.KEY_IGNITION, true);
                break;
            case 0x02:
                position.set(Position.KEY_IGNITION, false);
                break;
            case 0x03:
                position.set("DINChanged", true);
                break;
        }

        decodeStatus(position, buf);

        buf.skipBytes(8);

        if (buf.readableBytes() >= 2) {
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
        }

        if (buf.readableBytes() >= 2) {
            position.set(Position.KEY_POWER, buf.readUnsignedShort() / 100.0);
        }

        if (buf.readableBytes() >= 2) {
            position.set("AIN1", buf.readUnsignedShort() / 1000.0);
        }

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(PID_REPORT);
        response.writeShort(2); //size
        response.writeShort(seq); //seq

        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        return null;
    }

    private Object decodeWarning(int seq, Channel channel, SocketAddress remoteAddress, ByteBuf buf, DeviceSession deviceSession) throws Exception {
        if (deviceSession == null) {
            return null;
        }

        int warning = buf.readUnsignedByte();

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setTime(new Date());

        switch (warning) {
            case 0x01:
                position.set(Position.KEY_ALARM, "speed");
                break;
            case 0x02:
                position.set(Position.KEY_ALARM, "overlimit");
                break;
            case 0x03:
                position.set(Position.KEY_ALARM, "offroad");
                break;
        }

        ByteBuf response = Unpooled.buffer();
        response.writeByte(0x28);
        response.writeByte(0x28);
        response.writeByte(PID_WARNING);
        response.writeShort(2); //size
        response.writeShort(seq); //seq

        channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, ByteBuf buf) throws Exception {
        int pid = buf.readUnsignedByte();
        int seq = buf.readUnsignedByte();

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);

        switch (pid) {
            case PID_LOGIN:
                return decodeLogin(seq, channel, remoteAddress, buf, deviceSession);
            case PID_HBT:
                return decodeHBT(seq, channel, remoteAddress, buf, deviceSession);
            case PID_LOCATION:
                return decodeLocation(seq, channel, remoteAddress, buf, deviceSession);
            case PID_REPORT:
                return decodeReport(seq, channel, remoteAddress, buf, deviceSession);
            case PID_MESSAGE:
                return decodeMessage(seq, channel, remoteAddress, buf, deviceSession);
            case PID_WARNING:
                return decodeWarning(seq, channel, remoteAddress, buf, deviceSession);
            default:
                LOGGER.error("Unknown protocol id: {}", pid);
                return null;
        }
    }
}
