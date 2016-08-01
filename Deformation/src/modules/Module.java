package modules;

import java.util.ArrayList;
import java.util.HashMap;


public abstract class Module implements ModuleListener<Module>{
	public static class Event{
		private String name;
		private HashMap<String, Object> data;

		public Event(String s){
			setName(s);
			data = new HashMap<String,Object>();
		}
		
		public boolean containsKey(String key){
			return data.containsKey(key);
		}

		public void addData(String key, Object value){
			data.put(key, value);
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Object getData(String key) {
			return data.get(key);
		}
		
	}

	/*Suscribers to this module*/
	private ArrayList<ModuleListener<? extends Module>> listeners;
	/*Queue to retain the events*/
	protected ArrayList<Event> events;
	protected ArrayList<Event> response;

	
	public Module(){
		listeners = new ArrayList<ModuleListener<? extends Module>>();
		events = new ArrayList<Event>();
	}
	

	@Override
	public void listen(Event event) {
		events.add(event);
	}
	
	public void executeEvents(){
		while(!events.isEmpty()){
			Event event = events.get(0);
			executeEvent(event);
			events.remove(0);
		}
		/*Override this method to indicate which output send*/
		processOutput();
		/*Send responses to all listeners*/
		notifyModules();
	}
	
	public abstract void executeEvent(Event event);
	public abstract void processOutput();
	
	public void attachTo(Module module){
		module.attachModuleListener(this);
	}
	
	public void remove(ModuleListener<? extends Module> module){
		this.listeners.remove(module);
	}
	
	public void attachModuleListener(ModuleListener<? extends Module> listener){
		listeners.add(listener);
	}

	public void removehModuleListener(ModuleListener<? extends Module> listener){
		listeners.remove(listener);
	}
	
	public void notifyModules(){
		for(ModuleListener<? extends Module> listener : listeners){
			for(Event event : response){
				listener.listen(event);
			}
		}
	}
	
	public void addResponse(Event e){
		response.add(e);
	}
	
}
