package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import processing.core.PShape;
import processing.core.PVector;

public class Utilities {
	/*Some useful functions*/
	public static Vertex getNearest(ArrayList<Vertex> vertices, PVector p){
		float min_dist = 99999;
		Vertex min = null;
		for(Vertex v : vertices){
			if(PVector.dist(v.getPos(), p) < min_dist){
				min = v;
				min_dist = PVector.dist(v.getPos(), p);
			}
		}
		return min;
	}
	
	/*get vertices and their neighbors from a given Mesh*/
	public static Collection<Vertex> getVertices(PShape shape){
		HashMap<PVector,Vertex> vertices = new HashMap<PVector,Vertex>();
		int idx = 0;
		for(int i = 0; i < shape.getChildCount(); i++){
			PShape child = shape.getChild(i);
			Vertex prev = null;
			for(int j = 0; j < child.getVertexCount(); j++){
				PVector vec = child.getVertex(j);
				if(!vertices.containsKey(vec)){
					vertices.put(vec, new Vertex(vec, j, i, idx));
					idx++;
				}else{
					Vertex v = vertices.get(vec);
					v.addIdxs(j,i);									
				}
				Vertex v = vertices.get(vec);
				//add edge with the previous vertex
				if(prev != null){
					v.addNeighbor(prev);
				}
				prev = v;
			}
			//add an edge between the last and the first vertex
			Vertex v0 = vertices.get(child.getVertex(0));
			v0.addNeighbor(prev);
		}
		return vertices.values();
	}
}
