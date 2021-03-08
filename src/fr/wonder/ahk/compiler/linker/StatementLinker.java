package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

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
				ExpressionLinker.linkExpressions(unit, scope, ((ForSt) st).declaration.getExpressions(), typesTable, errors);
				declareVariable(((ForSt) st).declaration, scope, errors);
			}
			
			ExpressionLinker.linkExpressions(unit, scope, st.getExpressions(), typesTable, errors);
			linkStatement(unit, func, st, typesTable, errors);
			
			if(st instanceof VariableDeclaration)
				declareVariable((VariableDeclaration) st, scope, errors);
			if(st instanceof RangedForSt)
				declareVariable(((RangedForSt) st).getVariableDeclaration(), scope, errors);
		}
	}

	private static void declareVariable(VariableDeclaration decl, Scope scope, ErrorWrapper errors) {
		VarAccess declaration = scope.getVariable(decl.name);
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
					if(!typesTable.conversions.canConvertImplicitely(returnType, func.returnType))
						errors.add("Invalid return type, " + returnType +
								" cannot be converted to " + func.returnType + rst.getErr());
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
