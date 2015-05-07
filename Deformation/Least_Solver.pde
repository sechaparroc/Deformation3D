//Based on [Schaefer] : Image Deformation Using Moving Least Squares
//First Approach: Affine Transformations
double[][] A;
double[][] w;
void getA(ArrayList<PVector> img, ArrayList<PVector> control){
  A = new double[img.size()][control.size()];
  w = new double[img.size()][control.size()];
  int counter = 0;
  for(PVector v : img){
    double sum_weights = 0;
    PVector sum_weights_per_p = new PVector(0,0,0);
    PVector p_star;
    for(int k = 0; k < control.size(); k++){
        PVector pk = control.get(k); 
        double den = (PVector.dist(v, pk)*PVector.dist(v, pk));
        den = den < 0.00000000001 ? 0.00000000001 : den;
        w[counter][k] = 1/den;
        sum_weights += w[counter][k]; 
        sum_weights_per_p.x = sum_weights_per_p.x  + (float)w[counter][k]*pk.x;  
        sum_weights_per_p.y = sum_weights_per_p.y  + (float)w[counter][k]*pk.y;  
        sum_weights_per_p.z = sum_weights_per_p.z  + (float)w[counter][k]*pk.z;  
    }
    p_star = PVector.mult(sum_weights_per_p, 1.0/(float)sum_weights);
    PVector v_minus_p_s = PVector.sub(v,p_star);    
    for(int i = 0; i < control.size(); i++){
      double[][] pt_per_wp = new double[3][3]; 
      for(int j = 0; j < control.size(); j++){
        PVector pj = control.get(j); 
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
      PVector pi = control.get(i); 
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

ArrayList<PVector> calculateNewImage(ArrayList<PVector> img, ArrayList<PVector> out_control){
  if(out_control.size() < 4) return img;
  //testingpurposes
  int num_id = 0;
  int num_no_id = 0;
  int same = 0;
  println("begincontrol");
  print("[");
  for(int i = 0; i < control_points.size(); i++){
    print(control_points.get(i) + ", ");  
    if(control_points.get(i).x == control_points_out.get(i).x &&
        control_points.get(i).y == control_points_out.get(i).y && 
        control_points.get(i).z == control_points_out.get(i).z){
          same++;
        }
  }
  print("]");
  println("endcontrol");
  println("begincontrolout");
  print("[");
  for(int i = 0; i < control_points.size(); i++){
    print(control_points_out.get(i) + ", ");  
  }
  print("]");
  println("endcontrolout");
  println("same : " + same);
  println("total : " + control_points.size());
  //---------------

  
  ArrayList<PVector> dest = new ArrayList<PVector>();
  int counter = 0;
  for(PVector v : img){
    double sum_weights = 0;
    PVector sum_weights_per_q = new PVector(0,0,0);
    PVector q_star;
    for(int k = 0; k < out_control.size(); k++){
        PVector qk = out_control.get(k); 
        sum_weights += w[counter][k]; 
        sum_weights_per_q.x = sum_weights_per_q.x  + (float)w[counter][k]*qk.x;  
        sum_weights_per_q.y = sum_weights_per_q.y  + (float)w[counter][k]*qk.y;  
        sum_weights_per_q.z = sum_weights_per_q.z  + (float)w[counter][k]*qk.z;  
    }
    q_star = PVector.mult(sum_weights_per_q, 1.0/(float)sum_weights);
    PVector sum_A_q_j = new PVector (0,0,0);
    for(int j = 0; j < out_control.size(); j++){
        PVector qj = out_control.get(j); 
        PVector q_hat_j = PVector.sub(qj,q_star);
        sum_A_q_j.x += A[counter][j]*q_hat_j.x;  
        sum_A_q_j.y += A[counter][j]*q_hat_j.y;  
        sum_A_q_j.z += A[counter][j]*q_hat_j.z;  
    }
    PVector f_a_v = PVector.add(sum_A_q_j, q_star);
    //testingpurposes
    if(abs(v.x - f_a_v.x) < 0.0001 &&
        abs(v.y - f_a_v.y) < 0.0001  && 
        abs(v.z - f_a_v.z) < 0.0001 ){
          num_id++;
    }else{
      num_no_id++;
    }
    
    dest.add(f_a_v);
    counter++;
  }
  //testingpurposes
  println("id : " + num_id);
  println("no id : " + num_no_id);
  //---
  return dest;
}

void updateControlPoints(){
  if(control_points.size() < 4) return;
  getA(vertices,control_points);
}

void updateControlPoints(ArrayList<PVector> img){
  if(control_points.size() < 4) return;  
  getA(img,control_points);
}
