package com.cwt.liaohs.state;

public abstract class RecordState {
	public RecordStateManager stateManager;
	public RecordState(RecordStateManager stateManager){
		this.stateManager = stateManager;
	}
	public abstract void onStart();
	public abstract void onStop();
}
