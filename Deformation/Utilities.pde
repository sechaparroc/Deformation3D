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
  for(int i = 0; i < shape.getVertexCount(); i++){
    float x = shape.getVertex(i).x;
    float y = shape.getVertex(i).y;
    float z = shape.getVertex(i).z;
    minx = minx > x ? x : minx;
    miny = miny > y ? y : miny;
    minz = minz > z ? z : minz;
    maxx = maxx < x ? x : maxx;
    maxy = maxy < y ? y : maxy;
    maxz = maxz < z ? z : maxz;
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
  for(int i = 0; i < new_positions.size(); i++){
    figure.setVertex(i, new_positions.get(i));
  }
}
