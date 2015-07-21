import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import papaya.*;


ArrayList<PVector> control_points = new ArrayList<PVector>();
ArrayList<PVector> control_points_out = new ArrayList<PVector>();

PShape figure;
PShape deformed_figure;
Scene main_scene;
InteractiveModelFrame original_fig;
InteractiveModelFrame deformed_fig;

int all_width = 640;
int all_height = 360;

ArrayList<PVector> vertices = new ArrayList<PVector>();
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
  original_fig = new InteractiveModelFrame(main_scene, figure);
  println(figure);
  original_fig.translate(-50,50,0);
  original_fig.scale(0.5);
  original_fig.rotate(0,0,PI,0);
  //initial deformed shape without modifications
  deformed_fig = new InteractiveModelFrame(main_scene, deformed_figure);
  deformed_fig.translate(50,50,0);
  deformed_fig.scale(0.5);
  deformed_fig.rotate(0,0,PI,0);
  //get bounding rect center
  r_bounds = getCube(figure);
  r_center = new Vec((r_bounds[0].x() + r_bounds[1].x())/2., (r_bounds[0].y() + r_bounds[1].y())/2., (r_bounds[0].z() + r_bounds[1].z())/2.);
  println(figure.getChildCount());
  noSmooth();  
  fillWithColor(original_fig, figure, color(255,0,0));
  fillWithColor(deformed_fig, deformed_figure, color(100,0,130));  
}

public void draw() {
  handleAgents(); 
  background(0);
  lights();    

  original_fig.draw();
  deformed_fig.draw();
  if(bounding_rect) drawCube(original_fig);
  pushMatrix();
    original_fig.applyTransformation();//very efficient  
    drawControlPoints(control_points, color(0,0,255));
  popMatrix();
  drawControlPoints(control_points,control_points_out);  

}

void handleAgents(){
  main_scene.enableMotionAgent();
  main_scene.enableKeyboardAgent();
  if(drag_mode != -1) {
    main_scene.disableMotionAgent();
    main_scene.disableKeyboardAgent();
  }
}
