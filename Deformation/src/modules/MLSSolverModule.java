package modules;

import java.util.ArrayList;
import java.util.HashMap;

import data.*;
import logic.MLSSolver;

public class MLSSolverModule extends Module{
	
	/*Convenient flags to execute actions given events*/
	private HashMap<String, Boolean> state;
	
	private MLSSolver solver;
	private ArrayList<ControlPoint> controlPoints;
	private ArrayList<Vertex> input;
	private ArrayList<Vertex> output;
	
	public MLSSolverModule() {
		super();
		solver = new MLSSolver();
		state = new HashMap<String, Boolean>();
		state.put("UPDATE", false);
		state.put("EXECUTE", false);
		state.put("INLINE", false);//output == input is false
	}

	public MLSSolverModule(Mesh mesh) {
		this();
		processInput(mesh);
	}
	
	public void addControlPoint(ControlPoint cp){
		controlPoints.add(cp);
		solver.updateControlPoints(input, controlPoints);
	}

	public void removeControlPoint(ControlPoint cp){
		controlPoints.remove(cp);
	}

	public boolean isInline(){
		return state.get("INLINE");
	}
	
	public void setInline(boolean inline){
		state.put("INLINE", inline);
	}


	@Override
	public void executeEvent(Event event) {
		if(event.containsKey("POINT")){
			if(event.getName().equalsIgnoreCase("TRANSLATE_A")){
				state.put("UPDATE", true);
				state.put("EXECUTE", true);
			}
			else if(event.getName().equalsIgnoreCase("TRANSLATE_B")){
				state.put("EXECUTE", true);
			}
			else if(event.getName().equalsIgnoreCase("ADD")){
				controlPoints.add((ControlPoint) event.getData("POINT"));
				state.put("UPDATE", true);
				state.put("EXECUTE", true);
			}
			else if(event.getName().equalsIgnoreCase("REMOVE")){
				controlPoints.remove(event.getData("POINT"));
				state.put("UPDATE", true);
				state.put("EXECUTE", true);
			}
		}
	}

	/*Sends events indicating changes in the solver*/
	public void processOutput(){
		if(state.get("UPDATE")){
			solver.updateControlPoints(input, controlPoints);
		}
		if(state.get("EXECUTE")){
			solver.calculateNewImage(output, controlPoints);
			if(state.get("INLINE")) input = output;
			/*Send event*/
			Event event = new Event("VERTICES_MODIFIED");
			event.addData("VERTICES", output);
			addResponse(event);
		}
		/*set to false the flags again*/
		state.put("UPDATE",  false);
		state.put("EXECUTE", false);
	}
	
	public void processInput(Mesh mesh) {
		input = mesh.getVertices();
	}

	public ArrayList<Vertex> getOutput() {
		return output;
	}

	public void setOutput(ArrayList<Vertex> vertices_output) {
		this.output = vertices_output;
	}
	
	
	
}
