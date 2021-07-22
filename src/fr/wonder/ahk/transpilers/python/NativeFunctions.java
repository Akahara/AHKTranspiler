package fr.wonder.ahk.transpilers.python;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.FunctionArguments;
import fr.wonder.commons.types.Tuple;

class NativeFunctions {
	
	/**
	 * List of all known native functions
	 * an entry corresponds to a function:
	 * the tuple contains the function name and return type and is associated 
	 * with its arguments types and the corresponding native function file
	 */
	private static final Map<Tuple<String, VarType>, List<Tuple<VarType[], String>>> nativeFunctions = new HashMap<>();
	static {
		addNative("print", 	VarType.VOID, 	"kernel/print", VarType.STR);
		addNative("print", 	VarType.VOID, 	"kernel/print", VarType.INT);
		addNative("print", 	VarType.VOID, 	"kernel/print", VarType.FLOAT);
		addNative("print", 	VarType.VOID, 	"kernel/print", VarType.BOOL);
		addNative("print", 	VarType.VOID, 	"kernel/print_vsi", VarType.STR, VarType.INT);
		addNative("println", VarType.VOID, 	"kernel/println");
		addNative("exit", 	VarType.VOID, 	"kernel/exit", 	VarType.INT);
	}
	
	private static final Map<String, String> functionSources = new HashMap<>();
	
	private static void addNative(String name, VarType returnType, String func, VarType... args) {
		nativeFunctions.computeIfAbsent(new Tuple<>(name, returnType), t->new ArrayList<>()).add(new Tuple<>(args, func));
	}
	
	private static String getFunctionSourcePath(FunctionSection func, ConversionTable conversions) {
		List<Tuple<VarType[], String>> functions = nativeFunctions.get(new Tuple<>(func.name, func.returnType));
		if(functions == null)
			return null;
		for(Tuple<VarType[], String> nativeFunc : functions)
			if(FunctionArguments.argsMatch1c(nativeFunc.a, func.getArgumentTypes()))
				return nativeFunc.b;
		return null;
	}
	
	public static boolean isKnownNative(FunctionSection func, ConversionTable conversions) {
		return getFunctionSourcePath(func, conversions) != null;
	}
	
	public static void writeNative(FunctionSection func, StringBuilder sb, ConversionTable conversions) {
		sb.append("  @staticmethod\n");
		sb.append("  def " + func.getSignature().computedSignature + "(");
		for(int i = 0; i < func.arguments.length; i++) {
			sb.append((char)(i+97));
			if(i != func.arguments.length-1)
				sb.append(", ");
		}
		sb.append("):\n");
		sb.append(getNativeSource(func, conversions));
	}
	
	private static String getNativeSource(FunctionSection func, ConversionTable conversions) {
		String sourcePath = getFunctionSourcePath(func, conversions);
		if(sourcePath == null)
			throw new IllegalStateException("Unknown native function " + func.getSignature().computedSignature);
		// return already read source
		if(functionSources.containsKey(sourcePath))
			return functionSources.get(sourcePath);
		// read source
		InputStream in = NativeFunctions.class.getResourceAsStream("/py/natives/" + sourcePath + ".py");
		if(in == null)
			throw new IllegalStateException("Missing native python source for " + sourcePath + " !");
		try {
			String source = new String(in.readAllBytes());
			if(source.isBlank())
				source = "    pass\n";
			else
				source = "    " + source.replaceAll("\n", "    \n");
			if(!source.endsWith("\n"))
				source += "\n";
			functionSources.put(sourcePath, source);
			return source;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read native python source for " + sourcePath, e);
		}
	}
	
}
