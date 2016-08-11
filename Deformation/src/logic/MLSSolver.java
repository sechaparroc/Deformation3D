package logic;
import java.util.ArrayList;

import data.ControlPoint;
import data.Vertex;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import processing.core.PVector;
import remixlab.dandelion.geom.Vec;
import utilities.Utilities;

public class MLSSolver {
	//Based on [Schaefer] : Image Deformation Using Moving Least Squares
	//First Approach: Affine Transformations
	private double[][] A;
	private double[][] w;
	
	public void getA(ArrayList<Vertex> img, ArrayList<ControlPoint> control){
	  A = new double[img.size()][control.size()];
	  w = new double[img.size()][control.size()];
	  int counter = 0;
	  for(Vertex vertex : img){
		PVector v = vertex.getPos();  
	    double sum_weights = 0;
	    PVector sum_weights_per_p = new PVector(0,0,0);
	    PVector p_star;
	    for(int k = 0; k < control.size(); k++){
	        Vec aux = Utilities.PVectorToVec(control.get(k).getA()); 
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
	    	Vec aux = Utilities.PVectorToVec(control.get(j).getA());  
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
	      Vec aux = Utilities.PVectorToVec(control.get(i).getA());	      
	      PVector pi = new PVector(aux.x(), aux.y(),aux.z()); 
	      PVector p_hat_i = PVector.sub(pi,p_star);
	      //inverse
	      
  		  DenseMatrix M  = new DenseMatrix(pt_per_wp);
		  DenseMatrix I  = Matrices.identity(3);
		  DenseMatrix inv_pt_per_wp = new DenseMatrix(3, 3);
		  M.solve(I, inv_pt_per_wp);			
	      double[] Ai_1 = new double[3];
	      Ai_1[0]= (v_minus_p_s.x * inv_pt_per_wp.get(0, 0)) + (v_minus_p_s.y * inv_pt_per_wp.get(0, 1)) + (v_minus_p_s.z * inv_pt_per_wp.get(0, 2)); 
	      Ai_1[1]= (v_minus_p_s.x * inv_pt_per_wp.get(1, 0)) + (v_minus_p_s.y * inv_pt_per_wp.get(1, 1)) + (v_minus_p_s.z * inv_pt_per_wp.get(1, 2)); 
	      Ai_1[2]= (v_minus_p_s.x * inv_pt_per_wp.get(2, 0)) + (v_minus_p_s.y * inv_pt_per_wp.get(2, 1)) + (v_minus_p_s.z * inv_pt_per_wp.get(2, 2)); 
	      A[counter][i] = Ai_1[0] * p_hat_i.x * w[counter][i] + Ai_1[1] * p_hat_i.y * w[counter][i] + Ai_1[2] * p_hat_i.z * w[counter][i];    
	    }
	    counter++;
	  }
	}

	public void calculateNewImage(ArrayList<Vertex> out, ArrayList<ControlPoint> out_control){
      System.out.println("+*+ENTRAAAAAAA");
	  if(out_control.size() < 4) return;
	  int counter = 0;
	  for(Vertex vertex : out){
		PVector i = vertex.getPos().copy();	  
    	System.out.println("--ENTRAAAAAAA  Vec: " + i );
		double sum_weights = 0;
	    PVector sum_weights_per_q = new PVector(0,0,0);
	    PVector q_star;
	    for(int k = 0; k < out_control.size(); k++){
	    	Vec out_c = Utilities.PVectorToVec(out_control.get(k).getB());	    	
	        PVector qk = new PVector(out_c.x(), out_c.y(), out_c.z());
	        sum_weights += w[counter][k]; 
	        sum_weights_per_q.x = sum_weights_per_q.x  + (float)w[counter][k]*qk.x;  
	        sum_weights_per_q.y = sum_weights_per_q.y  + (float)w[counter][k]*qk.y;  
	        sum_weights_per_q.z = sum_weights_per_q.z  + (float)w[counter][k]*qk.z;  
	    }
	    q_star = PVector.mult(sum_weights_per_q, 1.0f/(float)sum_weights);
	    PVector sum_A_q_j = new PVector (0,0,0);
	    for(int j = 0; j < out_control.size(); j++){
	    	Vec out_c = Utilities.PVectorToVec(out_control.get(j).getB());	    	
	        PVector qj = new PVector(out_c.x(), out_c.y(), out_c.z());
	        PVector q_hat_j = PVector.sub(qj,q_star);
	        sum_A_q_j.x += A[counter][j]*q_hat_j.x;  
	        sum_A_q_j.y += A[counter][j]*q_hat_j.y;  
	        sum_A_q_j.z += A[counter][j]*q_hat_j.z;  
	    }
	    PVector f_a_v = PVector.add(sum_A_q_j, q_star);
	    vertex.setPos(f_a_v);
    	System.out.println("--ENTRAAAAAAA");
	    if(!i.equals(vertex.getPos())){
	    	System.out.println("--or : " + i);
	    	System.out.println("--df : " + vertex.getPos());
	    }
	    counter++;
	  }
	}

	public void updateControlPoints(ArrayList<Vertex> vertices, ArrayList<ControlPoint> control_points){
	  if(control_points.size() < 4) return;
	  getA(vertices,control_points);
	}
}
