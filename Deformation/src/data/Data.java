package data;

import java.util.ArrayList;

import controller.DataListener;

public abstract class Data {
	private ArrayList<DataListener> interactiveListeners = new ArrayList<DataListener>();
	private ArrayList<DataListener> logicListeners = new ArrayList<DataListener>();

	protected ArrayList<String> events;

	public Data(){
		initEvents();
	}
	
	public void attachInteractiveListener(DataListener observer){
		interactiveListeners.add(observer);
	}

	public void attachLogicListener(DataListener observer){
		logicListeners.add(observer);
	}
	
	public void notifyInteractiveListeners(String event){
		notifyAllListeners(event, interactiveListeners);
	}

	public void notifyLogicListeners(String event){
		notifyAllListeners(event, logicListeners);
	}
	
	public void notifyAllListeners(String event, ArrayList<DataListener> listeners){
		for (DataListener listener : listeners) {
			listener.listen(event, this);
		}
	}
	
	public void removeInteractiveListener(DataListener listener){
		interactiveListeners.remove(listener);
	}
	
	public void removeLogicListener(DataListener listener){
		logicListeners.remove(listener);
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
