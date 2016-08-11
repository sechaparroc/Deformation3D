package modules;

import interactive.Interactive;

public interface InteractiveListener<I extends Interactive> {
	void listen(String event, I interactive);
}
