package data;

import java.util.ArrayList;
import java.util.HashMap;
import processing.core.PVector;

public class Vertex{
	//position of the vertex according to a given Reference Frame
	private PVector pos;
	//Enables to store different kind of representations e.g (laplacian, local)
	private HashMap<String, PVector> coordinates;  
	//Store the proper pointers of the vertex associated with a given PShape
	private ArrayList<int []> idx_shape;
	private int idx;//is assigned according the way vertices are traversed
	private ArrayList<Vertex> neighbors; 
	
	public Vertex(PVector vv, int is, int ic, int idx){
		setPos(vv);			
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

	public PVector getPos() {
		return pos;
	}

	public void setPos(PVector pos) {
		this.pos = pos;
	}
	
	public ArrayList<Vertex> getNeighbors(){
		return this.neighbors;
	}
	
	public boolean hasRepresentation(String name){
		return coordinates.containsKey(name);
	}
	
	public void addRepresentation(String name, PVector v){
		coordinates.put(name, v);
	}
	
	public PVector getRepresentation(String name){
		return coordinates.get(name);
	}
}
