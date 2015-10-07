int drag_mode = -1;
//needed to know the change of position 
Vec initial_drag_pos = null;
int step_per_point = 10;
boolean clear_points = true;
boolean change_points = true;
boolean selection_active = false;
boolean selection_mode = false;
boolean use_selected = false;

void morphTransformationAction(){
  
  if(!use_selected){
    deformed_vertices = calculateNewImage(vertices,control_points);
    setVertices(deformed_figure, deformed_vertices);
  }
  else{
    deformed_vertices = calculateNewImage(selected_vertices,control_points);
    setVertices(deformed_figure, deformed_vertices, selected_vertices_i);
  }    
  //modify the shape
}

void addPoint(PVector v){
  if(control_points.size() == 0){
    control_points.add(new ControlPoint(main_scene, original_fig, new Vec(v.x,v.y,v.z)));
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
  control_points.add(best_pos,new ControlPoint(main_scene, original_fig, new Vec(v.x,v.y,v.z)));
  //update A
  updateControlPoints();
  
}

void mousePressed( ){
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

void mouseReleased(){
  selection_active = false;
  drag_mode = -1;
  initial_drag_pos = null;
}

void mouseDragged(){
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
  for(ControlPoint cp : control_points){
    pushMatrix();
    cp.applyTransformation();
    cp.drawShape();
    popMatrix();
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
boolean add_mode = true;
boolean bounding_rect = true;
boolean enable_texture = false;
int num_t = 1;
void keyPressed(){
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

  if (key == 'r'){
    bounding_rect = !bounding_rect;
  }
  //predefined transformation
  //auto points 
  if(key == 'v' || key == 'V'){
    step_per_point = vertices.size()/((int)random(15,30) + 1);
    addControlPointsAuto(true);
    updateControlPoints();
    morphTransformationAction();
  }
  //scale x
  if(key=='1'){
    scaleX(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  //scale y  
  if(key=='2'){
    scaleY(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  //scale z  
  if(key=='3'){
    scaleZ(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  //spline XZ
  //horizontal
  if(key=='4'){
    applyHorizontalZXSpline(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  //vertical
  if(key=='5'){
    applyVerticalZXSpline(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }  
  //spline YZ
  //spline horizontal 
  if(key=='6'){
    applyHorizontalYZSpline(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  //spline vertical 
  if(key=='7'){
    applyVerticalYZSpline(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  //spline XY
  //spline horizontal 
  if(key=='8'){
    applyHorizontalXYSpline(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  //spline vertical 
  if(key=='9'){
    applyVerticalXYSpline(clear_points);
    updateControlPoints();
    morphTransformationAction();
  }
  
  if(key=='0'){
    combination();
  }
  
  if(key =='z'){
      fillWithColor(original_fig, figure, color(255,0,0));
      fillWithColor(deformed_fig, deformed_figure, color(0,255,0));
    /*
    if(enable_texture){ 
      applyTexture(original_fig, figure);
      applyTexture(deformed_fig, deformed_figure);
      num_t = num_t != 24 ? (num_t + 1) : 1; 
    }
    else{
      fillWithColor(original_fig, figure, color(255,0,0));
      fillWithColor(deformed_fig, deformed_figure, color(100,0,130));
    }*/
    enable_texture = !enable_texture;
  }
}




