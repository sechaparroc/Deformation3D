package modules;

public interface ModuleListener<M extends Module> {
	void listen(Module.Event event);
}


