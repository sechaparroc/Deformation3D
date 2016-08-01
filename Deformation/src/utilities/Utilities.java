package utilities;

import java.util.ArrayList;
import processing.core.*;
import remixlab.dandelion.geom.*;
import remixlab.proscene.*;


public class Utilities {
	//Get a bounding box
	//get the faces of the bounding box
	public static Vec[][] original_box;
	public static Vec[][] deformed_box;

	//Convert from Vec to PVector
	public static PVector vecToPVector(Vec v){
		return new PVector(v.x(),v.y(),v.z());
	}

	//Convert PVector to Vec
	public static Vec PVectorToVec(PVector v){
		return new Vec(v.x,v.y,v.z);		
	}
	
	
	//Get a random float between the specified range
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

	public static void cloneWithTexture(InteractiveFrame f, PShape p, String texture){
	  PApplet ap = ((Scene) f.scene()).pApplet();
	  f.setShape(cloneWithTexture(ap, p, texture));
	}
	
	//apply a texture
	public static PShape cloneWithTexture(PApplet ap, PShape p, String texture){
	  Vec[] r_bounds = getCube(p);
	  PImage text = ap.loadImage(texture);
	  PShape p_group = ap.createShape(PConstants.GROUP);
	  //step between vertices
	  float dif = 0; 
	  float g_dif = 0;
	  float second_dif = 0;
	  
	  /*get the face with the higher dimension*/
	  if(r_bounds[1].x() - r_bounds[0].x() > dif) g_dif = 0;
	  else if(r_bounds[1].y() - r_bounds[0].y() > dif) g_dif = 1;
	  else if(r_bounds[1].z() - r_bounds[0].z() > dif) g_dif = 2;
	  dif = 0;
	  if(r_bounds[1].x() - r_bounds[0].x() > dif && g_dif != 0) second_dif = 0;
	  else if(r_bounds[1].y() - r_bounds[0].y() > dif && g_dif != 1)  second_dif = 1;
	  else if(r_bounds[1].z() - r_bounds[0].z() > dif && g_dif != 2)  second_dif = 2;
	  
	  //set up the texture width and height
	  float s_w = g_dif == 0 ? r_bounds[1].x() - r_bounds[0].x() : g_dif == 1 ? r_bounds[1].y() - r_bounds[0].y() : r_bounds[1].z() - r_bounds[0].z();
	  float s_h = second_dif == 0 ? r_bounds[1].x() - r_bounds[0].x() : second_dif == 1 ? r_bounds[1].y() - r_bounds[0].y() : r_bounds[1].z() - r_bounds[0].z();

	  float i_w = text.width;
	  float i_h = text.height;

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
	      p_clone.normal(n.x,n.y,n.z);
	      p_clone.vertex(idx.x,idx.y,idx.z, u,v);
	    }
	    p_clone.endShape();
	    p_group.addChild(p_clone);
	  }
	  return p_group;
	}
	
	//fill with a color
	public static PShape cloneShape(PApplet ap, PShape p, int c){
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
	      p_clone.normal(n.x,n.y,n.z);
	      p_clone.vertex(v.x,v.y,v.z);
	    }
	    p_clone.endShape();
	    p_group.addChild(p_clone);    
	  }
	  return p_group;
	}
	
	//fill with a color
	public static PShape cloneShape(InteractiveFrame f, PShape p, int c){
	  PApplet ap = ((Scene) f.scene()).pApplet();	  
	  return cloneShape(ap, p, c);
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
	}
	
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	//DEBUG METHODS
	public static PShape testShape(PApplet applet, int dx, int dy, int dz, int num){
		PShape p = applet.createShape(PConstants.GROUP);
		float[] step = {dx/(num*1f),dy/(num*1f),dz/(num*1f)}; 
		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f1 = applet.createShape();
				f1.beginShape(PConstants.POLYGON);
				f1.vertex(i*step[0], j*step[1], 0);
				f1.vertex((i+1)*step[0], j*step[1], 0);
				f1.vertex((i+1)*step[0], (j+1)*step[1], 0);
				f1.vertex(i*step[0], (j+1)*step[1], 0);
				f1.endShape(PConstants.CLOSE);
				p.addChild(f1);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f2 = applet.createShape();
				f2.beginShape(PConstants.POLYGON);
				f2.vertex(i*step[0], j*step[1], dz);
				f2.vertex((i+1)*step[0], j*step[1], dz);
				f2.vertex((i+1)*step[0], (j+1)*step[1], dz);
				f2.vertex(i*step[0], (j+1)*step[1], dz);
				f2.endShape();
				p.addChild(f2);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f3 = applet.createShape();
				f3.beginShape(PConstants.POLYGON);
				f3.vertex(0, i*step[1], j*step[2]);
				f3.vertex(0, (i+1)*step[1], j*step[2]);
				f3.vertex(0, (i+1)*step[1], (j+1)*step[2]);
				f3.vertex(0, i*step[1], (j+1)*step[2]);
				f3.endShape(PConstants.CLOSE);
				p.addChild(f3);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f4 = applet.createShape();
				f4.beginShape(PConstants.POLYGON);
				f4.vertex(dx, i*step[1], j*step[2]);
				f4.vertex(dx, (i+1)*step[1], j*step[2]);
				f4.vertex(dx, (i+1)*step[1], (j+1)*step[2]);
				f4.vertex(dx, i*step[1], (j+1)*step[2]);
				f4.endShape(PConstants.CLOSE);
				p.addChild(f4);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f5 = applet.createShape();
				f5.beginShape(PConstants.POLYGON);
				f5.vertex(i*step[0], 0, j*step[2]);
				f5.vertex(i*step[0], 0, (j+1)*step[2]);
				f5.vertex((i+1)*step[0], 0, (j+1)*step[2]);
				f5.vertex((i+1)*step[0], 0, j*step[2]);
				f5.endShape(PConstants.CLOSE);
				p.addChild(f5);
			}
		}

		for(int i = 0; i < num; i++){
			for(int j = 0; j < num; j++){
				PShape f6 = applet.createShape();
				f6.beginShape(PConstants.POLYGON);
				f6.vertex(i*step[0], dy, j*step[2]);
				f6.vertex(i*step[0], dy, (j+1)*step[2]);
				f6.vertex((i+1)*step[0], dy, (j+1)*step[2]);
				f6.vertex((i+1)*step[0], dy, j*step[2]);
				f6.endShape(PConstants.CLOSE);
				p.addChild(f6);
			}
		}
		return p;
	}
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------	

	
}
