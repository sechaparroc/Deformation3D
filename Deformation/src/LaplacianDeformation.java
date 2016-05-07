import java.util.ArrayList;
import java.util.HashMap;

import processing.core.PShape;
import processing.core.PVector;
import remixlab.dandelion.core.MatrixHelper;
import remixlab.dandelion.geom.Vec;
import smile.data.Dataset;
import smile.data.SparseDataset;
import smile.data.parser.SparseDatasetParser;
import smile.math.Math;
import smile.math.SparseArray;
import smile.math.matrix.CholeskyDecomposition;
import smile.math.matrix.IMatrix;
import smile.math.matrix.LUDecomposition;
import smile.math.matrix.Matrix;
import smile.math.matrix.QRDecomposition;
import smile.math.matrix.SparseMatrix;
import smile.util.SmileUtils;

/*
 * November 19 2015
 * Sebastian Chaparro
 * 
 * This is an implementation of Laplacian Surface Deformation 
 * The paper can be found at: 
 * http://igl.ethz.ch/projects/Laplacian-mesh-processing/Laplacian-mesh-editing/laplacian-mesh-editing.pdf
 * 
 * */

public class LaplacianDeformation {
	static HashMap<PVector, Vertex> vertices;
	static ArrayList<Face> faces;	
	static ArrayList<Edge> edges;	
	static HashMap<Vertex,Anchor> anchors;
	static SparseDataset A, L, M; 
	static boolean debug = false;
	
	public static class Anchor{
		Vertex vertex;
		Utilities.ControlPoint control_point;
		PVector pos;
		int idx;//id of the control point
		
		public Anchor(Vertex vv, Utilities.ControlPoint cp, int ii){
			control_point = cp;
			vertex = vv;
			idx = ii;
		}
		
		public void updatePosition(){
		    Vec ci = Deformation.original_fig.coordinatesOfFrom(new Vec(0,0,0),control_point);   
		    Vec cf = Deformation.original_fig.coordinatesOfFrom(control_point.B,control_point);
		    PVector i = new PVector(ci.x(), ci.y(), ci.z());
		    PVector f = new PVector(cf.x(), cf.y(), cf.z());		    
		    pos = PVector.sub(f, i);
			pos.add(vertex.v);
		}
	}
	
	public static class Face{
		ArrayList<Vertex> vertices = new ArrayList<Vertex>();
	}

	public static class Edge{
		Vertex v1;
		Vertex v2;
		
		public Edge(Vertex v, Vertex u){
			v1 = v;
			v2 = u;
		}
	}
	
	public static class Vertex{
		PVector v;
		PVector d;
		ArrayList<int[]> idx_shape;
		int idx; //according to way the vertices are traversed
		ArrayList<Vertex> neighbors;		
		ArrayList<Face> faces;		
		ArrayList<Edge> edges;		
		
		public Vertex(PVector vv, int is, int ic, int idx){
			v = vv;			
			idx_shape = new ArrayList<int[]>();
			int[] idxs = new int[]{ic,is}; 
			idx_shape.add(idxs);
			this.idx = idx;
			neighbors = new ArrayList<Vertex>();
		}
		
		public void addIdxs(int is, int ic){
			int[] idxs = new int[]{ic,is}; 
			idx_shape.add(idxs);
		}
		
		public void addNeighbor(Vertex n){
			if(!neighbors.contains(n)) neighbors.add(n);
			if(!n.neighbors.contains(this)) n.neighbors.add(this);
		}
	}
	
	public static void addEdge(SparseDataset A, Vertex v1, Vertex v2){
		//The whole vetex is used as arg if its desired to use other weight scheme
		A.set(v1.idx, v2.idx, 1);
		A.set(v2.idx, v1.idx, 1);		
	}
	
	public static void setup(PShape shape){
		getNeighbors(shape);
		getLaplacian();
		anchors = new HashMap<Vertex,Anchor>();
	}
	
	public static void getNeighbors(PShape shape){
		A = new SparseDataset();
		vertices = new HashMap<PVector,Vertex>();
		edges = new ArrayList<Edge>();
		faces = new ArrayList<Face>();
		int idx = 0;
		
		for(int i = 0; i < shape.getChildCount(); i++){
			PShape child = shape.getChild(i);
			//create a new Face
			Face face = new Face();
			Vertex prev = null;
			for(int j = 0; j < child.getVertexCount(); j++){
				PVector vec = child.getVertex(j);
				if(!vertices.containsKey(vec)){
					vertices.put(vec, new Vertex(vec, j, i, idx));
					idx++;
				}else{
					Vertex v = vertices.get(vec);
					//add the idx of the face and the num
					v.addIdxs(j,i);									
				}
				Vertex v = vertices.get(vec);
				//add edge with the previous vertex
				if(prev != null){
					v.addNeighbor(prev);
					addEdge(A,v,prev);
					edges.add(new Edge(v,prev));
				}
				prev = v;
				//add the vertex to the new face
				face.vertices.add(v);
			}
			//add an edge between the last and the first vertex
			Vertex v0 = vertices.get(child.getVertex(0));
			v0.addNeighbor(prev);
			addEdge(A,v0,prev);			
			edges.add(new Edge(v0,prev));
		}
	}
	
	public static void getLaplacian(){
		int n = vertices.size();
		//M is used as the matrix to get the new positions of the vertices
		M = new SparseDataset();
		L = new SparseDataset();
		for(Vertex v_i : vertices.values()){
			double dx = v_i.v.x;
			double dy = v_i.v.y;
			double dz = v_i.v.z;			
			L.set(v_i.idx, v_i.idx, 1);
			M.set(v_i.idx, v_i.idx, 1);
			M.set(v_i.idx + n, v_i.idx + n, 1);
			M.set(v_i.idx + 2*n, v_i.idx + 2*n, 1);
			int degree = v_i.neighbors.size();
			for(Vertex v_j : v_i.neighbors){
				L.set(v_i.idx, v_j.idx, -1./degree);
				dx += -(1./degree) * v_j.v.x;
				dy += -(1./degree) * v_j.v.y;
				dz += -(1./degree) * v_j.v.z;
				M.set(v_i.idx, v_j.idx, -1./degree);
				M.set(v_i.idx + n, v_j.idx + n, -1./degree);				
				M.set(v_i.idx + 2*n, v_j.idx + 2*n, -1./degree);				
			}
			v_i.d = new PVector((float)dx, (float)dy, (float)dz);
		}
		if(debug) printMat("Laplacian", L.toArray());
		if(debug) printMat("Initial M", M.toArray());		
	}
	
	public static Vertex getNearest(PVector p){
		float min_dist = 99999;
		Vertex min = null;
		for(Vertex v : vertices.values()){
			if(PVector.dist(v.v, p) < min_dist){
				min = v;
				min_dist = PVector.dist(v.v, p);
			}
		}
		return min;
	}
	
	public static void addAnchors(ArrayList<Utilities.ControlPoint> cps){
		addAnchors(cps, false);
	}
	public static void addAnchors(ArrayList<Utilities.ControlPoint> cps, boolean reset){
		if(reset) anchors.clear();
		int i = 0;
		for(Utilities.ControlPoint cp : cps){
			addAnchor(cp,i++);			
		}
	}
	
	public static void addAnchor(Utilities.ControlPoint cp, int i){
		//get the nearest point
	    Vec ci = Deformation.original_fig.coordinatesOfFrom(new Vec(0,0,0),cp);   
	    PVector p = new PVector(ci.x(), ci.y(), ci.z());		
		Vertex v = getNearest(p);
		Anchor anchor = new Anchor(v, cp, i);
		if(anchors.containsKey(v) == false)anchors.put(v, anchor);
		int deep = 0;
		ArrayList<Vertex> queue = new ArrayList<Vertex>();
		ArrayList<Vertex> used = new ArrayList<Vertex>();
		queue.add(anchor.vertex);
		while(deep < 1){
			ArrayList<Vertex> aux_queue = new ArrayList<Vertex>();
			while(!queue.isEmpty()){
				Vertex v_i = queue.remove(0);
				used.add(v);
				for(Vertex v_j : v_i.neighbors){
					if(anchors.containsKey(v_j) == false) anchors.put(v_j,new Anchor(v_j, cp, i));
					if(!used.contains(v_j))aux_queue.add(v_j);
				}
			}
			if(debug)System.out.println(deep);
			queue.addAll(aux_queue);
			deep++;
		}
	}
	
	public static void calculateLaplacian(){
		int n = vertices.size();	
		if(debug) printMat("laplacian",L.toArray(),30,30);
		for(Vertex v_i : vertices.values()){
			int num_n = v_i.neighbors.size();			
			double[][] T_data = new double[(num_n+1)*3][7];
			int idx = 0;
            T_data[idx] = 
            		new double[]{v_i.v.x, 0, v_i.v.z, -v_i.v.y, 1, 0, 0};
			T_data[idx + num_n + 1] = 
					new double[]{v_i.v.y, -v_i.v.z, 0, v_i.v.x, 0, 1, 0};			
			T_data[idx + 2*(num_n + 1)] = 
					new double[]{v_i.v.z, v_i.v.y, -v_i.v.x, 0, 0, 0, 1};			
			idx++;
			for(Vertex v_j : v_i.neighbors){
	            T_data[idx] = 
	            		new double[]{v_j.v.x, 0, v_j.v.z, -v_j.v.y, 1, 0, 0};
				T_data[idx + num_n + 1] = 
						new double[]{v_j.v.y, -v_j.v.z, 0, v_j.v.x, 0, 1, 0};			
				T_data[idx + 2*(num_n + 1)] = 
						new double[]{v_j.v.z, v_j.v.y, -v_j.v.x, 0, 0, 0, 1};			
				idx++;
			}
			
			QRDecomposition qr = new QRDecomposition(T_data);
			//Matrix T = new Matrix(T_data);
			//qr.inverse();
			double[][] T_inv = new double[7][(num_n+1)*3];
			qr.solve(Math.eye((num_n+1)*3, (num_n+1)*3), T_inv);
			if(debug)printMat("inverse T implicit",T_inv);
			
			//get the linear transformation coefficients
			double[] s =  T_inv[0];
			double[] h1 = T_inv[1];
			double[] h2 = T_inv[2];
			double[] h3 = T_inv[3];

			//s = new double[]{-1,0,0,-1,0,0};
			//a = new double[]{0,-1,0,0,-1,0};

			//apply the transformation to laplacian coords
			double[][] T_delta = new double[3][(num_n+1)*3];
			for(int i = 0; i < T_delta[0].length; i++){
				T_delta[0][i] =   s[i] * v_i.d.x - h3[i] * v_i.d.y + h2[i] * v_i.d.z;
				T_delta[1][i] =  h3[i] * v_i.d.x +  s[i] * v_i.d.y - h1[i] * v_i.d.z;				
				T_delta[2][i] = -h2[i] * v_i.d.x + h1[i] * v_i.d.y +  s[i] * v_i.d.z;				
			}
			//Update values on M
			idx = 0;
			M.set(v_i.idx      , v_i.idx      , M.get(v_i.idx      , v_i.idx      ) - T_delta[0][idx]);
			M.set(v_i.idx +   n, v_i.idx      , M.get(v_i.idx +   n, v_i.idx      ) - T_delta[1][idx]);
			M.set(v_i.idx + 2*n, v_i.idx      , M.get(v_i.idx + 2*n, v_i.idx      ) - T_delta[2][idx]);			
			M.set(v_i.idx      , v_i.idx +   n, M.get(v_i.idx      , v_i.idx +   n) - T_delta[0][idx + num_n + 1]);
			M.set(v_i.idx +   n, v_i.idx +   n, M.get(v_i.idx +   n, v_i.idx +   n) - T_delta[1][idx + num_n + 1]);
			M.set(v_i.idx + 2*n, v_i.idx +   n, M.get(v_i.idx + 2*n, v_i.idx +   n) - T_delta[2][idx + num_n + 1]);
			M.set(v_i.idx      , v_i.idx + 2*n, M.get(v_i.idx      , v_i.idx + 2*n) - T_delta[0][idx + 2*(num_n + 1)]);
			M.set(v_i.idx +   n, v_i.idx + 2*n, M.get(v_i.idx +   n, v_i.idx + 2*n) - T_delta[1][idx + 2*(num_n + 1)]);
			M.set(v_i.idx + 2*n, v_i.idx + 2*n, M.get(v_i.idx + 2*n, v_i.idx + 2*n) - T_delta[2][idx + 2*(num_n + 1)]);

			idx++;
			for(Vertex v_j : v_i.neighbors){
				M.set(v_i.idx      , v_j.idx      , M.get(v_i.idx      , v_j.idx      ) - T_delta[0][idx]);
				M.set(v_i.idx +   n, v_j.idx      , M.get(v_i.idx +   n, v_j.idx      ) - T_delta[1][idx]);
				M.set(v_i.idx + 2*n, v_j.idx      , M.get(v_i.idx + 2*n, v_j.idx      ) - T_delta[2][idx]);			
				M.set(v_i.idx      , v_j.idx +   n, M.get(v_i.idx      , v_j.idx +   n) - T_delta[0][idx + num_n + 1]);
				M.set(v_i.idx +   n, v_j.idx +   n, M.get(v_i.idx +   n, v_j.idx +   n) - T_delta[1][idx + num_n + 1]);
				M.set(v_i.idx + 2*n, v_j.idx +   n, M.get(v_i.idx + 2*n, v_j.idx +   n) - T_delta[2][idx + num_n + 1]);
				M.set(v_i.idx      , v_j.idx + 2*n, M.get(v_i.idx      , v_j.idx + 2*n) - T_delta[0][idx + 2*(num_n + 1)]);
				M.set(v_i.idx +   n, v_j.idx + 2*n, M.get(v_i.idx +   n, v_j.idx + 2*n) - T_delta[1][idx + 2*(num_n + 1)]);
				M.set(v_i.idx + 2*n, v_j.idx + 2*n, M.get(v_i.idx + 2*n, v_j.idx + 2*n) - T_delta[2][idx + 2*(num_n + 1)]);
				idx++;
			}
		}
	}		
	
	public static ArrayList<PVector> solveLaplacian(){
		int n = vertices.size();			
		SparseDataset M = new SparseDataset();
		SparseDataset M_T = new SparseDataset();
		for(int i = 0; i < LaplacianDeformation.M.size(); i++){
			for(int j = 0; j < LaplacianDeformation.M.ncols(); j++){
				double val = LaplacianDeformation.M.get(i, j);
				M.set(i,j,val);
				M_T.set(j,i,val);					
			}
		}
		int m_dim = M.size();
		double weight = 1;
		double[] RHS = new double[m_dim + 3*anchors.size()];		
		
		for(Anchor anchor : anchors.values()){
			anchor.updatePosition();
			M.set(m_dim, anchor.vertex.idx, weight);
			M_T.set(anchor.vertex.idx, m_dim, weight);
			RHS[m_dim++] = weight*anchor.pos.x;
			M.set(m_dim, anchor.vertex.idx + n, weight);
			M_T.set(anchor.vertex.idx + n, m_dim, weight);
			RHS[m_dim++] = weight*anchor.pos.y;
			M.set(m_dim, anchor.vertex.idx + 2*n, weight);
			M_T.set(anchor.vertex.idx + 2*n, m_dim, weight);
			RHS[m_dim++] = weight*anchor.pos.z;
		}
		//Solve
		SparseMatrix MMT = M.toSparseMatrix().transpose().times(M.toSparseMatrix()); 
		Matrix LHS = new Matrix(matrixToArray(MMT), true, true);
		Matrix M_aux = new Matrix(M_T.toArray());		
		//double 
		double[] RHSS = new double[M_aux.nrows()];
		M_aux.ax(RHS, RHSS);
		if(debug)printArr("rhs " + RHS.length, RHS);
		if(debug)printArr("new rhs", RHSS);
		if(debug)printMat("m cond", M.toArray());
		double[] new_coords = new double[LHS.ncols()];	
		CholeskyDecomposition ch = LHS.cholesky();
		ch.solve(RHSS, new_coords);		
		ArrayList<PVector> new_img = new ArrayList<PVector>();
		for(int i = 0; i < n; i++){
			new_img.add(new PVector((float)new_coords[i], (float)new_coords[i+n], (float)new_coords[i+2*n]));
		}
		return new_img;		
	}
	
	static void printMat(String name, double[][] m){
		System.out.println("---------------------");
		System.out.println(name);		
		System.out.println("---------------------");		
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[0].length; j++){
				System.out.printf("%.2f" + ", " , m[i][j]);
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
	
	static double[][] matrixToArray(IMatrix m){
		double[][] result = new double[m.nrows()][m.ncols()];
		for(int i = 0; i < m.nrows(); i++){
			for(int j = 0; j < m.ncols(); j++){
				result[i][j] = m.get(i, j);	
			}
		}
		return result;
	}
}
