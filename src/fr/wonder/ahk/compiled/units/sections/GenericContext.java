package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintPrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.GenericImplementationParameter;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.ArrayOperator;
import fr.wonder.commons.utils.Assertions;

public class GenericContext {
	
	public static final VarGenericType[] NO_GENERICS = new VarGenericType[0];
	public static final GenericContext NO_CONTEXT = new NoContext();
	public static final GenericContext EMPTY_CONTEXT = new GenericContext(null, NO_GENERICS); // FIX remove empty generic context
	
	public final VarGenericType[] generics;
	public final GenericContext parentContext;
	public final GenericImplementationParameter[] gips;
	
	public GenericContext(GenericContext parentContext, VarGenericType[] generics) {
		this.parentContext = parentContext;
		this.generics = generics;
		int gipCount = ArrayOperator.accumulate(generics, (acc, g) -> acc + g.typeRestrictions.length, 0);
		this.gips = new GenericImplementationParameter[gipCount];
		gipCount = 0;
		for(VarGenericType g : generics) {
			for(BlueprintRef bpRef : g.typeRestrictions) {
				this.gips[gipCount++] = new GenericImplementationParameter(g, bpRef);
			}
		}
	}
	
	/**
	 * Retrieve a generic type known by this context.
	 * 
	 * <p>
	 * Contrary to {@link #retrieveGenericType(String)} this method
	 * will return {@link Invalids#GENERIC_TYPE} and add an error
	 * if no generic exists with the target name.
	 */
	public VarGenericType getGenericType(Token tk, ErrorWrapper errors) {
		Assertions.assertTrue(tk.text.length() == 1, "A generic has a non 1-length name");
		char genericName = tk.text.charAt(0);
		for(VarGenericType gt : generics) {
			if(gt.name == genericName)
				return gt;
		}
		if(parentContext != null)
			return parentContext.getGenericType(tk, errors);
		errors.add("Unknown generic used '" + genericName + "':" + tk.getErr());
		return Invalids.GENERIC_TYPE;
	}
	
	/**
	 * Retrieve a generic type known by this context.
	 * 
	 * <p>
	 * Contrary to {@link #getGenericType(Token, ErrorWrapper)} this method can and
	 * will return null if there is no generic with the target name
	 */
	public VarGenericType retrieveGenericType(char name) {
		for(VarGenericType gt : generics) {
			if(gt.name == name)
				return gt;
		}
		if(parentContext != null)
			return parentContext.retrieveGenericType(name);
		return null;
	}
	
	public int gipIndex(BlueprintPrototype bp) {
		for(int i = 0; i < gips.length; i++)
			if(gips[i].typeRequirement.blueprint.equals(bp))
				return i;
		throw new IllegalArgumentException("This context does not implement blueprint " + bp);
	}
	
	public boolean hasGenericMembers() {
		return generics.length != 0;
	}
	
	public int indexOf(VarGenericType type) {
		for(int i = 0; i < generics.length; i++) {
			if(type == generics[i])
				return i;
		}
		return -1;
	}
	
	@Override
	public String toString() {
		return " " + Utils.genericBindingsString(generics);
	}
	
	private static class NoContext extends GenericContext {
		
		private NoContext() {
			super(null, new VarGenericType[0]);
		}
		
		public VarGenericType getGenericType(Token tk, ErrorWrapper errors) {
			errors.add("Generic '" + tk.text + "' cannot be used in a non-generic context:" + tk.getErr());
			return Invalids.GENERIC_TYPE;
		}
		
		@Override
		public String toString() {
			return "";
		}
		
	}

	
}
