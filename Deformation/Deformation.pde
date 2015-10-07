import papaya.*;
import remixlab.bias.core.*;
import remixlab.bias.event.*;
import remixlab.proscene.*;
import remixlab.dandelion.core.Constants.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.core.*;

//ArrayList<ControlPoint> control_points = new ArrayList<ControlPoint>(); 
ArrayList<ControlPoint> control_points = new ArrayList<ControlPoint>();

PShape figure;
PShape deformed_figure;
Scene main_scene;
CustomModelFrame original_fig;
CustomModelFrame deformed_fig;
SelectionArea selection;
int all_width = 640;
int all_height = 360;

ArrayList<PVector> vertices = new ArrayList<PVector>();
ArrayList<PVector> selected_vertices = new ArrayList<PVector>();
ArrayList<Integer> selected_vertices_i = new ArrayList<Integer>();

ArrayList<PVector> deformed_vertices = new ArrayList<PVector>();
Vec r_center;
Vec[] r_bounds;

public void setup() {
  size(640, 360, P3D);
  textureWrap(REPEAT);
  figure = loadShape("E:/Sebchap/UNAL/Processing/Programs2015/Programs/Fishbowl3D/fishbowl/data/shapes/TropicalFish15.obj");
  deformed_figure = loadShape("E:/Sebchap/UNAL/Processing/Programs2015/Programs/Fishbowl3D/fishbowl/data/shapes/TropicalFish15.obj");

  //figure = figure.getTessellation();
  //deformed_figure = deformed_figure.getTessellation();
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
  original_fig = new CustomModelFrame(main_scene, figure);
  println(figure);
  original_fig.translate(-50,50,0);
  original_fig.scale(0.5);
  original_fig.rotate(0,0,PI,0);
  //initial deformed shape without modifications
  deformed_fig = new CustomModelFrame(main_scene, deformed_figure);
  deformed_fig.translate(50,50,0);
  deformed_fig.scale(0.5);
  deformed_fig.rotate(0,0,PI,0);
  //get bounding rect center
  r_bounds = getCube(figure);
  r_center = new Vec((r_bounds[0].x() + r_bounds[1].x())/2., (r_bounds[0].y() + r_bounds[1].y())/2., (r_bounds[0].z() + r_bounds[1].z())/2.);
  println(figure.getChildCount());
  noSmooth();  
  fillWithColor(original_fig, figure, color(255,0,0));
  fillWithColor(deformed_fig, deformed_figure, color(0,255,0));

  main_scene.mouseAgent().setButtonBinding(Target.FRAME, RIGHT, DOF2Action.CUSTOM);
  main_scene.mouseAgent().setClickBinding(Target.FRAME, RIGHT, ClickAction.CUSTOM);     
  selection = new SelectionArea();  
}

public void draw() { 
  background(0);
  lights();
  original_fig.draw();
  deformed_fig.draw();
  if(bounding_rect) drawCube(original_fig);
  /*pushMatrix();
    original_fig.applyTransformation();//very efficient  
    drawControlPoints(control_points, color(0,0,255));
  popMatrix();*/
  pushMatrix();
    original_fig.applyTransformation();//very efficient      
    drawControlPoints();
  popMatrix();
  
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
