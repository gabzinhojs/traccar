import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import org.traccar.BaseFrameDecoder;

public class ITRFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
        Channel channel,
        SocketAddress remoteAddress,
        ByteBuf buf
    ) throws Exception {
        // Verifica se há bytes suficientes para o início do cabeçalho (2 bytes)
        if (buf.readableBytes() < 2) {
            return null;
        }

        // Valida os dois primeiros bytes do cabeçalho (0x28 0x28)
        if (
            buf.getUnsignedByte(buf.readerIndex()) != 0x28 ||
            buf.getUnsignedByte(buf.readerIndex() + 1) != 0x28
        ) {
            buf.skipBytes(1); // Descarta byte inválido
            return null;
        }

        // Verifica se há bytes suficientes para o cabeçalho completo (5 bytes)
        if (buf.readableBytes() < 5) {
            return null;
        }

        // Calcula o comprimento total da mensagem
        int contentLength = buf.getUnsignedShort(buf.readerIndex() + 3);
        int totalLength = 5 + contentLength;

        // Verifica se a mensagem completa está disponível
        if (buf.readableBytes() < totalLength) {
            return null;
        }

        // Retorna o frame decodificado
        return buf.readRetainedSlice(totalLength);
    }
}
