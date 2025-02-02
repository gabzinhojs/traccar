
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseFrameDecoder;

public class ITRFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            Channel channel, ByteBuf buf) throws Exception {

        // Verifica o início do pacote (validação do cabeçalho)
        if (buf.getByte(buf.readerIndex()) != 0x28 || buf.getByte(buf.readerIndex() + 1) != 0x28) {
            buf.readUnsignedByte(); // Descartar byte inválido
            return null;
        }

        // Verifica se há bytes suficientes para o cabeçalho e dados
        if (buf.readableBytes() < 7) {
            return null;
        }

        // Calcula o comprimento total da mensagem (cabeçalho + corpo)
        int length = 5 + buf.getUnsignedShort(buf.readerIndex() + 3); // Cabeçalho (5) + tamanho do conteúdo

        // Se houver bytes suficientes, retorna o slice da mensagem
        if (buf.readableBytes() >= length) {
            return buf.readRetainedSlice(length);
        }

        return null;
    }
