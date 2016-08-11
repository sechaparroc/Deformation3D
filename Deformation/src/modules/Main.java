package modules;

import java.util.ArrayList;
import java.util.HashMap;

import data.ControlPoint;
import interactive.Mesh;
import logic.MLSSolver;
import modules.InteractiveToDataModules.ControlPointModule;
import modules.InteractiveToDataModules.MeshModule;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;
import remixlab.dandelion.core.Camera;
import remixlab.dandelion.geom.Frame;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;
import utilities.Utilities;

public class Main extends PApplet{
	private static final long serialVersionUID = 1L;
	public static Scene main_scene;
	public static int all_width = 800;
	public static int all_height = 600;
	//public static Shape shape;
	//GUI VARIABLES
	public static int step_per_point = 10;
	public static int num_t = 1;	
	//needed to know the change of position 
	public static boolean clear_points = true;
	public static boolean change_points = true;
	//KEYBOARD HANDLING EVENTS
	public static boolean add_mode = true;
	public static boolean enable_texture = false;
	//Testing purposes
	boolean debug = false;
	
	//Modules
	public static MeshModule meshModule;
	public static ControlPointModule controlPointModule;
	public static MLSSolverModule mlsmodule;
	
	//Data to be used
	public static interactive.Mesh original;
	public static interactive.Mesh deformed;
	
	public interactive.ControlPoint cp;
	
	
	//Useful vars
	public static Vec[] original_bounds;
	public static Vec[] deformed_bounds;
	

	static public void main(String[] args) {
		PApplet.main(Main.class.getName());
	}	
	
	//----------------------------------------------------------------
	//SET UP THE SCENE AND THE MODULES TO BE USED---------------------
	//----------------------------------------------------------------
	//----------------------------------------------------------------
	
	public void settings(){
		size(all_width, all_height, PConstants.P3D);		
	}
	
	public void setupScene(){
   	  /*Consider the size of the scene according to the bounding box of the
	   * meshes*/	
	  float radius = Math.max(Math.abs(original_bounds[1].x() - original_bounds[0].x()), 
			  Math.abs(original_bounds[1].y() - original_bounds[0].y()));
	  radius = Math.max(radius, 
			  Math.abs(original_bounds[1].z() - original_bounds[0].z()));
		
	  main_scene.setBoundingBox(original.inverseCoordinatesOf(new Vec(-radius,-radius,-radius)), 
			  original.inverseCoordinatesOf(new Vec(radius,radius,radius)));
	  
	  //Apply some transformations
	  original.translate(-radius/2.f,0,0);
	  deformed.translate(radius/2.f,0,0);
      original.rotate(0,0,PConstants.PI,0);
      deformed.rotate(0,0,PConstants.PI,0);
	  
	  main_scene.showAll();
	  main_scene.camera().setType(Camera.Type.ORTHOGRAPHIC);	  
	}
	
	public void setup() {
	  textureWrap(REPEAT);
	  noSmooth();
 	  main_scene = new Scene(this);
	  main_scene.setDottedGrid(false);
	  setupModules();
  	  setupScene();
  	  cp = new interactive.ControlPoint(main_scene, original,  new Vec(40,40,40));
	}

	
	/*Initialize modules*/
	public void setupModules(){
	  controlPointModule = new ControlPointModule();
	  setupMeshModule("TropicalFish15.obj");
	  setupLogicModules();
	}
	
	/*Configure Modules to be used*/
	public void setupMeshModule(String path){
		meshModule = new MeshModule();
		//Load shapes
		PShape figure;
		if(path.equals("") || debug) 
			figure = utilities.Utilities.testShape(this, 100, 100, 100, 10);
		else{
			figure = loadShape(path);
		    figure = utilities.Utilities.cloneShape(this, figure, color(255,0,0));
		}
	    PShape deformed_figure = utilities.Utilities.cloneShape(this, figure, color(100,0,130));
	    figure.setFill(color(255,0,0));
	    deformed_figure.setFill(color(100,0,130));
	    if(path.equals("") || debug){
	    	figure.setStroke(true);
	    	deformed_figure.setStroke(true);
	    }
	    //create Meshes
	    original = new Mesh(main_scene, figure); 
	    deformed = new Mesh(main_scene, deformed_figure); 
	    
	    //calculate bounding boxes
	    original_bounds = Utilities.getCube(original.shape());
	    deformed_bounds = Utilities.getCube(original.shape());
	    
	    //relate meshes with modules (add data)
	    meshModule.addMesh(original);
	    meshModule.addMesh(deformed);
	}

	
	/*Arrange other modules to be used*/
	public void setupLogicModules(){
		mlsmodule = new MLSSolverModule();
		/*Attach the module to feed it (input)*/
		mlsmodule.attachTo(controlPointModule, controlPointModule.getPoints());
		mlsmodule.attachTo(meshModule, meshModule.getDataMesh(original));
		/*Set initial values*/
		mlsmodule.setInput(meshModule.getDataMesh(original));
		mlsmodule.setOutput(meshModule.getDataMesh(deformed));
		/*now attach responses form logic modules to interactive modules*/
		controlPointModule.attachTo(mlsmodule, mlsmodule.getControlPoints());
		meshModule.attachTo(mlsmodule, mlsmodule.getOutput());
	}
	
	/*Define the order in which the modules are going to be executed*/
	public void executeModules(){
		controlPointModule.executeEvents();
		mlsmodule.executeEvents();
		meshModule.executeEvents();
	}

	//----------------------------------------------------------------
	//----------------------------------------------------------------
	//----------------------------------------------------------------
	//----------------------------------------------------------------
	

	public void draw() { 
		executeModules();
		
	    ambientLight(80, 80, 80);
	    directionalLight(255, 255, 255, 0, 0, -1);
	    lightFalloff(1, 0, 0);
	    lightSpecular(0, 0, 0);
	    background(0);
	    
	    //draw the meshes

	    for(interactive.Mesh m : meshModule.getMeshes().getInteractive()){
	    	m.draw();
	    }
	    
	    for(interactive.ControlPoint cp : controlPointModule.getPoints().getInteractive()){
	    	cp.draw();
	    }
	    cp.draw();
	}
	
	
	//----------------------------------------------------------------
	//GUI METHODS-----------------------------------------------------
	//----------------------------------------------------------------
	//----------------------------------------------------------------

	public void mousePressed( ){
	  if(!add_mode) return;
	  //use point under pixel to locate the point in an appropriate depth position
	  Vec point_shape = utilities.Utilities.getPointUnderPixel(original, mouseX, mouseY);
	  if(point_shape == null) return;//if there's not a pixel don't do anything
	  if(mouseButton == LEFT){
	    //check if point exist
		boolean exist_point = false;
		  
		for(interactive.ControlPoint cp : controlPointModule.getPoints().getInteractive()){
			if(cp.checkIfGrabsInput(mouseX, mouseY)){
				exist_point = true;
				break;
			} 
		}
	    if(!exist_point){
	      //if not add to control points
	      interactive.ControlPoint point = new interactive.ControlPoint(main_scene, original, point_shape);
	      /*this will send an event to other modules*/
	      controlPointModule.addPoint(point);
	    }
	  }
	}


	//KEYBOARD HANDLING EVENTS
	public void keyPressed(){
	  if (key == 'x' || key == 'X'){
	    add_mode = !add_mode;
	  }

	  if(key == 'm' || key == 'm'){
	  }

	  if(key == 'l' || key == 'L'){
	  }
	  
	  if (key == 'r'){
	  }

	  //predefined transformation
	  //auto points 
	  if(key == 'v' || key == 'V'){
	    //Shape.step_per_point = shape.getVertices().size()/((int)random(15,30) + 1);
	    //shape.addControlPointsAuto(true);
		//  shape.executeDeformationAction();
	  }
	  //scale x
	  if(key=='1'){
		  //shape.scaleX(clear_points);
		  //shape.executeDeformationAction();
	  }
	  //scale y  
	  if(key=='2'){
		  //shape.scaleY(clear_points);
		  //shape.executeDeformationAction();
	  }
	  //scale z  
	  if(key=='3'){
		  //shape.scaleZ(clear_points);
		  //shape.executeDeformationAction();
	  }
	  //spline XZ
	  //horizontal
	  if(key=='4'){
		  //shape.applyHorizontalZXSpline(clear_points);
		  //shape.executeDeformationAction();
	  }
	  //vertical
	  if(key=='5'){
		  //shape.applyVerticalZXSpline(clear_points);
		  //shape.executeDeformationAction();
	  }  
	  //spline YZ
	  //spline horizontal 
	  if(key=='6'){
		//shape.applyHorizontalYZSpline(clear_points);
		//shape.executeDeformationAction();
	  }
	  //spline vertical 
	  if(key=='7'){
		//shape.applyVerticalYZSpline(clear_points);
  	    //shape.executeDeformationAction();
	  }
	  //spline XY
	  //spline horizontal 
	  if(key=='8'){
		//shape.applyHorizontalXYSpline(clear_points);
		//shape.executeDeformationAction();
	  }
	  //spline vertical 
	  if(key=='9'){
		//shape.applyVerticalXYSpline(clear_points);
		//shape.executeDeformationAction();
	  }
	}
}
