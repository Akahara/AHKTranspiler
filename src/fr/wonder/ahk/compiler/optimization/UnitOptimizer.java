package fr.wonder.ahk.compiler.optimization;

import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.handles.ProjectHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class UnitOptimizer {
	
	public static void optimize(ProjectHandle handle, Unit u, ErrorWrapper errors) {
		if(handle.manifest.LITERAL_OPTIMIZATION)
			optimizeExpressions(u, errors.subErrors("Errors while optimizing expressions"));
	}
	
	private static void optimizeExpressions(Unit u, ErrorWrapper errors) {
		for(VariableDeclaration var : u.variables) {
			ExpressionOptimizer.optimizeExpressions(var, errors);
		}
		for(FunctionSection func : u.functions) {
			for(Statement statement : func.body) {
				ExpressionOptimizer.optimizeExpressions(statement, errors);
			}
		}
		for(StructSection structure : u.structures) {
			for(VariableDeclaration member : structure.members) {
				ExpressionOptimizer.optimizeExpressions(member, errors);
			}
			for(ConstructorDefaultValue nullField : structure.nullFields) {
				ExpressionOptimizer.optimizeExpressions(nullField, errors);
			}
		}
	}
	
}
