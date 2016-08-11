package modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public abstract class Module implements ModuleListener<Module>{
	public class Event{
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
		
		public String toString(){
			return "Event: \n \t name : " + name + "\n \t data" + data;
		}
	}

	/*Subscribes to this module according to its attributes*/
	private HashMap<ModuleListener<? extends Module>, ArrayList<Object>> listeners;

	/*Queue to retain the events*/
	protected ArrayList<Event> events;
	protected HashMap<Event, ArrayList<Object>> response;

	
	public Module(){
		listeners = new HashMap<ModuleListener<? extends Module>, ArrayList<Object>>();
		response = new HashMap<Event,ArrayList<Object>>();
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
			System.out.println("Class " + this);
			System.out.println(event.toString());
			events.remove(0);
		}
		/*Override this method to indicate which output send*/
		processOutput();
		/*Send responses to all listeners*/
		notifyModules();
	}
	
	public abstract void executeEvent(Event event);
	public abstract void processOutput();
	
	public void attachTo(Module module, Object... objs){
		module.attachModuleListener(this, objs);
	}
	
	
	public void attachModuleListener(ModuleListener<? extends Module> listener, Object objs[]){
		ArrayList<Object> objects = new ArrayList<Object>(Arrays.asList(objs));
		listeners.put(listener, objects);
	}

	public void removehModuleListener(ModuleListener<? extends Module> listener){
		this.listeners.remove(listener);
	}
	
	public void notifyModules(){
		for(Map.Entry<Event,ArrayList<Object>> response_entry : response.entrySet()){
			for(Map.Entry<ModuleListener<? extends Module>, ArrayList<Object>> listeners_entry : listeners.entrySet()){
				if(response_entry.getValue().isEmpty() || listeners_entry.getValue().isEmpty()){
					listeners_entry.getKey().listen(response_entry.getKey());
					continue;
				}
				for(Object obj : response_entry.getValue()){
					if(listeners_entry.getValue().contains(obj)){
						listeners_entry.getKey().listen(response_entry.getKey());
						break;
					}
				}
			}
		}
		/*clear response*/
		response.clear();
	}
	
	public void addResponse(Event e, Object... objs){
		ArrayList<Object> objects = new ArrayList<Object>(Arrays.asList(objs));
		response.put(e, objects);
	}
}
