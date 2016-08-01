package controller;

import data.Data;

public interface DataListener<D extends Data> {
	void listen(String event, D object);
}
