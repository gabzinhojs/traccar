public class IterProtocol extends BaseProtocol {
    
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
