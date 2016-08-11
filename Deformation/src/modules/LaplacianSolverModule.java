package modules;

import java.util.ArrayList;
import java.util.HashMap;

import data.*;
import logic.LaplacianSolver;
import modules.Module.Event;

public class LaplacianSolverModule extends Module{
	/*Convenient flags to execute actions given events*/
	private HashMap<String, Boolean> state;
	
	private LaplacianSolver solver;
	private ArrayList<ControlPoint> controlPoints;
	private Mesh input;
	private Mesh output;

	public LaplacianSolverModule() {
		super();
		solver = new LaplacianSolver();
		state = new HashMap<String, Boolean>();
		state.put("RESET_ANCHORS", false);
		state.put("EXECUTE", false);
		state.put("INLINE", false);//output == input is false
	}
	
	public LaplacianSolverModule(Mesh mesh){
		this();
		processInput(mesh);
		solver.setup(input.getVertices());
	}

	public void addControlPoint(ControlPoint cp){
		controlPoints.add(cp);
		 // TODO Very inefficient: Just update the Row 
		solver.addAnchor(input.getVertices(), cp);
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
		if(event.containsKey("DATA_CONTROL_POINT")){
			if(event.getName().equalsIgnoreCase("TRANSLATE_A")){
				state.put("RESET_ANCHORS", true);
				state.put("EXECUTE", true);
			}
			else if(event.getName().equalsIgnoreCase("TRANSLATE_B")){
				state.put("RESET_ANCHORS", true);
				state.put("EXECUTE", true);
			}
			else if(event.getName().equalsIgnoreCase("ADD")){
				controlPoints.add((ControlPoint) event.getData("DATA_CONTROL_POINT"));
				state.put("UPDATE", true);
				state.put("EXECUTE", true);
			}
			else if(event.getName().equalsIgnoreCase("REMOVE")){
				controlPoints.remove(event.getData("DATA_CONTROL_POINT"));
				state.put("UPDATE", true);
				state.put("EXECUTE", true);
			}
		}
	}

	@Override
	public void processOutput() {
		if(state.get("UPDATE")){
			solver.addAnchors(input.getVertices(), controlPoints, true);
		}
		if(state.get("EXECUTE")){
			solver.solveLaplacian(output.getVertices());
			if(state.get("INLINE")) input = output;
			/*Send event*/
			Event event = new Event("VERTICES_MODIFIED");
			event.addData("DATA_MESH", output);
			addResponse(event);
		}
		/*set to false the flags again*/
		state.put("UPDATE",  false);
		state.put("EXECUTE", false);
	}

	public void processInput(Mesh mesh) {
		input = mesh;
	}

	public Mesh getOutput() {
		return output;
	}

	public void setOutput(Mesh output) {
		this.output = output;
	}

}
