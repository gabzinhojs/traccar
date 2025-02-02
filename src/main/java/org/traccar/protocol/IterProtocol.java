package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;

public class IterProtocol extends BaseProtocol {

    public IterProtocol() {
        addServer(new TrackerServer(false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new IterFrameDecoder());
                pipeline.addLast(new IterProtocolDecoder(IterProtocol.this));
                pipeline.addLast(new IterProtocolEncoder(IterProtocol.this));
            }
        });
        addServer(new TrackerServer(true) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline) {
                pipeline.addLast(new IterFrameDecoder());
                pipeline.addLast(new IterProtocolDecoder(IterProtocol.this));
                pipeline.addLast(new IterProtocolEncoder(IterProtocol.this));
            }
        });
    }
}
