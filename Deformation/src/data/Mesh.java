package data;

import java.util.ArrayList;
import java.util.HashMap;

import processing.core.PVector;


public class Mesh extends Data{
	
	private ArrayList<Vertex> vertices;
	private ArrayList<Face> faces;

	public static class Face{

		private HashMap<Vertex, PVector> vertices;
		
		public Face(){
			vertices = new HashMap<Vertex, PVector>();
		}
		
		public void addVertex(Vertex v, PVector normal){
			vertices.put(v,normal);
		}
		
		public HashMap<Vertex,PVector> getVertices(){
			return vertices;
		}
	}
	
	public Mesh(ArrayList<Vertex> vertices, ArrayList<Face> faces){
		this.setVertices(vertices);
		this.setFaces(faces);
	}

	public Mesh(){
		vertices = new ArrayList<Vertex>();
		faces = new ArrayList<Face>();
	}
	
	@Override
	protected void setEvents() {
		// TODO Auto-generated method stub
		
	}

	public ArrayList<Vertex> getVertices() {
		return vertices;
	}


	public void setVertices(ArrayList<Vertex> vertices) {
		this.vertices = vertices;
	}

	public ArrayList<Face> getFaces() {
		return faces;
	}

	public void setFaces(ArrayList<Face> faces) {
		this.faces = faces;
	}

}
