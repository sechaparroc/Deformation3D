package modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import data.Data;
import data.Vertex;
import interactive.Interactive;
import interactive.Mesh;
import modules.Module.Event;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;
import remixlab.dandelion.geom.Frame;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.Scene;


public class InteractiveToDataModules {
	/*
	 * Here the modules will process and relate Interactive information to Data and 
	 * Data to Interactive information, Also the Behavior Modules will be feed by
	 * this modules
	 * */
	public static class List<D extends Data, I extends Interactive>{
		/*
		 * It's assumed that different interactive Frames could be related with
		 * same data
		 * */
		private ArrayList<Item> items;
		
		
		private class Item{
			private D data;
			private ArrayList<I> interactive;
						
			public Item(D data, I... interactive){
				this.data = data;
				this.interactive = new ArrayList<I>();
				for(int i = 0; i < interactive.length; i++){
					this.interactive.add(interactive[i]);
				}
			}
		}
		
		public List(){
			items = new ArrayList<Item>();
		}

		public void add(D data, I... interactive){
			Item item = null;
			for(Item i : items){
				if(i.data.equals(data)){
					item = i;
					break;
				}
			}
			if(item == null)items.add(new Item(data, interactive));
			else{
				if(item.interactive == null) item.interactive = new ArrayList<I>();
				for(int i = 0; i < interactive.length; i++)
					item.interactive.add(interactive[i]);
			}
		}
		
		public boolean contains(D data){
			for(Item i : items){
				if(i.data.equals(data)) return true;
			}
			return false;
		}

		
		public boolean contains(I interactive){
			for(Item i : items){
				if(i.interactive.contains(interactive)) return true;
			}
			return false;
		}
		
		public ArrayList<I> get(D data){
			for(Item i : items){
				if(i.data.equals(data)) return i.interactive;
			}
			return null;
		}
		
		public D get(I interactive){
			for(Item i : items){
				if(i.interactive.contains(interactive)) return i.data;
			}
			return null;
		}
		
		public ArrayList<I> getInteractive(){
			ArrayList<I> interactive = new ArrayList<I>();
			for(Item i : items){
				interactive.addAll(i.interactive);
			}
			return interactive;
		}
	}
	
	
	public static class MeshModule extends Module implements InteractiveListener<interactive.Mesh>{
		/*Process Interactive Meshes to Data Meshes and vice versa*/

		
		private List<data.Mesh, interactive.Mesh> meshes;
		
		public List<data.Mesh, interactive.Mesh> getMeshes() {
			return meshes;
		}

		public void setMeshes(List<data.Mesh, interactive.Mesh> meshes) {
			this.meshes = meshes;
		}

		public data.Mesh getDataMesh(interactive.Mesh imesh){
			return meshes.get(imesh);
		}
		
		public ArrayList<interactive.Mesh> getInteractiveMesh(data.Mesh mesh){
			return meshes.get(mesh);
		}
		
		public MeshModule(){
			super();
			meshes = new List<data.Mesh, interactive.Mesh>();
		}
		
		public void addMesh(interactive.Mesh mesh){
			if(!meshes.contains(mesh)) createMesh(mesh);
			mesh.attach(this);
		}
		
		public void addMesh(Scene scene, Frame f, data.Mesh mesh){
			if(!meshes.contains(mesh)) createMesh(scene, f, mesh);
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
			
			data.Mesh mesh = new data.Mesh(new ArrayList<Vertex>(vertices.values()), faces);
			
			meshes.add(mesh, interactive);

			Event event = new Event("ADD");
			event.addData("DATA_MESH", mesh);
			addResponse(event);
			
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
			interactive.Mesh mesh = 
					f != null ? new interactive.Mesh(scene, f, shape) : new interactive.Mesh(scene, shape); 
			
			meshes.add(object, mesh);
			mesh.attach(this);
			
			Event event = new Event("ADD");
			event.addData("INTERACTIVE_MESH", mesh);
			addResponse(event);
		}
		
		public void setMeshVertices(data.Mesh mesh){
			ArrayList<interactive.Mesh> imeshes = getInteractiveMesh(mesh);
			for(interactive.Mesh im : imeshes){
				PShape shape = im.shape();
		    	for(Vertex v : mesh.getVertices()){
		    		for(int[] idx : v.getIdx_shape()){
		    			shape.getChild(idx[0]).setVertex(idx[1], v.getPos());
		    		}
		    	}
				Event event = new Event("VERTICES_MODIFIED");
				event.addData("INTERACTIVE_MESH", im);
				addResponse(event);
			}		
		}
		

		@Override
		public void listen(String event, interactive.Mesh interactive) {

		}

		@Override
		public void executeEvent(Event event) {
			if(event.containsKey("INTERACTIVE_MESH")){
				if(event.getName().equalsIgnoreCase("CREATE_DATA_MESH")){
					addMesh((interactive.Mesh)event.getData("INTERACTIVE_MESH"));
				}
			}else if(event.containsKey("INTERACTIVE_MESHES")){
				if(event.getName().equalsIgnoreCase("CREATE_DATA_MESHES")){
					addMeshes((Collection<interactive.Mesh>)event.getData("INTERACTIVE_MESHES"));
				}
			}else if(event.containsKey("DATA_MESH")){
				if(event.getName().equalsIgnoreCase("CREATE_INTERACTIVE_MESH")){
					addMesh((Scene)event.getData("SCENE"), 
							(Frame)event.getData("FRAME"),
							(data.Mesh)event.getData("DATA_MESH"));						
				}else if(event.getName().equalsIgnoreCase("VERTICES_MODIFIED")){
					setMeshVertices((data.Mesh) event.getData("DATA_MESH"));
				}
			}else if(event.containsKey("DATA_MESHES")){
				if(event.getName().equalsIgnoreCase("CREATE_INTERACTIVE_MESHES")){
					addMeshes((Scene)event.getData("SCENE"), 
							(Frame)event.getData("FRAME"),
							(Collection<data.Mesh>)event.getData("DATA_MESHES"));						
				}
			}
		}
		
		public void addMeshes(Scene scene, Frame f, Collection<data.Mesh> info){
			for(data.Mesh mesh : info){
				addMesh(scene, f, mesh);
			}
		}

		public void addMeshes(Collection<interactive.Mesh> info){
			for(interactive.Mesh mesh : info){
				addMesh(mesh);
			}
		}

		@Override
		public void processOutput() {

		}
		
		
	}

	public static class ControlPointModule extends Module implements InteractiveListener<interactive.ControlPoint> {
		/*Process Interactive control points to Data representation and vice versa
		 * It is possible that the same Data objects has multiple Interactive objects related
		 * but not the other way around.
		 * */
		private List<data.ControlPoint, interactive.ControlPoint> points;

		public List<data.ControlPoint, interactive.ControlPoint> getPoints() {
			return points;
		}

		public void setPoints(List<data.ControlPoint, interactive.ControlPoint> points) {
			this.points = points;
		}

		public data.ControlPoint getDataControlPoints(interactive.ControlPoint ipoint){
			return points.get(ipoint);
		}

		public ArrayList<interactive.ControlPoint> getInteractiveControlPoint(data.ControlPoint point){
			return points.get(point);
		}
		
		public ControlPointModule(){
			super();
			points = new List<data.ControlPoint, interactive.ControlPoint>();
		}
		
		public data.ControlPoint addPoint(interactive.ControlPoint point){
			if(!points.contains(point)){
				point.attach(this);
				return createPoint(point);
			}
			return null;

		}
		
		public interactive.ControlPoint addPoint(Scene scene, Frame f, data.ControlPoint point){
			if(!points.contains(point)) return createPoint(scene, f, point);
			return null;
		}

		
		public data.ControlPoint createPoint(interactive.ControlPoint interactive){
			PVector A = utilities.Utilities.vecToPVector(interactive.translation());
			PVector B = A.copy();
			if(interactive.getB() != null)
				B = utilities.Utilities.vecToPVector(
						interactive.localInverseCoordinatesOf(interactive.getB().translation()));
			data.ControlPoint cp = new data.ControlPoint(A, B);
			points.add(cp, interactive);

			Event event = new Event("ADD");
			event.addData("DATA_CONTROL_POINT", cp);
			addResponse(event);
			return cp;
		}
		
		public interactive.ControlPoint createPoint(Scene scene, Frame f, data.ControlPoint object){
			Vec A = utilities.Utilities.PVectorToVec(object.getA());
			Vec B = utilities.Utilities.PVectorToVec(object.getB());
			interactive.ControlPoint cp = 
					new interactive.ControlPoint(scene, f, A);
			points.add(object, cp);
			cp.attach(this);

			Event event = new Event("ADD");
			event.addData("INTERACTIVE_CONTROL_POINT", cp);
			addResponse(event);
			
			return cp;
		}
		
		public void translateA(data.ControlPoint point){
			ArrayList<interactive.ControlPoint> ipoints = getInteractiveControlPoint(point);
			for(interactive.ControlPoint icp : ipoints){
		    	icp.setTranslation(utilities.Utilities.PVectorToVec(point.getA()));
				Event event = new Event("TRANSLATE_A");
				event.addData("INTERACTIVE_CONTROL_POINT", icp);
				addResponse(event);
			}		
		}

		public void translateB(data.ControlPoint point){
			ArrayList<interactive.ControlPoint> ipoints = getInteractiveControlPoint(point);
			for(interactive.ControlPoint icp : ipoints){
		    	Vec B = icp.localCoordinatesOf(utilities.Utilities.PVectorToVec(point.getB()));
		    	icp.getB().setTranslation(B);
				Event event = new Event("TRANSLATE_B");
				event.addData("INTERACTIVE_CONTROL_POINT", icp);
				addResponse(event);
			}		
		}

		public void translateA(interactive.ControlPoint icp){
			data.ControlPoint cp = points.get(icp);
			cp.setA(utilities.Utilities.vecToPVector(icp.translation()));
			Event event = new Event("TRANSLATE_A");
			event.addData("DATA_CONTROL_POINT", cp);
			addResponse(event);
		}

		public void translateB(interactive.ControlPoint icp){
			data.ControlPoint cp = points.get(icp);
			System.out.println("Object : " + cp);
			cp.setB(utilities.Utilities.vecToPVector(icp.localInverseCoordinatesOf(icp.getB().translation())));
			Event event = new Event("TRANSLATE_B");
			event.addData("DATA_CONTROL_POINT", cp);
			addResponse(event);
		}
		
		
		@Override
		public void listen(String event, interactive.ControlPoint interactive) {
			data.ControlPoint object = points.get(interactive);
			System.out.println("Event: " + event);
			System.out.println("Object: " + interactive);
			
			if(event.equalsIgnoreCase("TRANSLATE_A")){
				translateA(interactive);
			}else if(event.equalsIgnoreCase("TRANSLATE_B")){
				translateB(interactive);
			}
		}

		@Override
		public void executeEvent(Event event) {
			if(event.containsKey("INTERACTIVE_CONTROL_POINT")){
				if(event.getName().equalsIgnoreCase("CREATE_DATA_CONTROL_POINT")){
					addPoint((interactive.ControlPoint)event.getData("INTERACTIVE_CONTROL_POINT"));
				}else if(event.getName().equalsIgnoreCase("TRANSLATE_A")){
					translateA((interactive.ControlPoint)event.getData("INTERACTIVE_CONTROL_POINT"));
				}else if(event.getName().equalsIgnoreCase("TRANSLATE_B")){
					translateB((interactive.ControlPoint)event.getData("INTERACTIVE_CONTROL_POINT"));
				}
			}else if(event.containsKey("INTERACTIVE_CONTROL_POINTS")){
				if(event.getName().equalsIgnoreCase("CREATE_DATA_CONTROL_POINT")){
					addPoints((Collection<interactive.ControlPoint>)event.getData("INTERACTIVE_CONTROL_POINTS"));
				}
			}else if(event.containsKey("DATA_CONTROL_POINT")){
				if(event.getName().equalsIgnoreCase("CREATE_INTERACTIVE_CONTROL_POINT")){
					addPoint((Scene)event.getData("SCENE"), 
							(Frame)event.getData("FRAME"),
							(data.ControlPoint)event.getData("DATA_CONTROL_POINT"));						
				}else if(event.getName().equalsIgnoreCase("TRANSLATE_A")){
					translateA((data.ControlPoint)event.getData("INTERACTIVE_CONTROL_POINT"));
				}else if(event.getName().equalsIgnoreCase("TRANSLATE_B")){
					translateB((data.ControlPoint)event.getData("INTERACTIVE_CONTROL_POINT"));
				}
			}else if(event.containsKey("DATA_CONTROL_POINTS")){
				if(event.getName().equalsIgnoreCase("CREATE_INTERACTIVE_CONTROL_POINTS")){
					addPoints((Scene)event.getData("SCENE"), 
							(Frame)event.getData("FRAME"),
							(Collection<data.ControlPoint>)event.getData("DATA_CONTROL_POINTS"));						
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

		}
	}
}
