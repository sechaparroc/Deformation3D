import java.util.*;

import papaya.*;
import processing.core.*;
import remixlab.bias.core.*;
import remixlab.bias.event.*;
import remixlab.proscene.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.core.*;

public class Deformation extends PApplet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//ArrayList<ControlPoint> control_points = new ArrayList<ControlPoint>(); 
	public static ArrayList<Utilities.ControlPoint> control_points = new ArrayList<Utilities.ControlPoint>();
	public static PShape figure;
	public static PShape deformed_figure;
	public static Scene main_scene;
	public static InteractiveFrame original_fig;
	public static InteractiveFrame deformed_fig;
	public static Utilities.SelectionArea selection;
	public static int all_width = 640;
	public static int all_height = 360;

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
		size(640, 360, PConstants.P3D);		
	}
	
	public void setup() {
	  textureWrap(REPEAT);
	  figure = loadShape("TropicalFish15.obj");
	  deformed_figure = loadShape("TropicalFish15.obj");
	  if(debug){
		  figure = testShape(100, 100, 100);
		  figure.fill(color(0,255,0));
		  figure.stroke(color(0,0,255));
		  deformed_figure = testShape(100,100,100);
		  deformed_figure.fill(color(0,255,0));
		  deformed_figure.stroke(color(0,0,255));
	  }

	  //figure = figure.getTessellation();
	  //deformed_figure = deformed_figure.getTessellation();
	  noSmooth();  
	  figure =  Utilities.fillWithColor(this,figure, color(255,0,0));
	  deformed_figure = Utilities.fillWithColor(this,deformed_figure, color(255,0,0));
	  
	  
	  for(int j = 0; j < figure.getChildCount(); j++){
	    PShape aux = figure.getChild(j);
	    for(int i = 0; i < aux.getVertexCount(); i++){
	      //deformed_figure.setVertex(i,PVector.mult(deformed_figure.getVertex(i),100));
	      //figure.setVertex(i,PVector.mult(figure.getVertex(i),100));
	      vertices.add(aux.getVertex(i));  
	    }
	  }
	  for(int j = 0; j < deformed_figure.getChildCount(); j++){
	    PShape aux = deformed_figure.getChild(j);
	    for(int i = 0; i < aux.getVertexCount(); i++){
	      //deformed_figure.setVertex(i,PVector.mult(deformed_figure.getVertex(i),100));
	      //figure.setVertex(i,PVector.mult(figure.getVertex(i),100));
	      deformed_vertices.add(aux.getVertex(i));  
	    }
	  }
	  //figure.setFill(color(255,0,0));
	  //deformed_figure.setFill(color(100,0,130));
	  main_scene = new Scene(this);
	  main_scene.setDottedGrid(false);
	  main_scene.setRadius(160);
	  main_scene.showAll();  
	  //associate the shape with the original shape frame
	  original_fig = new InteractiveFrame(main_scene, figure);
	  println(figure);
	  original_fig.translate(-50,50,0);
	  original_fig.scale(0.5f);
	  original_fig.rotate(0,0,PI,0);
	  //initial deformed shape without modifications
	  deformed_fig = new InteractiveFrame(main_scene, deformed_figure);
	  deformed_fig.translate(50,50,0);
	  deformed_fig.scale(0.5f);
	  deformed_fig.rotate(0,0,PI,0);
	  //get bounding rect center
	  r_bounds = Utilities.getCube(figure);
	  r_center = new Vec((r_bounds[0].x() + r_bounds[1].x())/2.f, (r_bounds[0].y() + r_bounds[1].y())/2.f, (r_bounds[0].z() + r_bounds[1].z())/2.f);
	  println(figure.getChildCount());
	  selection = new Utilities.SelectionArea(this); 
	  //setup laplacian
	  LaplacianDeformation.setup(deformed_fig.shape());
	  LaplacianDeformation.calculateLaplacian();
	}

	public void draw() { 
	  background(0);
	  lights();
	  original_fig.draw();
	  deformed_fig.draw();
	  if(bounding_rect) Utilities.drawCube(original_fig);
	  /*pushMatrix();
	    original_fig.applyTransformation();//very efficient  
	    drawControlPoints(control_points, color(0,0,255));
	  popMatrix();*/
	  //pushMatrix();
	    //original_fig.applyTransformation();//very efficient      
	    drawControlPoints();
	  //popMatrix();
	  
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
	      original_fig.applyTransformation();//very efficient      
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
	    control_points.add(new Utilities.ControlPoint(main_scene, original_fig, new Vec(v.x,v.y,v.z)));
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
	  control_points.add(best_pos,new Utilities.ControlPoint(main_scene, original_fig, new Vec(v.x,v.y,v.z)));
	  //update A
	  LeastSolver.updateControlPoints();
	}

	public void mousePressed( ){
	  if(selection_mode){
	    selection_active = true;
	    selection.init =  new Vec(mouseX, mouseY);
	    selection.end = new Vec(mouseX, mouseY);  
	    //main_scene.eye().projectedCoordinatesOf(new Vec(mouseX, mouseY));
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
	    int pos = getControlPoint(point_world.x(),point_world.y(), point_world.z());
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
	    //main_scene.eye().projectedCoordinatesOf();
	  }
	}

	int getPoint(ArrayList<PVector> list,float x, float y, float z){
	  for(int i = 0; i < list.size(); i++){
	    PVector p = list.get(i);
	    Vec point_world = original_fig.inverseCoordinatesOf(new Vec(p.x, p.y, p.z)); 
	    println("saved coord inv :" + point_world);  
	    if(isInside(point_world,x,y,z)) return i;
	  }
	  return -1;
	}

	boolean isInside(Vec p, float x, float y, float z){
	  if(Math.abs( p.x() - x ) <= 5){
	    if(Math.abs( p.y() - y ) <= 5){
	      if(Math.abs( p.z() - z ) <= 5){
	        return true;
	      }
	    }
	  }
	  return false;
	}

	int getControlPoint(float x, float y, float z){
	  for(int i = 0; i < control_points.size(); i++){
	    Vec point_world = original_fig.inverseCoordinatesOf(control_points.get(i).position());
	    if(isInside(point_world,x,y,z)) return i;
	  }
	  return -1;
	}

	void drawControlPoints(ArrayList<PVector> control_points){
	  drawControlPoints(control_points, color(0,255,0));
	}

	void drawControlPoints(ArrayList<PVector> control_points, int col){
	  PGraphics p = main_scene.pg();
	  p.pushStyle();
	  p.strokeWeight(5);
	  p.stroke(0,255,0);
	  //get coordinates in local frame
	  //p.point(r_deformed_world_figure.getCenterX(),r_deformed_world_figure.getCenterY());
	  p.stroke(col);
	  for(PVector point : control_points){
	    p.point(point.x,point.y, point.z);
	  }  
	  p.popStyle();
	  change_points = false;
	}

	void drawControlPoints(){
	  for(Utilities.ControlPoint cp : control_points){
	    //pushMatrix();
	    //cp.applyTransformation();
	    cp.draw();
	    //cp.drawShape();
	    //popMatrix();
	  }
	}

	void drawControlPoints(ArrayList<PVector> control_points, ArrayList<PVector> control_points_out){
	  PGraphics p = main_scene.pg();
	  p.pushStyle();
	  p.stroke(0,200,140);
	  p.strokeWeight(5);  
	  //get coordinates in local frame
	  //println(r_deformed_world_figure);
	  //p.point(r_deformed_world_figure.getCenterX(),r_deformed_world_figure.getCenterY());
	  for(int i = 0; i < control_points.size(); i++){
	    Vec p_w = original_fig.inverseCoordinatesOf(new Vec(control_points.get(i).x,control_points.get(i).y,control_points.get(i).z));
	    Vec p_o_w = original_fig.inverseCoordinatesOf(new Vec(control_points_out.get(i).x,control_points_out.get(i).y, control_points_out.get(i).z));
	    p.strokeWeight(1);
	    p.stroke(255,255,255);
	    p.line(p_w.x(),p_w.y(),p_w.z(),p_o_w.x(),p_o_w.y(),p_o_w.z());
	    p.strokeWeight(5);
	    p.stroke(0,0,255);
	    p.point(p_w.x(),p_w.y(),p_w.z());
	    p.stroke(0,255,0);
	    p.point(p_o_w.x(),p_o_w.y(),p_o_w.z());
	  }  
	  p.popStyle();
	  change_points = false;
	}

	//find the point in the bounding box usingpixel under point
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
		  int i = 0;
		  for(LaplacianDeformation.Vertex vertex : LaplacianDeformation.vertices.values()){
			  for(int[] idxs : vertex.idx_shape){
				  PShape face = deformed_fig.shape().getChild(idxs[0]);
				  //System.out.println("prev - : " + face.getVertex(idxs[1]));
				  face.setVertex(idxs[1], new_positions.get(vertex.idx));
				  //System.out.println("next - : " + face.getVertex(idxs[1]));
			  }
			  i++;
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
	
	public PShape testShape(int dx, int dy, int dz){
		PShape p = createShape(GROUP);
		PShape f1 = createShape();
		f1.beginShape(POLYGON);
		f1.vertex(0, 0, 0);
		f1.vertex(dx, 0, 0);
		f1.vertex(dx, dy, 0);
		f1.vertex(0, dy, 0);
		f1.endShape(CLOSE);

		PShape f2 = createShape();
		f2.beginShape(POLYGON);
		f2.vertex(0, 0, dz);
		f2.vertex(dx, 0, dz);
		f2.vertex(dx, dy, dz);
		f2.vertex(0, dy, dz);
		f2.endShape();

		PShape f3 = createShape();
		f3.beginShape(POLYGON);
		f3.vertex(0, 0, 0);
		f3.vertex(0, dy, 0);
		f3.vertex(0, dy, dz);
		f3.vertex(0, 0, dz);
		f3.endShape(CLOSE);

		PShape f4 = createShape();
		f4.beginShape(POLYGON);
		f4.vertex(dx, 0, 0);
		f4.vertex(dx, dy, 0);
		f4.vertex(dx, dy, dz);
		f4.vertex(dx, 0, dz);
		f4.endShape(CLOSE);

		PShape f5 = createShape();
		f5.beginShape(POLYGON);
		f5.vertex(0, 0, 0);
		f5.vertex(0, 0, dz);
		f5.vertex(dx, 0, dz);
		f5.vertex(dx, 0, 0);
		f5.endShape(CLOSE);

		PShape f6 = createShape();
		f6.beginShape(POLYGON);
		f6.vertex(0, dy, 0);
		f6.vertex(0, dy, dz);
		f6.vertex(dx, dy, dz);
		f6.vertex(dx, dy, 0);
		f6.endShape(CLOSE);
		p.addChild(f1);
		p.addChild(f2);
		p.addChild(f3);
		p.addChild(f4);
		p.addChild(f5);
		p.addChild(f6);
		return p;
	}
}