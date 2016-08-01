package controller;

import java.util.ArrayList;
import java.util.HashMap;

import data.Vertex;
import interactive.Mesh;
import processing.core.PShape;
import processing.core.PVector;

public class InteractiveMeshController extends InteractiveController<data.Mesh, interactive.Mesh>{

	public InteractiveMeshController(Mesh interactive, boolean create) {
		super(interactive, create);
	}

	public InteractiveMeshController(Mesh interactive) {
		super(interactive);
	}

	@Override
	public void listen(String event, Mesh interactive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public data.Mesh interactiveToData(Mesh interactive) {
		PShape shape = interactive.shape();
		HashMap<PVector,Vertex> vertices = new HashMap<PVector,Vertex>();
		ArrayList<data.Mesh.Face> faces = new ArrayList<data.Mesh.Face>();
		int idx = 0;
		for(int i = 0; i < shape.getChildCount(); i++){
			PShape child = shape.getChild(i);
			Vertex prev = null;
			data.Mesh.Face face = new data.Mesh.Face();
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
				face.addVertex(v,child.getNormal(j));
			}
			//add an edge between the last and the first vertex
			Vertex v0 = vertices.get(child.getVertex(0));
			v0.addNeighbor(prev);
			faces.add(face);
		}
		return new data.Mesh(new ArrayList<Vertex>(vertices.values()), faces);
	}

}
