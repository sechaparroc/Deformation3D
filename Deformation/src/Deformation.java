import java.util.*;

import processing.core.*;
import remixlab.bias.core.*;
import remixlab.bias.event.*;
import remixlab.proscene.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.constraint.AxisPlaneConstraint;
import remixlab.dandelion.constraint.EyeConstraint;
import remixlab.dandelion.constraint.AxisPlaneConstraint.Type;
import remixlab.dandelion.core.*;

public class Deformation extends PApplet{
	private static final long serialVersionUID = 1L;
	public static Scene main_scene;
	public static int all_width = 800;
	public static int all_height = 600;
	public static Shape shape;
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

	static public void main(String[] args) {
		PApplet.main(Deformation.class.getName());
	}	
	
	public void settings(){
		size(all_width, all_height, PConstants.P3D);		
	}
	
	public void setupScene(){
	  //get bounding rect center
	  Vec[] r_bounds = shape.getR_bounds();
	  //set radius
	  float radius = Math.max(Math.abs(r_bounds[1].x() - r_bounds[0].x()), Math.abs(r_bounds[1].y() - r_bounds[0].y()));
	  radius = Math.max(radius, Math.abs(r_bounds[1].z() - r_bounds[0].z()));
	  main_scene.setBoundingBox(shape.getOriginal_fig().inverseCoordinatesOf(new Vec(-radius*.6f,-radius*.6f,-radius*.6f)), 
			  shape.getOriginal_fig().inverseCoordinatesOf(new Vec(radius*.6f,radius*.6f,radius*.6f)));
	  main_scene.showAll();
	  main_scene.camera().setType(Camera.Type.ORTHOGRAPHIC);	  
	}
	
	public void setup() {
	  textureWrap(REPEAT);
	  noSmooth();
   	  main_scene = new Scene(this);
	  main_scene.setDottedGrid(false);
	  shape = new Shape(this, main_scene,"TropicalFish15.obj");
	  //setup the scene
	  setupScene();
	  println(shape.getOriginal_fig().shape().getChildCount());
	  
	  print(testing("sdaf", "asdf", "2s24s"));
	  print(testing("sdaf"));
	  print(testing());
	  
	  
	  
	}

	public ArrayList<String> testing(String...strings){
		return new ArrayList<String>(Arrays.asList(strings));
	}
	
	public void print(ArrayList<String> s){
		System.out.println("--------------- test------------------");
		if(s.isEmpty()) System.out.println("Array is empty");
		for(String ss : s){
			System.out.println(ss);
		}
		System.out.println("--------------- test------------------");
	}
	
	
	public void draw() { 
	  ambientLight(80, 80, 80);
	  directionalLight(255, 255, 255, 0, 0, -1);
	  lightFalloff(1, 0, 0);
	  lightSpecular(0, 0, 0);
	  background(0);
	  shape.draw();
	}

	//----------------------------------------------------------------
	//GUI METHODS-----------------------------------------------------
	//----------------------------------------------------------------
	//----------------------------------------------------------------

	

	public void mousePressed( ){
	  if(!add_mode) return;
	  //use point under pixel to locate the point in an appropriate depth position
	  Vec point_shape = shape.getIntersectionPoint(mouseX, mouseY);
	  if(point_shape == null) return;//if there's not a pixel don't do anything
	  if(mouseButton == LEFT){
	    //check if point exist
	    int pos = shape.getControlPoint(mouseX, mouseY);
	    if(pos == -1){
	      //if not add to control points
	      shape.addPoint(new PVector(point_shape.x(), point_shape.y(), point_shape.z()));
	    }
	  }
	}


	//KEYBOARD HANDLING EVENTS
	public void keyPressed(){
	  if (key == 'x' || key == 'X'){
	    add_mode = !add_mode;
	  }

	  if(key == 'm' || key == 'm'){
		  shape.setSolver(Shape.Solver.MLS);
		  shape.executeDeformationAction();
	  }

	  if(key == 'l' || key == 'L'){
		  shape.setSolver(Shape.Solver.LAPLACIAN);
		  shape.executeDeformationAction();
	  }
	  
	  if (key == 'r'){
	    shape.setShowBoundingBox(!shape.isShowingBoundingBox());
	  }

	  //predefined transformation
	  //auto points 
	  if(key == 'v' || key == 'V'){
	    Shape.step_per_point = shape.getVertices().size()/((int)random(15,30) + 1);
	    shape.addControlPointsAuto(true);
		  shape.executeDeformationAction();
	  }
	  //scale x
	  if(key=='1'){
		  shape.scaleX(clear_points);
		  shape.executeDeformationAction();
	  }
	  //scale y  
	  if(key=='2'){
		  shape.scaleY(clear_points);
		  shape.executeDeformationAction();
	  }
	  //scale z  
	  if(key=='3'){
		  shape.scaleZ(clear_points);
		  shape.executeDeformationAction();
	  }
	  //spline XZ
	  //horizontal
	  if(key=='4'){
		  shape.applyHorizontalZXSpline(clear_points);
		  shape.executeDeformationAction();
	  }
	  //vertical
	  if(key=='5'){
		  shape.applyVerticalZXSpline(clear_points);
		  shape.executeDeformationAction();
	  }  
	  //spline YZ
	  //spline horizontal 
	  if(key=='6'){
		shape.applyHorizontalYZSpline(clear_points);
		shape.executeDeformationAction();
	  }
	  //spline vertical 
	  if(key=='7'){
		shape.applyVerticalYZSpline(clear_points);
  	    shape.executeDeformationAction();
	  }
	  //spline XY
	  //spline horizontal 
	  if(key=='8'){
		shape.applyHorizontalXYSpline(clear_points);
		shape.executeDeformationAction();
	  }
	  //spline vertical 
	  if(key=='9'){
		shape.applyVerticalXYSpline(clear_points);
		shape.executeDeformationAction();
	  }
	}
}