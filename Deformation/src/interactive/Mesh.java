package interactive;

import processing.core.PShape;
import remixlab.dandelion.core.Eye;
import remixlab.dandelion.core.GenericFrame;
import remixlab.dandelion.geom.Frame;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;

public class Mesh extends Interactive{

	public Mesh(Eye eye) {
		super(eye);
	}

	public Mesh(InteractiveFrame arg0) {
		super(arg0);
	}

	public Mesh(Scene scn, GenericFrame referenceFrame, Object obj, String methodName) {
		super(scn, referenceFrame, obj, methodName);
	}

	public Mesh(Scene scn, Frame f, PShape ps) {
		super(scn, ps);
		this.setReferenceFrame(f);
	}

	public Mesh(Scene scn, GenericFrame referenceFrame) {
		super(scn, referenceFrame);
	}

	public Mesh(Scene scn, Object obj, String methodName) {
		super(scn, obj, methodName);
	}

	public Mesh(Scene scn, PShape ps) {
		super(scn, ps);
	}

	public Mesh(Scene scn) {
		super(scn);
	}

	@Override
	protected void setEvents() {
		/*No Additional events required*/
	}
}
