package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

public class ITRProtocol extends BaseProtocol {
    
    @Inject
    public ITRProtocol() {
        // Definindo os comandos suportados pelo protocolo
        setSupportedDataCommands(
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_CUSTOM
        );

        // Adicionando o servidor de rastreador com o protocolo iTR
        addServer(new TrackerServer(false, getName()) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                // Adicionando os manipuladores de protocolo à pipeline
                pipeline.addLast(new ITRFrameDecoder());
                pipeline.addLast(new ITRProtocolEncoder(ITRProtocol.this));
                pipeline.addLast(new ITRProtocolDecoder(ITRProtocol.this));
            }
        });
    }

}
