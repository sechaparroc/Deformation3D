import java.util.ArrayList;
import java.util.Random;

import processing.core.PShape;
import processing.core.PVector;
import processing.core.PApplet;
import processing.core.PConstants;
import remixlab.dandelion.geom.Point;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;

public class Shape {
	
	//Define the solver to be used
	public enum Solver{
		LAPLACIAN, MLS;
	}
	
	private static boolean debug = false;
	private ArrayList<ControlPoint> control_points = new ArrayList<ControlPoint>();
	private Scene scene;
	private PApplet applet;
	private InteractiveFrame original_fig;
	private InteractiveFrame deformed_fig;
	private ArrayList<PVector> vertices = new ArrayList<PVector>();
	private ArrayList<PVector> deformed_vertices = new ArrayList<PVector>();
	private boolean show_bounding_box = true;
	private Vec r_center;
	private Vec[] r_bounds;
	//Deformation Solvers
	private LaplacianDeformation laplacianDeformation;
	private LeastSolver leastSolver;
	private Solver solver;

	
	public void calculateBoundingBox(){
		r_bounds = Utilities.getCube(original_fig.shape());
	    r_center = new Vec((r_bounds[0].x() + r_bounds[1].x())/2.f, (r_bounds[0].y() + r_bounds[1].y())/2.f, (r_bounds[0].z() + r_bounds[1].z())/2.f);
	    original_fig.rotate(0,0,PConstants.PI,0);
	    deformed_fig.rotate(0,0,PConstants.PI,0);
	}
	
	public Shape(PApplet ap, Scene sc, String path){
		scene = sc;
		applet = ap;
	    //Load the figures and set the color or texture
		PShape figure;
		if(path.equals("") || debug) 
			figure = getDebugFigure();
		else{
			figure = ap.loadShape(path);
		    figure = Utilities.cloneShape(ap, figure, ap.color(255,0,0));
			
		}
	    PShape deformed_figure = Utilities.cloneShape(ap, figure, ap.color(100,0,130));
	    figure.setFill(ap.color(255,0,0));
	    deformed_figure.setFill(ap.color(100,0,130));

	    if(path.equals("") || debug){
	    	figure.setStroke(true);
	    	deformed_figure.setStroke(true);
	    }
	    
	    //keep the references of the vertices of the shapes in a List
		for(int j = 0; j < figure.getChildCount(); j++){
		  PShape aux = figure.getChild(j);
		  PShape aux_def = deformed_figure.getChild(j);
		  for(int i = 0; i < aux.getVertexCount(); i++){
		    vertices.add(aux.getVertex(i));  
		    deformed_vertices.add(aux_def.getVertex(i));  
		  }
		}
  	    //associate the shape with the original shape frame
	    original_fig = new InteractiveFrame(scene, figure);
	    //initial deformed shape without modifications
		deformed_fig = new InteractiveFrame(scene, deformed_figure);
	    calculateBoundingBox();  
		float radius = Math.max(Math.abs(r_bounds[1].x() - r_bounds[0].x()), Math.abs(r_bounds[1].y() - r_bounds[0].y()));
		//setup the initial position of the interactive frames
		original_fig.translate(-radius*.6f/2.f,0,0);
		deformed_fig.translate(radius*.6f/2.f,0,0);
	    //Create a Deformation solvers
	    leastSolver = new LeastSolver();
	    laplacianDeformation = new LaplacianDeformation();
	    laplacianDeformation.setup(deformed_fig.shape());
	    laplacianDeformation.calculateLaplacian();
	    //set default solver to be used
	    solver = Solver.MLS;
	    original_fig.disableHighlighting();
	    deformed_fig.disableHighlighting();
	}
	
	public void draw(){
	    original_fig.draw();
	    deformed_fig.draw();
	    if(show_bounding_box){
	    	Utilities.drawCube(original_fig);
	    }
	    drawControlPoints();
	    
	}

	public void drawControlPoints(){
		for(ControlPoint cp : control_points) cp.draw();
	}
	
	public int getControlPoint(float x, float y){
		for(int i = 0; i < control_points.size(); i++){
			if(control_points.get(i).checkPickedSection(x, y) != -1) return i;
		}
		return -1;
	}
	
	public void executeDeformationAction(){
		switch(solver){
			case MLS:
  		        leastSolver.updateControlPoints(control_points, vertices);
				deformed_vertices = leastSolver.calculateNewImage(vertices,control_points);
			    Utilities.setVertices(deformed_fig.shape(), deformed_vertices);
			    break;
			case LAPLACIAN:
				laplacianDeformation.addAnchors(control_points, true);
				ArrayList<PVector> new_positions = laplacianDeformation.solveLaplacian();
				for(LaplacianDeformation.Vertex vertex : laplacianDeformation.getVertices().values()){
					for(int[] idxs : vertex.getIdx_shape()){
						PShape face = deformed_fig.shape().getChild(idxs[0]);
						face.setVertex(idxs[1], new_positions.get(vertex.getIdx()));
					}
				}
				break;
		}
	}

	public void addPoint(PVector v){
		  if(control_points.size() == 0){
			ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(v.x,v.y,v.z), control_points);
		    control_points.add(cp);
		    cp.setAction(this, "executeDeformationAction");
		    return;
		  }
		  Vec vec = new Vec(v.x,v.y,v.z);
		  float min_dist_l = 9999;
		  int best_pos = control_points.size();
		  for(int i = 0; i < control_points.size(); i++){
		      Vec left = control_points.get(i).position();
		      if(vec.distance(left) < min_dist_l){
		        min_dist_l = vec.distance(left);
		        best_pos = i+1;        
		      }      
		  }
		  ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(v.x,v.y,v.z), control_points);
		  control_points.add(best_pos, cp);
		  cp.setAction(this, "executeDeformationAction");
		  executeDeformationAction();
	}

	//find the point in the bounding box using pixel under point
	public Vec getIntersectionPoint(float x, float y){
	  Vec pup = scene.pointUnderPixel(new Point(x, y));
	  return pup == null ? null : original_fig.coordinatesOf(pup);
	}
	
	//----------------------------------------
	//----------------------------------------
	//----------------------------------------
	/*
	 * STABLISH SOME PREDEFINED DEFORMATIONS
	*/
	//----------------------------------------
	//----------------------------------------
	//----------------------------------------

	public static float mod_factor = 8.f;
	public static int step_per_point = 10;
	//SOME PREDEFINED DEFORMATIONS
	public void addControlPointsAuto(boolean rand){
	  //clear
	  while(!control_points.isEmpty()){
		  scene.inputHandler().removeGrabber(control_points.get(0));
		  control_points.remove(0);
	  }
	  for(int i = 0; i < vertices.size(); i+= step_per_point){
	    //get coordinates in local frame
	    if(!rand){
	      ControlPoint cp = new ControlPoint(scene, 
	    		  original_fig, new Vec(vertices.get(i).x,
	    				  vertices.get(i).y,vertices.get(i).z),
	    		  		  control_points);
	      control_points.add(cp);
	      cp.setAction(this, "executeDeformationAction");
	    }else{
	      PVector v = vertices.get(i);
	      PVector new_v = new PVector(v.x - r_center.x(), v.y - r_center.y(),
	    		  v.z - r_center.z());                                          
	      new_v.mult((float)Math.random() + 1.f);
	      new_v.add(v);
	      ControlPoint cp = new ControlPoint(scene, 
	    		  original_fig, new Vec(new_v.x,new_v.y,new_v.z), control_points);
	      control_points.add(cp);
	      float r_out_x = (int)(Math.random()*100) % 2 == 0 ? (float)(Math.random()*(0.5) + 1) : (float)(-1*(Math.random()*(0.5) + 1));
	      float r_out_y = (int)(Math.random()*100) % 2 == 0 ? (float)(Math.random()*(0.5) + 1) : (float)(-1*(Math.random()*(0.5) + 1));
	      float r_out_z = (int)(Math.random()*100) % 2 == 0 ? (float)(Math.random()*(0.5) + 1) : (float)(-1*(Math.random()*(0.5) + 1));
	      cp.setB( new Vec(new_v.x*r_out_x, new_v.y*r_out_y, new_v.z*r_out_z));
	      cp.setAction(this, "executeDeformationAction");
	    }
	  }  
	}

	public void scaleX(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }  
	  float r_width = r_bounds[1].x() - r_bounds[0].x();
	  if(r_width < 0) r_width = -1*r_width;
	  //two parallel faces surrounding the width of the shape
	  Vec[] f1 = new Vec[4];
	  //top left
	  f1[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f1[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z());
	  //top right
	  f1[2] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z());
	  //bottom right
	  f1[3] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f1_c  = new Vec(r_bounds[0].x(),(r_bounds[0].y()+r_bounds[1].y())/2.f, (r_bounds[0].z()+r_bounds[1].z())/2.f);  

	  Vec[] f2 = new Vec[4];
	  //top left
	  f2[0] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f2[1] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
	  //top right
	  f2[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
	  //bottom right
	  f2[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f2_c  = new Vec(r_bounds[1].x(),(r_bounds[0].y()+r_bounds[1].y())/2.f, (r_bounds[0].z()+r_bounds[1].z())/2.f);  
	  Random r = new Random();
	  Vec movement = new Vec((r_width/8)*(float)r.nextGaussian(), 0,0);
	  Vec new_f1_c = Vec.add(new Vec(0,0,0), movement);
	  Vec new_f2_c = Vec.subtract(new Vec(0,0,0), movement);
	  
	  for(int i = 0; i < 4; i++){
	    Vec new_f1 = Vec.add(new Vec(0,0,0), movement);
	    Vec new_f2 = Vec.subtract(new Vec(0,0,0), movement);
	    ControlPoint cp = new ControlPoint(
	    		scene, original_fig, f1[i], control_points);
	    cp.setB( new_f1);
	    control_points.add(cp);
	    cp.setAction(this, "executeDeformationAction");
	    
	    ControlPoint cp2 = new ControlPoint(
	    		scene, original_fig, f2[i], control_points);
	    cp2.setB( new_f2);	    
	    control_points.add(cp2);
	    cp2.setAction(this, "executeDeformationAction");
	    
	  }
	  ControlPoint cp = new ControlPoint(
			  scene, original_fig, f1_c, control_points);
	  control_points.add(cp);
	  cp.setB( new_f1_c);
      cp.setAction(this, "executeDeformationAction");
	  ControlPoint cp2 = new ControlPoint(
			  scene, original_fig, f2_c, control_points);
	  control_points.add(cp2);
	  cp2.setB( new_f2_c);
      cp2.setAction(this, "executeDeformationAction");
	}

	public void scaleY(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }  
	  float r_width = r_bounds[1].y() - r_bounds[0].y();
	  if(r_width < 0) r_width = -1*r_width;
	  //two parallel faces surrounding the height of the shape
	  Vec[] f1 = new Vec[4];
	  //top left
	  f1[0] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z()); 
	  //bottom left
	  f1[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
	  //top right
	  f1[2] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
	  //bottom right
	  f1[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f1_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, r_bounds[0].y(), (r_bounds[0].z()+r_bounds[1].z())/2.f);  
	  Vec[] f2 = new Vec[4];
	  //top left
	  f2[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f2[1] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z());
	  //top right
	  f2[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z());
	  //bottom right
	  f2[3] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
	  Vec f2_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, r_bounds[1].y(), (r_bounds[0].z()+r_bounds[1].z())/2.f);  
	  Random random = new Random();
	  Vec movement = new Vec(0, (r_width/8)*(float)random.nextGaussian(), 0);
	  Vec new_f1_c = Vec.add(new Vec(0,0,0), movement);
	  Vec new_f2_c = Vec.subtract(new Vec(0,0,0), movement);
	  
	  for(int i = 0; i < 4; i++){
	    Vec new_f1 = Vec.add(new Vec(0,0,0), movement);
	    Vec new_f2 = Vec.subtract(new Vec(0,0,0), movement);
	    ControlPoint cp = new ControlPoint(scene, original_fig, f1[i], control_points);
	    control_points.add(cp);
	    cp.setB( new_f1);
	    cp.setAction(this, "executeDeformationAction");
	    ControlPoint cp2 = new ControlPoint(scene, original_fig, f2[i], control_points);
	    control_points.add(cp2);
	    cp2.setB( new_f2);
	    cp2.setAction(this, "executeDeformationAction");
	  }
	  ControlPoint cp = new ControlPoint(scene, original_fig, f1_c, control_points);
	  control_points.add(cp);
	  cp.setB( new_f1_c);
	  cp.setAction(this, "executeDeformationAction");
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, f2_c, control_points);
	  control_points.add(cp2);
	  cp2.setB( new_f2_c);
	  cp2.setAction(this, "executeDeformationAction");
	}

	public void scaleZ(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }  
	  float r_width = r_bounds[1].z() - r_bounds[0].z();
	  if(r_width < 0) r_width = -1*r_width;
	  //two parallel faces surrounding the height of the shape
	  Vec[] f1 = new Vec[4];
	  //top left
	  f1[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z()); 
	  //bottom left
	  f1[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
	  //top right
	  f1[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
	  //bottom right
	  f1[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f1_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, (r_bounds[0].y()+r_bounds[1].y())/2.f, r_bounds[0].z());  
	  Vec[] f2 = new Vec[4];
	  //top left
	  f2[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f2[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z());
	  //top right
	  f2[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z());
	  //bottom right
	  f2[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
	  Vec f2_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, (r_bounds[0].y()+r_bounds[1].y())/2.f, r_bounds[1].z());  
	  Random random = new Random();
	  Vec movement = new Vec(0,0, (r_width/8)*(float)random.nextGaussian());
	  Vec new_f1_c = Vec.add(new Vec(0,0,0), movement);
	  Vec new_f2_c = Vec.subtract(new Vec(0,0,0), movement);
	  
	  for(int i = 0; i < 4; i++){
	    Vec new_f1 = Vec.add(new Vec(0,0,0), movement);
	    Vec new_f2 = Vec.subtract(new Vec(0,0,0), movement);
	    ControlPoint cp = new ControlPoint(scene, original_fig, f1[i], control_points);
	    control_points.add(cp);
	    cp.setB( new_f1);
	    cp.setAction(this, "executeDeformationAction");
	    ControlPoint cp2 = new ControlPoint(scene, original_fig, f2[i], control_points);
	    control_points.add(cp2);
	    cp2.setB( new_f2);
	    cp2.setAction(this, "executeDeformationAction");
	  }
	  ControlPoint cp = new ControlPoint(scene, original_fig, f1_c, control_points);
	  control_points.add(cp);
	  cp.setB( new_f1_c);
	  cp.setAction(this, "executeDeformationAction");
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, f2_c, control_points);
	  control_points.add(cp2);
	  cp2.setB( new_f2_c);
	  cp2.setAction(this, "executeDeformationAction");
	}

	public void applyHorizontalZXSpline(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float x_pos = min_x + i*(r_w/(quantity-1));
	      float y_mode = min_y; 
	      float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, y_pos, (max_z + min_z)/2.f));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float y_mode = min_y; 
	  float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, y_pos, point.z), control_points);
	    control_points.add(cp);
	    cp.setB( new Vec(0, spline_control.get(c).y - y_pos, 0));
	    cp.setAction(this, "executeDeformationAction");
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_y_mode = min_y + r_h;
	  y_pos = -(y_pos - y_mode) + inv_y_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, y_pos, point.z), control_points);
	    control_points.add(cp);
	    point.y = -(point.y - y_mode) + inv_y_mode;
	    cp.setB( new Vec(0, point.y - y_pos, 0));
	    cp.setAction(this, "executeDeformationAction");
	  }
	  //add 2 anchor points
	  ControlPoint cp = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, min_z), control_points);
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, max_z), control_points);  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	  cp.setAction(this, "executeDeformationAction");
	  cp2.setAction(this, "executeDeformationAction");
	}

	public void applyVerticalZXSpline(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float z_pos = min_z + i*(r_l/(quantity-1));
	      float y_mode = min_y; 
	      float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor); 
	      spline_control.add(new PVector((max_x + min_x)/2.f, y_pos, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float y_mode = min_y; 
	  float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, y_pos, point.z), control_points);
	    control_points.add(cp);
	    cp.setB( new Vec(0, spline_control.get(c).y - y_pos, 0));
	    cp.setAction(this, "executeDeformationAction");
	    c++;
	  }

	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_y_mode = min_y + r_h;
	  y_pos = -(y_pos - y_mode) + inv_y_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, y_pos, point.z), control_points);
	    control_points.add(cp);
	    point.y = -(point.y - y_mode) + inv_y_mode;
	    cp.setB( new Vec(0, point.y - y_pos, 0));
	    cp.setAction(this, "executeDeformationAction");
	  }
	  //add 2 anchor points
	  ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(min_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f), control_points);
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, new Vec(max_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f), control_points);  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	  cp.setAction(this, "executeDeformationAction");
	  cp2.setAction(this, "executeDeformationAction");
	}



	public void applyHorizontalYZSpline(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float z_pos = min_z + i*(r_l/(quantity-1));
	      float x_mode = min_x; 
	      float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, (max_y + min_y)/2.f, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float x_mode = min_x; 
	  float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(x_pos, point.y, point.z), control_points);
	    control_points.add(cp);
	    cp.setB( new Vec(spline_control.get(c).x - x_pos, 0, 0));
	    cp.setAction(this, "executeDeformationAction");
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_x_mode = min_x + r_w;
	  x_pos = -(x_pos - x_mode) + inv_x_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(x_pos, point.y, point.z), control_points);
	    control_points.add(cp);
	    point.x = -(point.x - x_mode) + inv_x_mode;
	    cp.setB( new Vec(point.x - x_pos, 0,0));
	    cp.setAction(this, "executeDeformationAction");
	  }
	  //add 2 anchor points
	  ControlPoint cp = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, min_y, (min_z + max_z)/2.f), control_points);
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, max_y, (min_z + max_z)/2.f), control_points);  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	  cp.setAction(this, "executeDeformationAction");
	  cp2.setAction(this, "executeDeformationAction");
	}

	public void applyVerticalYZSpline(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float y_pos = min_y + i*(r_h/(quantity-1));
	      float x_mode = min_x; 
	      float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, y_pos, (max_z + min_z)/2.f));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float x_mode = min_x; 
	  float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(x_pos, point.y, point.z), control_points);
	    control_points.add(cp);
	    cp.setB( new Vec(spline_control.get(c).x - x_pos, 0,0));
	    cp.setAction(this, "executeDeformationAction");
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_x_mode = min_x + r_w;
	  x_pos = -(x_pos - x_mode) + inv_x_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(x_pos, point.y, point.z), control_points);
	    control_points.add(cp);
	    point.x = -(point.x - x_mode) + inv_x_mode;
	    cp.setB( new Vec(point.x - x_pos, 0,0));
	    cp.setAction(this, "executeDeformationAction");
	  }
	  //add 2 anchor points
	  ControlPoint cp = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, min_z), control_points);
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, max_z), control_points);  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	  cp.setAction(this, "executeDeformationAction");
	  cp2.setAction(this, "executeDeformationAction");
	}

	public void applyVerticalXYSpline(boolean clear){
	  //clear
  	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float y_pos = min_y + i*(r_h/(quantity-1));
	      float z_mode = min_z; 
	      float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor); 
	      spline_control.add(new PVector((max_x + min_x)/2.f, y_pos, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float z_mode = min_z; 
	  float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, point.y, z_pos), control_points);
	    control_points.add(cp);
	    cp.setB( new Vec(0, 0, spline_control.get(c).z - z_pos));
	    cp.setAction(this, "executeDeformationAction");
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_z_mode = min_z + r_l;
	  z_pos = -(z_pos - z_mode) + inv_z_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, point.y, z_pos), control_points);
	    control_points.add(cp);
	    point.z = -(point.z - z_mode) + inv_z_mode;
	    cp.setB( new Vec(0, 0, point.z - z_pos));
	    cp.setAction(this, "executeDeformationAction");
	  }
	  //add 2 anchor points
	  ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(min_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f), control_points);
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, new Vec(max_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f), control_points);  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	  cp.setAction(this, "executeDeformationAction");
	  cp2.setAction(this, "executeDeformationAction");
	}

	public void applyHorizontalXYSpline(boolean clear){
	  //clear
	  if(clear){
		  while(!control_points.isEmpty()){
			  scene.inputHandler().removeGrabber(control_points.get(0));
			  control_points.remove(0);
		  }
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float x_pos = min_x + i*(r_w/(quantity-1));
	      float z_mode = min_z; 
	      float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, (max_y + min_y)/2.f, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float z_mode = min_z; 
	  float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, point.y, z_pos), control_points);
	    control_points.add(cp);
	    cp.setB( new Vec(0,0, spline_control.get(c).z - z_pos));
	    cp.setAction(this, "executeDeformationAction");
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_z_mode = min_z + r_l;
	  z_pos = -(z_pos - z_mode) + inv_z_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    ControlPoint cp = new ControlPoint(scene, original_fig, new Vec(point.x, point.y, z_pos), control_points);
	    control_points.add(cp);
	    point.z = -(point.z - z_mode) + inv_z_mode;
	    cp.setB( new Vec(0,0, point.z - z_pos));
	    cp.setAction(this, "executeDeformationAction");
	  }
	  //add 2 anchor points
	  ControlPoint cp = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, min_y, (min_z + max_z)/2.f), control_points);
	  ControlPoint cp2 = new ControlPoint(scene, original_fig, new Vec((min_x + max_x)/2.f, max_y, (min_z + max_z)/2.f), control_points);  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	  cp.setAction(this, "executeDeformationAction");
	  cp2.setAction(this, "executeDeformationAction");
	}
	
	
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//GETTERS AND SETTERS
	public ArrayList<ControlPoint> getControl_points() {
		return control_points;
	}
	public void setControl_points(ArrayList<ControlPoint> control_points) {
		this.control_points = control_points;
	}
	public InteractiveFrame getOriginal_fig() {
		return original_fig;
	}
	public void setOriginal_fig(InteractiveFrame original_fig) {
		this.original_fig = original_fig;
	}
	public InteractiveFrame getDeformed_fig() {
		return deformed_fig;
	}
	public void setDeformed_fig(InteractiveFrame deformed_fig) {
		this.deformed_fig = deformed_fig;
	}
	public ArrayList<PVector> getVertices() {
		return vertices;
	}
	public void setVertices(ArrayList<PVector> vertices) {
		this.vertices = vertices;
	}
	public ArrayList<PVector> getDeformed_vertices() {
		return deformed_vertices;
	}
	public void setDeformed_vertices(ArrayList<PVector> deformed_vertices) {
		this.deformed_vertices = deformed_vertices;
	}
	public Vec getR_center() {
		return r_center;
	}
	public void setR_center(Vec r_center) {
		this.r_center = r_center;
	}
	public Vec[] getR_bounds() {
		return r_bounds;
	}
	public void setR_bounds(Vec[] r_bounds) {
		this.r_bounds = r_bounds;
	}

	public PShape getDebugFigure(){
		  return testShape(100, 100, 100, 10);
	}
	public Scene getScene() {
		return scene;
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

	public Solver getSolver() {
		return solver;
	}

	public void setSolver(Solver solver) {
		this.solver = solver;
	}

	public LaplacianDeformation getLaplacianDeformation() {
		return laplacianDeformation;
	}

	public void setLaplacianDeformation(LaplacianDeformation laplacianDeformation) {
		this.laplacianDeformation = laplacianDeformation;
	}

	public LeastSolver getLeastSolver() {
		return leastSolver;
	}

	public void setLeastSolver(LeastSolver leastSolver) {
		this.leastSolver = leastSolver;
	}

	public boolean isShowingBoundingBox() {
		return show_bounding_box;
	}

	public void setShowBoundingBox(boolean bounding) {
		this.show_bounding_box = bounding;
	}

	public PApplet getApplet() {
		return applet;
	}

	public void setApplet(PApplet ap) {
		this.applet = ap;
	}
	
	
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	//DEBUG METHODS
	public PShape testShape(int dx, int dy, int dz, int num){
		PShape p = applet.createShape(PConstants.GROUP);
		float[] step = {dx/(num*1f),dy/(num*1f),dz/(num*1f)}; 
		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f1 = applet.createShape();
				f1.beginShape(PConstants.POLYGON);
				f1.vertex(i*step[0], j*step[1], 0);
				f1.vertex((i+1)*step[0], j*step[1], 0);
				f1.vertex((i+1)*step[0], (j+1)*step[1], 0);
				f1.vertex(i*step[0], (j+1)*step[1], 0);
				f1.endShape(PConstants.CLOSE);
				p.addChild(f1);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f2 = applet.createShape();
				f2.beginShape(PConstants.POLYGON);
				f2.vertex(i*step[0], j*step[1], dz);
				f2.vertex((i+1)*step[0], j*step[1], dz);
				f2.vertex((i+1)*step[0], (j+1)*step[1], dz);
				f2.vertex(i*step[0], (j+1)*step[1], dz);
				f2.endShape();
				p.addChild(f2);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f3 = applet.createShape();
				f3.beginShape(PConstants.POLYGON);
				f3.vertex(0, i*step[1], j*step[2]);
				f3.vertex(0, (i+1)*step[1], j*step[2]);
				f3.vertex(0, (i+1)*step[1], (j+1)*step[2]);
				f3.vertex(0, i*step[1], (j+1)*step[2]);
				f3.endShape(PConstants.CLOSE);
				p.addChild(f3);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f4 = applet.createShape();
				f4.beginShape(PConstants.POLYGON);
				f4.vertex(dx, i*step[1], j*step[2]);
				f4.vertex(dx, (i+1)*step[1], j*step[2]);
				f4.vertex(dx, (i+1)*step[1], (j+1)*step[2]);
				f4.vertex(dx, i*step[1], (j+1)*step[2]);
				f4.endShape(PConstants.CLOSE);
				p.addChild(f4);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f5 = applet.createShape();
				f5.beginShape(PConstants.POLYGON);
				f5.vertex(i*step[0], 0, j*step[2]);
				f5.vertex(i*step[0], 0, (j+1)*step[2]);
				f5.vertex((i+1)*step[0], 0, (j+1)*step[2]);
				f5.vertex((i+1)*step[0], 0, j*step[2]);
				f5.endShape(PConstants.CLOSE);
				p.addChild(f5);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f6 = applet.createShape();
				f6.beginShape(PConstants.POLYGON);
				f6.vertex(i*step[0], dy, j*step[2]);
				f6.vertex(i*step[0], dy, (j+1)*step[2]);
				f6.vertex((i+1)*step[0], dy, (j+1)*step[2]);
				f6.vertex((i+1)*step[0], dy, j*step[2]);
				f6.endShape(PConstants.CLOSE);
				p.addChild(f6);
			}
		}
		return p;
	}
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------	
}
