package com.bluebox;


public abstract class WorkerThread implements Runnable {

	private int progress = 0;
	private String id;

	public WorkerThread(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int p) {
		this.progress = p;
	}

	
	@Override
	public boolean equals(Object obj) {
		if (((WorkerThread)obj).getId().equals(getId()))
				return true;
		return super.equals(obj);
	}

	public abstract void run();
}
