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
        if (buf.readableBytes() < 5) {
            return null;
        }

        int start = buf.readerIndex();
        if (
            buf.getUnsignedByte(start) != 0x28 ||
            buf.getUnsignedByte(start + 1) != 0x28
        ) {
            buf.skipBytes(1);
            return null;
        }

        int length = buf.getUnsignedShort(start + 3) + 5;
        if (buf.readableBytes() >= length) {
            return buf.readRetainedSlice(length);
        }

        return null;
    }
}
