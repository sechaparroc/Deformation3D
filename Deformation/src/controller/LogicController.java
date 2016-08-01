package controller;

import java.util.ArrayList;

import data.*;

public abstract class LogicController implements DataListener<Data> {

	protected class Event{
		private String name;
		private Data data;
		
		public Event(String s, Data d){
			setName(s);
			setData(d);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Data getData() {
			return data;
		}

		public void setData(Data data) {
			this.data = data;
		}
		
	}
	
	/*Queue to retain the events*/
	protected ArrayList<Event> events = new ArrayList<Event>();

	public void addEvent(String name, Data d){
		events.add(new Event(name, d));
	}

	public abstract void executeEvents();
	
	@Override
	public void listen(String event, Data object) {
		addEvent(event, object);
	}
	
	public void attachTo(Data data){
		data.attachLogicListener(this);
	}
	
	public void remove(Data data){
		data.removeLogicListener(this);
	}
}
