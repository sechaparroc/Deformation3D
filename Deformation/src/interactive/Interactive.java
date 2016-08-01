package interactive;

import java.util.ArrayList;

import controller.InteractiveListener;
import processing.core.PShape;
import remixlab.dandelion.core.Eye;
import remixlab.dandelion.core.GenericFrame;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;

public abstract class Interactive extends InteractiveFrame{

	private ArrayList<InteractiveListener> listeners = new ArrayList<InteractiveListener>();
	protected ArrayList<String> events;
	
	public Interactive(Eye eye) {
		super(eye);
		initEvents();
	}
	public Interactive(InteractiveFrame arg0) {
		super(arg0);
		initEvents();
	}
	public Interactive(Scene scn, GenericFrame referenceFrame, Object obj, String methodName) {
		super(scn, referenceFrame, obj, methodName);
		initEvents();
	}
	public Interactive(Scene scn, GenericFrame referenceFrame, PShape ps) {
		super(scn, referenceFrame, ps);
		initEvents();
	}
	public Interactive(Scene scn, GenericFrame referenceFrame) {
		super(scn, referenceFrame);
		initEvents();
	}
	public Interactive(Scene scn, Object obj, String methodName) {
		super(scn, obj, methodName);
		initEvents();
	}
	public Interactive(Scene scn, PShape ps) {
		super(scn, ps);
		initEvents();
	}
	public Interactive(Scene scn) {
		super(scn);
		initEvents();
	}
	
	public void attach(InteractiveListener observer){
		listeners.add(observer);
	}
	public void notifyAllListeners(String state){
		for (InteractiveListener listener : listeners) {
			listener.listen(state, this);
		}
	}

	/*Notify all but sender*/
	public void notifyAllListeners(String state, InteractiveListener sender){
		for (InteractiveListener listener : listeners) {
			if(!listener.equals(sender))listener.listen(state, this);
		}
	}

	
	/*This events will be common to all interactive tools*/
	private void initEvents(){
		events = new ArrayList<String>();
		events.add("ADD");
		events.add("REMOVE");
		setEvents();
	}
	
	protected abstract void setEvents();

}
