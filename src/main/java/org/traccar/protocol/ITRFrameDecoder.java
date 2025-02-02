package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

public class ITRFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
        ChannelHandlerContext ctx,
        Channel channel,
        ByteBuf buf
    ) throws Exception {
        if (buf.readableBytes() < 2) {
            return null;
        }

        if (
            buf.getUnsignedByte(buf.readerIndex()) != 0x28 ||
            buf.getUnsignedByte(buf.readerIndex() + 1) != 0x28
        ) {
            buf.skipBytes(1);
            return null;
        }

        if (buf.readableBytes() < 5) {
            return null;
        }

        int length = buf.getUnsignedShort(buf.readerIndex() + 3) + 5;
        return buf.readableBytes() >= length
            ? buf.readRetainedSlice(length)
            : null;
    }
}
