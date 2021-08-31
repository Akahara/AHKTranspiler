package fr.wonder.ahk.transpilers.ahl.values;

public class VPointer implements Value {
	
	private int id;
	
	public VPointer(int id) {
		this.id = id;
	}
	
	public int id() {
		return id;
	}
	
	public void setId(int newId) {
		this.id = newId;
	}
	
}
