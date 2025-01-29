package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

public class iTRProtocol extends BaseProtocol {

    public iTRProtocol() {
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
                pipeline.addLast(new iTRFrameDecoder());
                pipeline.addLast(new iTRProtocolEncoder(iTRProtocol.this));
                pipeline.addLast(new iTRProtocolDecoder(iTRProtocol.this));
            }
        });
    }

}
