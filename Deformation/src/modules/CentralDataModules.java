package modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import controller.InteractiveListener;
import data.Vertex;
import interactive.Mesh;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;
import remixlab.dandelion.geom.Frame;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.Scene;


public class CentralDataModules {
	/*
	 * Here the modules will process and relate Interactive information to Data and 
	 * Data to Interactive information, Also the Behavior Modules will be feed by
	 * this modules
	 * */
	
	public static class MeshModule extends Module implements InteractiveListener<interactive.Mesh>{
		/*Process Interactive Meshes to Data Meshes and vice versa*/
		private HashMap<interactive.Mesh, data.Mesh> meshes;
		
		public MeshModule(){
			super();
			meshes = new HashMap<interactive.Mesh, data.Mesh>();
		}
		
		public void addMesh(interactive.Mesh mesh){
			if(!meshes.containsKey(mesh)) createMesh(mesh);
			mesh.attach(this);

		}
		
		public void addMesh(Scene scene, Frame f, data.Mesh mesh){
			if(!meshes.values().contains(mesh)) createMesh(scene, f, mesh);
		}

		
		public void createMesh(interactive.Mesh interactive){
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
			meshes.put(interactive, new data.Mesh(new ArrayList<Vertex>(vertices.values()), faces));
		}
		
		public void createMesh(Scene scene, Frame f, data.Mesh object){
			PApplet ap = scene.pApplet();
			PShape shape = ap.createShape(PConstants.GROUP);
			for(data.Mesh.Face face : object.getFaces()){
				PShape child = ap.createShape(PConstants.POLYGON);
				child.beginShape();
				for (Map.Entry<Vertex, PVector> entry : face.getVertices().entrySet()) {
					PVector pos = entry.getKey().getPos();
					PVector normal = entry.getValue();
					child.vertex(pos.x, pos.y, pos.z);
					child.normal(normal.x, normal.y, normal.z);
				}				
				child.endShape(PConstants.CLOSE);
				shape.addChild(child);
			}
			interactive.Mesh mesh = new interactive.Mesh(scene, f, shape); 
			meshes.put(mesh, object);
			mesh.attach(this);
		}

		@Override
		public void listen(String event, Mesh interactive) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void executeEvent(Event event) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void processOutput() {
			// TODO Auto-generated method stub
			
		}
	}

	public static class ControlPointModule extends Module implements InteractiveListener<interactive.ControlPoint> {
		/*Process Interactive control points to Data representation and vice versa
		 * It is possible that the same Data objects has multiple Interactive objects related
		 * but not the other way around.
		 * */

		private HashMap<interactive.ControlPoint, data.ControlPoint> points;
		
		public ControlPointModule(){
			super();
			points = new HashMap<interactive.ControlPoint, data.ControlPoint>();
		}
		
		public void addPoint(interactive.ControlPoint point){
			if(!points.containsKey(point)) createPoint(point);
			point.attach(this);

		}
		
		public void addPoint(Scene scene, Frame f, data.ControlPoint point){
			if(!points.values().contains(point)) createPoint(scene, f, point);
		}

		
		public void createPoint(interactive.ControlPoint interactive){
			PVector A = utilities.Utilities.vecToPVector(interactive.translation());
			PVector B = utilities.Utilities.vecToPVector(interactive.localCoordinatesOf(interactive.getB().translation()));
			points.put(interactive, new data.ControlPoint(A, B));
		}
		
		public void createPoint(Scene scene, Frame f, data.ControlPoint object){
			Vec A = utilities.Utilities.PVectorToVec(object.getA());
			Vec B = utilities.Utilities.PVectorToVec(object.getA());
			interactive.ControlPoint cp = 
					new interactive.ControlPoint(scene, f, A);
			cp.getB().translate(B);
			points.put(cp, object);
			cp.attach(this);
		}
		
		@Override
		public void listen(String event, interactive.ControlPoint interactive) {
			data.ControlPoint object = points.get(interactive);
			if(event.equalsIgnoreCase("TRANSLATE_A")){
				object.setA(utilities.Utilities.vecToPVector(interactive.translation()));
			}else if(event.equalsIgnoreCase("TRANSLATE_B")){
				object.setB(utilities.Utilities.vecToPVector(interactive.localCoordinatesOf(interactive.getB().translation())));
			}
		}

		@Override
		public void executeEvent(Event event) {
			if(event.getName().equalsIgnoreCase("ADD_INTERACTIVE_POINT")){
				if(event.containsKey("POINT")){
					addPoint((Scene)event.getData("SCENE"), 
							(Frame)event.getData("FRAME"),
							(data.ControlPoint)event.getData("POINT"));						
				}else{
					addPoints((Scene)event.getData("SCENE"), 
							(Frame)event.getData("FRAME"),
							(Collection<data.ControlPoint>)event.getData("POINTS"));						
				}
			}
			else if(event.getName().equalsIgnoreCase("ADD_DATA_POINT")){
				if(event.containsKey("POINT")){
					addPoint((interactive.ControlPoint)event.getData("POINT"));						
				}else{
					addPoints((Collection<interactive.ControlPoint>)event.getData("POINTS"));						
				}
			}
		}
		
		public void addPoints(Scene scene, Frame f, Collection<data.ControlPoint> info){
			for(data.ControlPoint point : info){
				addPoint(scene, f, point);
			}
		}

		public void addPoints(Collection<interactive.ControlPoint> info){
			for(interactive.ControlPoint point : info){
				addPoint(point);
			}
		}

		@Override
		public void processOutput() {
			// TODO Auto-generated method stub
			
		}
	}
}
