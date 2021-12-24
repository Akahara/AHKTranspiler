package fr.wonder.ahk.compiler.types;

import static fr.wonder.ahk.compiled.expressions.types.VarType.BOOL;
import static fr.wonder.ahk.compiled.expressions.types.VarType.FLOAT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

public class ConversionTable {
	
	private static final Map<VarType, Set<VarType>> implicitConversions = new HashMap<>();
	private static final Map<VarType, Set<VarType>> explicitConversions = new HashMap<>();
	
	private ConversionTable() {};
	
	static {
		addImplicitConversion(INT,   FLOAT);
		addImplicitConversion(BOOL,  INT);
		addImplicitConversion(INT, BOOL);
		addImplicitConversion(FLOAT, BOOL);
		
		addExplicitConversion(FLOAT, INT);
	}
	
	private static void addImplicitConversion(VarType from, VarType to) {
		implicitConversions.computeIfAbsent(from, x -> new HashSet<>()).add(to);
	}
	
	private static void addExplicitConversion(VarType from, VarType to) {
		explicitConversions.computeIfAbsent(from, x -> new HashSet<>()).add(to);
	}
	
	public static boolean canConvertImplicitely(VarType from, VarType to) {
		return from.equals(to) || implicitConversions.getOrDefault(from, Collections.emptySet()).contains(to);
	}
	
	/**
	 * Explicit and Implicit conversions are exclusive to each other.
	 * (if {@link #canConvertImplicitely(VarType, VarType)} returned true, this method will return false)
	 */
	public static boolean canConvertExplicitely(VarType from, VarType to) {
		return explicitConversions.computeIfAbsent(from, x -> new HashSet<>()).contains(to);
	}
	
	public static VarType getCommonParent(VarType t1, VarType t2) {
		if(t1.equals(t2))
			return t1;
		if(t1 instanceof VarNativeType && t2 instanceof VarNativeType) {
			if(canConvertImplicitely(t1, t2))
				return t2;
			else if(canConvertImplicitely(t2, t1))
				return t1;
		}
		return null;
	}

}
