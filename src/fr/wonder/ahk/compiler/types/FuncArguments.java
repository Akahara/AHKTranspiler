package fr.wonder.ahk.compiler.types;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public class FuncArguments {

	public static boolean argsMatch0c(VarType[] args, VarType[] provided) {
		return Arrays.equals(args, provided);
	}

	/** redirects to {@link argsMatch0c} */
	public static boolean argsMatch0c(VarType[] args, VarType[] provided, ConversionTable conversions) {
		return argsMatch0c(args, provided);
	}

	public static boolean argsMatch1c(VarType[] args, VarType[] provided, ConversionTable conversions) {
		if(args.length != provided.length)
			return false;
		boolean casted = false;
		for(int i = 0; i < args.length; i++) {
			if(args[i] == provided[i]) {
				continue;
			} else if(!casted && conversions.canConvertImplicitely(provided[i], args[i])) {
				casted = true;
			} else {
				return false;
			}
		}
		return true;
	}

	public static boolean argsMatchXc(VarType[] args, VarType[] provided, ConversionTable conversions) {
		if(args.length != provided.length)
			return false;
		for(int i = 0; i < args.length; i++)
			if(!conversions.canConvertImplicitely(provided[i], args[i]))
				return false;
		return true;
	}

}
