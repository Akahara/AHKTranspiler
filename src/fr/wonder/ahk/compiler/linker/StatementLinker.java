package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionExpression;
import fr.wonder.ahk.compiled.expressions.types.VarCompositeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForEachSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.MultipleAffectationSt;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.ArrayOperator;

class StatementLinker {

	static void linkStatements(TypesTable typesTable, Unit unit, Scope scope,
			FunctionSection func, ErrorWrapper errors) {
		
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
			
			// handle special statements
			if(st instanceof ForSt && ((ForSt) st).declaration != null) {
				ForSt forst = (ForSt) st;
				ExpressionLinker.linkExpressions(unit, scope, forst.declaration, typesTable, errors);
				linkStatement(unit, func, forst.declaration, typesTable, errors);
				declareVariable(forst.declaration, scope, errors);
				ExpressionLinker.linkExpressions(unit, scope, forst.affectation, typesTable, errors);
				linkStatement(unit, func, forst.affectation, typesTable, errors);
			}
			
			ExpressionLinker.linkExpressions(unit, scope, st, typesTable, errors);
			linkStatement(unit, func, st, typesTable, errors);
			
			if(st instanceof VariableDeclaration)
				declareVariable((VariableDeclaration) st, scope, errors);
			else if(st instanceof RangedForSt)
				declareVariable(((RangedForSt) st).getVariableDeclaration(), scope, errors);
			else if(st instanceof ForEachSt)
				declareVariable(((ForEachSt) st).declaration, scope, errors);
		}
	}

	private static void declareVariable(VariableDeclaration decl, Scope scope, ErrorWrapper errors) {
		VarAccess declaration = scope.getVariable(decl.name);
		decl.setSignature(new Signature(VarAccess.INNER_UNIT, decl.name, decl.name));
		
		if(declaration instanceof VariablePrototype) {
			errors.add("Redeclaration of existing variable " + decl.name + ":" + decl.getErr());
		} else {
			scope.registerVariable(decl);
		}
	}

	private static void linkStatement(Unit lunit, FunctionSection func, Statement st,
			TypesTable typesTable, ErrorWrapper errors) {
		
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
					if(!ConversionTable.canConvertImplicitely(returnType, func.returnType))
						errors.add("Invalid return type, " + returnType +
								" cannot be converted to " + func.returnType + rst.getErr());
				}
			}
			
		} else if(st instanceof IfSt) {
			Expression condition = ((IfSt) st).getCondition();
			if(!ConversionTable.canConvertImplicitely(condition.getType(), VarType.BOOL))
				errors.add("Invalid expression, conditions can only have the bool type:" + st.getErr());
			
		} else if(st instanceof ElseSt) {
			Expression condition = ((ElseSt) st).getCondition();
			if(condition != null && !ConversionTable.canConvertImplicitely(condition.getType(), VarType.BOOL))
				errors.add("Invalid expression, conditions can only have the bool type:" + st.getErr());
			
		} else if(st instanceof AffectationSt) {
			Linker.checkAffectationType(st, 1, ((AffectationSt) st).getVariable().getType(), errors);
			
		} else if(st instanceof MultipleAffectationSt) {
			MultipleAffectationSt a = (MultipleAffectationSt) st;
			Expression[] variables = a.getVariables();
			Expression[] values = a.getValues();
			VarType[] valuesTypes;
			if(a.isUnwrappedFunction()) {
				if(!(values[0] instanceof FunctionExpression) ||
					!(values[0].getType() instanceof VarCompositeType)) {
					errors.add("Invalid affectation, the right hand side of the"
							+ " assignement is not a composite type " + values[0].getErr());
					return;
				}
				valuesTypes = ((VarCompositeType) values[0].getType()).types;
			} else {
				valuesTypes = ArrayOperator.map(values, VarType[]::new, Expression::getType);
			}
			
			if(valuesTypes.length != variables.length) {
				errors.add("Invalid affectation, " + variables.length + " variables for " 
						+ valuesTypes.length + " values");
				return;
			}
			
			if(!a.isUnwrappedFunction()) {
				for(int i = 0; i < variables.length; i++)
					Linker.checkAffectationType(a, i, variables[i].getType(), errors);
			} else {
				for(int i = 0; i < variables.length; i++) {
					VarType from = valuesTypes[i], to = variables[i].getType();
					if(!from.equals(to))
						errors.add("Type mismatch, " + from + " does not match " + to + " (function"
								+ " unwrapping cannot not use implicit conversions)" + st.getErr());
				}
			}
			
		} else if(st instanceof VariableDeclaration) {
			Linker.checkAffectationType(st, 0, ((VariableDeclaration) st).getType(), errors);
			
		}
	}

}
