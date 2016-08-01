package controller;

import interactive.ControlPoint;
import processing.core.PVector;

public class InteractiveControlPointController extends InteractiveController<data.ControlPoint, interactive.ControlPoint>{
	public InteractiveControlPointController(ControlPoint interactive, boolean create) {
		super(interactive, create);
	}

	public InteractiveControlPointController(ControlPoint interactive) {
		super(interactive);
	}

	@Override
	public void listen(String event, ControlPoint interactive) {
		if(event.equalsIgnoreCase("TRANSLATE_A")){
			dataObject.setA(utilities.Utilities.vecToPVector(interactive.translation()));
		}else if(event.equalsIgnoreCase("TRANSLATE_B")){
			dataObject.setB(utilities.Utilities.vecToPVector(interactive.localCoordinatesOf(interactive.getB().translation())));
		}
	}

	@Override
	public data.ControlPoint interactiveToData(ControlPoint interactive) {
		PVector A = utilities.Utilities.vecToPVector(interactive.translation());
		PVector B = utilities.Utilities.vecToPVector(interactive.localCoordinatesOf(interactive.getB().translation()));
		return new data.ControlPoint(A,B);
	}
}
