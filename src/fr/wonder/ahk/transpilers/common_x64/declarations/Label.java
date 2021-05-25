package fr.wonder.ahk.transpilers.common_x64.declarations;

public class Label implements Declaration {

	public final String name;
	
	public Label(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + ":";
	}

}
