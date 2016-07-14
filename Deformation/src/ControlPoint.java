import java.lang.reflect.Method;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import remixlab.bias.event.ClickEvent;
import remixlab.bias.event.DOF2Event;
import remixlab.bias.event.DOF3Event;
import remixlab.dandelion.geom.Frame;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.MouseAgent;
import remixlab.proscene.Scene;

//control points
/*
 * An interactive tool used for enable the deformation of a shape
 * */
public class ControlPoint extends InteractiveFrame{
  public static float RADIUS_POINT = 15;
  Vec B;
  private float radius;
  private PShape p;
  private ArrayList<ControlPoint> control_points;
  private int selected_section = 0;  
  
  private Class<?> actionClass = null;
  private Method actionMethod = null;
  
  @Override
  public Scene scene() {
    return (Scene) gScene;
  }
  
  public void setB(Vec B){
	  this.B = B;
	  updateShape();
  }
  
  private void initialSetup(Vec A, ArrayList<ControlPoint> control_points){
	setGroup(control_points);
    B = new Vec(0,0,0); radius = RADIUS_POINT;  
    this.translate(A);
    this.disableHighlighting();
    setupProfile();	    
  }
  
  public void setGroup(ArrayList<ControlPoint> control_points){
	  this.control_points = control_points;
  }
  
  public void setupProfile(){
	  setClickBinding(MouseAgent.RIGHT_ID, 1, "remove");
	  setMotionBinding(MouseAgent.RIGHT_ID, "performTranslation");
	  updateShape();
  }
  
  public ControlPoint(Scene sc, Vec A, ArrayList<ControlPoint> control_points){
    super(sc);        
    initialSetup(A,control_points);	    
  }  
  
  public ControlPoint(Scene sc, Frame f, Vec A, ArrayList<ControlPoint> control_points){
    super(sc);
    this.setReferenceFrame(f);
    initialSetup(A,control_points);
  }  

  public ControlPoint(Scene sc, Vec A, float r, ArrayList<ControlPoint> control_points){
    super(sc);    
    initialSetup(A, control_points);	    
    radius = r;
  }

  public ControlPoint(Scene sc, Vec A, Vec f, ArrayList<ControlPoint> control_points){
    super(sc);    
    initialSetup(A, control_points);	    
    B = f; radius = RADIUS_POINT;
    setupProfile();
  }

  public ControlPoint(Scene sc, Vec A, Vec f, float r, ArrayList<ControlPoint> control_points){
    super(sc);    
    initialSetup(A, control_points);	    
    B = f; radius = r;
    setupProfile();
  }

  public void setAction(Class<?> cls, String methodName) {
    try {
      actionMethod = cls.getMethod(methodName, null);
      actionClass = cls;
    } catch (Exception e) {
      PApplet.println("Something went wrong when registering your " + methodName + " method");
      e.printStackTrace();
    }
  }	  

  protected boolean executeAction() {
    if (actionClass != null) {
      try {
        actionMethod.invoke(null, null);
        return true;
      } catch (Exception e) {
        PApplet.println("Something went wrong when invoking your " + drawHandlerMethod.getName() + " method");
        e.printStackTrace();
        return false;
      }
    }
    return false;
  }	  
  
  public void executeActions(){
      LeastSolver.updateControlPoints();
      Deformation.morphTransformationAction();    
  }
  
  public boolean IsInsideA(Vec v){
    return Vec.distance(v, position()) <= radius ? true : false;
  }
  public boolean IsInsideB(Vec v){
    Vec f = Vec.add(position(), B);
    return Vec.distance(v, f) <= radius ? true : false;
  }     
  
  public void remove(ClickEvent event) {
	  remove();
  }

  public void remove() {
      control_points.remove(this);
      scene().inputHandler().removeGrabber(this);
      executeAction();	  
  }

  
  //check the position of the mouse	  
  public int checkPickedSection(float x, float y){
    float threshold = (float) radius/2.f;
    Vec B_q = inverseCoordinatesOf(B);
    Vec proj = scene().eye().projectedCoordinatesOf(B_q);
    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){ 
      return 1;
    }
    proj = scene().eye().projectedCoordinatesOf(position());
    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){
      return 0;
    }
    return -1;
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
		selected_section = checkPickedSection(event.x(),event.y());
	    updateShape();
	}
    if(selected_section == -1) return;
    if(selected_section ==  0){
      translate(screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
    } 
    if(selected_section == 1){
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
