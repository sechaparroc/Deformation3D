//Get a bounding box
//get the faces of the bounding box
Vec[][] original_box;
Vec[][] deformed_box;

//draw a rect bound
public void drawCube(InteractiveModelFrame m){
  //set translation, rotation
  Vec[][] cub = getFaces(m);
  pushStyle();
  stroke(color(255,0,0));
  fill(color(255,0,0,5));
  for(int i = 0; i < cub.length; i++){
    beginShape();
    vertex(cub[i][0].x(), cub[i][0].y(), cub[i][0].z());
    vertex(cub[i][1].x(), cub[i][1].y(), cub[i][1].z());
    vertex(cub[i][3].x(), cub[i][3].y(), cub[i][3].z());
    vertex(cub[i][2].x(), cub[i][2].y(), cub[i][2].z());
    endShape(CLOSE);
  }
  popStyle();
}

//---------------------------------
//Used for bounding box collisions
public Vec[] getCube(PShape shape) {
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

public Vec[][] getFaces(InteractiveModelFrame m){
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
public Vec getIntersectionPlaneRect(Vec r, Vec l, Vec p, Vec p1, Vec p2){
  //get the plane eq:
  Vec u = Vec.subtract(p1,p);
  Vec v = Vec.subtract(p2,p);
  float[][] A = {{-l.x(),u.x(),v.x()},{-l.y(),u.y(),v.y()},{-l.z(),u.z(),v.z()}};
  Vec r_minus_p = Vec.subtract(r,p);
  float[][] B = {{r_minus_p.x()},{r_minus_p.y()},{r_minus_p.z()}};
  println("solution : ", B);
  Vec intersection = Vec.add(r,Vec.multiply(l,B[0][0]));
  return intersection;
}

//just check if the point is iner the bounds of a rect
public boolean getIntersectionSubPlane(Vec v, Vec min, Vec max){
  if(v.x() >=min.x() && v.x() <=max.x()){
    if(v.y() >=min.y() && v.y() <=max.y()){
      return true;
    }
  }
  return false;
}

void setVertices(PShape figure, ArrayList<PVector> new_positions){
  int cont = 0;
  for(int j = 0; j < figure.getChildCount(); j++){
    PShape p = figure.getChild(j); 
    for(int i = 0; i < p.getVertexCount(); i++){
      p.setVertex(i, new_positions.get(cont));
      cont++;
    }
  }
}

void setVertices(PShape figure, ArrayList<PVector> new_positions, ArrayList<Integer> pos){
  int cont = 0;
  int k = 0;
  int w = 0;
  for(int i = 0; i < pos.size(); i++){
    println(" pos -- : " + pos.get(i));
  }
  
  for(int j = 0; j < figure.getChildCount(); j++){
    if(w == pos.size()) return;
    PShape p = figure.getChild(j); 
    for(int i = 0; i < p.getVertexCount(); i++){
      if(cont == pos.get(w++)){
        println("indice : " + cont);        
        p.setVertex(i, new_positions.get(k++));
      }
      cont++;
    }
  }
}

//apply a texture
public void applyTexture(InteractiveModelFrame f, PShape p){
  Vec[] r_bounds = getCube(p);
  PImage text = loadImage("E:/Sebchap/UNAL/Processing/Programs2015/Programs/Fishbowl3D/fishbowl/data/textures/" + num_t + ".png");
  PShape p_group = createShape(GROUP);
  
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
    PShape p_clone = createShape();
    p_clone.beginShape(POLYGON);
    p_clone.texture(text);
    p_clone.noFill();
    p_clone.noStroke();
    p_clone.textureMode(IMAGE);  
    for(int i = 0; i < pc.getVertexCount(); i++){
      PVector idx = pc.getVertex(i);
      PVector n = pc.getNormal(i);
      float u = g_dif == 0 ? idx.x - r_bounds[0].x(): g_dif == 1 ? idx.y - r_bounds[0].y(): idx.z - r_bounds[0].z(); 
      float v = second_dif == 0 ? idx.x - r_bounds[0].x() : second_dif == 1 ? idx.y - r_bounds[0].y() : idx.z - r_bounds[0].z();
      u = i_w*u*1./s_w;
      v = i_h*v*1./s_h;    
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
public void fillWithColor(InteractiveModelFrame f, PShape p, color c){
  println("entra 1");
  PShape p_group = createShape(GROUP);
  for(int j = 0; j < p.getChildCount(); j++){
    println("entra 2 j : " + j);
    PShape pc = p.getChild(j);
    PShape p_clone = createShape();
    p_clone.beginShape(POLYGON);
    p_clone.fill(c);
    p_clone.noStroke();
    for(int i = 0; i < pc.getVertexCount(); i++){
      println("entra 3 i : " + i);
      PVector v = pc.getVertex(i);
      PVector n = pc.getNormal(i);
      p_clone.vertex(v.x,v.y,v.z);
      p_clone.normal(n.x,n.y,n.z);
    }
    println("sale 2 j : " + j);    
    p_clone.endShape();
    p_group.addChild(p_clone);    
  }
  println("entra 4");
  p = p_group;
  f.setShape(p);
}

//control points
float RADIUS_POINT = 9;
int cont = 0;

public class ControlPoint extends InteractiveFrame{
  Vec B;
  float radius;
  
  public ControlPoint(Scene sc, Vec i){
    super(sc);        
    B = new Vec(0,0,0); radius = RADIUS_POINT;  
    this.translate(i);
  }  
  
  public ControlPoint(Scene sc, Frame f, Vec i){
    super(sc,f);        
    B = new Vec(0,0,0); radius = RADIUS_POINT;  
    this.translate(i);
  }  

  public ControlPoint(Scene sc, Vec i, float r){
    super(sc);    
    B = new Vec(0,0,0); radius = r;
    translate(i);
  }

  public ControlPoint(Scene sc, Vec i, Vec f){
    super(sc);    
    B = f; radius = RADIUS_POINT;
    translate(i);  
  }

  public ControlPoint(Scene sc, Vec i, Vec f, float r){
    super(sc);    
    B = f; radius = r;
    translate(i);  
  }
  
  public boolean IsInsideA(Vec v){
    return Vec.distance(v, position()) <= radius ? true : false;
  }
  public boolean IsInsideB(Vec v){
    Vec f = Vec.add(position(), B);
    return Vec.distance(v, f) <= radius ? true : false;
  }     

  @Override
  public void performCustomAction(ClickEvent event) {
      control_points.remove(this);
      //this.detachFromEye();
      //update A
      updateControlPoints();
      morphTransformationAction();    
  }
  int moving_point = -1;
  
  @Override
  public boolean checkIfGrabsInput(float x, float y){
    float threshold = radius/2.;
    Vec B_q = inverseCoordinatesOf(B);
    Vec proj = scene().eye().projectedCoordinatesOf(B_q);
    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){ 
      println("entra " + cont++);
      moving_point = 1;
      return true;
    }
    proj = scene().eye().projectedCoordinatesOf(position());
    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){
      println("entra " + cont++);
      moving_point = 0;
      return true;      
    }
    if(!isInInteraction()) moving_point = -1;    
    return false;
  }

  public Vec checkPickedPoint(float x, float y){
    float threshold = radius/2.;
    println("x : "+ x + " y : " + y);
    println("xx : "+ mouseX + " yy : " + mouseY);
    println("pos : " + position());
    Vec B_q = inverseCoordinatesOf(B);
    Vec proj = scene().eye().projectedCoordinatesOf(B_q);
    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){ 
      println("entra1");
      return B;
    }
    proj = scene().eye().projectedCoordinatesOf(position());
    if((Math.abs(x - proj.vec[0]) < threshold) && (Math.abs(y - proj.vec[1]) < threshold)){
      println("entra2");     
      return position();
    }
    return null;
  } 

  @Override
  public void performCustomAction(DOF2Event event) {
    println("superior");
    //Vec v = checkPickedPoint(event.x(), event.y());
    //println(v);
    if(moving_point == -1) return;
    Vec p = position();
    if(moving_point == 0){
      println("traslada");
      translate(screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
    } 
    if(moving_point == 1) customTranslation(event);
    morphTransformationAction();
  }
  @Override
  public void performCustomAction(DOF3Event event) {
    Vec v = checkPickedPoint(event.x(), event.y());
    if(v == null) return;
    Vec p = position();
    if(v.x() == p.x() && v.y() == p.y() && v.z() == p.z()){
      translate(screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
    } 
    if(v == B)customTranslation(event);
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
  
  public void drawShape(){
      pushStyle();
      stroke(255,255,255);
      Vec aux = B;
      line(0,0,0,aux.x(), aux.y(), aux.z());
      stroke(0,0,255);
      strokeWeight(radius);
      point(aux.x(), aux.y(), aux.z());
      //translate(aux.x(), aux.y(), aux.z());
      stroke(0,255,0);
      point(0,0,0);
      popStyle();
  }
}


public class CustomModelFrame extends InteractiveModelFrame{
  public CustomModelFrame(Scene sc, PShape s){
    super(sc, s);
  }
  
  @Override
  public void performCustomAction(DOF2Event event) {
      translate(screenToVec(Vec.multiply(new Vec(isEyeFrame() ? -event.dx() : event.dx(),
        (scene().isRightHanded() ^ isEyeFrame()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
  }
}

public class SelectionArea{
  Vec init; Vec end;
  
  public SelectionArea(){
    init = new Vec();
    end  = new Vec();
  }
  
  public SelectionArea(float x, float y){
    init = new Vec(x,y);
  }

  public void draw(){
    pushStyle();
    fill(color(200,200,200,100));
    rect(init.x(), init.y(), end.x() - init.x(), end.y() - init.y());
    popStyle();
  }
  
  public void sortCorners(){
    Vec tl = new Vec(0,0); 
    Vec br = new Vec(0,0); 
    tl.setX(min(init.vec[0], end.vec[0]));
    tl.setY(min(init.vec[1], end.vec[1]));
    br.setX(max(init.vec[0], end.vec[0]));
    br.setY(max(init.vec[1], end.vec[1]));
    init = tl;
    end = br;
    println("--init : " + init);
    println("--end : " + end);

  }
  
  public ArrayList<PVector> getVertices(ArrayList<PVector> vertices){
    ArrayList<PVector> new_vertices = new ArrayList<PVector>(); 
    selected_vertices_i = new ArrayList<Integer>();
    sortCorners();
    println("--init : " + init);
    println("--end : " + end);
    int c = 0;
    for(PVector i : vertices){
      Vec v = new Vec(i.x,i.y,i.z);
      v = original_fig.inverseCoordinatesOf(v);
      v = main_scene.eye().projectedCoordinatesOf(v);
      if(v.vec[0] >= init.vec[0] && v.vec[0] <= end.vec[0]){
        if(v.vec[1] >= init.vec[1] && v.vec[1] <= end.vec[1]){
          new_vertices.add(i);         
          selected_vertices_i.add(c);          
        }
      }
      c++;
    }
    return new_vertices;
  }
}

