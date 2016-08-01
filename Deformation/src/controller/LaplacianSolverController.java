package controller;

import java.util.ArrayList;

import data.ControlPoint;
import data.Mesh;
import data.Vertex;
import logic.LaplacianSolver;

public class LaplacianSolverController extends LogicController{
	
	private LaplacianSolver solver;
	private ArrayList<ControlPoint> controlPoints;
	private ArrayList<Vertex> input;//input
	private ArrayList<Vertex> output;//output
	
	
	/*Convinient flags to execute actions given events*/
	boolean reset_anchors;
	boolean execute;
	
	public LaplacianSolverController(Mesh mesh) {
		processInput(mesh);
		solver = new LaplacianSolver();
		solver.setup(input);
	}
	
	public void addControlPoint(ControlPoint cp){
		attachTo(cp);
		controlPoints.add(cp);
		solver.addAnchor(input, cp);
	}

	public void removeControlPoint(ControlPoint cp){
		this.remove(cp);
		controlPoints.remove(cp);
	}
	
	@Override
	public void executeEvents(){
		reset_anchors = false; 
		execute = false; 
		while(!events.isEmpty()){
			LogicController.Event event = events.get(0);
			if(event.getData() instanceof ControlPoint){
				executeEvent(event.getName(), (ControlPoint) event.getData());
			}else if(event.getData() instanceof Mesh){
				//Implement if necessary
			}
			events.remove(0);
		}
		if(reset_anchors)  solver.addAnchors(input, controlPoints, true);
		if(execute) solver.solveLaplacian(output);
	}
	
	public void executeEvent(String event, ControlPoint cp){
		if(event.equalsIgnoreCase("TRANSLATE_A")){
			/*If the origin of the control point is modified then 
			 * The Precomputed Matrix A must be calculated again*/		
			reset_anchors = true;
			execute = true;			
		}else if(event.equalsIgnoreCase("TRANSLATE_B")){
			reset_anchors = true;
			execute = true;			
		}else if(event.equalsIgnoreCase("REMOVE")){
			removeControlPoint(cp);
			reset_anchors = true;
			execute = true;			
		}
	}
	

	public void processInput(Mesh mesh) {
		input = mesh.getVertices();
	}

	public void processOutput(ArrayList<Vertex> vertices_output) {
		if(output == vertices_output) return;
		//Change positions of each vertex in the output array
		for(int i = 0; i < output.size(); i++){
			vertices_output.get(i).setPos(output.get(i).getPos());
		}
	}

	public ArrayList<Vertex> getOutput() {
		return output;
	}

	public void setOutput(ArrayList<Vertex> vertices_output) {
		this.output = vertices_output;
	}	
}
