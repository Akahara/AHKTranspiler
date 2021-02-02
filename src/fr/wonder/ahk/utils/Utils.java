package fr.wonder.ahk.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.commons.utils.ReflectUtils;

public class Utils {
	
	public static int countChar(String s, char c) {
		int t = 0;
		for(int i = 0; i < s.length(); i++)
			if(s.charAt(i) == c)
				t++;
		return t;
	}
	
	public static String untokenize(Token[] tokens) {
		String s = "";
		for(Token t : tokens)
			s += t.text + "|";
		return s;
	}
	
	public static int getTokenIdx(Token[] tokens, TokenBase base, int start) {
		for(int i = start; i < tokens.length; i++)
			if(tokens[i].base == base)
				return i;
		return -1;
	}
	
	public static <T> List<T> asList(T[] array) {
//		List<T> list = new ArrayList<>();
//		for(T t : array)
//			list.add(t);
//		return list;
		return new ArrayList<>(Arrays.asList(array));
	}

	public static <T> int getFirstIndex(T[] line, T token) {
		for(int i = 0; i < line.length; i++) {
			if(line[i] == token)
				return i;
		}
		return -1;
	}

	public static <T> boolean arrayContains(T[] array, T t) {
		for(T tt : array)
			if(t == tt)
				return true;
		return false;
	}

	public static <T> String toString(T[] array) {
		if(array == null)
			return "null";
		StringBuilder sb = new StringBuilder();
		for(T t : array) {
			sb.append(Objects.toString(t));
			sb.append(", ");
		}
		if(sb.length() > 0)
			sb.setLength(sb.length()-2);
		return sb.toString();
	}
	
	public static <T> String mapToString(T[] array, Function<T, ?> getter) {
		if(array == null)
			return "null";
		StringBuilder sb = new StringBuilder();
		for(T t : array) {
			sb.append(getter.apply(t));
			sb.append(", ");
		}
		if(sb.length() > 0)
			sb.setLength(sb.length()-2);
		return sb.toString();
	}
	
	public static void dump(Unit unit) {
		System.out.println("-".repeat(20));
		System.out.println("Unit name: " + unit.name);
		System.out.println("Unit base: " + unit.base);
		for(VariableDeclaration var : unit.variables)
			System.out.println(var.getType() + " " + var.name + " = " + var.getDefaultValue());
		for(FunctionSection sec : unit.functions) {
			System.out.println(sec);
			for(Statement s : sec.body)
				System.out.println("  "+s+';');
		}
		System.out.println("-".repeat(20));
	}
	
	public static <T> void dump(T[] array, int start, int stop) {
		dump(Arrays.copyOfRange(array, start, stop));
	}

	@SafeVarargs
	public static <T> void dump(T... array) {
		System.out.println(Arrays.toString(array));
	}
	
	public static <T> void dump(T[][] array) {
		System.out.println(Arrays.deepToString(array).replaceAll("],", "],\n"));
	}
	
	public static void dump(Collection<?> list) {
		for(Object o : list)
			System.out.println(o);
	}
	
	public static void dump(Map<?, ?> map) {
		System.out.println(Arrays.toString(map.entrySet().toArray()));
	}
	
	public static void dump(Object o) {
		System.out.println("-".repeat(20));
		ReflectUtils.printObject(o);
		System.out.println("-".repeat(20));
	}
	
}
