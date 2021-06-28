package fr.wonder.ahk.transpilers.common_x64.instructions;

import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.commons.utils.ArrayOperator;

public class CastedInstruction extends Operation {
	
	public final OpCode opCode;
	public final MemSize[] casts;
	private final String strRepr;
	
	public CastedInstruction(OpCode opCode, Object... params) {
		super(opCode, getOperands(params));
		this.opCode = opCode;
		
		if(ArrayOperator.contains(params, null))
			throw new IllegalArgumentException("Null param");
		if(params.length == 0)
			throw new IllegalArgumentException("Cannot create a casted instruction without arguments");
		if(!(params[params.length-1] instanceof OperationParameter))
			throw new IllegalArgumentException("Invalid last argument");
		for(int i = 0; i < params.length-1; i++) {
			if(params[i] instanceof MemSize && params[i+1] instanceof MemSize)
				throw new IllegalArgumentException("An argument has two different casts");
		}
		this.casts = new MemSize[this.operands.length];
		int idx = 0;
		for(Object p : params) {
			if(p instanceof MemSize)
				this.casts[idx] = (MemSize) p;
			else
				idx++;
		}
		String strRepr = opCode.toString().toLowerCase() + " ";
		strRepr += (casts[0] == null ? "" : casts[0].name+" ") + operands[0].toString();
		for(int i = 1; i < this.operands.length; i++) {
			MemSize cast = casts[i];
			strRepr += ","+(cast == null ? "" : cast.name+" ") + operands[i].toString();
		}
		this.strRepr = strRepr;
	}
	
	private static OperationParameter[] getOperands(Object[] params) {
		return ArrayOperator.filter(params, OperationParameter.class);
	}
	
	@Override
	public String toString() {
		return strRepr;
	}
	
}
