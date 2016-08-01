package data;

import controller.DataListener;
import processing.core.PVector;

public class ControlPoint extends Data{
	private PVector A;
	private PVector B;
	
	public ControlPoint(PVector A, PVector B){
		this.A = A;
		this.B = B;
	}
	
	public PVector getA() {
		return A;
	}
	public void setA(PVector a) {
		A = a;
	}
	public PVector getB() {
		return B;
	}
	public void setB(PVector b) {
		B = b;
	}
	
	
	@Override
	protected void setEvents() {
		events.add("TRANSLATE_A");
		events.add("TRANSLATE_B");
	}
}
