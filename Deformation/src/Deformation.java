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
	public static ArrayList<ControlPoint> control_points = new ArrayList<ControlPoint>();
	public static PShape figure;
	public static PShape deformed_figure;
	public static Scene main_scene;
	public static InteractiveFrame original_fig;
	public static InteractiveFrame deformed_fig;
	public static Utilities.SelectionArea selection;
	public static int all_width = 800;
	public static int all_height = 600;

	public static ArrayList<PVector> vertices = new ArrayList<PVector>();
	public static ArrayList<PVector> selected_vertices = new ArrayList<PVector>();
	public static ArrayList<Integer> selected_vertices_i = new ArrayList<Integer>();
	public static ArrayList<PVector> deformed_vertices = new ArrayList<PVector>();
	public static Vec r_center;
	public static Vec[] r_bounds;

	//GUI VARIABLES
	public static int step_per_point = 10;
	public static int num_t = 1;	
	public static int drag_mode = -1;
	//needed to know the change of position 
	public static Vec initial_drag_pos = null;
	public static boolean clear_points = true;
	public static boolean change_points = true;
	public static boolean selection_active = false;
	public static boolean selection_mode = false;
	public static boolean use_selected = false;
	//KEYBOARD HANDLING EVENTS
	public static boolean add_mode = true;
	public static boolean bounding_rect = true;
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
	  r_bounds = Utilities.getCube(figure);
	  r_center = new Vec((r_bounds[0].x() + r_bounds[1].x())/2.f, (r_bounds[0].y() + r_bounds[1].y())/2.f, (r_bounds[0].z() + r_bounds[1].z())/2.f);
	  original_fig.rotate(0,0,PI,0);
	  deformed_fig.rotate(0,0,PI,0);
	  
	  //set radius
	  float radius = Math.max(Math.abs(r_bounds[1].x() - r_bounds[0].x()), Math.abs(r_bounds[1].y() - r_bounds[0].y()));
	  radius = Math.max(radius, Math.abs(r_bounds[1].z() - r_bounds[0].z()));
	  main_scene.setBoundingBox(original_fig.inverseCoordinatesOf(new Vec(-radius*.6f,-radius*.6f,-radius*.6f)), 
			  original_fig.inverseCoordinatesOf(new Vec(radius*.6f,radius*.6f,radius*.6f)));
	  main_scene.showAll();
	  main_scene.camera().setType(Camera.Type.ORTHOGRAPHIC);	  
	  //setup the initial position of the interactive frames
	  original_fig.translate(-radius*.6f/2.f,0,0);
	  deformed_fig.translate(radius*.6f/2.f,0,0);
	
	}
	
	public void setup() {
	  textureWrap(REPEAT);
	  //Load the figures and set the color or texture
	  figure = loadShape("TropicalFish15.obj");
	  deformed_figure = Utilities.cloneShape(this, figure, color(100,0,130));
	  if(debug) debugCreateFigures();
	  noSmooth();
	  //keep the references of the vertices of the shapes in a List
	  for(int j = 0; j < figure.getChildCount(); j++){
	    PShape aux = figure.getChild(j);
	    PShape aux_def = deformed_figure.getChild(j);
	    for(int i = 0; i < aux.getVertexCount(); i++){
	      vertices.add(aux.getVertex(i));  
	      deformed_vertices.add(aux_def.getVertex(i));  
	    }
	  }
	  figure.setFill(color(255,0,0));
	  deformed_figure.setFill(color(100,0,130));
	  
   	  main_scene = new Scene(this);
	  main_scene.setDottedGrid(false);
	  
	  //associate the shape with the original shape frame
	  original_fig = new InteractiveFrame(main_scene, figure);
	  //initial deformed shape without modifications
	  deformed_fig = new InteractiveFrame(main_scene, deformed_figure);
	  //setup the scene
	  setupScene();
	  
	  println(figure.getChildCount());
	  selection = new Utilities.SelectionArea(this); 
	  
	  //setup laplacian
	  LaplacianDeformation.setup(deformed_fig.shape());
	  LaplacianDeformation.calculateLaplacian();
	}

	public void draw() { 
	  ambientLight(80, 80, 80);
	  directionalLight(255, 255, 255, 0, 0, -1);
	  lightFalloff(1, 0, 0);
	  lightSpecular(0, 0, 0);
	  background(0);
	  original_fig.draw();
	  deformed_fig.draw();
	  if(bounding_rect) Utilities.drawCube(original_fig);
      drawControlPoints();
	  if(selection_active){
	    main_scene.beginScreenDrawing();
	    selection.draw();    
	    main_scene.endScreenDrawing();
	  }
	  if(selected_vertices.size() > 0){
	    pushMatrix();
	    pushStyle();
	    stroke(color(200,200,200));
	    strokeWeight(7);
	      original_fig.applyTransformation();
	      for(PVector p : selected_vertices){
	        point(p.x,p.y,p.z);
	      }
	    popStyle();
	    popMatrix();
	  }
	  
	}

	void handleAgents(){
	  main_scene.enableMotionAgent();
	  main_scene.enableKeyboardAgent();
	  if(drag_mode != -1) {
	    main_scene.disableMotionAgent();
	    main_scene.disableKeyboardAgent();
	  }
	}
	
	//----------------------------------------------------------------
	//GUI METHODS-----------------------------------------------------
	//----------------------------------------------------------------
	//----------------------------------------------------------------
	public static void morphTransformationAction(){
	  if(!use_selected){
	    deformed_vertices = LeastSolver.calculateNewImage(vertices,control_points);
	    Utilities.setVertices(deformed_figure, deformed_vertices);
	  }
	  else{
	    deformed_vertices = LeastSolver.calculateNewImage(selected_vertices,control_points);
	    Utilities.setVertices(deformed_figure, deformed_vertices, selected_vertices_i);
	  }    
	}

	void addPoint(PVector v){
	  if(control_points.size() == 0){
		ControlPoint cp = new ControlPoint(main_scene, original_fig, new Vec(v.x,v.y,v.z), control_points);
	    control_points.add(cp);
	    setMSLDeformationAction(cp);
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
	  ControlPoint cp = new ControlPoint(main_scene, original_fig, new Vec(v.x,v.y,v.z), control_points);
	  control_points.add(best_pos, cp);
	  setMSLDeformationAction(cp);
	  //update A
	  LeastSolver.updateControlPoints();
	}

	public void mousePressed( ){
	  if(selection_mode){
	    selection_active = true;
	    selection.init =  new Vec(mouseX, mouseY);
	    selection.end = new Vec(mouseX, mouseY);  
	    return;  
	  }
	  if(!add_mode) return;
	  //use point under pixel to locate the point in an aproppiate deph position
	  Vec point_shape = getIntersectionPoint(mouseX, mouseY);
	  if(point_shape == null) return;//if there's not a pixel don't do anything
	  //proyections in world frame to make easier comparisons between points
	  Vec point_world = original_fig.inverseCoordinatesOf(point_shape);
	  if(mouseButton == LEFT){
	    //check if point exist
	    int pos = getControlPoint(mouseX, mouseY);
	    if(pos == -1){
	      //if not add to control points
	      addPoint(new PVector(point_shape.x(), point_shape.y(), point_shape.z()));
	      morphTransformationAction();
	    }else{
	      //needed vars to manage the drag and drop
	      drag_mode = pos;
	      initial_drag_pos = main_scene.eye().unprojectedCoordinatesOf(new Vec(mouseX, mouseY));
	    }
	  }
	}

	public void mouseReleased(){
	  selection_active = false;
	  drag_mode = -1;
	  initial_drag_pos = null;
	}

	public void mouseDragged(){
	  if(selection_active){
	    selection.end = new Vec(mouseX, mouseY);  
	  }
	}

	int getControlPoint(float x, float y){
	  for(int i = 0; i < control_points.size(); i++){
	    if(control_points.get(i).checkPickedSection(x, y) != -1) return i;
	  }
	  return -1;
	}

	void drawControlPoints(){
	  for(ControlPoint cp : control_points) cp.draw();
	}

	//find the point in the bounding box using pixel under point
	public Vec getIntersectionPoint(float x, float y){
	  Vec pup = main_scene.pointUnderPixel(new Point(x, y));
	  return pup == null ? null : original_fig.coordinatesOf(pup);
	}

	//KEYBOARD HANDLING EVENTS
	public void keyPressed(){
	  if (key == 'c'){
	    selection_mode = add_mode;
	    add_mode = !add_mode;
	    if(selection_mode) main_scene.disableMotionAgent();
	    else main_scene.enableMotionAgent();
	  }
	  if (key == 'x' || key == 'X'){
	    add_mode = selection_mode;
	    selection_mode = !selection_mode; 
	    if(selection_mode) main_scene.disableMotionAgent();
	    else{ 
	      main_scene.enableMotionAgent();
	      selected_vertices = selection.getVertices(vertices);
	      println("llega con size : " + selected_vertices.size());
	    }
	  }
	  
	  if(key == 'b' || key == 'B'){
	    use_selected = !use_selected;
	    if(selected_vertices.size() == 0) use_selected = false;
	  }

	  if(key == 'l' || key == 'L'){
		  LaplacianDeformation.addAnchors(control_points, true);
		  ArrayList<PVector> new_positions = LaplacianDeformation.solveLaplacian();
		  for(LaplacianDeformation.Vertex vertex : LaplacianDeformation.vertices.values()){
			  for(int[] idxs : vertex.idx_shape){
				  PShape face = deformed_fig.shape().getChild(idxs[0]);
				  face.setVertex(idxs[1], new_positions.get(vertex.idx));
			  }
		  }
	  }
	  
	  if (key == 'r'){
	    bounding_rect = !bounding_rect;
	  }
	  //predefined transformation
	  //auto points 
	  if(key == 'v' || key == 'V'){
	    step_per_point = vertices.size()/((int)random(15,30) + 1);
	    LeastSolver.addControlPointsAuto(true);
	    LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //scale x
	  if(key=='1'){
		LeastSolver.scaleX(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //scale y  
	  if(key=='2'){
		LeastSolver.scaleY(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //scale z  
	  if(key=='3'){
		LeastSolver.scaleZ(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //spline XZ
	  //horizontal
	  if(key=='4'){
		LeastSolver.applyHorizontalZXSpline(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //vertical
	  if(key=='5'){
		LeastSolver.applyVerticalZXSpline(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }  
	  //spline YZ
	  //spline horizontal 
	  if(key=='6'){
		LeastSolver.applyHorizontalYZSpline(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //spline vertical 
	  if(key=='7'){
		LeastSolver.applyVerticalYZSpline(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //spline XY
	  //spline horizontal 
	  if(key=='8'){
		LeastSolver.applyHorizontalXYSpline(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  //spline vertical 
	  if(key=='9'){
		LeastSolver.applyVerticalXYSpline(clear_points);
		LeastSolver.updateControlPoints();
	    morphTransformationAction();
	  }
	  
	  if(key=='0'){
		LeastSolver.combination();
		deformed_fig.setShape(deformed_figure);
	  }
	}
	
	public static void setLaplacianDeformationAction(ControlPoint point){
		point.setAction(Deformation.class, "executeAction");
	}

	public static void setMSLDeformationAction(ControlPoint point){
		point.setAction(Deformation.class, "executeAction");		
	}

	public static void executeAction(){
	    LeastSolver.updateControlPoints();
	    Deformation.morphTransformationAction();
	}
	
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	//DEBUG METHODS
	public void debugCreateFigures(){
		  figure = testShape(100, 100, 100, 10);
		  figure.fill(color(0,255,0));
		  figure.stroke(color(0,0,255));
		  deformed_figure = testShape(100,100,100, 10);
		  deformed_figure.fill(color(0,255,0));
		  deformed_figure.stroke(color(0,0,255));
	}
	public PShape testShape(int dx, int dy, int dz, int num){
		PShape p = createShape(GROUP);
		float[] step = {dx/(num*1f),dy/(num*1f),dz/(num*1f)}; 
		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f1 = createShape();
				f1.beginShape(POLYGON);
				f1.vertex(i*step[0], j*step[1], 0);
				f1.vertex((i+1)*step[0], j*step[1], 0);
				f1.vertex((i+1)*step[0], (j+1)*step[1], 0);
				f1.vertex(i*step[0], (j+1)*step[1], 0);
				f1.endShape(CLOSE);
				p.addChild(f1);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f2 = createShape();
				f2.beginShape(POLYGON);
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
				PShape f3 = createShape();
				f3.beginShape(POLYGON);
				f3.vertex(0, i*step[1], j*step[2]);
				f3.vertex(0, (i+1)*step[1], j*step[2]);
				f3.vertex(0, (i+1)*step[1], (j+1)*step[2]);
				f3.vertex(0, i*step[1], (j+1)*step[2]);
				f3.endShape(CLOSE);
				p.addChild(f3);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f4 = createShape();
				f4.beginShape(POLYGON);
				f4.vertex(dx, i*step[1], j*step[2]);
				f4.vertex(dx, (i+1)*step[1], j*step[2]);
				f4.vertex(dx, (i+1)*step[1], (j+1)*step[2]);
				f4.vertex(dx, i*step[1], (j+1)*step[2]);
				f4.endShape(CLOSE);
				p.addChild(f4);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f5 = createShape();
				f5.beginShape(POLYGON);
				f5.vertex(i*step[0], 0, j*step[2]);
				f5.vertex(i*step[0], 0, (j+1)*step[2]);
				f5.vertex((i+1)*step[0], 0, (j+1)*step[2]);
				f5.vertex((i+1)*step[0], 0, j*step[2]);
				f5.endShape(CLOSE);
				p.addChild(f5);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f6 = createShape();
				f6.beginShape(POLYGON);
				f6.vertex(i*step[0], dy, j*step[2]);
				f6.vertex(i*step[0], dy, (j+1)*step[2]);
				f6.vertex((i+1)*step[0], dy, (j+1)*step[2]);
				f6.vertex((i+1)*step[0], dy, j*step[2]);
				f6.endShape(CLOSE);
				p.addChild(f6);
			}
		}
		return p;
	}
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	
	
}