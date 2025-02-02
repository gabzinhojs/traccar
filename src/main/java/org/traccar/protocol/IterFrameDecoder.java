package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.traccar.NetworkMessage;

import java.util.List;

public class IterFrameDecoder extends ByteToMessageDecoder {

    private static final int HEADER_LENGTH = 5;
    private static final int MARKER = 0x2828;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        while (buf.readableBytes() >= HEADER_LENGTH) {
            int originalReaderIndex = buf.readerIndex();

            int marker = buf.readUnsignedShort();
            if (marker == MARKER) {
                int pid = buf.readUnsignedByte();
                int length = buf.readUnsignedShort();

                if (buf.readableBytes() >= length) {
                    ByteBuf frame = buf.readRetainedSlice(length);
                    out.add(new NetworkMessage(frame, ctx.channel().remoteAddress()));
                } else {
                    buf.readerIndex(originalReaderIndex);
                    break;
                }
            } else {
                buf.readerIndex(originalReaderIndex + 1);
            }
        }
    }
}
