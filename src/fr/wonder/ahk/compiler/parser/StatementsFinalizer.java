package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarCompositeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.CompositeReturnSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;

public class StatementsFinalizer {

	private static final Map<Class<? extends LabeledStatement>, Class<? extends LabeledStatement>> sectionsPairs = Map.of(
			IfSt.class, ElseSt.class
	);

	/** Adds functionEndSt to complete single line ifs, elses ... */
	public static void finalizeStatements(FunctionSection function) {
		UnitSource source = function.getSource();
		
		List<Statement> statements = new ArrayList<>(Arrays.asList(function.body));
		
		// close single line statements
		for(int s = 0; s < statements.size(); s++) {
			Statement st = statements.get(s);
			if(st instanceof LabeledStatement) {
				s = closeStatement(statements, s)-1;
			}
		}
		
		Statement lastStatement = statements.isEmpty() ? null : statements.get(statements.size()-1);
		if(function.returnType != VarType.VOID &&
				!(lastStatement instanceof ReturnSt || lastStatement instanceof CompositeReturnSt)) {
			int sourceLoc = lastStatement == null ? function.sourceStop : lastStatement.sourceStop;
			
			if(function.returnType instanceof VarCompositeType) {
				VarCompositeType composite = (VarCompositeType) function.returnType;
				Expression[] returnValues = new Expression[composite.types.length];
				for(int i = 0; i < returnValues.length; i++)
					returnValues[i] = StatementParser.getDefaultValue(composite.types[i], source, sourceLoc);
				statements.add(new CompositeReturnSt(source, sourceLoc, -1, returnValues));
			} else {
				Expression returned = StatementParser.getDefaultValue(function.returnType, source, sourceLoc);
				statements.add(new ReturnSt(source, sourceLoc, -1, returned));
			}
		}
		
		function.body = statements.toArray(Statement[]::new);
	}

	private static int closeStatement(List<Statement> statements, int idx) {
		LabeledStatement toClose = (LabeledStatement) statements.get(idx);
		if(toClose.singleLine) {
			if(statements.size() == idx) {
				statements.add(new SectionEndSt(toClose.getSource(), statements.get(statements.size()-1).sourceStop));
				return statements.size();
			} else if(statements.get(idx+1) instanceof LabeledStatement) {
				idx = closeStatement(statements, idx+1);
			} else {
				idx += 2;
			}
			statements.add(idx, new SectionEndSt(toClose.getSource(), statements.get(idx-1).sourceStop));
			idx++;
			// handle section-end special cases
			if(statements.size() != idx && StatementsFinalizer.sectionsPairs.get(toClose.getClass()) == statements.get(idx).getClass())
				idx = closeStatement(statements, idx);
			return idx;
		} else {
			for(int s = idx+1; s < statements.size(); s++) {
				Statement st = statements.get(s);
				if(st instanceof LabeledStatement) {
					s = closeStatement(statements, s);
				} else if(st instanceof SectionEndSt) {
					s++;
					// handle section-end special cases
					if(statements.size() != s && StatementsFinalizer.sectionsPairs.get(toClose.getClass()) == statements.get(idx).getClass())
						s = closeStatement(statements, s);
					return s;
				}
			}
			return statements.size();
		}
	}

}
