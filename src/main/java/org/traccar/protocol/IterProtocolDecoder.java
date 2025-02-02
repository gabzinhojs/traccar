package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

public class IterProtocolDecoder extends BaseProtocolDecoder {

    public IterProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, int pid, int sequence) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(0x2828); // marker
            response.writeByte(pid); // PID
            response.writeShort(2); // length
            response.writeShort(sequence); // sequence
            channel.writeAndFlush(response);
        }
    }

    private Position decodePosition(ByteBuf buf, int sequence, DeviceSession deviceSession) {
        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        // Time
        position.setTime(new Date(buf.readUnsignedInt() * 1000));

        // Mask
        int mask = buf.readUnsignedByte();
        boolean hasGps = BitUtil.check(mask, 0);
        boolean hasLbs = BitUtil.check(mask, 1);

        if (hasGps) {
            position.setValid(true);
            
            // GPS data
            position.setLatitude(buf.readInt() / 1800000.0);
            position.setLongitude(buf.readInt() / 1800000.0);
            position.setAltitude(buf.readShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
            position.setCourse(buf.readUnsignedShort());
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if (hasLbs) {
            // Cell info
            int mcc = buf.readUnsignedShort();
            int mnc = buf.readUnsignedShort();
            int lac = buf.readUnsignedShort();
            long cid = buf.readUnsignedInt();
            int rxLevel = buf.readUnsignedByte();

            Network network = new Network(CellTower.from(mcc, mnc, lac, cid, rxLevel));
            position.setNetwork(network);
        }

        // Status
        position.set(Position.KEY_STATUS, buf.readUnsignedShort());

        // Battery
        position.set(Position.KEY_BATTERY, buf.readUnsignedShort() * 0.001);

        // Analog inputs
        position.set(Position.PREFIX_ADC + 0, buf.readUnsignedShort() * 0.01);
        position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort() * 0.01);

        // Mileage
        position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        int sequence = buf.readUnsignedShort();
        List<Position> positions = new LinkedList<>();

        int pid = buf.getByte(buf.readerIndex() - 7);

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (deviceSession == null) {
            return null;
        }

        switch (pid) {
            case 0x01: // Login package
                sendResponse(channel, pid, sequence);
                return null;
            case 0x03: // Heartbeat package
                sendResponse(channel, pid, sequence);
                return null;
            case 0x12: // Location package
            case 0x14: // Warning package
            case 0x15: // Report package
                Position position = decodePosition(buf, sequence, deviceSession);
                if (position.getValid()) {
                    positions.add(position);
                }
                sendResponse(channel, pid, sequence);
                break;
            default:
                break;
        }

        return positions.isEmpty() ? null : positions;
    }
}
