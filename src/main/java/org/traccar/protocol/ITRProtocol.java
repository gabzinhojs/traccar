package org.traccar.protocol;

import jakarta.inject.Inject;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.Protocol;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

public class ITRProtocol extends BaseProtocol {

    private static final Protocol PROTOCOL = new Protocol(
        "itr",
        "ITR Protocol",
        false // Indica se o protocolo suporta comunicação sem conexão (ex: UDP)
    );

    @Inject
    public ITRProtocol() {
        super(PROTOCOL); // Passa o objeto PROTOCOL para o construtor da superclasse
        setSupportedDataCommands(
            Command.TYPE_ENGINE_STOP,
            Command.TYPE_ENGINE_RESUME,
            Command.TYPE_CUSTOM
        );

        addServer(
            new TrackerServer(false, getName()) {
                @Override
                protected void addProtocolHandlers(PipelineBuilder pipeline) {
                    // Remove StringEncoder/StringDecoder pois o protocolo é binário
                    pipeline.addLast(new ITRFrameDecoder()); // Decodificador de frames (inbound)
                    pipeline.addLast(new ITRProtocolEncoder(ITRProtocol.this)); // Codificador de comandos (outbound)
                    pipeline.addLast(new ITRProtocolDecoder(ITRProtocol.this)); // Decodificador de dados (inbound)
                }
            }
        );
    }
}
