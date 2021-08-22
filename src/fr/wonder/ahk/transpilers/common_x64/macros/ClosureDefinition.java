package fr.wonder.ahk.transpilers.common_x64.macros;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.wonder.ahk.transpilers.common_x64.instructions.Instruction;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;

public class ClosureDefinition implements Instruction {
	
	public final String label, closureFunction;
	public final int argumentCount;
	public final OperationParameter[] closureParameters;
	
	public ClosureDefinition(String label, String closureFunction,
			int argumentCount, Object... closureParameters) {
		
		this.label = label;
		this.closureFunction = closureFunction;
		this.argumentCount = argumentCount;
		this.closureParameters = OperationParameter.asOperationParameters(closureParameters);
	}
	
	@Override
	public String toString() {
		return "def_closure " + label + "," + closureFunction + "," + argumentCount + ", " + 
				Stream.of(closureParameters).map(Object::toString).collect(Collectors.joining(","));
	}
	
}
