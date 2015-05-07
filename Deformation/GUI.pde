int drag_mode = -1;
//needed to know the change of position 
Vec initial_drag_pos = null;

boolean change_points = true;

void morphTransformationAction(){
  deformed_vertices = calculateNewImage(vertices,control_points_out);
  //modify the shape
  setVertices(deformed_figure, deformed_vertices);
}

void addPoint(PVector v){
  if(control_points.size() == 0){
    control_points.add(v);
    control_points_out.add(v);
    return;
  }
  float min_dist_l = 9999;
  int best_pos = control_points.size();
  for(int i = 0; i < control_points.size(); i++){
      PVector left = control_points.get(i);
      if(v.dist(left) < min_dist_l){
        min_dist_l = v.dist(left);
        best_pos = i+1;        
      }      
  }
  control_points.add(best_pos,v);
  control_points_out.add(best_pos,v);
  //update A
  updateControlPoints();
  
}

void mousePressed( ){
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
  else if(mouseButton == RIGHT){
    //remove a point
    int pos = getControlPoint(point_world.x(), point_world.y(), point_world.z());
    if(pos != -1){
      control_points.remove(pos);
      control_points_out.remove(pos);
      //update A
      updateControlPoints();
      morphTransformationAction();
    }
  }
}

void mouseReleased(){
  drag_mode = -1;
  initial_drag_pos = null;
}

void mouseDragged(){
  if(drag_mode != -1){
    //get coordinates in world
    Vec point_world = main_scene.eye().unprojectedCoordinatesOf(new Vec(mouseX, mouseY));
    //get the vector position change, and according to this modify the local point
    Vec delta = Vec.subtract(point_world,initial_drag_pos);
    //set the position
    Vec p = new Vec(control_points.get(drag_mode).x, control_points.get(drag_mode).y, control_points.get(drag_mode).z);
    Vec control_world = original_fig.inverseCoordinatesOf(p);
    Vec control_world_new = Vec.add(control_world, delta);
    Vec point_shape = original_fig.coordinatesOf(control_world_new);
    //println("drag : " + drag_mode);
    control_points_out.set(drag_mode, new PVector(point_shape.x(), point_shape.y(), point_shape.z()));
    morphTransformationAction();
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
    PVector p = control_points.get(i);
    Vec point_world = original_fig.inverseCoordinatesOf(new Vec(p.x, p.y, p.z));
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
boolean add_mode = true;
boolean bounding_rect = true;
void keyPressed(){
  if (key == 'c'){
    add_mode = !add_mode;
  }
  if (key == 'r'){
    bounding_rect = !bounding_rect;
  }
}

