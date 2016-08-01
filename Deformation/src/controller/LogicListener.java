package controller;

import logic.Logic;

public interface LogicListener<L extends Logic> {
	void listen(String event, L logic);
}
