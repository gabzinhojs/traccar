package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;

public class IterProtocol extends BaseProtocol {

    public IterProtocol(Config config) {
        super(config);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new IterFrameDecoder());
                pipeline.addLast(new IterProtocolDecoder(IterProtocol.this));
                pipeline.addLast(new IterProtocolEncoder(IterProtocol.this));
            }
        });
        addServer(new TrackerServer(config, getName(), true) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new IterFrameDecoder());
                pipeline.addLast(new IterProtocolDecoder(IterProtocol.this));
                pipeline.addLast(new IterProtocolEncoder(IterProtocol.this));
            }
        });
    }
}
