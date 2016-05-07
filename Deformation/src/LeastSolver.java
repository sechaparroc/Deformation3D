import java.util.*;

import papaya.*;
import processing.core.*;
import remixlab.dandelion.geom.*;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;

public class LeastSolver {
	//Based on [Schaefer] : Image Deformation Using Moving Least Squares
	//First Approach: Affine Transformations
	public static float mod_factor = 8.f;
	public static double[][] A;
	public static double[][] w;

	public static void getA(ArrayList<PVector> img, ArrayList<Utilities.ControlPoint> control){
	  A = new double[img.size()][control.size()];
	  w = new double[img.size()][control.size()];
	  int counter = 0;
	  for(PVector v : img){
	    double sum_weights = 0;
	    PVector sum_weights_per_p = new PVector(0,0,0);
	    PVector p_star;
	    for(int k = 0; k < control.size(); k++){
	        Vec aux = Deformation.original_fig.coordinatesOfFrom(new Vec(0,0,0),control.get(k)); 
	        //Vec aux = control.get(k).inverseCoordinatesOf(new Vec(0,0,0));
	        PVector pk = new PVector(aux.x(), aux.y(),aux.z()); 
	        double den = (PVector.dist(v, pk)*PVector.dist(v, pk));
	        den = den < 0.00000000001 ? 0.00000000001 : den;
	        w[counter][k] = 1/den;
	        sum_weights += w[counter][k]; 
	        sum_weights_per_p.x = sum_weights_per_p.x  + (float)w[counter][k]*pk.x;  
	        sum_weights_per_p.y = sum_weights_per_p.y  + (float)w[counter][k]*pk.y;  
	        sum_weights_per_p.z = sum_weights_per_p.z  + (float)w[counter][k]*pk.z;  
	    }
	    p_star = PVector.mult(sum_weights_per_p, 1.0f/(float)sum_weights);
	    PVector v_minus_p_s = PVector.sub(v,p_star);    
	    for(int i = 0; i < control.size(); i++){
	      double[][] pt_per_wp = new double[3][3]; 
	      for(int j = 0; j < control.size(); j++){
	        Vec aux = Deformation.original_fig.coordinatesOfFrom(new Vec(0,0,0),control.get(j)); 
	        //Vec aux = control.get(j).inverseCoordinatesOf(new Vec(0,0,0));
	        PVector pj = new PVector(aux.x(), aux.y(),aux.z()); 
	        PVector p_hat_j = PVector.sub(pj,p_star);
	        pt_per_wp[0][0] += w[counter][j]*p_hat_j.x*p_hat_j.x;            
	        pt_per_wp[0][1] += w[counter][j]*p_hat_j.y*p_hat_j.x;            
	        pt_per_wp[0][2] += w[counter][j]*p_hat_j.z*p_hat_j.x;            
	        pt_per_wp[1][0] += w[counter][j]*p_hat_j.x*p_hat_j.y;            
	        pt_per_wp[1][1] += w[counter][j]*p_hat_j.y*p_hat_j.y;            
	        pt_per_wp[1][2] += w[counter][j]*p_hat_j.z*p_hat_j.y;            
	        pt_per_wp[2][0] += w[counter][j]*p_hat_j.x*p_hat_j.z;            
	        pt_per_wp[2][1] += w[counter][j]*p_hat_j.y*p_hat_j.z;            
	        pt_per_wp[2][2] += w[counter][j]*p_hat_j.z*p_hat_j.z;            
	      }   
	      //Vec aux = control.get(i).position();
	      Vec aux = Deformation.original_fig.coordinatesOfFrom(new Vec(0,0,0),control.get(i)); 
	      //Vec aux = control.get(i).inverseCoordinatesOf(new Vec(0,0,0));
	      PVector pi = new PVector(aux.x(), aux.y(),aux.z()); 
	      PVector p_hat_i = PVector.sub(pi,p_star);
	      //inverse
	      float[][] inv_pt_per_wp = papaya.Mat.inverse(Cast.doubleToFloat(pt_per_wp));     
	      double[] Ai_1 = new double[3];
	      Ai_1[0]= (v_minus_p_s.x * inv_pt_per_wp[0][0]) + (v_minus_p_s.y * inv_pt_per_wp[0][1]) + (v_minus_p_s.z * inv_pt_per_wp[0][2]); 
	      Ai_1[1]= (v_minus_p_s.x * inv_pt_per_wp[1][0]) + (v_minus_p_s.y * inv_pt_per_wp[1][1]) + (v_minus_p_s.z * inv_pt_per_wp[1][2]); 
	      Ai_1[2]= (v_minus_p_s.x * inv_pt_per_wp[2][0]) + (v_minus_p_s.y * inv_pt_per_wp[2][1]) + (v_minus_p_s.z * inv_pt_per_wp[2][2]); 

	      A[counter][i] = Ai_1[0] * p_hat_i.x * w[counter][i] + Ai_1[1] * p_hat_i.y * w[counter][i] + Ai_1[2] * p_hat_i.z * w[counter][i];    
	    }
	    counter++;
	  }
	}

	public static ArrayList<PVector> calculateNewImage(ArrayList<PVector> img, ArrayList<Utilities.ControlPoint> out_control){
	  if(out_control.size() < 4) return img;
	  //testingpurposes
	  int num_id = 0;
	  int num_no_id = 0;
	  int same = 0;
	  //println("begincontrol");
	  //print("[");
	  for(int i = 0; i < Deformation.control_points.size(); i++){
	    //print(control_points.get(i) + ", ");
	    Vec ci = Deformation.original_fig.coordinatesOfFrom(new Vec(0,0,0),Deformation.control_points.get(i));   
	    //Vec ci = control_points.get(i).position();
	    //Vec ci = control_points.get(i).inverseCoordinatesOf(new Vec(0,0,0));
	    Vec cf = Deformation.original_fig.coordinatesOfFrom(Deformation.control_points.get(i).B,Deformation.control_points.get(i));   
	    //Vec cf = Vec.add(control_points.get(i).B, ci); 
	    if(ci.x() == cf.x() && ci.y() == cf.y() && ci.z() == cf.z()){
	          same++;
	    }
	  }
	  
	  ArrayList<PVector> dest = new ArrayList<PVector>();
	  int counter = 0;
	  for(PVector v : img){
	    double sum_weights = 0;
	    PVector sum_weights_per_q = new PVector(0,0,0);
	    PVector q_star;
	    for(int k = 0; k < out_control.size(); k++){
	        //Vec out_c = Vec.add(control_points.get(k).B, control_points.get(k).position());
	        Vec out_c = Deformation.original_fig.coordinatesOfFrom(
	        		Deformation.control_points.get(k).B,Deformation.control_points.get(k));
	        //Vec out_c = control_points.get(k).inverseCoordinatesOf(control_points.get(k).B);
	        PVector qk = new PVector(out_c.x(), out_c.y(), out_c.z());
	        sum_weights += w[counter][k]; 
	        sum_weights_per_q.x = sum_weights_per_q.x  + (float)w[counter][k]*qk.x;  
	        sum_weights_per_q.y = sum_weights_per_q.y  + (float)w[counter][k]*qk.y;  
	        sum_weights_per_q.z = sum_weights_per_q.z  + (float)w[counter][k]*qk.z;  
	    }
	    q_star = PVector.mult(sum_weights_per_q, 1.0f/(float)sum_weights);
	    PVector sum_A_q_j = new PVector (0,0,0);
	    for(int j = 0; j < out_control.size(); j++){
	        Vec out_c = Deformation.original_fig.coordinatesOfFrom(
	        		Deformation.control_points.get(j).B,Deformation.control_points.get(j));
	        //Vec out_c = Vec.add(control_points.get(j).B, control_points.get(j).position());
	        //Vec out_c =  control_points.get(j).inverseCoordinatesOf(control_points.get(j).B); 
	        PVector qj = new PVector(out_c.x(), out_c.y(), out_c.z());
	        PVector q_hat_j = PVector.sub(qj,q_star);
	        sum_A_q_j.x += A[counter][j]*q_hat_j.x;  
	        sum_A_q_j.y += A[counter][j]*q_hat_j.y;  
	        sum_A_q_j.z += A[counter][j]*q_hat_j.z;  
	    }
	    PVector f_a_v = PVector.add(sum_A_q_j, q_star);
	    //testingpurposes
	    if(Math.abs(v.x - f_a_v.x) < 0.0001 &&
	    	Math.abs(v.y - f_a_v.y) < 0.0001  && 
	    	Math.abs(v.z - f_a_v.z) < 0.0001 ){
	          num_id++;
	    }else{
	      num_no_id++;
	    }
	    
	    dest.add(f_a_v);
	    counter++;
	  }
	  //testingpurposes
	  //println("id : " + num_id);
	  //println("no id : " + num_no_id);
	  //---
	  return dest;
	}

	public static void updateControlPoints(){
	  if(Deformation.control_points.size() < 4) return;
	  getA(Deformation.vertices,Deformation.control_points);
	}

	public static void updateControlPoints(ArrayList<PVector> img){
	  if(Deformation.control_points.size() < 4) return;  
	  getA(img,Deformation.control_points);
	}

	//SOME PREDEFINED DEFORMATIONS
	public static void addControlPointsAuto(boolean rand){
	  //clear
	  Deformation.control_points.clear();
	  for(int i = 0; i < Deformation.vertices.size(); i+= Deformation.step_per_point){
	    //get coordinates in local frame
	    //control_points.add(edges.get(i));
	    if(!rand){
	      Utilities.ControlPoint cp = new Utilities.ControlPoint(Deformation.main_scene, 
	    		  Deformation.original_fig, new Vec(Deformation.vertices.get(i).x,
	    				  Deformation.vertices.get(i).y,Deformation.vertices.get(i).z));
	      Deformation.control_points.add(cp);
	    }else{
	      PVector v = Deformation.vertices.get(i);
	      PVector new_v = new PVector(v.x - Deformation.r_center.x(), v.y - Deformation.r_center.y(),
	    		  v.z - Deformation.r_center.z());                                          
	      new_v.mult((float)Math.random() + 1.f);
	      new_v.add(v);
	      Utilities.ControlPoint cp = new Utilities.ControlPoint(Deformation.main_scene, 
	    		  Deformation.original_fig, new Vec(new_v.x,new_v.y,new_v.z));
	      Deformation.control_points.add(cp);
	      float r_out_x = (int)(Math.random()*100) % 2 == 0 ? (float)(Math.random()*(0.5) + 1) : (float)(-1*(Math.random()*(0.5) + 1));
	      float r_out_y = (int)(Math.random()*100) % 2 == 0 ? (float)(Math.random()*(0.5) + 1) : (float)(-1*(Math.random()*(0.5) + 1));
	      float r_out_z = (int)(Math.random()*100) % 2 == 0 ? (float)(Math.random()*(0.5) + 1) : (float)(-1*(Math.random()*(0.5) + 1));
	      cp.setB( new Vec(new_v.x*r_out_x, new_v.y*r_out_y, new_v.z*r_out_z));
	    }
	  }  
	}

	public static void scaleX(boolean clear){
	  //clear
	  if(clear){
		  Deformation.control_points.clear();
	  }  
	  Vec[] r_bounds = Deformation.r_bounds;
	  float r_width = r_bounds[1].x() - r_bounds[0].x();
	  if(r_width < 0) r_width = -1*r_width;
	  //two parallel faces surrounding the width of the shape
	  Vec[] f1 = new Vec[4];
	  //top left
	  f1[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f1[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z());
	  //top right
	  f1[2] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z());
	  //bottom right
	  f1[3] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f1_c  = new Vec(r_bounds[0].x(),(r_bounds[0].y()+r_bounds[1].y())/2.f, (r_bounds[0].z()+r_bounds[1].z())/2.f);  

	  Vec[] f2 = new Vec[4];
	  //top left
	  f2[0] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f2[1] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
	  //top right
	  f2[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
	  //bottom right
	  f2[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f2_c  = new Vec(r_bounds[1].x(),(r_bounds[0].y()+r_bounds[1].y())/2.f, (r_bounds[0].z()+r_bounds[1].z())/2.f);  
	  Random r = new Random();
	  Vec movement = new Vec((r_width/8)*(float)r.nextGaussian(), 0,0);
	  Vec new_f1_c = Vec.add(new Vec(0,0,0), movement);
	  Vec new_f2_c = Vec.subtract(new Vec(0,0,0), movement);
	  
	  for(int i = 0; i < 4; i++){
	    Vec new_f1 = Vec.add(new Vec(0,0,0), movement);
	    Vec new_f2 = Vec.subtract(new Vec(0,0,0), movement);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(
	    		Deformation.main_scene, Deformation.original_fig, f1[i]);
	    cp.setB( new_f1);
	    Deformation.control_points.add(cp);
	    Utilities.ControlPoint cp2 = new Utilities.ControlPoint(
	    		Deformation.main_scene, Deformation.original_fig, f2[i]);
	    cp2.setB( new_f2);
	    Deformation.control_points.add(cp2);
	  }
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(
			  Deformation.main_scene, Deformation.original_fig, f1_c);
	  Deformation.control_points.add(cp);
	  cp.setB( new_f1_c);
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(
			  Deformation.main_scene, Deformation.original_fig, f2_c);
	  Deformation.control_points.add(cp2);
	  cp2.setB( new_f2_c);
	}

	public static void scaleY(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
	  
	  if(clear){
	    control_points.clear();
	  }  
	  float r_width = r_bounds[1].y() - r_bounds[0].y();
	  if(r_width < 0) r_width = -1*r_width;
	  //two parallel faces surrounding the height of the shape
	  Vec[] f1 = new Vec[4];
	  //top left
	  f1[0] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z()); 
	  //bottom left
	  f1[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
	  //top right
	  f1[2] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
	  //bottom right
	  f1[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f1_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, r_bounds[0].y(), (r_bounds[0].z()+r_bounds[1].z())/2.f);  
	  Vec[] f2 = new Vec[4];
	  //top left
	  f2[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f2[1] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z());
	  //top right
	  f2[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z());
	  //bottom right
	  f2[3] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
	  Vec f2_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, r_bounds[1].y(), (r_bounds[0].z()+r_bounds[1].z())/2.f);  
	  Random random = new Random();
	  Vec movement = new Vec(0, (r_width/8)*(float)random.nextGaussian(), 0);
	  Vec new_f1_c = Vec.add(new Vec(0,0,0), movement);
	  Vec new_f2_c = Vec.subtract(new Vec(0,0,0), movement);
	  
	  for(int i = 0; i < 4; i++){
	    Vec new_f1 = Vec.add(new Vec(0,0,0), movement);
	    Vec new_f2 = Vec.subtract(new Vec(0,0,0), movement);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, f1[i]);
	    control_points.add(cp);
	    cp.setB( new_f1);
	    Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, f2[i]);
	    control_points.add(cp2);
	    cp2.setB( new_f2);
	  }
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, f1_c);
	  control_points.add(cp);
	  cp.setB( new_f1_c);
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, f2_c);
	  control_points.add(cp2);
	  cp2.setB( new_f2_c);
	}

	public static void scaleZ(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
		
	  if(clear){
	    control_points.clear();
	  }  
	  float r_width = r_bounds[1].z() - r_bounds[0].z();
	  if(r_width < 0) r_width = -1*r_width;
	  //two parallel faces surrounding the height of the shape
	  Vec[] f1 = new Vec[4];
	  //top left
	  f1[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[0].z()); 
	  //bottom left
	  f1[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[0].z());
	  //top right
	  f1[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[0].z());
	  //bottom right
	  f1[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[0].z());
	  Vec f1_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, (r_bounds[0].y()+r_bounds[1].y())/2.f, r_bounds[0].z());  
	  Vec[] f2 = new Vec[4];
	  //top left
	  f2[0] = new Vec(r_bounds[0].x(), r_bounds[1].y(), r_bounds[1].z()); 
	  //bottom left
	  f2[1] = new Vec(r_bounds[0].x(), r_bounds[0].y(), r_bounds[1].z());
	  //top right
	  f2[2] = new Vec(r_bounds[1].x(), r_bounds[1].y(), r_bounds[1].z());
	  //bottom right
	  f2[3] = new Vec(r_bounds[1].x(), r_bounds[0].y(), r_bounds[1].z());
	  Vec f2_c  = new Vec((r_bounds[0].x()+r_bounds[1].x())/2.f, (r_bounds[0].y()+r_bounds[1].y())/2.f, r_bounds[1].z());  
	  Random random = new Random();
	  Vec movement = new Vec(0,0, (r_width/8)*(float)random.nextGaussian());
	  Vec new_f1_c = Vec.add(new Vec(0,0,0), movement);
	  Vec new_f2_c = Vec.subtract(new Vec(0,0,0), movement);
	  
	  for(int i = 0; i < 4; i++){
	    Vec new_f1 = Vec.add(new Vec(0,0,0), movement);
	    Vec new_f2 = Vec.subtract(new Vec(0,0,0), movement);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, f1[i]);
	    control_points.add(cp);
	    cp.setB( new_f1);
	    Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, f2[i]);
	    control_points.add(cp2);
	    cp2.setB( new_f2);
	  }
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, f1_c);
	  control_points.add(cp);
	  cp.setB( new_f1_c);
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, f2_c);
	  control_points.add(cp2);
	  cp2.setB( new_f2_c);
	}

	public static void applyHorizontalZXSpline(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
		
	  if(clear){
	    control_points.clear();
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float x_pos = min_x + i*(r_w/(quantity-1));
	      float y_mode = min_y; 
	      float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, y_pos, (max_z + min_z)/2.f));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float y_mode = min_y; 
	  float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, y_pos, point.z));
	    control_points.add(cp);
	    cp.setB( new Vec(0, spline_control.get(c).y - y_pos, 0));
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_y_mode = min_y + r_h;
	  y_pos = -(y_pos - y_mode) + inv_y_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, y_pos, point.z));
	    control_points.add(cp);
	    point.y = -(point.y - y_mode) + inv_y_mode;
	    cp.setB( new Vec(0, point.y - y_pos, 0));
	  }
	  //add 2 anchor points
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, min_z));
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, max_z));  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	}

	public static void applyVerticalZXSpline(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
		
	  if(clear){
	    control_points.clear();
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float z_pos = min_z + i*(r_l/(quantity-1));
	      float y_mode = min_y; 
	      float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor); 
	      spline_control.add(new PVector((max_x + min_x)/2.f, y_pos, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float y_mode = min_y; 
	  float y_pos = y_mode + Utilities.random(-r_h*1.f/mod_factor, r_h*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, y_pos, point.z));
	    control_points.add(cp);
	    cp.setB( new Vec(0, spline_control.get(c).y - y_pos, 0));
	    c++;
	  }

	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_y_mode = min_y + r_h;
	  y_pos = -(y_pos - y_mode) + inv_y_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, y_pos, point.z));
	    control_points.add(cp);
	    point.y = -(point.y - y_mode) + inv_y_mode;
	    cp.setB( new Vec(0, point.y - y_pos, 0));
	  }
	  //add 2 anchor points
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(min_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f));
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, new Vec(max_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f));  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	}



	public static void applyHorizontalYZSpline(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
	  if(clear){
	    control_points.clear();
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float z_pos = min_z + i*(r_l/(quantity-1));
	      float x_mode = min_x; 
	      float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, (max_y + min_y)/2.f, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float x_mode = min_x; 
	  float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(x_pos, point.y, point.z));
	    control_points.add(cp);
	    cp.setB( new Vec(spline_control.get(c).x - x_pos, 0, 0));
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_x_mode = min_x + r_w;
	  x_pos = -(x_pos - x_mode) + inv_x_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(x_pos, point.y, point.z));
	    control_points.add(cp);
	    point.x = -(point.x - x_mode) + inv_x_mode;
	    cp.setB( new Vec(point.x - x_pos, 0,0));
	  }
	  //add 2 anchor points
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, min_y, (min_z + max_z)/2.f));
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, max_y, (min_z + max_z)/2.f));  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	}

	public static void applyVerticalYZSpline(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
	  if(clear){
	    control_points.clear();
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float y_pos = min_y + i*(r_h/(quantity-1));
	      float x_mode = min_x; 
	      float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, y_pos, (max_z + min_z)/2.f));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float x_mode = min_x; 
	  float x_pos = x_mode + Utilities.random(-r_w*1.f/mod_factor, r_w*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(x_pos, point.y, point.z));
	    control_points.add(cp);
	    cp.setB( new Vec(spline_control.get(c).x - x_pos, 0,0));
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_x_mode = min_x + r_w;
	  x_pos = -(x_pos - x_mode) + inv_x_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(x_pos, point.y, point.z));
	    control_points.add(cp);
	    point.x = -(point.x - x_mode) + inv_x_mode;
	    cp.setB( new Vec(point.x - x_pos, 0,0));
	  }
	  //add 2 anchor points
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, min_z));
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, (min_y + max_y)/2.f, max_z));  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	}

	public static void applyVerticalXYSpline(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
  	  if(clear){
	    control_points.clear();
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float y_pos = min_y + i*(r_h/(quantity-1));
	      float z_mode = min_z; 
	      float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor); 
	      spline_control.add(new PVector((max_x + min_x)/2.f, y_pos, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float z_mode = min_z; 
	  float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, point.y, z_pos));
	    control_points.add(cp);
	    cp.setB( new Vec(0, 0, spline_control.get(c).z - z_pos));
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_z_mode = min_z + r_l;
	  z_pos = -(z_pos - z_mode) + inv_z_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, point.y, z_pos));
	    control_points.add(cp);
	    point.z = -(point.z - z_mode) + inv_z_mode;
	    cp.setB( new Vec(0, 0, point.z - z_pos));
	  }
	  //add 2 anchor points
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(min_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f));
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, new Vec(max_x, (min_y + max_y)/2.f, (min_z + max_z)/2.f));  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	}

	public static void applyHorizontalXYSpline(boolean clear){
	  //clear
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
	  Vec[] r_bounds = Deformation.r_bounds;
	  InteractiveFrame original_fig = Deformation.original_fig;
	  Scene main_scene = Deformation.main_scene;
		
	  if(clear){
	    control_points.clear();
	  }
	  ArrayList<PVector> spline_control = new ArrayList<PVector>();

	  int quantity = 12;
	  float e = 0.5f; //get a new point for each 2 control points
	  float t = 0;
	  float min_x = r_bounds[0].x();
	  float min_y = r_bounds[0].y();
	  float min_z = r_bounds[0].z();
	  float max_x = r_bounds[1].x();
	  float max_y = r_bounds[1].y();
	  float max_z = r_bounds[1].z();
	  float r_w = (r_bounds[1].x() - r_bounds[0].x());  
	  float r_h = (r_bounds[1].y() - r_bounds[0].y());  
	  float r_l = (r_bounds[1].z() - r_bounds[0].z());  

	  for(int i = 0; i < quantity; i++){
	      float x_pos = min_x + i*(r_w/(quantity-1));
	      float z_mode = min_z; 
	      float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor); 
	      spline_control.add(new PVector(x_pos, (max_y + min_y)/2.f, z_pos));
	  }
	  spline_control = Splines.drawCurve(spline_control, t, e, false);
	  //apply the same transformation to all the points
	  float z_mode = min_z; 
	  float z_pos = z_mode + Utilities.random(-r_l*1.f/mod_factor, r_l*1.f/mod_factor);
	  int c = 0;
	  for(PVector point : spline_control){
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, point.y, z_pos));
	    control_points.add(cp);
	    cp.setB( new Vec(0,0, spline_control.get(c).z - z_pos));
	    c++;
	  }
	  //put the same calculated points in the oposite place
	  //apply the same transformation to all the points
	  float inv_z_mode = min_z + r_l;
	  z_pos = -(z_pos - z_mode) + inv_z_mode; 
	  for(int i = 0; i < spline_control.size(); i++){
	    PVector point = new PVector(spline_control.get(i).x, spline_control.get(i).y, spline_control.get(i).z);
	    Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec(point.x, point.y, z_pos));
	    control_points.add(cp);
	    point.z = -(point.z - z_mode) + inv_z_mode;
	    cp.setB( new Vec(0,0, point.z - z_pos));
	  }
	  //add 2 anchor points
	  Utilities.ControlPoint cp = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, min_y, (min_z + max_z)/2.f));
	  Utilities.ControlPoint cp2 = new Utilities.ControlPoint(main_scene, original_fig, new Vec((min_x + max_x)/2.f, max_y, (min_z + max_z)/2.f));  
	  control_points.add(cp);
	  control_points.add(cp2);
	  cp.setB( new Vec(0,0,0));
	  cp2.setB( new Vec(0,0,0));
	}

	public static void combination(){
	  ArrayList<Utilities.ControlPoint> control_points = Deformation.control_points;
  	  ArrayList<PVector> new_img = new ArrayList<PVector>();
	  new_img.addAll(Deformation.vertices);
	  //splineht
	  applyHorizontalYZSpline(true);
	  updateControlPoints(new_img);
	  new_img = calculateNewImage(new_img,control_points);
	  //splines
	  applyVerticalZXSpline(true);
	  updateControlPoints(new_img);
	  new_img = calculateNewImage(new_img,control_points);
	  applyVerticalXYSpline(true);
	  updateControlPoints(new_img);
	  new_img = calculateNewImage(new_img,control_points);
	  //scalew
	  scaleX(true);
	  updateControlPoints(new_img);
	  new_img = calculateNewImage(new_img,control_points);
	  //scaleh
	  scaleY(true);
	  updateControlPoints(new_img);
	  new_img = calculateNewImage(new_img,control_points);
	  //scalel
	  scaleZ(true);
	  updateControlPoints(new_img);
	  new_img = calculateNewImage(new_img,control_points);
	  //modify the shape
	  Deformation.deformed_vertices = new_img;
	  Utilities.setVertices(Deformation.deformed_figure, Deformation.deformed_vertices);
	}
}
