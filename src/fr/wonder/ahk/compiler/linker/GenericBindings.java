package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarBoundStructType;
import fr.wonder.ahk.compiled.expressions.types.VarCompositeType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarSelfType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.BoundStructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintImplementation;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintTypeParameter;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.GenericImplementationParameter;
import fr.wonder.ahk.compiled.units.sections.BlueprintRef;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.utils.StringUtils;

public class GenericBindings {
	
	public GenericBindings() {}
	
	/**
	 * Validates generic bindings for a context.
	 * 
	 * <p>
	 * If the bindings are unacceptable an error is logged and false is returned,
	 * otherwise true is returned.
	 */
	public static boolean validateBindings(GenericContext receiverContext, VarType[] bindings,
			SourceElement queryElement, ErrorWrapper errors) {
		if(receiverContext.hasGenericMembers() && bindings == null) {
			errors.add("Expected generic bindings:" + queryElement.getErr());
			return false;
		}
		if(!receiverContext.hasGenericMembers()) {
			if(bindings != null) {
				errors.add("Unexpected generic bindings:" + queryElement.getErr());
				return false;
			}
			return true;
		}
		
		if(receiverContext.generics.length != bindings.length) {
			errors.add("Invalid number of generics, type expected " + receiverContext.generics.length +
					" but got " + bindings.length + queryElement.getErr());
			return false;
		}
		
		boolean validBinding = true;
		for(int i = 0; i < bindings.length; i++) {
			if(!isValidBinding(receiverContext.generics[i], bindings[i])) {
				validBinding = false;
				errors.add("Invalid binding used: " + bindings[i] +
						" is not acceptable for generic " + receiverContext.generics[i] +
						" (requires " + StringUtils.join(",", receiverContext.generics[i].typeRestrictions, bpref->bpref.blueprint.getName()) +
						")"+ queryElement.getErr());
			}
		}
		return validBinding;
	}
	
	private static boolean isValidBinding(VarGenericType generic, VarType binding) {
		if(generic.typeRestrictions.length == 0)
			return true;
		if(binding instanceof VarStructType) {
			StructPrototype structure = ((VarStructType) binding).structure;
			for(BlueprintRef requiredBlueprint : generic.typeRestrictions) {
				boolean found = false;
				for(BlueprintImplementation impl : structure.implementedBlueprints) {
					if(impl.bpRef.equals(requiredBlueprint)) {
						found = true;
						break;
					}
				}
				if(!found)
					return false;
			}
			return true;
		} else if(binding instanceof VarGenericType) {
			BlueprintRef[] implementedBlueprints = ((VarGenericType) binding).typeRestrictions;
			for(BlueprintRef requiredBlueprint : generic.typeRestrictions) {
				boolean found = false;
				for(BlueprintRef impl : implementedBlueprints) {
					if(impl.equals(requiredBlueprint)) {
						found = true;
						break;
					}
				}
				if(!found)
					return false;
			}
			return true;
		} else { // TODO native types default blueprints
			return false;
		}
	}
	
	public StructPrototype bindGenerics(StructPrototype structure, VarType[] bindings, GenericContext bindingsContext) {
		
		if(bindings == null)
			return structure;
		
		VarStructType selfBinding = new VarStructType(structure.getName());
		
		VariablePrototype[] boundMembers = new VariablePrototype[structure.members.length];
		for(int i = 0; i < boundMembers.length; i++)
			boundMembers[i] = bindMemberGenerics(structure, structure.members[i], bindings, selfBinding, bindingsContext);

//		FunctionPrototype[] boundFunctions = new FunctionPrototype[structure.functions.length];
		
		OverloadedOperatorPrototype[] boundOperators = new OverloadedOperatorPrototype[structure.overloadedOperators.length];
		for(int i = 0; i < boundOperators.length; i++)
			boundOperators[i] = bindOperatorGenerics(structure, structure.overloadedOperators[i], bindings, selfBinding, bindingsContext);
		
		ConstructorPrototype[] boundConstructors = new ConstructorPrototype[structure.constructors.length];
		for(int i = 0; i < boundConstructors.length; i++)
			boundConstructors[i] = bindConstructorGenerics(structure, structure.constructors[i], bindings, selfBinding, bindingsContext);
				
		return new BoundStructPrototype(
				boundMembers,
				boundConstructors,
				boundOperators,
				bindingsContext,
				structure.implementedBlueprints,
				structure.modifiers,
				structure);
	}
	
	private VariablePrototype bindMemberGenerics(StructPrototype structure,
			VariablePrototype member, VarType[] bindings, VarType selfBinding, GenericContext bindingsContext) {
		
		VarType boundType = bindType(structure.genericContext, member.type, bindings, selfBinding, bindingsContext);
		if(boundType == member.type)
			return member;
		return new VariablePrototype(member.signature, boundType, member.modifiers);
	}
	
	private FunctionPrototype bindFunctionGenerics(GenericContext context,
			FunctionPrototype func, VarType[] bindings, VarType selfBinding, GenericContext bindingsContext) {
		
		VarFunctionType boundType = (VarFunctionType) bindType(context, func.functionType, bindings, selfBinding, bindingsContext);
		if(boundType == func.functionType)
			return func;
		return new FunctionPrototype(func.signature, boundType, context, func.modifiers);
	}
	
	private OverloadedOperatorPrototype bindOperatorGenerics(StructPrototype structure,
			OverloadedOperatorPrototype operator, VarType[] bindings, VarType selfBinding, GenericContext bindingsContext) {
		
		VarType boundLeft = bindType(structure.genericContext, operator.loType, bindings, selfBinding, bindingsContext);
		VarType boundRight = bindType(structure.genericContext, operator.roType, bindings, selfBinding, bindingsContext);
		VarType boundResult = bindType(structure.genericContext, operator.resultType, bindings, selfBinding, bindingsContext);
		FunctionPrototype boundFunc = bindFunctionGenerics(structure.genericContext, operator.function, bindings, selfBinding, bindingsContext); // FIX wrong context
		
		if(boundLeft == operator.loType && boundRight == operator.roType && boundResult == operator.roType &&
				boundFunc == operator.function)
			return operator;
		OverloadedOperatorPrototype bop = new OverloadedOperatorPrototype(
				operator.operator, boundLeft, boundRight, boundResult, operator.signature);
		bop.function = boundFunc;
		return bop;
	}
	
	private ConstructorPrototype bindConstructorGenerics(StructPrototype structure,
			ConstructorPrototype constructor, VarType[] bindings, VarType selfBinding, GenericContext bindingsContext) {
		
		VarType[] boundArgs = bindTypes(structure.genericContext, constructor.argTypes, bindings, selfBinding, bindingsContext);
		if(boundArgs == constructor.argTypes)
			return constructor;
		return new ConstructorPrototype(boundArgs, constructor.argNames,
				constructor.modifiers, constructor.signature);
	}
	
	public VarType bindType(GenericContext context, VarType type, VarType[] bindings, VarType selfBinding, GenericContext bindingsContext) {
		if(type instanceof VarGenericType) {
			int index = context.indexOf((VarGenericType) type);
			if(index == -1)
				return type; // this generic is declared in another context
			// ie. struct<X> that declare a generic function f<Y> that has nothing to do with X
			return bindings[index];
			
		} else if(type instanceof VarNativeType) {
			return type;
			
		} else if(type instanceof VarArrayType) {
			VarType base = bindType(context, ((VarArrayType) type).componentType, bindings, selfBinding, bindingsContext);
			return base == ((VarArrayType) type).componentType ? type : new VarArrayType(base);
			
		} else if(type instanceof VarCompositeType) {
			VarCompositeType composite = (VarCompositeType) type;
			VarType[] bound = bindTypes(context, composite.types, bindings, selfBinding, bindingsContext);
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
			VarType returnType = bindType(context, ftype.returnType, bindings, selfBinding, bindingsContext);
			VarType[] args = bindTypes(context, ftype.arguments, bindings, selfBinding, bindingsContext);
			return new VarFunctionType(returnType, args, newContext);
			
		} else if(type == VarSelfType.SELF) {
			if(selfBinding == null)
				throw new IllegalArgumentException("Self type encountered in non-struct context");
			return selfBinding;
			
		} else {
			throw new UnimplementedException("Unimplemented generic replacement: " + type.getClass());
		}
	}
	
	private VarType[] bindTypes(GenericContext context, VarType[] types, VarType[] bindings, VarType selfBinding, GenericContext bindingsContext) {
		boolean hasGenericUses = false;
		VarType[] boundTypes = new VarType[types.length];
		for(int i = 0; i < boundTypes.length; i++) {
			boundTypes[i] = bindType(context, types[i], bindings, selfBinding, bindingsContext);
			if(boundTypes[i] != types[i])
				hasGenericUses = true;
		}
		return hasGenericUses ? boundTypes : types;
	}

	public BlueprintTypeParameter[] createBPTPs(GenericContext genericContext, VarType[] genericBindings) {
		BlueprintTypeParameter[] typesParameters = new BlueprintTypeParameter[genericContext.gips.length];
		int gipIndex = 0;
		for(int i = 0; i < genericContext.gips.length; i++) {
			typesParameters[gipIndex++] = getBPTP(genericContext.gips[i], (VarStructType) genericBindings[i]);
		}
		return typesParameters;
	}
	
	private BlueprintTypeParameter getBPTP(GenericImplementationParameter gip, VarStructType binding) {
		
		for(BlueprintImplementation impl : binding.structure.implementedBlueprints) {
			if(impl.bpRef.equals(gip.typeRequirement))
				return new BlueprintTypeParameter(gip, impl);
		}
		throw new UnreachableException("Invalid type binding, structure " +
				binding + " does not implement " + gip.typeRequirement);
	}

}
