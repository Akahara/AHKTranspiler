package fr.wonder.ahk.compiler.types;

import static fr.wonder.ahk.compiled.expressions.types.VarType.BOOL;
import static fr.wonder.ahk.compiled.expressions.types.VarType.FLOAT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

public class ConversionTable {
	
	private static final Map<VarType, Set<VarType>> implicitConversions = new HashMap<>();
	private static final Map<VarType, Set<VarType>> explicitConversions = new HashMap<>();
	
	private ConversionTable() {};
	
	static {
		addImplicitConversion(INT,   FLOAT);
		addImplicitConversion(BOOL,  INT);
		addImplicitConversion(INT,   BOOL);
		addImplicitConversion(FLOAT, BOOL);
		
		addExplicitConversion(FLOAT, INT);
	}
	
	private static void addImplicitConversion(VarType from, VarType to) {
		implicitConversions.computeIfAbsent(from, x -> new HashSet<>()).add(to);
	}
	
	private static void addExplicitConversion(VarType from, VarType to) {
		explicitConversions.computeIfAbsent(from, x -> new HashSet<>()).add(to);
	}
	
	public static boolean canConvertImplicitly(VarType from, VarType to) {
		if(from.equals(to))
			return true;
		if(implicitConversions.getOrDefault(from, Collections.emptySet()).contains(to))
			return true;
		if(from instanceof VarFunctionType && to instanceof VarFunctionType)
			return canConvertFunctionImplicitly((VarFunctionType) from, (VarFunctionType) to);
		return false;
	}
	
	/**
	 * @see #canConvertExplicitly(VarType, VarType)
	 */
	private static boolean canConvertFunctionImplicitly(VarFunctionType from, VarFunctionType to) {
		if(from.arguments.length != to.arguments.length)
			return false;
		if(!canConvertImplicitly(from.returnType, to.returnType))
			return false;
		for(int i = 0; i < from.arguments.length; i++) {
			if(!canConvertImplicitly(to.arguments[i], from.arguments[i]))
				return false;
		}
		return true;
	}
	
	/**
	 * Returns true iff {@code from} can be explicitly converted to {@code to}.
	 * <p>
	 * Implicit conversions are specific types of explicit conversions, if {@code from}
	 * can be implicitly converted to {@code to}, it can also be explicitly converted to
	 * {@code to}.
	 */
	public static boolean canConvertExplicitly(VarType from, VarType to) {
		if(canConvertImplicitly(from, to))
			return true;
		if(explicitConversions.getOrDefault(from, Collections.emptySet()).contains(to))
			return true;
		if(from instanceof VarFunctionType && to instanceof VarFunctionType) {
			return canConvertFunctionExplicitly((VarFunctionType) from, (VarFunctionType) to);
		}
		return false;
	}
	
	/**
	 * A function type can be converted to another if each of its arguments is suitable for the
	 * corresponding destination type argument and its return type is also suitable for the
	 * destination type return type.
	 * <p>
	 * For example :
	 * <blockquote><pre>
	 * alias FF = float(float);
	 * alias II = int(int);
	 * FF f = (float x):float => x+3.5;
	 * II i = f;
	 * float y = i(3);
	 * </pre></blockquote>
	 * to call {@code i(3)}, 3 must be converted to a float, f must be called on 3.0 and the return
	 * value of f (6.5) must be converted back to an int.
	 * <p>
	 * A function type conversion can be implicit if all of the involved conversions are implicit,
	 * otherwise it must be made explicitly.
	 */
	private static boolean canConvertFunctionExplicitly(VarFunctionType from, VarFunctionType to) {
		if(from.arguments.length != to.arguments.length)
			return false;
		if(!canConvertExplicitly(from.returnType, to.returnType))
			return false;
		for(int i = 0; i < from.arguments.length; i++) {
			if(!canConvertExplicitly(to.arguments[i], from.arguments[i]))
				return false;
		}
		return true;
	}
	
	public static VarType getCommonParent(VarType t1, VarType t2) {
		if(t1.equals(t2))
			return t1;
		if(t1 instanceof VarNativeType && t2 instanceof VarNativeType) {
			if(canConvertImplicitly(t1, t2))
				return t2;
			else if(canConvertImplicitly(t2, t1))
				return t1;
		}
		if(t1 instanceof VarFunctionType && t2 instanceof VarFunctionType) {
			VarFunctionType f1 = (VarFunctionType) t1;
			VarFunctionType f2 = (VarFunctionType) t2;
			if(f1.arguments.length != f2.arguments.length)
				return null;
			VarType commonReturnType = getCommonParent(f1.returnType, f2.returnType);
			VarType[] commonArgTypes = new VarType[f1.arguments.length];
			for(int i = 0; i < f1.arguments.length; i++)
				commonArgTypes[i] = getCommonParent(f1.arguments[i], f2.arguments[i]);
			return new VarFunctionType(commonReturnType, commonArgTypes);
		}
		return null;
	}

}
