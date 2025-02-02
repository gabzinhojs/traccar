package org.traccar.protocol;

import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import jakarta.inject.Inject;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.Protocol;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

public class ITRProtocol extends BaseProtocol {

    @Inject
    public ITRProtocol() {
        super(protocol);
        setSupportedDataCommands(
            Command.TYPE_ENGINE_STOP,
            Command.TYPE_ENGINE_RESUME,
            Command.TYPE_CUSTOM
        );

        addServer(
            new TrackerServer(false, getName()) {
                @Override
                protected void addProtocolHandlers(PipelineBuilder pipeline) {
                    pipeline.addLast(new StringEncoder());
                    pipeline.addLast(new StringDecoder());
                    pipeline.addLast(new ITRProtocolEncoder(ITRProtocol.this));
                    pipeline.addLast(new ITRProtocolDecoder(ITRProtocol.this));
                }
            }
        );
    }
}
