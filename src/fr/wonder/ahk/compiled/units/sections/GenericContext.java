package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class GenericContext {
	
	public static final VarGenericType[] NO_GENERICS = new VarGenericType[0];
	public static final GenericContext NO_CONTEXT = new NoContext();
	public static final GenericContext EMPTY_CONTEXT = new GenericContext(null, NO_GENERICS);
	
	public final VarGenericType[] generics;
	public final GenericContext parentContext;
	
	public GenericContext(GenericContext parentContext, VarGenericType[] generics) {
		this.parentContext = parentContext;
		this.generics = generics;
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
		for(VarGenericType gt : generics) {
			if(gt.getName().equals(tk.text))
				return gt;
		}
		if(parentContext != null)
			return parentContext.getGenericType(tk, errors);
		errors.add("Unknown generic used '" + tk.text + "':" + tk.getErr());
		return Invalids.GENERIC_TYPE;
	}
	
	/**
	 * Retrieve a generic type known by this context.
	 * 
	 * <p>
	 * Contrary to {@link #getGenericType(Token, ErrorWrapper)} this method can and
	 * will return null if there is no generic with the target name
	 */
	public VarGenericType retrieveGenericType(String name) {
		for(VarGenericType gt : generics) {
			if(gt.getName().equals(name))
				return gt;
		}
		if(parentContext != null)
			return parentContext.retrieveGenericType(name);
		return null;
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
		return "<" + Utils.toString(generics) + ">";
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
			return "<No Context>";
		}
		
	}

	
}
