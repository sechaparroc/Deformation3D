package interactive;

import processing.core.PConstants;
import processing.core.PShape;
import remixlab.bias.event.ClickEvent;
import remixlab.bias.event.DOF2Event;
import remixlab.dandelion.core.GenericFrame;
import remixlab.dandelion.geom.Frame;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.MouseAgent;
import remixlab.proscene.Scene;

//control points
/*
 * An interactive tool used for enable the deformation of a shape
 * */
public class ControlPoint extends Interactive{

  public class Image extends InteractiveFrame{
	public Image(Scene scn, GenericFrame reference) {
		super(scn);
		this.setReferenceFrame(reference);
	    this.setHighlightingMode(HighlightingMode.NONE);
		updateShape();
  	    setMotionBinding(MouseAgent.LEFT_ID, "translate");

	}
	public void updateShape(){
		  Vec aux = coordinatesOf(referenceFrame().position());
		  PShape line = scene().pApplet().createShape(PConstants.LINE, 0,0,0, aux.x(), aux.y(), aux.z());
		  line.setStroke(scene().pApplet().color(255,255,255));
		  PShape p = scene().pApplet().createShape(PConstants.BOX, radius, radius, radius);
		  p.setFill(scene().pApplet().color(0,0,255));
		  PShape shape = scene().pApplet().createShape(PConstants.GROUP);
		  shape.addChild(p);
		  shape.addChild(line);
		  setShape(shape);
	  }
	  public void translate(DOF2Event e){
		  super.translate(e);
		  updateShape();
		  if(e.flushed()) ControlPoint.this.notifyAllListeners("TRANSLATE_B");
	  }
	  /*
	   * TODO: When Rotation notify Translate B
	   * Modify Rotation to translate according to Reference Frame
	   * */
	  
  }
	
  public static float RADIUS_POINT = 15;
  private Image B;
  private float radius;
  private boolean translateImage = false;
  
  @Override
  public Scene scene() {
    return (Scene) gScene;
  }
  
  public InteractiveFrame getB(){
	  return B;
  }
  
  public void setB(Image B){
	  this.B = B;
  }
  
  private void initialSetup(Vec A){
	radius = RADIUS_POINT;  
    this.translate(A);
    this.setHighlightingMode(HighlightingMode.NONE);
    setupProfile();	    
  }
  
  public void setupProfile(){
	  setClickBinding(MouseAgent.RIGHT_ID, 1, "remove");
	  setMotionBinding(MouseAgent.RIGHT_ID, "performTranslation");
	  updateShape();
  }
  
  public ControlPoint(Scene sc, Vec A){
    super(sc);        
    initialSetup(A);	    
  }  
  
  public ControlPoint(Scene sc, Frame f, Vec A){
    super(sc);
    this.setReferenceFrame(f);
    initialSetup(A);
  }  

  public ControlPoint(Scene sc, Vec A, float r){
    super(sc);    
    initialSetup(A);	    
    radius = r;
    setupProfile();
  }

  public ControlPoint(Scene sc, Vec A, Vec f){
    super(sc);    
    initialSetup(A);
    B = new Image(sc, this);
    B.translate(f); radius = RADIUS_POINT;
    setupProfile();
  }

  public ControlPoint(Scene sc, Vec A, Vec f, float r){
    super(sc);    
    initialSetup(A);	    
    B = new Image(sc, this);
    B.translate(f); radius = RADIUS_POINT;
    setupProfile();
  }

  public void remove(ClickEvent event) {
      scene().inputHandler().removeGrabber(this);
      if(B != null) scene().inputHandler().removeGrabber(B);
      notifyAllListeners("REMOVE");
  }

  public void performTranslation(DOF2Event event) {
	  if(event.fired() && B == null){
		B = new Image(this.scene(), this);
		translateImage = true;
	  }
	  
	  if(translateImage == false){
	      translate(screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
	    	        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
	  }else{
		  translateImage(event);
	  }
	  
	  if(event.flushed() && translateImage){
		  translateImage = false;
	  }
	  else if(event.flushed() && !translateImage) notifyAllListeners("TRANSLATE_A");
  }
  
  public void translateImage(DOF2Event event){
	  B.translate(event);
  }


  public void updateShape(){
	  PShape p = scene().pApplet().createShape(PConstants.BOX, radius, radius, radius);
	  p.setFill(scene().pApplet().color(0,255,0));
	  setShape(p);
  }
  
  public void draw(){
	  super.draw();
	  if(B != null) B.draw();
  }

  
	@Override
	protected void setEvents() {
		events.add("TRANSLATE_A");
		events.add("TRANSLATE_B");
		/*Currently unused*/
		events.add("ROTATE_A");
		events.add("ROTATE_B");
	}
}
