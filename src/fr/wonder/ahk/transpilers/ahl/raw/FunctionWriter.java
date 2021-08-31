package fr.wonder.ahk.transpilers.ahl.raw;

import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.MultipleAffectationSt;
import fr.wonder.ahk.compiled.statements.OperationSt;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.statements.WhileSt;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.ahl.InstructionSequence;
import fr.wonder.commons.exceptions.UnimplementedException;

public class FunctionWriter {
	
	public final UnitWriter writer;
	public final FunctionSection func;
	public final ExpressionWriter expressions;
	public final InstructionSequence instructions = new InstructionSequence();
	
	private FunctionWriter(UnitWriter writer, FunctionSection func) {
		this.writer = writer;
		this.func = func;
		this.expressions = new ExpressionWriter(this);
	}
	
	public static InstructionSequence writeFunction(UnitWriter writer, FunctionSection func) {
		FunctionWriter fw = new FunctionWriter(writer, func);
		fw.writeFunction();
		return fw.instructions;
	}
	
	private void writeFunction() {
		for(int i = 0; i < func.body.length; i++) {
			Statement st = func.body[i];
			
			if(st instanceof SectionEndSt) {
				writeSectionEndStatement((SectionEndSt) st);
				
			} else if(st instanceof VariableDeclaration) {
				writeVarDeclaration((VariableDeclaration) st);
				
			} else if(st instanceof IfSt) {
				writeIfStatement((IfSt) st);
				
			} else if(st instanceof ElseSt) {
				writeElseStatement((ElseSt) st);
				
			} else if(st instanceof WhileSt) {
				writeWhileStatement((WhileSt) st);
				
			} else if(st instanceof ForSt) {
				writeForStatement((ForSt) st);
				
			} else if(st instanceof RangedForSt) {
				writeRangedForStatement((RangedForSt) st);
				
			} else if(st instanceof ReturnSt) {
				writeReturnStatement((ReturnSt) st);
				
			} else if(st instanceof FunctionSt) {
				writeFunctionStatement((FunctionSt) st);
				
			} else if(st instanceof OperationSt) {
				writeOperationStatement((OperationSt) st);
				
			} else if(st instanceof AffectationSt) {
				writeAffectationStatement((AffectationSt) st);
				
			} else if(st instanceof MultipleAffectationSt) {
				writeMultipleAffectationSt((MultipleAffectationSt) st);
				
			} else {
				throw new UnimplementedException("Unhandled statement type: " + st.getClass().getSimpleName());
			}
		}
	}

	private void writeMultipleAffectationSt(MultipleAffectationSt st) {
		throw new UnimplementedException();
	}

	private void writeAffectationStatement(AffectationSt st) {
		throw new UnimplementedException();
	}

	private void writeOperationStatement(OperationSt st) {
		throw new UnimplementedException();
	}

	private void writeFunctionStatement(FunctionSt st) {
		
	}

	private void writeReturnStatement(ReturnSt st) {
		throw new UnimplementedException();
	}

	private void writeRangedForStatement(RangedForSt st) {
		throw new UnimplementedException();
	}

	private void writeForStatement(ForSt st) {
		throw new UnimplementedException();
	}

	private void writeWhileStatement(WhileSt st) {
		throw new UnimplementedException();
	}

	private void writeElseStatement(ElseSt st) {
		throw new UnimplementedException();
	}

	private void writeIfStatement(IfSt st) {
		throw new UnimplementedException();
	}

	private void writeVarDeclaration(VariableDeclaration st) {
		throw new UnimplementedException();
	}

	private void writeSectionEndStatement(SectionEndSt st) {
		throw new UnimplementedException();
	}
	
}
