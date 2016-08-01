package controller;

import data.Data;
import interactive.Interactive;

public interface InteractiveMapper<D extends Data, I extends Interactive> {
	public D interactiveToData(I interactive);
}