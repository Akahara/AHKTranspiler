package fr.wonder.ahk.transpilers.common_x64;

import static fr.wonder.ahk.transpilers.common_x64.instructions.OpCode.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
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

public class InstructionSet {
	
	public final List<Instruction> instructions = new ArrayList<>();
	
	public static boolean needsCasting(OperationParameter o1, OperationParameter o2) {
		return o1 instanceof MemAddress && (o2 instanceof ImmediateValue || o2 instanceof LabelAddress);
	}

	public void add(Instruction instruction) {
		instructions.add(instruction);
	}

	public void addAll(int index, Collection<Instruction> instructions) {
		this.instructions.addAll(index, instructions);
	}
	
	public void add(OpCode instruction, OperationParameter... params) {
		add(new Operation(instruction, params));
	}
	
	public void add(OpCode instruction, Object... params) {
		add(new Operation(instruction, OperationParameter.asOperationParameters(params)));
	}
	
	public void addCasted(OpCode instruction, Object... params) {
		add(new CastedInstruction(instruction, OperationParameter.asExtendedOperationParameters(params)));
	}
	
	public void mov(Address to, Object from) {
		OperationParameter f = OperationParameter.asOperationParameter(from);
		if(needsCasting(to, f))
			add(new MovOperation(to, f, MemSize.QWORD));
		else
			add(new MovOperation(to, f));
	}
	
	public void section(String section) { add(new SectionDeclaration(section)); }
	public void comment(String text) { add(new Comment(text)); }
	public void comment(String text, int indentation) { add(new Comment(text, indentation)); }
	public void label(String label) { add(new Label(label)); }
	public void ret() { add(RET); }
	public void ret(int stackSize) { add(RET, stackSize); }
	public void call(String label) { add(CALL, label); }
	public void call(OperationParameter target) { add(CALL, target); }
	public void jmp(String label) { add(JMP, label); }
	public void push(OperationParameter target) { add(PUSH, target); }
	public void pop(Address target) { add(POP, target); }
	public void pop() { add(ADD, Register.RSP, MemSize.POINTER_SIZE); }
	public void clearRegister(Register target) { add(XOR, target, target); }
	public void lea(Register reg, MemAddress add) { add(LEA, reg, add); }
	public void xor(Address target, Address with) { add(XOR, target, with); }
	public void cmp(Address a, Object b) { add(CMP, a, b); }
	public void test(Address a, Address b) { add(TEST, a, b); }
	public void test(Register reg) { test(reg, reg); }

	public void repeat(OpCode stringOperation) {
		add(new RepeatedInstruction(new Operation(stringOperation)));
	}
	
	public void createStackFrame() {
		push(Register.RBP);
		mov(Register.RBP, Register.RSP);
	}

	public void endStackFrame() {
		mov(Register.RSP, Register.RBP);
		pop(Register.RBP);		
	}
	
	public void skip() {
		add(EmptyLine.EMPTY_LINE);
	}
	
	public void skip(int lineCount) {
		while(lineCount-- > 0)
			skip();
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
