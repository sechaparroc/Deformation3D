package data;

public interface DataListener<D extends Data> {
	void listen(String event, D object);
}
