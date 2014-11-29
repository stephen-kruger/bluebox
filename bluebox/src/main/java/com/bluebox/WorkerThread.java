package com.bluebox;


public abstract class WorkerThread implements Runnable {

	private int progress = 0;
	private String id, status;
	private boolean stop = false;

	public WorkerThread(String id) {
		this.id = id;
		this.status = "Initialising";
	}

	public String getId() {
		return id;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int p) {
		this.progress = p;
		this.status = "Running";
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object obj) {
		if (((WorkerThread)obj).getId().equals(getId()))
				return true;
		return super.equals(obj);
	}

	public abstract void run();

	public void stop() {
		stop  = true;
	}
	
	public boolean isStopped() {
		return stop;
	}
}
