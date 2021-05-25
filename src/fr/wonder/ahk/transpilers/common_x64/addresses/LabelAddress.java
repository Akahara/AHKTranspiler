package fr.wonder.ahk.transpilers.common_x64.addresses;

public class LabelAddress implements Address {

	public final String label;
	
	public LabelAddress(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return label;
	}
}
