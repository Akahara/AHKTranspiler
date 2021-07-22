package fr.wonder.ahk.compiler.types;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public class FunctionArguments {

	public static boolean matchNoConversions(VarType[] args, VarType[] provided) {
		return Arrays.equals(args, provided);
	}

	public static int getMinimumConvertionCount(VarType[] args, VarType[] provided) {
		int count = 0;
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals(provided[i]))
				continue;
			else if(ConversionTable.canConvertImplicitely(provided[i], args[i]))
				count++;
			else
				return -1;
		}
		return count;
	}

}
