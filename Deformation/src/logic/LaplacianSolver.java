package logic;
import java.util.ArrayList;
import java.util.HashMap;

import data.ControlPoint;
import data.Vertex;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.CGS;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.IterativeSolver;
import no.uib.cipr.matrix.sparse.IterativeSolverNotConvergedException;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;
import processing.core.PShape;
import processing.core.PVector;
import remixlab.dandelion.geom.Vec;

/*
 * 
 * Sebastian Chaparro
 * 
 * This is an implementation of Laplacian Surface Deformation 
 * The paper can be found at: 
 * http://igl.ethz.ch/projects/Laplacian-mesh-processing/Laplacian-mesh-editing/laplacian-mesh-editing.pdf
 * 
 * */
/*
 * Cause the app is using multithreads >_<
 * https://github.com/xianyi/OpenBLAS/wiki/faq#multi-threaded*/


public class LaplacianSolver{
	private static int idx_anchor_count = 0;
	private LinkedSparseMatrix A, L, M;
	private boolean debug = false;
	private HashMap<Vertex,Constraint> constraints;
	
	public class Constraint{
		private Vertex vertex;
		private ControlPoint anchor;
		private PVector pos;
		private int idx;//id of the control point
		
		public Constraint(Vertex vv, ControlPoint aa){
			anchor = aa;
			vertex = vv;
			idx = idx_anchor_count++;
		}
		
		public void updatePosition(){
		    PVector i = anchor.getA();
		    PVector f = anchor.getB();		    
		    pos = PVector.sub(f, i);
			pos.add(vertex.getPos());
		}

		public Vertex getVertex() {
			return vertex;
		}

		public void setVertex(Vertex vertex) {
			this.vertex = vertex;
		}

		public ControlPoint getAnchor() {
			return anchor;
		}

		public void setAnchor(ControlPoint anchor) {
			this.anchor = anchor;
		}

		public PVector getPos() {
			return pos;
		}

		public void setPos(PVector pos) {
			this.pos = pos;
		}

		public int getIdx() {
			return idx;
		}

		public void setIdx(int idx) {
			this.idx = idx;
		}
	}
	
	
	
	public void addEdge(LinkedSparseMatrix A, Vertex v1, Vertex v2){
		//The whole vetex is used as arg if its desired to use other weight scheme
		A.set(v1.getIdx(), v2.getIdx(), 1);
		A.set(v2.getIdx(), v1.getIdx(), 1);		
	}
	
	public void setup(ArrayList<Vertex> vertices){
		getNeighbors(vertices);
		getLaplacian(vertices);
	}
	
	public void getNeighbors(ArrayList<Vertex> vertices){
		/*Create an Sparse Matrix corresponding to the adjacency matrix*/
		int n = vertices.size();
		A = new LinkedSparseMatrix(n, n);
		for(Vertex v_i : vertices){
			for(Vertex v_j : v_i.getNeighbors()){
				addEdge(A, v_i, v_j);
			}
		}
		
	}
	
	public void getLaplacian(ArrayList<Vertex> vertices){
		int n = vertices.size();
		//M is used as the matrix to get the new positions of the vertices
		M = new LinkedSparseMatrix(3*n,3*n);
		L = new LinkedSparseMatrix(n,n);
		for(Vertex v_i : vertices){
			double dx = v_i.getPos().x;
			double dy = v_i.getPos().y;
			double dz = v_i.getPos().z;			
			L.set(v_i.getIdx(), v_i.getIdx(), 1);
			M.set(v_i.getIdx(), v_i.getIdx(), 1);
			M.set(v_i.getIdx() + n, v_i.getIdx() + n, 1);
			M.set(v_i.getIdx() + 2*n, v_i.getIdx() + 2*n, 1);
			int degree = v_i.getNeighbors().size();
			for(Vertex v_j : v_i.getNeighbors()){
				L.set(v_i.getIdx(), v_j.getIdx(), -1./degree);
				dx += -(1./degree) * v_j.getPos().x;
				dy += -(1./degree) * v_j.getPos().y;
				dz += -(1./degree) * v_j.getPos().z;
				M.set(v_i.getIdx(), v_j.getIdx(), -1./degree);
				M.set(v_i.getIdx() + n, v_j.getIdx() + n, -1./degree);				
				M.set(v_i.getIdx() + 2*n, v_j.getIdx() + 2*n, -1./degree);				
			}
			v_i.addRepresentation("laplacian", new PVector((float)dx, (float)dy, (float)dz));
		}
		if(debug) printSparseMat("Laplacian", L, 30,30);
		if(debug) printSparseMat("Initial M", M, 30, 30);		
	}
	
	
	public void addAnchors(ArrayList<Vertex> vertices, ArrayList<ControlPoint> cps){
		addAnchors(vertices, cps, false);
	}
	public void addAnchors(ArrayList<Vertex> vertices, ArrayList<ControlPoint> cps, boolean reset){
		if(reset) this.constraints.clear();
		int i = 0;
		for(ControlPoint cp : cps){
			addAnchor(vertices, cp);			
		}
	}
	
	public void addAnchor(ArrayList<Vertex> vertices, ControlPoint cp){
		//get the nearest point
	    Vec ci = utilities.Utilities.PVectorToVec(cp.getA());   
	    PVector p = new PVector(ci.x(), ci.y(), ci.z());		
		Vertex v = data.Utilities.getNearest(vertices,p);
		Constraint anchor = new Constraint(v, cp);
		if(constraints.containsKey(v) == false)constraints.put(v, anchor);
		int deep = 0;
		ArrayList<Vertex> queue = new ArrayList<Vertex>();
		ArrayList<Vertex> used  = new ArrayList<Vertex>();
		queue.add(anchor.vertex);
		while(deep < 1){
			ArrayList<Vertex> aux_queue = new ArrayList<Vertex>();
			while(!queue.isEmpty()){
				Vertex v_i = queue.remove(0);
				used.add(v);
				for(Vertex v_j : v_i.getNeighbors()){
					if(constraints.containsKey(v_j) == false) 
						constraints.put(v_j,new Constraint(v_j, cp));
					if(!used.contains(v_j))aux_queue.add(v_j);
				}
			}
			if(debug)System.out.println(deep);
			queue.addAll(aux_queue);
			deep++;
		}
	}
	
	public void calculateLaplacian(HashMap<PVector,Vertex> vertices){
		int n = vertices.size();	
		if(debug) printSparseMat("laplacian",L,30,30);
		for(Vertex v_i : vertices.values()){
			int num_n = v_i.getNeighbors().size();			
			double[][] T_data = new double[(num_n+1)*3][7];
			int idx = 0;
            T_data[idx] = 
            		new double[]{v_i.getPos().x, 0, v_i.getPos().z, -v_i.getPos().y, 1, 0, 0};
			T_data[idx + num_n + 1] = 
					new double[]{v_i.getPos().y, -v_i.getPos().z, 0, v_i.getPos().x, 0, 1, 0};			
			T_data[idx + 2*(num_n + 1)] = 
					new double[]{v_i.getPos().z, v_i.getPos().y, -v_i.getPos().x, 0, 0, 0, 1};			
			idx++;
			for(Vertex v_j : v_i.getNeighbors()){
	            T_data[idx] = 
	            		new double[]{v_j.getPos().x, 0, v_j.getPos().z, -v_j.getPos().y, 1, 0, 0};
				T_data[idx + num_n + 1] = 
						new double[]{v_j.getPos().y, -v_j.getPos().z, 0, v_j.getPos().x, 0, 1, 0};			
				T_data[idx + 2*(num_n + 1)] = 
						new double[]{v_j.getPos().z, v_j.getPos().y, -v_j.getPos().x, 0, 0, 0, 1};			
				idx++;
			}
			
			DenseMatrix T  = new DenseMatrix(T_data);
			DenseMatrix I  = Matrices.identity((num_n+1)*3);
			DenseMatrix T_inv = new DenseMatrix(7, (num_n+1)*3);
			T.solve(I, T_inv);			
			
			//if(debug)printMat("inverse T implicit",T_inv);
			
			//get the linear transformation coefficients
			double[] s =  new double[T_inv.numColumns()];
			double[] h1 = new double[T_inv.numColumns()];
			double[] h2 = new double[T_inv.numColumns()];
			double[] h3 = new double[T_inv.numColumns()];

			for(int i = 0; i < T_inv.numColumns(); i++){
				s[i] = T_inv.get(0, i);
				h1[i] = T_inv.get(1, i);
				h2[i] = T_inv.get(2, i);
				h3[i] = T_inv.get(3, i);
			}
			

			//s = new double[]{-1,0,0,-1,0,0};
			//a = new double[]{0,-1,0,0,-1,0};
			//apply the transformation to laplacian coords
			double[][] T_delta = new double[3][(num_n+1)*3];
			for(int i = 0; i < T_delta[0].length; i++){
				T_delta[0][i] =   s[i] * v_i.getRepresentation("laplacian").x - h3[i] * v_i.getRepresentation("laplacian").y + h2[i] * v_i.getRepresentation("laplacian").z;
				T_delta[1][i] =  h3[i] * v_i.getRepresentation("laplacian").x +  s[i] * v_i.getRepresentation("laplacian").y - h1[i] * v_i.getRepresentation("laplacian").z;				
				T_delta[2][i] = -h2[i] * v_i.getRepresentation("laplacian").x + h1[i] * v_i.getRepresentation("laplacian").y +  s[i] * v_i.getRepresentation("laplacian").z;				
			}
			//Update values on M
			idx = 0;
			M.set(v_i.getIdx()      , v_i.getIdx()      , M.get(v_i.getIdx()      , v_i.getIdx()      ) - T_delta[0][idx]);
			M.set(v_i.getIdx() +   n, v_i.getIdx()      , M.get(v_i.getIdx() +   n, v_i.getIdx()      ) - T_delta[1][idx]);
			M.set(v_i.getIdx() + 2*n, v_i.getIdx()      , M.get(v_i.getIdx() + 2*n, v_i.getIdx()      ) - T_delta[2][idx]);			
			M.set(v_i.getIdx()      , v_i.getIdx() +   n, M.get(v_i.getIdx()      , v_i.getIdx() +   n) - T_delta[0][idx + num_n + 1]);
			M.set(v_i.getIdx() +   n, v_i.getIdx() +   n, M.get(v_i.getIdx() +   n, v_i.getIdx() +   n) - T_delta[1][idx + num_n + 1]);
			M.set(v_i.getIdx() + 2*n, v_i.getIdx() +   n, M.get(v_i.getIdx() + 2*n, v_i.getIdx() +   n) - T_delta[2][idx + num_n + 1]);
			M.set(v_i.getIdx()      , v_i.getIdx() + 2*n, M.get(v_i.getIdx()      , v_i.getIdx() + 2*n) - T_delta[0][idx + 2*(num_n + 1)]);
			M.set(v_i.getIdx() +   n, v_i.getIdx() + 2*n, M.get(v_i.getIdx() +   n, v_i.getIdx() + 2*n) - T_delta[1][idx + 2*(num_n + 1)]);
			M.set(v_i.getIdx() + 2*n, v_i.getIdx() + 2*n, M.get(v_i.getIdx() + 2*n, v_i.getIdx() + 2*n) - T_delta[2][idx + 2*(num_n + 1)]);

			idx++;
			for(Vertex v_j : v_i.getNeighbors()){
				M.set(v_i.getIdx()      , v_j.getIdx()      , M.get(v_i.getIdx()      , v_j.getIdx()      ) - T_delta[0][idx]);
				M.set(v_i.getIdx() +   n, v_j.getIdx()      , M.get(v_i.getIdx() +   n, v_j.getIdx()      ) - T_delta[1][idx]);
				M.set(v_i.getIdx() + 2*n, v_j.getIdx()      , M.get(v_i.getIdx() + 2*n, v_j.getIdx()      ) - T_delta[2][idx]);			
				M.set(v_i.getIdx()      , v_j.getIdx() +   n, M.get(v_i.getIdx()      , v_j.getIdx() +   n) - T_delta[0][idx + num_n + 1]);
				M.set(v_i.getIdx() +   n, v_j.getIdx() +   n, M.get(v_i.getIdx() +   n, v_j.getIdx() +   n) - T_delta[1][idx + num_n + 1]);
				M.set(v_i.getIdx() + 2*n, v_j.getIdx() +   n, M.get(v_i.getIdx() + 2*n, v_j.getIdx() +   n) - T_delta[2][idx + num_n + 1]);
				M.set(v_i.getIdx()      , v_j.getIdx() + 2*n, M.get(v_i.getIdx()      , v_j.getIdx() + 2*n) - T_delta[0][idx + 2*(num_n + 1)]);
				M.set(v_i.getIdx() +   n, v_j.getIdx() + 2*n, M.get(v_i.getIdx() +   n, v_j.getIdx() + 2*n) - T_delta[1][idx + 2*(num_n + 1)]);
				M.set(v_i.getIdx() + 2*n, v_j.getIdx() + 2*n, M.get(v_i.getIdx() + 2*n, v_j.getIdx() + 2*n) - T_delta[2][idx + 2*(num_n + 1)]);
				idx++;
			}
		}
	}		
	
	public void solveLaplacian(ArrayList<Vertex> output){
		int n = output.size();			
		int m_dim = M.numColumns();
		
		LinkedSparseMatrix M   = new LinkedSparseMatrix(m_dim + 3*constraints.size(), m_dim);
		LinkedSparseMatrix M_T = new LinkedSparseMatrix(m_dim, m_dim  + 3*constraints.size());
		
		for(MatrixEntry e : this.M){
			int row = e.row();
			int col = e.column();
			M.set(row, col, e.get());			
			M_T.set(col, row, e.get());			
		}
		double weight = 1;
		double[] RHS = new double[m_dim + 3*constraints.size()];
		
		for(Constraint constraint : constraints.values()){
			constraint.updatePosition();
			M.set(m_dim, constraint.vertex.getIdx(), weight);
			M_T.set(constraint.vertex.getIdx(), m_dim, weight);
			RHS[m_dim++] = weight*constraint.pos.x;
			M.set(m_dim, constraint.vertex.getIdx() + n, weight);
			M_T.set(constraint.vertex.getIdx() + n, m_dim, weight);
			RHS[m_dim++] = weight*constraint.pos.y;
			M.set(m_dim, constraint.vertex.getIdx() + 2*n, weight);
			M_T.set(constraint.vertex.getIdx() + 2*n, m_dim, weight);
			RHS[m_dim++] = weight*constraint.pos.z;
		}
		//Solve
		Vector RHS_VEC = new DenseVector(RHS);
		Vector RHSS = new DenseVector(M_T.numRows());
		LinkedSparseMatrix LHS = new LinkedSparseMatrix(M_T.numRows(), M_T.numRows());
		M_T.mult(M, LHS);
		M_T.mult(RHS_VEC, RHSS);


		if(debug)printArr("rhs " + RHS.length, RHS);
		if(debug)printArr("rhs " + RHS.length, ((DenseVector)RHS_VEC).getData());
		if(debug)printArr("new rhs", ((DenseVector)RHSS).getData());
		if(debug)printSparseMat("m cond", M, 30,30);
		if(debug)printSparseMat("LHS ", LHS, 30, 30);
		Vector new_coords = new DenseVector(LHS.numColumns());	
		CompRowMatrix LHSS = new CompRowMatrix(LHS);
		
		IterativeSolver solver = new CGS(new_coords);

		try {
		  solver.solve(LHSS, RHSS, new_coords);
		} catch (IterativeSolverNotConvergedException e) {
		  System.err.println("Iterative solver failed to converge");
		}
        
        
        if(debug)printArr("new coords: " + new_coords.size(), ((DenseVector)new_coords).getData());
	
		for(Vertex v : output){
			int i = v.getIdx();
			v.setPos(new PVector((float)new_coords.get(i), (float)new_coords.get(i+n), (float)new_coords.get(i+2*n)));
		}
	}
	
	static void printMat(String name, DenseMatrix m){
		System.out.println("---------------------");
		System.out.println(name);		
		System.out.println("---------------------");		
		for(int i = 0; i < m.numRows(); i++){
			for(int j = 0; j < m.numColumns(); j++){
				System.out.printf("%.2f" + ", " , m.get(i, j));
			}
			System.out.println();
		}
		System.out.println("---------------------");		
		System.out.println("---------------------");		
	}

	static void printMat(String name, double[][] m, int r, int c){
		System.out.println("---------------------");
		System.out.println(name);		
		System.out.println("---------------------");
		int rr = Math.min(r, m.length);
		int cc = Math.min(c, m[0].length);		
		for(int i = 0; i < rr; i++){
			for(int j = 0; j < cc; j++){
				System.out.printf("%.2f" + ", \t" , m[i][j]);
			}
			System.out.println();
		}
		System.out.println("---------------------");		
		System.out.println("---------------------");		
	}
	
	static void printSparseMat(String name, LinkedSparseMatrix m){
		System.out.println("---------------------");
		System.out.println(name);		
		System.out.println("---------------------");
		int rows = m.numRows();
		int cols = m.numColumns();		
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				System.out.printf("%.2f" + ", \t" , m.get(i, j));
			}
			System.out.println();
		}
		System.out.println("---------------------");		
		System.out.println("---------------------");		
		
	}
	
	static void printSparseMat(String name, LinkedSparseMatrix m, int r, int c){
		System.out.println("---------------------");
		System.out.println(name);		
		System.out.println("---------------------");

		int rows = Math.min(r,m.numRows());
		int cols = Math.min(c,m.numColumns());		
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				System.out.printf("%.2f" + ", \t" , m.get(i, j));
			}
			System.out.println();
		}
		System.out.println("---------------------");		
		System.out.println("---------------------");		
		
	}

	
	static void printArr(String name, double[] m){
		System.out.println("---------------------");
		System.out.println(name);		
		System.out.println("---------------------");		
		for(int i = 0; i < m.length; i++){
				System.out.printf("%.2f" + ", " , m[i]);
		}
		System.out.println("---------------------");		
		System.out.println("---------------------");		
	}
	
	static double[][] matrixMult(double[][] m1, double[][] m2){
		int n = m1.length;
		int m = m1[0].length;
		int l = m2[0].length;
		double[][] result = new double[n][l];
		for(int i = 0; i < n; i++){
			for(int k = 0; k < m; k++){
				for(int j = 0; j < l; j++){
					result[i][j] += m1[i][j] * m2[j][k]; 
				}			
			}
		}
		return result;
	}

	//Getters & Setters----------------------------
	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	//---------------------------------------------
}
