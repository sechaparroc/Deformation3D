import java.util.ArrayList;
import java.util.HashMap;

import no.uib.cipr.matrix.DenseCholesky;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.CG;
import no.uib.cipr.matrix.sparse.CGS;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.ICC;
import no.uib.cipr.matrix.sparse.ILU;
import no.uib.cipr.matrix.sparse.IterativeSolver;
import no.uib.cipr.matrix.sparse.IterativeSolverNotConvergedException;
import no.uib.cipr.matrix.sparse.LinkedSparseMatrix;
import no.uib.cipr.matrix.sparse.OutputIterationReporter;
import no.uib.cipr.matrix.sparse.Preconditioner;
import processing.core.PShape;
import processing.core.PVector;
import remixlab.dandelion.geom.Vec;

/*
 * November 19 2015
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


public class LaplacianDeformation {
	private HashMap<PVector, Vertex> vertices;
	private ArrayList<Face> faces;	
	private ArrayList<Edge> edges;	
	private HashMap<Vertex,Anchor> anchors;
	private LinkedSparseMatrix A, L, M;
	private boolean debug = true;
	
	public static class Anchor{
		private Vertex vertex;
		private ControlPoint control_point;
		private PVector pos;
		private int idx;//id of the control point
		
		public Anchor(Vertex vv, ControlPoint cp, int ii){
			control_point = cp;
			vertex = vv;
			idx = ii;
		}
		
		public void updatePosition(){
		    Vec ci = control_point.translation();   
		    Vec cf = control_point.localInverseCoordinatesOf(control_point.getB()); 
		    PVector i = new PVector(ci.x(), ci.y(), ci.z());
		    PVector f = new PVector(cf.x(), cf.y(), cf.z());		    
		    pos = PVector.sub(f, i);
			pos.add(vertex.v);
		}

		public Vertex getVertex() {
			return vertex;
		}

		public void setVertex(Vertex vertex) {
			this.vertex = vertex;
		}

		public ControlPoint getControl_point() {
			return control_point;
		}

		public void setControl_point(ControlPoint control_point) {
			this.control_point = control_point;
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
	
	public static class Face{
		private ArrayList<Vertex> vertices = new ArrayList<Vertex>();

		public boolean addvertex(Vertex v){
			return vertices.add(v);
		}
	}

	public static class Edge{
		private Vertex v1;
		private Vertex v2;
		
		public Edge(Vertex v, Vertex u){
			v1 = v;
			v2 = u;
		}
	}
	
	public static class Vertex{
		private PVector v; //vertex position
		private PVector d; //laplacian coordinates
		private ArrayList<int[]> idx_shape;
		private int idx; //according to the way in which the vertices are traversed
		private ArrayList<Vertex> neighbors;		
		private ArrayList<Face> faces;		
		private ArrayList<Edge> edges;		
		
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

		public ArrayList<int[]> getIdx_shape() {
			return idx_shape;
		}

		public void setIdx_shape(ArrayList<int[]> idx_shape) {
			this.idx_shape = idx_shape;
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
		A.set(v1.idx, v2.idx, 1);
		A.set(v2.idx, v1.idx, 1);		
	}
	
	public void setup(PShape shape){
		getNeighbors(shape);
		getLaplacian();
		anchors = new HashMap<Vertex,Anchor>();
	}
	
	public void getNeighbors(PShape shape){
		//A = new SparseDataset();
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
					//addEdge(A,v,prev);
					edges.add(new Edge(v,prev));
				}
				prev = v;
				//add the vertex to the new face
				face.vertices.add(v);
			}
			//add an edge between the last and the first vertex
			Vertex v0 = vertices.get(child.getVertex(0));
			v0.addNeighbor(prev);
			//addEdge(A,v0,prev);			
			edges.add(new Edge(v0,prev));
		}
		/*Create an Sparse Matrix corresponding to the adjacency matrix*/
		int n = vertices.size();
		A = new LinkedSparseMatrix(n, n);
		for(Vertex v_i : vertices.values()){
			for(Vertex v_j : v_i.neighbors){
				addEdge(A, v_i, v_j);
			}
		}
		
	}
	
	public void getLaplacian(){
		int n = vertices.size();
		//M is used as the matrix to get the new positions of the vertices
		M = new LinkedSparseMatrix(3*n,3*n);
		L = new LinkedSparseMatrix(n,n);
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
		if(debug) printSparseMat("Laplacian", L, 30,30);
		if(debug) printSparseMat("Initial M", M, 30, 30);		
	}
	
	public Vertex getNearest(PVector p){
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
	
	public void addAnchors(ArrayList<ControlPoint> cps){
		addAnchors(cps, false);
	}
	public void addAnchors(ArrayList<ControlPoint> cps, boolean reset){
		if(reset) anchors.clear();
		int i = 0;
		for(ControlPoint cp : cps){
			addAnchor(cp,i++);			
		}
	}
	
	public void addAnchor(ControlPoint cp, int i){
		//get the nearest point
	    Vec ci = cp.translation();   
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
	
	public void calculateLaplacian(){
		int n = vertices.size();	
		if(debug) printSparseMat("laplacian",L,30,30);
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
	
	public ArrayList<PVector> solveLaplacian(){
		int n = vertices.size();			
		int m_dim = M.numColumns();
		
		LinkedSparseMatrix M   = new LinkedSparseMatrix(m_dim + 3*anchors.size(), m_dim);
		LinkedSparseMatrix M_T = new LinkedSparseMatrix(m_dim, m_dim  + 3*anchors.size());
		
		for(MatrixEntry e : this.M){
			int row = e.row();
			int col = e.column();
			M.set(row, col, e.get());			
			M_T.set(col, row, e.get());			
		}
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
		
		//ILU ilu = new ILU(LHSS.copy());
		//ilu.setMatrix(LHSS);
		//ilu.apply(RHSS, new_coords);
		
		IterativeSolver solver = new CGS(new_coords);
		// Create a Cholesky preconditioner
		//Preconditioner P = new ILU(LHSS.copy());
		// Set up the preconditioner, and attach it
		//P.setMatrix(LHSS);
		//solver.setPreconditioner(P);
		// Add a convergence monitor
		//solver.getIterationMonitor().setIterationReporter(new OutputIterationReporter());
		// Start the solver, and check for problems
		try {
		  solver.solve(LHSS, RHSS, new_coords);
		} catch (IterativeSolverNotConvergedException e) {
		  System.err.println("Iterative solver failed to converge");
		}
        
        
        if(debug)printArr("new coords: " + new_coords.size(), ((DenseVector)new_coords).getData());
		
		ArrayList<PVector> new_img = new ArrayList<PVector>();

		
		for(int i = 0; i < n; i++){
			new_img.add(new PVector((float)new_coords.get(i), (float)new_coords.get(i+n), (float)new_coords.get(i+2*n)));
		}
		return new_img;		
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
	public HashMap<PVector, Vertex> getVertices() {
		return vertices;
	}

	public void setVertices(HashMap<PVector, Vertex> vertices) {
		this.vertices = vertices;
	}

	public ArrayList<Face> getFaces() {
		return faces;
	}

	public void setFaces(ArrayList<Face> faces) {
		this.faces = faces;
	}

	public ArrayList<Edge> getEdges() {
		return edges;
	}

	public void setEdges(ArrayList<Edge> edges) {
		this.edges = edges;
	}

	public HashMap<Vertex, Anchor> getAnchors() {
		return anchors;
	}

	public void setAnchors(HashMap<Vertex, Anchor> anchors) {
		this.anchors = anchors;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	//---------------------------------------------
}
