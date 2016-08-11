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
	private Mesh input;
	private Mesh output;
	
	public MLSSolverModule() {
		super();
		solver = new MLSSolver();
		state = new HashMap<String, Boolean>();
		state.put("UPDATE", false);
		state.put("EXECUTE", false);
		state.put("INLINE", false);//output == input is false
		controlPoints = new ArrayList<ControlPoint>();
	}
	
	public void setup(){
		solver.updateControlPoints(input.getVertices(), controlPoints);
	}
	

	public ArrayList<ControlPoint> getControlPoints() {
		return controlPoints;
	}

	public void setControlPoints(ArrayList<ControlPoint> controlPoints) {
		this.controlPoints = controlPoints;
	}

	public Mesh getInput() {
		return input;
	}

	public void setInput(Mesh input) {
		this.input = input;
		setup();
	}

	
	public MLSSolverModule(Mesh mesh) {
		this();
		processInput(mesh);
	}
	
	public void addControlPoint(ControlPoint cp){
		controlPoints.add(cp);
		solver.updateControlPoints(input.getVertices(), controlPoints);
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
				state.put("UPDATE", true);
				state.put("EXECUTE", true);
			}
			else if(event.getName().equalsIgnoreCase("TRANSLATE_B")){
				System.out.println("ENTRA");
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

	/*Sends events indicating changes in the solver*/
	@Override
	public void processOutput(){
		if(state.get("UPDATE")){
			solver.updateControlPoints(input.getVertices(), controlPoints);
		}
		if(state.get("EXECUTE")){
		    System.out.println("++ENTRAAAAAAA");			
			solver.calculateNewImage(output.getVertices(), controlPoints);
			if(state.get("INLINE")) input = output;
			
			for(int i = 0; i < input.getVertices().size(); i++){
				if(!input.getVertices().get(i).getPos().equals(output.getVertices().get(i).getPos())){
					System.out.println("or : " + input.getVertices().get(i).getPos());
					System.out.println("df : " + output.getVertices().get(i).getPos());
				}
			}
			
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
