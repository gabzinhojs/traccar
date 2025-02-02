package org.traccar.protocol;

import jakarta.inject.Inject;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

public class ITRProtocol extends BaseProtocol {

    @Inject
    public ITRProtocol(Config config) {
        addServer(
            new TrackerServer(config, "itr", false) {
                @Override
                protected void addProtocolHandlers(
                    PipelineBuilder pipeline,
                    Config config
                ) {
                    pipeline.addLast(new ITRFrameDecoder());
                    pipeline.addLast(new ITRProtocolEncoder(ITRProtocol.this));
                    pipeline.addLast(new ITRProtocolDecoder(ITRProtocol.this));
                }
            }
        );
        setSupportedDataCommands(
            Command.TYPE_ENGINE_STOP,
            Command.TYPE_ENGINE_RESUME,
            Command.TYPE_CUSTOM
        );
    }
}
