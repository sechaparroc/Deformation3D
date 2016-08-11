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
		setHighlightingMode(HighlightingMode.NONE);
	}

	public Mesh(Scene scn, Frame f, PShape ps) {
		super(scn, ps);
		setHighlightingMode(HighlightingMode.NONE);
		this.setReferenceFrame(f);
	}

	public Mesh(Scene scn, GenericFrame referenceFrame) {
		super(scn, referenceFrame);
		setHighlightingMode(HighlightingMode.NONE);
	}

	public Mesh(Scene scn, Object obj, String methodName) {
		super(scn, obj, methodName);
		setHighlightingMode(HighlightingMode.NONE);
	}

	public Mesh(Scene scn, PShape ps) {
		super(scn, ps);
		setHighlightingMode(HighlightingMode.NONE);
	}

	public Mesh(Scene scn) {
		super(scn);
		setHighlightingMode(HighlightingMode.NONE);
	}

	@Override
	protected void setEvents() {
		/*No Additional events required*/
	}
}
