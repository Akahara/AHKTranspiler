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
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.ArrayOperator;

class StatementLinker {

	private final Linker linker;
	
	StatementLinker(Linker linker) {
		this.linker = linker;
	}
	
	void linkStatements(Unit unit, Scope scope, FunctionSection func, ErrorWrapper errors) {
		
		List<LabeledStatement> openedSections = new ArrayList<>();
		LabeledStatement latestClosedStatement = null;
		
		for(FunctionArgument arg : func.arguments) {
			scope.registerVariable(arg);
		}
		
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
				linker.expressions.linkExpressions(unit, scope, forst.declaration, func.genericContext, errors);
				linkStatement(unit, func, forst.declaration, errors);
				declareVariable(forst.declaration, scope, errors);
				linker.expressions.linkExpressions(unit, scope, forst.affectation, func.genericContext, errors);
				linkStatement(unit, func, forst.affectation, errors);
			}
			
			linker.expressions.linkExpressions(unit, scope, st, func.genericContext, errors);
			linkStatement(unit, func, st, errors);
			
			if(st instanceof VariableDeclaration)
				declareVariable((VariableDeclaration) st, scope, errors);
			else if(st instanceof RangedForSt)
				declareVariable(((RangedForSt) st).getVariableDeclaration(), scope, errors);
			else if(st instanceof ForEachSt)
				declareVariable(((ForEachSt) st).declaration, scope, errors);
		}
	}

	private void declareVariable(VariableDeclaration decl, Scope scope, ErrorWrapper errors) {
		VarAccess declaration = scope.getVariable(decl.name);
		decl.setSignature(Signatures.scopedVariableSignature(decl.name));
		
		if(declaration != null) {
			errors.add("Redeclaration of existing variable " + decl.name + ":" + decl.getErr());
		} else {
			scope.registerVariable(decl.getPrototype());
		}
	}

	private void linkStatement(Unit lunit, FunctionSection func, Statement st, ErrorWrapper errors) {
		
		if(st instanceof ReturnSt) {
			linkReturnSt(func, (ReturnSt) st, errors);
			
		} else if(st instanceof IfSt) {
			linkIfSt((IfSt) st, errors);
			
		} else if(st instanceof ElseSt) {
			linkElseSt((ElseSt) st, errors);
			
		} else if(st instanceof AffectationSt) {
			linkAffectationSt((AffectationSt) st, errors);
			
		} else if(st instanceof MultipleAffectationSt) {
			linkMultipleAffectationSt((MultipleAffectationSt) st, errors);
			
		} else if(st instanceof VariableDeclaration) {
			linkVariableDeclarationSt((VariableDeclaration) st, errors);
			
		}
	}

	private void linkElseSt(ElseSt st, ErrorWrapper errors) {
		Expression condition = st.getCondition();
		if(condition != null && !ConversionTable.canConvertImplicitely(condition.getType(), VarType.BOOL))
			errors.add("Invalid expression, conditions can only have the bool type:" + st.getErr());
	}

	private void linkAffectationSt(AffectationSt st, ErrorWrapper errors) {
		linker.checkAffectationType(st, 1, st.getVariable().getType(), errors);
	}

	private void linkMultipleAffectationSt(MultipleAffectationSt st, ErrorWrapper errors) {
		Expression[] variables = st.getVariables();
		Expression[] values = st.getValues();
		VarType[] valuesTypes;
		if(st.isUnwrappedFunction()) {
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
		
		if(!st.isUnwrappedFunction()) {
			for(int i = 0; i < variables.length; i++)
				linker.checkAffectationType(st, i, variables[i].getType(), errors);
		} else {
			for(int i = 0; i < variables.length; i++) {
				VarType from = valuesTypes[i], to = variables[i].getType();
				if(!from.equals(to))
					errors.add("Type mismatch, " + from + " does not match " + to + " (function"
							+ " unwrapping cannot not use implicit conversions)" + st.getErr());
			}
		}
	}

	private void linkVariableDeclarationSt(VariableDeclaration st, ErrorWrapper errors) {
		linker.checkAffectationType(st, 0, st.getType(), errors);
	}

	private void linkReturnSt(FunctionSection func, ReturnSt st, ErrorWrapper errors) {
		Expression returned = st.getExpression();
		if(func.returnType == VarType.VOID) {
			if(returned != null) {
				errors.add("A void function cannot return a value:" + st.getErr());
				return;
			}
		} else {
			if(returned == null) {
				errors.add("This function must return a value of type " + func.returnType + st.getErr());
				return;
			}
			linker.checkAffectationType(st, 0, func.returnType, errors);
		}
	}

	private void linkIfSt(IfSt st, ErrorWrapper errors) {
		Expression condition = st.getCondition();
		if(!ConversionTable.canConvertImplicitely(condition.getType(), VarType.BOOL))
			errors.add("Invalid expression, conditions can only have the bool type:" + st.getErr());
	}

}
