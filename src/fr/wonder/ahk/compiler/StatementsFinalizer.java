package fr.wonder.ahk.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;

public class StatementsFinalizer {

	private static final Map<Class<? extends LabeledStatement>, Class<? extends LabeledStatement>> sectionsPairs = Map.of(
			IfSt.class, ElseSt.class
	);

	/** Adds functionEndSt to complete single line ifs, elses ... */
	public static void finalizeStatements(UnitSource source, FunctionSection function) {
		List<Statement> statements = new ArrayList<>(Arrays.asList(function.body));
		
		// close single line statements
		for(int s = 0; s < statements.size(); s++) {
			Statement st = statements.get(s);
			if(st instanceof LabeledStatement) {
				s = closeStatement(source, statements, s)-1;
			}
		}
		
		function.body = statements.toArray(Statement[]::new);
	}

	private static int closeStatement(UnitSource source, List<Statement> statements, int idx) {
		LabeledStatement toClose = (LabeledStatement) statements.get(idx);
		if(toClose.singleLine) {
			if(statements.size() == idx) {
				statements.add(new SectionEndSt(source, statements.get(statements.size()-1).sourceStop));
				return statements.size();
			} else if(statements.get(idx+1) instanceof LabeledStatement) {
				idx = closeStatement(source, statements, idx+1);
			} else {
				idx += 2;
			}
			statements.add(idx, new SectionEndSt(source, statements.get(idx-1).sourceStop));
			idx++;
			// handle section-end special cases
			if(statements.size() != idx && StatementsFinalizer.sectionsPairs.get(toClose.getClass()) == statements.get(idx).getClass())
				idx = closeStatement(source, statements, idx);
			return idx;
		} else {
			for(int s = idx+1; s < statements.size(); s++) {
				Statement st = statements.get(s);
				if(st instanceof LabeledStatement) {
					s = closeStatement(source, statements, s);
				} else if(st instanceof SectionEndSt) {
					s++;
					// handle section-end special cases
					if(statements.size() != s && StatementsFinalizer.sectionsPairs.get(toClose.getClass()) == statements.get(idx).getClass())
						s = closeStatement(source, statements, s);
					return s;
				}
			}
			return statements.size();
		}
	}

}
