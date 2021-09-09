package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarBoundStructType;
import fr.wonder.ahk.compiled.expressions.types.VarCompositeType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.BoundStructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

public class GenericBindings {
	
	/**
	 * Validates generic bindings for a context.
	 * 
	 * <p>
	 * If the bindings are unacceptable an error is logged and false is returned,
	 * otherwise true is returned.
	 */
	public static boolean validateBindings(GenericContext context, VarType[] bindings,
			SourceElement queryElement, ErrorWrapper errors) {
		if(context.hasGenericMembers() && bindings == null) {
			errors.add("Expected generic bindings:" + queryElement.getErr());
			return false;
		}
		if(!context.hasGenericMembers()) {
			if(bindings != null) {
				errors.add("Unexpected generic bindings:" + queryElement.getErr());
				return false;
			}
			return true;
		}
		
		if(context.generics.length != bindings.length) {
			errors.add("Invalid number of generics, type expected " + context.generics.length +
					" but got " + bindings.length + queryElement.getErr());
			return false;
		}
		
		boolean validBinding = true;
		for(int i = 0; i < bindings.length; i++) {
			if(!context.generics[i].isValidBinding(bindings[i])) {
				validBinding = false;
				errors.add("Invalid binding used: " + bindings[i] +
						" is not acceptable for generic " + context.generics[i]);
			}
		}
		return validBinding;
	}
	
	public StructPrototype bindGenerics(StructPrototype structure, VarType[] bindings, GenericContext bindingsContext) {
		
		if(bindings == null)
			return structure;
		
		VariablePrototype[] boundMembers = new VariablePrototype[structure.members.length];
		for(int i = 0; i < boundMembers.length; i++)
			boundMembers[i] = bindMemberGenerics(structure, structure.members[i], bindings, bindingsContext);

		//		FunctionPrototype[] boundFunctions = new FunctionPrototype[structure.functions.length];
		
		OverloadedOperatorPrototype[] boundOperators = new OverloadedOperatorPrototype[structure.overloadedOperators.length];
		for(int i = 0; i < boundOperators.length; i++)
			boundOperators[i] = bindOperatorGenerics(structure, structure.overloadedOperators[i], bindings, bindingsContext);
		
		ConstructorPrototype[] boundConstructors = new ConstructorPrototype[structure.constructors.length];
		for(int i = 0; i < boundConstructors.length; i++)
			boundConstructors[i] = bindConstructorGenerics(structure, structure.constructors[i], bindings, bindingsContext);
				
		return new BoundStructPrototype(
				boundMembers,
				boundConstructors,
				boundOperators,
				bindingsContext,
				structure.modifiers,
				structure);
	}
	
	private VariablePrototype bindMemberGenerics(StructPrototype structure,
			VariablePrototype member, VarType[] bindings, GenericContext bindingsContext) {
		
		VarType boundType = bindType(structure.genericContext, member.type, bindings, bindingsContext);
		if(boundType == member.type)
			return member;
		return new VariablePrototype(member.signature, boundType, member.modifiers);
	}
	
	private FunctionPrototype bindFunctionGenerics(GenericContext context,
			FunctionPrototype func, VarType[] bindings, GenericContext bindingsContext) {
		
		VarFunctionType boundType = (VarFunctionType) bindType(context, func.functionType, bindings, bindingsContext);
		if(boundType == func.functionType)
			return func;
		return new FunctionPrototype(func.signature, boundType, context, func.modifiers);
	}
	
	private OverloadedOperatorPrototype bindOperatorGenerics(StructPrototype structure,
			OverloadedOperatorPrototype operator, VarType[] bindings, GenericContext bindingsContext) {
		
		VarType boundLeft = bindType(structure.genericContext, operator.loType, bindings, bindingsContext);
		VarType boundRight = bindType(structure.genericContext, operator.roType, bindings, bindingsContext);
		VarType boundResult = bindType(structure.genericContext, operator.roType, bindings, bindingsContext);
		FunctionPrototype boundFunc = bindFunctionGenerics(structure.genericContext, operator.function, bindings, bindingsContext); // FIX wrong context
		
		if(boundLeft == operator.loType && boundRight == operator.roType && boundResult == operator.roType &&
				boundFunc == operator.function)
			return operator;
		OverloadedOperatorPrototype bop = new OverloadedOperatorPrototype(
				operator.operator, boundLeft, boundRight, boundResult, operator.signature);
		bop.function = boundFunc;
		return bop;
	}
	
	private ConstructorPrototype bindConstructorGenerics(StructPrototype structure,
			ConstructorPrototype constructor, VarType[] bindings, GenericContext bindingsContext) {
		
		VarType[] boundArgs = bindTypes(structure.genericContext, constructor.argTypes, bindings, bindingsContext);
		if(boundArgs == constructor.argTypes)
			return constructor;
		return new ConstructorPrototype(boundArgs, constructor.argNames,
				constructor.modifiers, constructor.signature);
	}
	
	public VarType bindType(GenericContext context, VarType type, VarType[] bindings, GenericContext bindingsContext) {
		if(type instanceof VarGenericType) {
			int index = context.indexOf((VarGenericType) type);
			if(index == -1)
				return type; // this generic is declared in another context
			// ie. struct<X> that declare a generic function f<Y> that has nothing to do with X
			return bindings[index];
			
		} else if(type instanceof VarNativeType) {
			return type;
			
		} else if(type instanceof VarArrayType) {
			VarType base = bindType(context, ((VarArrayType) type).componentType, bindings, bindingsContext);
			return base == ((VarArrayType) type).componentType ? type : new VarArrayType(base);
			
		} else if(type instanceof VarCompositeType) {
			VarCompositeType composite = (VarCompositeType) type;
			VarType[] bound = bindTypes(context, composite.types, bindings, bindingsContext);
			return bound == composite.types ? type : new VarCompositeType(composite.names, bound);
			
		} else if(type == VarType.VOID) {
			return type;
			
		} else if(type instanceof VarBoundStructType) {
			VarBoundStructType btype = (VarBoundStructType) type;
			VarBoundStructType newType = new VarBoundStructType(btype.name, bindings);
			newType.structure = bindGenerics(btype.structure, bindings, bindingsContext);
			return newType;
			
		} else if(type instanceof VarStructType) {
			return type;
			
		} else if(type instanceof VarFunctionType) {
			VarFunctionType ftype = (VarFunctionType) type;
			GenericContext newContext = ftype.genericContext == context ? GenericContext.NO_CONTEXT : ftype.genericContext;
			VarType returnType = bindType(context, ftype.returnType, bindings, bindingsContext);
			VarType[] args = bindTypes(context, ftype.arguments, bindings, bindingsContext);
			return new VarFunctionType(returnType, args, newContext);
			
		} else {
			throw new UnimplementedException("Unimplemented generic replacement: " + type.getClass());
		}
	}
	
	private VarType[] bindTypes(GenericContext context, VarType[] types, VarType[] bindings, GenericContext bindingsContext) {
		boolean hasGenericUses = false;
		VarType[] boundTypes = new VarType[types.length];
		for(int i = 0; i < boundTypes.length; i++) {
			boundTypes[i] = bindType(context, types[i], bindings, bindingsContext);
			if(boundTypes[i] != types[i])
				hasGenericUses = true;
		}
		return hasGenericUses ? boundTypes : types;
	}
	
}
