package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.LinkedUnit;
import fr.wonder.ahk.compiler.Natives;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.handles.AHKCompiledHandle;
import fr.wonder.ahk.handles.AHKLinkedHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class Linker {
	
	public static AHKLinkedHandle link(AHKCompiledHandle handle, ErrorWrapper errors) throws WrappedException {
		// search for all required native units
		List<Unit> nativeUnits = new ArrayList<>();
		for(Unit u : handle.units) {
			ErrorWrapper nativeImportErrors = errors.subErrrors("Missive native import in unit " + u.fullBase);
			for(String importation : u.importations) {
				if(importation.startsWith(Natives.ahkImportBase)) {
					List<Unit> importedUnits = Natives.getUnits(importation, nativeImportErrors);
					
					if(importedUnits != null) {
						for(Unit iu : importedUnits) {
							if(!nativeUnits.contains(iu))
								nativeUnits.add(iu);
						}
					}
				}
			}
		}
		errors.assertNoErrors();
		
		// the list of linked native required units, separate from linkedUnits to
		// be able to search for importations through native units only
		LinkedUnit[] linkedNatives = new LinkedUnit[nativeUnits.size()];
		// the list of linked units
		LinkedUnit[] linkedUnits = new LinkedUnit[handle.units.length+nativeUnits.size()];
		
		// check unit duplicates
		for(int u = 0; u < handle.units.length; u++) {
			Unit unit = handle.units[u];
			linkedUnits[u] = new LinkedUnit(unit);
			for(int j = 0; j < u; j++) {
				if(handle.units[j].fullBase.equals(unit.fullBase))
					errors.add("Duplicate unit found with base " + unit.fullBase);
			}	
		}
		// create linked native units, there cannot be duplicates at this point
		// also bind importations as a native unit can only import other natives
		for(int u = 0; u < linkedNatives.length; u++)
			linkedNatives[u] = new LinkedUnit(nativeUnits.get(u));
		for(int u = 0; u < linkedNatives.length; u++) {
			LinkedUnit lu = linkedNatives[u];
			for(int i = 0; i < lu.importations.length; i++)
				lu.importations[i] = searchImportedUnit(linkedNatives, nativeUnits.get(u).importations[i]);
			linkedUnits[handle.units.length + u] = lu;
		}

		errors.assertNoErrors();
		
		AHKLinkedHandle linkedHandle = new AHKLinkedHandle(linkedUnits, linkedNatives);
		
		// link all project unit importations (not native units)
		for(int u = 0; u < handle.units.length; u++) {
			Unit unit = handle.units[u];
			for(int i = 0; i < unit.importations.length; i++) {
				String importation = unit.importations[i];
				// search in the project & native units
				linkedUnits[u].importations[i] = searchImportedUnit(linkedUnits, importation);
				if(linkedUnits[u].importations[i] == null)
					errors.add("Missing importation in unit " + unit.fullBase + " for " + importation);
			}
		}
		
		errors.assertNoErrors();
		
		// prelink units
		for(LinkedUnit lunit : linkedUnits) {
			prelinkUnit(lunit);
		}
		
		// actually link unit statements
		for(int u = 0; u < linkedUnits.length; u++) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to link unit " + linkedUnits[u].fullBase);
			ValueDeclaration[] externFields = linkUnit(linkedHandle, linkedUnits[u], subErrors);
			linkedHandle.externFields.put(linkedUnits[u], externFields);
		}
		
		errors.assertNoErrors();
		
		return linkedHandle;
	}
	
	/** Returns the unit of {@code units} which full base is {@code fullBase}, null if none matches */
	private static LinkedUnit searchImportedUnit(LinkedUnit[] units, String fullBase) {
		for(LinkedUnit lu : units) {
			if(lu.fullBase.equals(fullBase))
				return lu;
		}
		return null;
	}
	
	/** Computes functions and variables signatures */
	private static void prelinkUnit(LinkedUnit lunit) {
		for(FunctionSection func : lunit.functions) {
			func.setSignature(new Signature(lunit, func.name, func.name + "_" +
					func.getFunctionType().getSignature()));
		}
	}
	
	/**
	 * Assumes that all types and signatures have been computed already.
	 * 
	 * @return all fields that can be accessed globally
	 */
	private static ValueDeclaration[] linkUnit(AHKLinkedHandle handle, LinkedUnit lunit, ErrorWrapper errors) {
		List<ValueDeclaration> externFields = new ArrayList<>();
		
		for(int i = 0; i < lunit.variables.length; i++) {
			VariableDeclaration var = lunit.variables[i];
			if(var.getVisibility() == DeclarationVisibility.GLOBAL)
				externFields.add(var);
			linkExpressions(lunit, new UnitScope(lunit), var.getExpressions(), handle.typesTable, errors);
			for(int j = 0; j < i; j++) {
				if(lunit.variables[j].name.equals(var.name)) {
					errors.add("Two variables have the same name: " + var.name +
							lunit.variables[j].getErr() + lunit.variables[i].getErr());
				}
			}
		}
		
		for(int i = 0; i < lunit.functions.length; i++) {
			FunctionSection func = lunit.functions[i];
			
			if(func.getVisibility() == DeclarationVisibility.GLOBAL)
				externFields.add(func);
			
			// check variable name conflicts
			for(VariableDeclaration var : lunit.variables) {
				if(var.name.equals(func.name))
					errors.add("A function has the same name as a variable: " + var.name + func.getErr() + var.getErr());
			}
			
			// check signatures duplicates
			String funcSig = func.getSignature().computedSignature;
			for(int j = 0; j < i; j++) {
				if(lunit.functions[j].getSignature().computedSignature.equals(funcSig)) {
					errors.add("Two functions have the same signature: " + funcSig +
							lunit.functions[j].getErr() + lunit.functions[i].getErr());
				}
			}
			
			// check duplicate names in arguments
			for(int j = 1; j < func.arguments.length; j++) {
				for(int k = 0; k < j; k++) {
					if(func.arguments[j].name.equals(func.arguments[k].name))
						errors.add("Two arguments have the same name" + func.getErr());
				}
			}
			
			// link variables
			linkStatements(handle, lunit, func, errors.subErrrors("Errors in function " + funcSig));
		}
		
		return externFields.toArray(ValueDeclaration[]::new);
	}
	
	private static void linkStatements(AHKLinkedHandle handle, LinkedUnit lunit, FunctionSection func, ErrorWrapper errors) {
		Scope scope = new UnitScope(lunit).innerScope();
		List<LabeledStatement> openedSections = new ArrayList<>();
		LabeledStatement latestClosedStatement = null;
		
		for(FunctionArgument arg : func.arguments)
			scope.registerVariable(arg);
		
		for(Statement st : func.body) {
			if(st instanceof SectionEndSt) {
				scope = scope.outerScope();
				latestClosedStatement = openedSections.remove(openedSections.size()-1);
				latestClosedStatement.sectionEnd = (SectionEndSt)st;
				((SectionEndSt) st).closedStatement = latestClosedStatement;
				
			} else if(st instanceof LabeledStatement) {
				scope = scope.innerScope();
				openedSections.add((LabeledStatement) st);
				
				if(st instanceof ElseSt) {
					if(!(latestClosedStatement instanceof IfSt)) {
						errors.add("Else statement without if " + st.getErr());
					} else {
						((ElseSt) st).closedIf = (IfSt) latestClosedStatement;
						((IfSt) latestClosedStatement).elseStatement = (ElseSt) st;
					}
				}
				
				latestClosedStatement = null;
			}
			
			if(st instanceof ForSt && ((ForSt) st).declaration != null) {
				linkExpressions(lunit, scope, ((ForSt) st).declaration.expressions, handle.typesTable, errors);
				declareVariable(lunit, ((ForSt) st).declaration, scope, errors);
			}
			
			linkExpressions(lunit, scope, st.expressions, handle.typesTable, errors);
			linkStatement(lunit, func, st, handle.typesTable, errors);
			
			if(st instanceof VariableDeclaration)
				declareVariable(lunit, (VariableDeclaration) st, scope, errors);
		}
	}
	
	private static void declareVariable(LinkedUnit lunit, VariableDeclaration decl, Scope scope, ErrorWrapper errors) {
		ValueDeclaration declaration = scope.getVariable(decl.name);
		if(declaration instanceof VariableDeclaration) {
			errors.add("Redeclaration of existing variable " + decl.name + ":" + decl.getErr() + declaration.getErr());
		} else {
			scope.registerVariable(decl);
		}
	}
	
	private static void linkExpressions(LinkedUnit lunit, Scope scope, Expression[] expressions, TypesTable typesTable, ErrorWrapper errors) {
		
		for(int i = 0; i < expressions.length; i++) {
			Expression exp = expressions[i];
			
			linkExpressions(lunit, scope, exp.expressions, typesTable, errors);
			
			if(exp instanceof VarExp) {
				// search for the variable/function declaration
				VarExp vexp = (VarExp) exp;
				ValueDeclaration var = scope.getVariable(vexp.variable);
				vexp.declaration = var;
				
				if(var == null) {
					errors.add("Usage of undeclared variable " + vexp.variable + vexp.getErr());
					vexp.varType = VarType.NULL;
				} else {
					vexp.varType = var.getType();
				}
				
			} else if(exp instanceof ArrayExp) {
				ArrayExp array = (ArrayExp) exp;
				if(array.getLength() == 0) {
					array.type = new VarArrayType(VarType.VOID);
				} else {
					Expression[] values = array.getValues();
					VarType componentsType = values[0].getType();
					for(int j = 1; j < values.length; j++) {
						componentsType = typesTable.conversions.getCommonParent(componentsType, values[j].getType());
						if(componentsType == null)
							break;
					}
					if(componentsType == null) {
						errors.add("No shared type in array declaration" + array.getErr());
						componentsType = VarType.VOID;
					}
					array.type = new VarArrayType(componentsType);
				}
				
			} else if(exp instanceof FunctionCallExp) {
				FunctionCallExp fexp = (FunctionCallExp) exp;
				if(fexp.getFunction() instanceof VarExp) {
					// replace the FunctionCallExp by a FunctionExp
					FunctionSection function = searchMatchingFunction(lunit, fexp, typesTable, errors);
					if(function != null) {
						FunctionExp functionExpression = new FunctionExp(lunit.source, fexp, function);
						exp = expressions[i] = functionExpression;
						for(int j = 0; j < fexp.getArguments().length; j++) {
							Expression argument = functionExpression.getArguments()[j];
							if(function.argumentTypes[j] != argument.getType()) {
								ConversionExp conv = new ConversionExp(lunit.source, argument, function.argumentTypes[j], true);
								functionExpression.expressions[j] = conv;
							}
						}
					} else {
						// TODO implement functions as variables
						errors.add("No matching function " + fexp.getErr());
					}
				} else {
					// TODO implement functions as expressions
					errors.add(">>> Unimplemented function call!" + fexp.getErr());
				}
				
			} else if(exp instanceof OperationExp) {
				OperationExp oexp = (OperationExp) exp;
				Operation op = typesTable.getOperation(oexp);
				if(op == null) {
					errors.add("Unimplemented operation! " + oexp.operationString() + oexp.getErr());
					oexp.setOperation(Operation.NOOP);
				} else {
					// #setOperation replaces the operation expression operands by cast expressions if needed
					oexp.setOperation(op);
				}
			}
			
			exp.computeValueType(typesTable, errors);
		}
	}
	
	private static FunctionSection searchMatchingFunction(LinkedUnit lunit, FunctionCallExp fexp, TypesTable typesTable, ErrorWrapper errors) {
		UnitScope unitScope = new UnitScope(lunit);
		String funcName = ((VarExp) fexp.getFunction()).variable;
		VarType[] argumentsTypes = fexp.getArgumentsTypes();
		{ // search without casting
			FunctionSection function = unitScope.getFunctionStrict(funcName, argumentsTypes);
			if(function != null)
				return function;
		}
		{ // search with a single cast
			int singleConversionCount = unitScope.countMatchingFunction1c(funcName, argumentsTypes, typesTable.conversions);
			if(singleConversionCount == 1)
				return unitScope.getFunction1c(funcName, argumentsTypes, typesTable.conversions);
			if(singleConversionCount > 1) {
				errors.add("Multiple matching functions " + fexp.getErr());
				return null;
			}
		}
		{ // search with any number of casts
			int anyConversionCount = unitScope.countMatchingFunctionXc(funcName, argumentsTypes, typesTable.conversions);
			if(anyConversionCount == 1)
				return unitScope.getFunctionXc(funcName, argumentsTypes, typesTable.conversions);
			if(anyConversionCount > 1) {
				errors.add("Multiple matching functions " + fexp.getErr());
				return null;
			}
			return null;
		}
	}
	
	private static void linkStatement(LinkedUnit lunit, FunctionSection func, Statement st, TypesTable typesTable, ErrorWrapper errors) {
		
		if(st instanceof ReturnSt) {
			ReturnSt rst = (ReturnSt) st;
			if(func.returnType == VarType.VOID) {
				if(rst.getExpression() != null)
					errors.add("A void function cannot return a value:" + rst.getErr());
			} else {
				Expression returnExp = rst.getExpression();
				if(returnExp == null) {
					errors.add("This function must return a value of type " + func.returnType + rst.getErr());
				} else {
					VarType returnType = returnExp.getType();
					if(!typesTable.conversions.canConvertImplicitely(returnType, func.returnType))
						errors.add("Invalid return type, " + returnType + " cannot be converted to " + func.returnType + rst.getErr());
				}
			}
			
		} else if(st instanceof IfSt) {
			Expression condition = ((IfSt) st).getCondition();
			if(!typesTable.conversions.canConvertImplicitely(condition.getType(), VarType.BOOL))
				errors.add("Invalid expression, conditions can only have the bool type:" + st.getErr());
			
		} else if(st instanceof ElseSt) {
			Expression condition = ((ElseSt) st).getCondition();
			if(condition != null && !typesTable.conversions.canConvertImplicitely(condition.getType(), VarType.BOOL))
				errors.add("Invalid expression, conditions can only have the bool type:" + st.getErr());
			
		}
	}
	
}
