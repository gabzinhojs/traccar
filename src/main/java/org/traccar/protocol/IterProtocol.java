package org.traccar.protocol;

import javax.inject.Inject;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.pipeline.PipelineBuilder;

public class IterProtocol extends BaseProtocol {
    
    @Inject
    public IterProtocol(Config config) {
        super(config);
        
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new IterProtocolDecoder(IterProtocol.this));
            }
        });
        
        addServer(new TrackerServer(config, getName(), true) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new IterProtocolDecoder(IterProtocol.this));
            }
        });
    }
}
