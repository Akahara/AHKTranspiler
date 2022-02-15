package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.GenericImplementationParameter;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.ArrayOperator;
import fr.wonder.commons.utils.Assertions;

public class GenericContext {
	
	public static final GenericContext NO_CONTEXT = new NoContext();
	
	public final TypeParameter[] typeParameters;
	public final GenericImplementationParameter[] gips;
	
	public GenericContext(TypeParameter[] typeParameters) {
		this.typeParameters = typeParameters;
		int gipCount = ArrayOperator.accumulate(typeParameters, (acc, g) -> acc + g.typeRestrictions.length, 0);
		this.gips = new GenericImplementationParameter[gipCount];
		gipCount = 0;
		for(TypeParameter tp : typeParameters) {
			for(BlueprintRef bpRef : tp.typeRestrictions) {
				this.gips[gipCount++] = new GenericImplementationParameter(tp, bpRef);
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
		Assertions.assertTrue(tk.base == TokenBase.VAR_GENERIC, "Not a generic token");
		Assertions.assertTrue(tk.text.length() == 1, "A generic has a non 1-length name");
		char genericName = tk.text.charAt(0);
		for(TypeParameter tp : typeParameters) {
			if(tp.name == genericName)
				return tp.typeInstance;
		}
		errors.add("Unknown generic used '" + genericName + "':" + tk.getErr());
		return Invalids.GENERIC_TYPE;
	}
	
//	public int gipIndex(BlueprintPrototype bp) {
//		for(int i = 0; i < gips.length; i++)
//			if(gips[i].typeRequirement.blueprint.equals(bp))
//				return i;
//		throw new IllegalArgumentException("This context does not implement blueprint " + bp);
//	}
	
	public boolean hasGenericMembers() {
		return typeParameters.length != 0;
	}
	
	public int indexOf(TypeParameter type) {
		return ArrayOperator.indexOf(typeParameters, type);
	}
	
	@Override
	public String toString() {
		return " " + Utils.typeParametersString(typeParameters);
	}
	
	private static class NoContext extends GenericContext {
		
		private NoContext() {
			super(new TypeParameter[0]);
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
