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


public void setup() {
  size(all_width, all_height, P3D);
  figure = loadShape("/home/sebchap/Processing/Programs/Deformation3D/Deformation/data/mesh.obj");
  deformed_figure = loadShape("/home/sebchap/Processing/Programs/Deformation3D/Deformation/data/mesh.obj");
  figure = figure.getTessellation();
  deformed_figure = deformed_figure.getTessellation();
  for(int i = 0; i < figure.getVertexCount(); i++){
    //deformed_figure.setVertex(i,PVector.mult(deformed_figure.getVertex(i),100));
    //figure.setVertex(i,PVector.mult(figure.getVertex(i),100));
    vertices.add(figure.getVertex(i));  
    deformed_vertices.add(deformed_figure.getVertex(i));  
  }
  figure.setFill(color(255,0,0));
  deformed_figure.setFill(color(100,0,130));
  main_scene = new Scene(this);
  main_scene.setDottedGrid(false);
  main_scene.setRadius(160);
  main_scene.showAll();  
  //associate the shape with the original shape frame
  original_fig = new InteractiveModelFrame(main_scene, figure);
  original_fig.translate(-50,50,0);
  original_fig.scale(30.0);
  original_fig.rotate(0,0,PI,0);
  //initial deformed shape without modifications
  deformed_fig = new InteractiveModelFrame(main_scene, deformed_figure);
  deformed_fig.translate(50,50,0);
  deformed_fig.scale(30.0);
  deformed_fig.rotate(0,0,PI,0);
  
  println(figure.getVertexCount());
  noSmooth();  
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


