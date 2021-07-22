package fr.wonder.ahk.transpilers.common_x64;

import static fr.wonder.ahk.transpilers.common_x64.instructions.OpCode.*;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.declarations.Comment;
import fr.wonder.ahk.transpilers.common_x64.declarations.EmptyLine;
import fr.wonder.ahk.transpilers.common_x64.declarations.Label;
import fr.wonder.ahk.transpilers.common_x64.declarations.SectionDeclaration;
import fr.wonder.ahk.transpilers.common_x64.instructions.CastedInstruction;
import fr.wonder.ahk.transpilers.common_x64.instructions.Instruction;
import fr.wonder.ahk.transpilers.common_x64.instructions.MovOperation;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.Operation;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.ahk.transpilers.common_x64.instructions.RepeatedInstruction;
import fr.wonder.commons.utils.ArrayOperator;

public class InstructionSet {
	
	public final List<Instruction> instructions = new ArrayList<>();

	public void add(Instruction instruction) {
		instructions.add(instruction);
	}
	
	public void add(OpCode instruction, OperationParameter... params) {
		add(new Operation(instruction, params));
	}
	
	public void add(OpCode instruction, Object... params) {
		OperationParameter[] ps = ArrayOperator.map(
				params, 
				OperationParameter[]::new,
				InstructionSet::asOperationParameter);
		add(new Operation(instruction, ps));
	}
	
	public void addCasted(OpCode instruction, Object... params) {
		Object[] ps = ArrayOperator.map(
				params,
				InstructionSet::asExtendedOperationParameter);
		add(new CastedInstruction(instruction, ps));
	}
	
	private static Object asExtendedOperationParameter(Object param) {
		if(param instanceof MemSize)
			return param;
		return asOperationParameter(param);
	}
	
	private static OperationParameter asOperationParameter(Object param) {
		if(param instanceof OperationParameter)
			return (OperationParameter) param;
		if(param instanceof String)
			return new LabelAddress((String) param);
		else if(param instanceof Integer)
			return new ImmediateValue((Integer) param);
		
		throw new IllegalArgumentException("Unknown operand type: " + param.getClass() + " " + param);
	}
	
	public void label(String label) { add(new Label(label)); }
	public void ret() { add(RET); }
	public void ret(int stackSize) { add(RET, stackSize); }
	public void call(String label) { add(CALL, label); }
	public void jmp(String label) { add(JMP, label); }
	public void mov(Address to, Object from) { add(new MovOperation(to, asOperationParameter(from))); }
	public void mov(Address to, Object from, MemSize cast) { add(new MovOperation(to, asOperationParameter(from), cast)); }
	public void push(OperationParameter target) { add(PUSH, target); }
	public void pop(Address target) { add(POP, target); }
	public void clearRegister(Register target) { add(XOR, target, target); }
	
	public void xor(Address target, Address with) { add(XOR, target, with); }

	public void repeat(OpCode stringOperation) {
		add(new RepeatedInstruction(new Operation(stringOperation)));
	}
	
	public void cmp(Address a, Object b) { add(CMP, a, b); }
	public void test(Address a, Address b) { add(TEST, a, b); }
	public void test(Register reg) { test(reg, reg); }
	
	public void section(String section) { add(new SectionDeclaration(section)); }
	
	public void createStackFrame() {
		push(Register.RBP);
		mov(Register.RBP, Register.RSP);
	}

	public void endStackFrame() {
		mov(Register.RSP, Register.RBP);
		pop(Register.RBP);		
	}
	
	public void skip() {
		add(EmptyLine.INSTANCE);
	}
	
	public void skip(int lineCount) {
		while(lineCount-- > 0)
			skip();
	}
	
	public void comment(String text) {
		instructions.add(new Comment(text));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Instruction i : instructions) {
			if(i instanceof Operation)
				sb.append("  ");
			sb.append(i.toString());
			sb.append('\n');
		}
		return sb.toString();
	}
	
}
