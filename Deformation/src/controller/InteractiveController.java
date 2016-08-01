package controller;

import data.Data;
import interactive.Interactive;

public abstract class InteractiveController<D extends Data, I extends Interactive> implements InteractiveListener<I>, InteractiveMapper<D, I>{
	protected D dataObject;

	public InteractiveController(I interactive, boolean create){
		interactive.attach(this);
		if(create)this.interactiveToData(interactive);
	}
	
	public InteractiveController(I interactive){
		this(interactive, false);
	}

	public D getDataObject() {
		return dataObject;
	}

	public void setDataObject(D dataObject) {
		this.dataObject = dataObject;
	}

}
