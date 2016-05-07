import java.util.ArrayList;
import processing.core.*;
import remixlab.bias.core.BogusEvent;
import remixlab.bias.event.*;
import remixlab.bias.ext.Profile;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.proscene.*;


public class Utilities {
	//Get a bounding box
	//get the faces of the bounding box
	public static Vec[][] original_box;
	public static Vec[][] deformed_box;

	//get a random float between the specified range
	public static float random(float a, float b){
		float r = (float)Math.random();
		float n = r*(Math.abs(b - a)) + Math.min(a, b); 
		return n;
	}
	
	//draw a rect bound
	public static void drawCube(InteractiveFrame m){
	  //set translation, rotation
	  Vec[][] cub = getFaces(m);
	  PApplet p = ((Scene)m.scene()).pApplet();
	  p.pushStyle();
	  p.stroke(p.color(255,0,0));
	  p.fill(p.color(255,0,0,5));
	  for(int i = 0; i < cub.length; i++){
		p.beginShape();
		p.vertex(cub[i][0].x(), cub[i][0].y(), cub[i][0].z());
		p.vertex(cub[i][1].x(), cub[i][1].y(), cub[i][1].z());
		p.vertex(cub[i][3].x(), cub[i][3].y(), cub[i][3].z());
		p.vertex(cub[i][2].x(), cub[i][2].y(), cub[i][2].z());
		p.endShape(PConstants.CLOSE);
	  }
	  p.popStyle();
	}

	//---------------------------------
	//Used for bounding box collisions
	public static  Vec[] getCube(PShape shape) {
	  Vec v[] = new Vec[2];
	  float minx = 999;
	  float miny = 999;
	  float maxx = -999;
	  float maxy = -999;
	  float minz = 999;
	  float maxz = -999;  
	  for(int j = 0; j < shape.getChildCount(); j++){
	    PShape aux = shape.getChild(j);
	    for(int i = 0; i < aux.getVertexCount(); i++){
	      float x = aux.getVertex(i).x;
	      float y = aux.getVertex(i).y;
	      float z = aux.getVertex(i).z;
	      minx = minx > x ? x : minx;
	      miny = miny > y ? y : miny;
	      minz = minz > z ? z : minz;
	      maxx = maxx < x ? x : maxx;
	      maxy = maxy < y ? y : maxy;
	      maxz = maxz < z ? z : maxz;
	    }
	  }
	  
	  v[0] = new Vec(minx,miny, minz);
	  v[1] = new Vec(maxx,maxy, maxz);
	  return v;
	}

	public static  Vec[][] getFaces(InteractiveFrame m){
	  Vec[] cub = getCube(m.shape());
	  Vec[][] faces = new Vec[6][4];
	  faces[0][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[0].z()));
	  faces[0][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[0].z()));
	  faces[0][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[0].z()));
	  faces[0][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[0].z()));

	  faces[1][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[1].z()));
	  faces[1][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[1].z()));
	  faces[1][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[1].z()));
	  faces[1][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[1].z()));

	  faces[2][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[0].z()));
	  faces[2][1] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[0].z()));
	  faces[2][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[1].z()));
	  faces[2][3] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[1].z()));

	  faces[3][0] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[0].z()));
	  faces[3][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[0].z()));
	  faces[3][2] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[1].z()));
	  faces[3][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[1].z()));

	  faces[4][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[0].z()));
	  faces[4][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[0].z()));
	  faces[4][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[0].y(), cub[1].z()));
	  faces[4][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[0].y(), cub[1].z()));

	  faces[5][0] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[0].z()));
	  faces[5][1] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[0].z()));
	  faces[5][2] = m.inverseCoordinatesOf(new Vec(cub[0].x(), cub[1].y(), cub[1].z()));
	  faces[5][3] = m.inverseCoordinatesOf(new Vec(cub[1].x(), cub[1].y(), cub[1].z()));
	  
	  return faces;
	}

	//a rect is definedby (x,y,z) = r + l*t, where r and l are vectors (l is the direction and r the cut point) 
	public static  Vec getIntersectionPlaneRect(Vec r, Vec l, Vec p, Vec p1, Vec p2){
	  //get the plane eq:
	  Vec u = Vec.subtract(p1,p);
	  Vec v = Vec.subtract(p2,p);
	  float[][] A = {{-l.x(),u.x(),v.x()},{-l.y(),u.y(),v.y()},{-l.z(),u.z(),v.z()}};
	  Vec r_minus_p = Vec.subtract(r,p);
	  float[][] B = {{r_minus_p.x()},{r_minus_p.y()},{r_minus_p.z()}};
	  System.out.println("solution : " + B);
	  Vec intersection = Vec.add(r,Vec.multiply(l,B[0][0]));
	  return intersection;
	}

	//just check if the point is iner the bounds of a rect
	public static boolean getIntersectionSubPlane(Vec v, Vec min, Vec max){
	  if(v.x() >=min.x() && v.x() <=max.x()){
	    if(v.y() >=min.y() && v.y() <=max.y()){
	      return true;
	    }
	  }
	  return false;
	}

	/*
	 * Given the new position of the vertices modify a specified shape
	 * */
	public static void setVertices(PShape figure, ArrayList<PVector> new_positions){
	  int cont = 0;
	  for(int j = 0; j < figure.getChildCount(); j++){
	    PShape p = figure.getChild(j); 
	    for(int i = 0; i < p.getVertexCount(); i++){
	      p.setVertex(i, new_positions.get(cont));
	      cont++;
	    }
	  }
	}

	/*
	 * Given the new position of the vertices and their idxs modify a specified shape
	 * */	
	public static void setVertices(PShape figure, ArrayList<PVector> new_positions, ArrayList<Integer> pos){
	  int cont = 0;
	  int k = 0;
	  int w = 0;
	  for(int i = 0; i < pos.size(); i++){
	    System.out.println(" pos -- : " + pos.get(i));
	  }
	  
	  for(int j = 0; j < figure.getChildCount(); j++){
	    if(w == pos.size()) return;
	    PShape p = figure.getChild(j); 
	    for(int i = 0; i < p.getVertexCount(); i++){
	      if(cont == pos.get(w++)){
	        System.out.println("indice : " + cont);        
	        p.setVertex(i, new_positions.get(k++));
	      }
	      cont++;
	    }
	  }
	}

	//apply a texture
	public static void applyTexture(InteractiveFrame f, PShape p){
	  Vec[] r_bounds = getCube(p);
	  PApplet ap = ((Scene) f.scene()).pApplet();
	  PImage text = ap.loadImage("E:/Sebchap/UNAL/Processing/Programs2015/Programs/Fishbowl3D/fishbowl/data/textures/" + Deformation.num_t + ".png");
	  PShape p_group = ap.createShape(PConstants.GROUP);
	  
	  //step btwn vertices
	  float dif = 0; 
	  float g_dif = 0;
	  float second_dif = 0;
	  if(r_bounds[1].x() - r_bounds[0].x() > dif){
	    g_dif = 0;
	  }
	  if(r_bounds[1].y() - r_bounds[0].y() > dif){
	    g_dif = 1;
	  }
	  if(r_bounds[1].z() - r_bounds[0].z() > dif){
	    g_dif = 2;
	  }

	  dif = 0;
	  if(r_bounds[1].x() - r_bounds[0].x() > dif && g_dif != 0){
	    second_dif = 0;
	  }
	  if(r_bounds[1].y() - r_bounds[0].y() > dif && g_dif != 1){
	    second_dif = 1;
	  }
	  if(r_bounds[1].z() - r_bounds[0].z() > dif && g_dif != 2){
	    second_dif = 2;
	  }
	  
	  float s_w = g_dif == 0 ? r_bounds[1].x() - r_bounds[0].x() : g_dif == 1 ? r_bounds[1].y() - r_bounds[0].y() : r_bounds[1].z() - r_bounds[0].z();
	  float s_h = second_dif == 0 ? r_bounds[1].x() - r_bounds[0].x() : second_dif == 1 ? r_bounds[1].y() - r_bounds[0].y() : r_bounds[1].z() - r_bounds[0].z();
	  float i_w = text.width;
	  float i_h = text.height;
	  float rep = 0;
	  for(int j = 0; j < p.getChildCount(); j++){
	    PShape pc = p.getChild(j);
	    PShape p_clone = ap.createShape();
	    p_clone.beginShape(PConstants.POLYGON);
	    p_clone.texture(text);
	    p_clone.noFill();
	    p_clone.noStroke();
	    p_clone.textureMode(PConstants.IMAGE);  
	    for(int i = 0; i < pc.getVertexCount(); i++){
	      PVector idx = pc.getVertex(i);
	      PVector n = pc.getNormal(i);
	      float u = g_dif == 0 ? idx.x - r_bounds[0].x(): g_dif == 1 ? idx.y - r_bounds[0].y(): idx.z - r_bounds[0].z(); 
	      float v = second_dif == 0 ? idx.x - r_bounds[0].x() : second_dif == 1 ? idx.y - r_bounds[0].y() : idx.z - r_bounds[0].z();
	      u = i_w*u*1.f/s_w;
	      v = i_h*v*1.f/s_h;    
	      p_clone.vertex(idx.x,idx.y,idx.z, u,v);
	      p_clone.normal(n.x,n.y,n.z);
	    }
	    p_clone.endShape();
	    p_group.addChild(p_clone);
	  }
	  p = p_group;
	  f.setShape(p);
	}
	
	//fill with a color
	//fill with a color
	public static PShape fillWithColor(PApplet ap, PShape p, int c){
	  PShape p_group = ap.createShape(PConstants.GROUP);
	  for(int j = 0; j < p.getChildCount(); j++){
	    PShape pc = p.getChild(j);
	    PShape p_clone = ap.createShape();
	    p_clone.beginShape(PConstants.POLYGON);
	    p_clone.fill(c);
	    p_clone.noStroke();
	    for(int i = 0; i < pc.getVertexCount(); i++){
	      PVector v = pc.getVertex(i);
	      PVector n = pc.getNormal(i);
	      p_clone.vertex(v.x,v.y,v.z);
	      p_clone.normal(n.x,n.y,n.z);
	    }
	    p_clone.endShape();
	    p_group.addChild(p_clone);    
	  }
	  return p_group;
	}
	
	

	//fill with a color
	public static PShape fillWithColor(InteractiveFrame f, PShape p, int c){
	  PApplet ap = ((Scene) f.scene()).pApplet();	  
	  PShape p_group = ap.createShape(PConstants.GROUP);
	  for(int j = 0; j < p.getChildCount(); j++){
	    PShape pc = p.getChild(j);
	    PShape p_clone = ap.createShape();
	    p_clone.beginShape(PConstants.POLYGON);
	    p_clone.fill(c);
	    p_clone.noStroke();
	    for(int i = 0; i < pc.getVertexCount(); i++){
	      PVector v = pc.getVertex(i);
	      PVector n = pc.getNormal(i);
	      p_clone.vertex(v.x,v.y,v.z);
	      p_clone.normal(n.x,n.y,n.z);
	    }
	    p_clone.endShape();
	    p_group.addChild(p_clone);    
	  }
	  f.setShape(p);
	  return p_group;
	}

	//control points
	/*
	 * An interactive tool used for enable the deformation of a shape
	 * */
	public static float RADIUS_POINT = 15;
	public static int cont = 0;

	public static class ControlPoint extends InteractiveFrame{
	  Vec B;
	  float radius;
	  PShape p;
	  
	  @Override
	  public Scene scene() {
	    return (Scene) gScene;
	  }
	  
	  public void setB(Vec B){
		  this.B = B;
		  updateShape();
	  }
	  
	  public void setupProfile(){
		  //setMouseBindings();
		  setClickBinding(MouseAgent.RIGHT_ID, 1, "remove");
		  setMotionBinding(MouseAgent.RIGHT_ID, "performTranslation");
		  //this.addGraphicsHandler(this, "drawShape");
		  updateShape();
	  }
	  
	  public ControlPoint(Scene sc, Vec i){
	    super(sc);        
	    B = new Vec(0,0,0); radius = RADIUS_POINT;  
	    this.translate(i);
	    setupProfile();
	  }  
	  
	  public ControlPoint(Scene sc, Frame f, Vec i){
	    super(sc);
	    this.setReferenceFrame(f);
	    B = new Vec(0,0,0); radius = RADIUS_POINT;  
	    this.translate(i);
	    setupProfile();
	  }  

	  public ControlPoint(Scene sc, Vec i, float r){
	    super(sc);    
	    B = new Vec(0,0,0); radius = r;
	    translate(i);
	    setupProfile();
	  }

	  public ControlPoint(Scene sc, Vec i, Vec f){
	    super(sc);    
	    B = f; radius = RADIUS_POINT;
	    translate(i);  
	    setupProfile();
	  }

	  public ControlPoint(Scene sc, Vec i, Vec f, float r){
	    super(sc);    
	    B = f; radius = r;
	    translate(i);  
	    setupProfile();
	  }
	  
	  public boolean IsInsideA(Vec v){
	    return Vec.distance(v, position()) <= radius ? true : false;
	  }
	  public boolean IsInsideB(Vec v){
	    Vec f = Vec.add(position(), B);
	    return Vec.distance(v, f) <= radius ? true : false;
	  }     

	  public void remove(ClickEvent event) {
	      Deformation.control_points.remove(this);
	      //this.detachFromEye();
	      //update A
	      LeastSolver.updateControlPoints();
	      Deformation.morphTransformationAction();    
	  }
	  int moving_point = -1;
	  //check the position of the mouse
	  
	  public void selectAction(float x, float y){
	    float threshold = (float) radius/2.f;
	    Vec B_q = inverseCoordinatesOf(B);
	    Vec proj = scene().eye().projectedCoordinatesOf(B_q);
	    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){ 
	      moving_point = 1;
	      return;
	    }
	    proj = scene().eye().projectedCoordinatesOf(position());
	    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){
	      moving_point = 0;
	      return;
	    }
	    moving_point = -1;    
	  }
	  
	  public Vec checkPickedPoint(float x, float y){
	    float threshold = radius/2.f;
	    System.out.println("x : "+ x + " y : " + y);
	    //System.out.println("xx : "+ mouseX + " yy : " + mouseY);
	    System.out.println("pos : " + position());
	    Vec B_q = inverseCoordinatesOf(B);
	    Vec proj = scene().eye().projectedCoordinatesOf(B_q);
	    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){ 
	      System.out.println("entra1");
	      return B;
	    }
	    proj = scene().eye().projectedCoordinatesOf(position());
	    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){
	      System.out.println("entra2");     
	      return position();
	    }
	    return null;
	  } 

	  public void performTranslation(DOF2Event event) {
		if(event.fired() || event.flushed()){
			selectAction(event.x(),event.y());
		    updateShape();
		}
		System.out.println("sel : " + moving_point + " coords x: " + event.x() + " y : " + event.y());
	    //Vec v = checkPickedPoint(event.x(), event.y());
	    //System.out.println(v);
	    if(moving_point == -1) return;
	    Vec p = position();
	    if(moving_point == 0){
	      System.out.println("traslada");
	      translate(screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
	        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
	    } 
	    if(moving_point == 1){
	    	customTranslation(event);
	    }
	    if(event.flushed()) Deformation.morphTransformationAction();
	  }
	  public void performTranslation3(DOF3Event event) {
	    Vec v = checkPickedPoint(event.x(), event.y());
	    if(v == null) return;
	    Vec p = position();
	    if(v.x() == p.x() && v.y() == p.y() && v.z() == p.z()){
	      translate(screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
	        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
	    } 
	    if(v == B){
	    	customTranslation(event);
	    }
	  }

	  public void customTranslation(DOF2Event event){
	    Vec dif = screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
	        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity()));    
	    B.add(dif);
	  }

	  public void customTranslation(DOF3Event event){
	    Vec dif = screenToVec(Vec.multiply(
	        new Vec(event.dx(), scene().isRightHanded() ? -event.dy() : event.dy(), -event.dz()),
	        this.translationSensitivity()));
	    B.add(dif);   
	  }
	  
	  public void updateShape(){
	      Vec aux = B;
		  PShape line = scene().pApplet().createShape(PConstants.LINE, 0,0,0, aux.x(), aux.y(), aux.z());
		  line.setStroke(scene().pApplet().color(255,255,255));
		  PShape p1 = scene().pApplet().createShape(PConstants.BOX, radius, radius, radius);
		  p1.translate(0,0,0);
		  p1.setFill(scene().pApplet().color(0,255,0));
		  PShape p2 = scene().pApplet().createShape(PConstants.BOX, radius, radius, radius);
		  p2.translate(aux.x(), aux.y(), aux.z());
		  p2.setFill(scene().pApplet().color(0,0,255));
		  PShape p = scene().pApplet().createShape(PConstants.GROUP);
		  p.addChild(p1);
		  p.addChild(p2);
		  p.addChild(line);
		  setShape(p);
	  }
	  
	  public void drawShape(){
		  PGraphics p = scene().pApplet().g;
	      p.pushStyle();
	      p.stroke(255,255,255);
	      Vec aux = B;
	      p.line(0,0,0,aux.x(), aux.y(), aux.z());
	      p.stroke(0,0,255);
	      p.strokeWeight(radius);
	      p.point(aux.x(), aux.y(), aux.z());
	      //translate(aux.x(), aux.y(), aux.z());
	      p.stroke(0,255,0);
	      p.point(0,0,0);
	      p.popStyle();
	  }
	}

	/*Select the vertices of a shape that are bounded by the selected area*/
	public static class SelectionArea{
	  Vec init; Vec end;
	  PApplet p;
	  
	  public SelectionArea(PApplet ap){
	    init = new Vec();
	    end  = new Vec();
	    p = ap;
	  }
	  
	  public SelectionArea(PApplet ap, float x, float y){
	    init = new Vec(x,y);
	    p = ap;
	  }

	  public void draw(){
	    p.pushStyle();
	    p.fill(p.color(200,200,200,100));
	    p.rect(init.x(), init.y(), end.x() - init.x(), end.y() - init.y());
	    p.popStyle();
	  }
	  
	  public void sortCorners(){
	    Vec tl = new Vec(0,0); 
	    Vec br = new Vec(0,0); 
	    tl.setX(Math.min(init.vec[0], end.vec[0]));
	    tl.setY(Math.min(init.vec[1], end.vec[1]));
	    br.setX(Math.max(init.vec[0], end.vec[0]));
	    br.setY(Math.max(init.vec[1], end.vec[1]));
	    init = tl;
	    end = br;
	    System.out.println("--init : " + init);
	    System.out.println("--end : " + end);

	  }
	  
	  public ArrayList<PVector> getVertices(ArrayList<PVector> vertices){
	    ArrayList<PVector> new_vertices = new ArrayList<PVector>(); 
	    Deformation.selected_vertices_i = new ArrayList<Integer>();
	    sortCorners();
	    System.out.println("--init : " + init);
	    System.out.println("--end : " + end);
	    int c = 0;
	    for(PVector i : vertices){
	      Vec v = new Vec(i.x,i.y,i.z);
	      v = Deformation.original_fig.inverseCoordinatesOf(v);
	      v = Deformation.main_scene.eye().projectedCoordinatesOf(v);
	      if(v.vec[0] >= init.vec[0] && v.vec[0] <= end.vec[0]){
	        if(v.vec[1] >= init.vec[1] && v.vec[1] <= end.vec[1]){
	          new_vertices.add(i);         
	          Deformation.selected_vertices_i.add(c);          
	        }
	      }
	      c++;
	    }
	    return new_vertices;
	  }
	}
}
