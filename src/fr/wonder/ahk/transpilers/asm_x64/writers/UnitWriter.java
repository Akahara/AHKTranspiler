package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.ahk.transpilers.asm_x64.units.ConcreteType;
import fr.wonder.ahk.transpilers.asm_x64.units.ConcreteTypesTable;
import fr.wonder.ahk.transpilers.asm_x64.units.NoneExp;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;
import fr.wonder.ahk.transpilers.asm_x64.writers.data.DataSectionWriter;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.declarations.GlobalVarReservation;
import fr.wonder.ahk.transpilers.common_x64.declarations.SectionDeclaration;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.ahk.transpilers.common_x64.instructions.SpecialInstruction;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class UnitWriter {
	
	public static InstructionSet writeUnit(LinkedHandle handle,
			Unit unit, ConcreteTypesTable types, ErrorWrapper errors) {
		
		UnitWriter uw = new UnitWriter(handle, unit, types);
		// variables that must be initialized (because they are not literals or computable constants)
		List<VariableDeclaration> initializableVariables = new ArrayList<>();
		// all other variables (which values can be computed beforehand)
		List<VariableDeclaration> initializedVariables = new ArrayList<>();
		
		for(VariableDeclaration var : unit.variables) {
			if(var.getDefaultValue() instanceof LiteralExp<?>)
				initializedVariables.add(var);
			else
				initializableVariables.add(var);
		}
		
		uw.collectDeclaredConstantsAndExternFields();
		
		uw.writeSpecialSegment(errors);
		uw.writeDataSegment(initializedVariables, errors);
		uw.writeBSSSegment(errors);
		uw.writeTextSegment(initializableVariables, errors);
		uw.finalizeSegments();
		
		return uw.instructions;
	}
	
	public final InstructionSet instructions = new InstructionSet();
	
	public final LinkedHandle project;
	public final Unit unit;
	
	public final RegistryManager registries;
	
	public final ConcreteTypesTable types;
	
	private final DataSectionWriter dataSectionWriter;
	
	/** populated by {@link #writeDataSegment(ErrorWrapper)} and used by {@link #getStringConstantLabel(StrLiteral)} */
	private final List<StrLiteral> strConstants = new ArrayList<>();
	
	private int specialCallCount = 0;
	
	private UnitWriter(LinkedHandle handle, Unit unit, ConcreteTypesTable types) {
		this.project = handle;
		this.unit = unit;
		this.registries = new RegistryManager(this);
		this.types = types;
		this.dataSectionWriter = new DataSectionWriter(this);
	}
	
	// ------------------------ registries & labels ------------------------
	
	public String getStringConstantLabel(StrLiteral lit) {
		// beware! strConstants.indexOf cannot be used because the #equals method of StrLiteral
		// will return true if two literals hold equal strings.
		for(int i = 0; i < strConstants.size(); i++) {
			if(strConstants.get(i) == lit)
				return "str_cst_" + i;
		}
		throw new IllegalStateException("Unregistered string constant");
	}
	
	/**
	 * Returns the assembly text corresponding to a literal expression
	 * <ul>
	 *   <li><b>Ints</b> are not converted</li>
	 *   <li><b>Floats</b> are converted using the {@code __float64__} directive</li>
	 *   <li><b>Bools</b> are converted to 0 or 1</li>
	 *   <li><b>Strings</b> are converted to their labels in the data segment</li>
	 * </ul>
	 */
	public String getValueString(LiteralExp<?> exp) {
		if(exp instanceof IntLiteral)
			return String.valueOf(((IntLiteral) exp).value);
		else if(exp instanceof FloatLiteral)
			return ((FloatLiteral)exp).value == 0 ? "0" : "__float64__("+((FloatLiteral)exp).value+")";
		else if(exp instanceof BoolLiteral)
			return ((BoolLiteral) exp).value ? "1" : "0";
		else if(exp instanceof StrLiteral)
			return getStringConstantLabel((StrLiteral) exp);
		else
			throw new IllegalStateException("Unhandled literal type " + exp.getClass());
	}
	
	public String requireExternLabel(String label) {
		dataSectionWriter.addExternDeclaration(label);
		return label;
	}

	public MemAddress requireExternLabel(MemAddress address) {
		dataSectionWriter.addExternDeclaration(((LabelAddress) address.base).label);
		return address;
	}
	
	// ------------------------------ segments -----------------------------
	
	private void writeSpecialSegment(ErrorWrapper errors) {
		instructions.add(new SpecialInstruction("%include \"intrinsic.asm\""));
		instructions.skip(1);
	}
	
	// *** Data segment ***
	
	private void collectDeclaredConstantsAndExternFields() {
		for(Prototype<?> i : unit.prototype.externalAccesses) {
			// extern variable or function
			if(i instanceof VarAccess)
				requireExternLabel(RegistryManager.getGlobalRegistry((VarAccess) i));
		}
		
		for(VariableDeclaration var : unit.variables)
			collectStrConstants(var);
		for(FunctionSection func : unit.functions) {
			for(Statement st : func.body)
				collectStrConstants(st);
		}
	}
	
	private void writeDataSegment(List<VariableDeclaration> initializedVariables, ErrorWrapper errors) {
		instructions.section(SectionDeclaration.DATA);
		instructions.skip();
		
		dataSectionWriter.writeGlobalDeclarations();
		dataSectionWriter.writeVariableDeclarations(initializedVariables);
		dataSectionWriter.writeFunctionClosures();
		dataSectionWriter.writeLambdas();
		dataSectionWriter.writeStrConstants(strConstants);
	}
	
	private void collectStrConstants(ExpressionHolder holder) {
		for(Expression e : holder.getExpressions()) {
			if(e instanceof StrLiteral)
				strConstants.add((StrLiteral) e);
			else
				collectStrConstants(e);
		}
	}
	
	// *** BSS Segment ***
	
	private void writeBSSSegment(ErrorWrapper errors) {
		if(unit.structures.length == 0)
			return;
		
		instructions.section(SectionDeclaration.BSS);
		instructions.skip();

		for(StructSection struct : unit.structures) {
			String nullLabel = registries.getStructNullRegistry(struct.getPrototype());
			instructions.add(new GlobalVarReservation(nullLabel, MemSize.QWORD, struct.members.length));
		}
		
		instructions.skip(2);
	}
	
	// *** Text segment ***
	
	private void writeTextSegment(List<VariableDeclaration> initializableVariables, ErrorWrapper errors) {
		instructions.section(SectionDeclaration.TEXT);
		instructions.skip();
		
		FunctionSection initFunction = FunctionSection.dummyFunction();
		FunctionWriter initFunctionWriter = new FunctionWriter(this, initFunction);
		
		// write the initialization function
		instructions.label(RegistryManager.getUnitInitFunctionRegistry(unit));
		instructions.createStackFrame();
		
		for(StructSection struct : unit.structures) {
			ConcreteType concreteType = types.getConcreteType(struct.getPrototype());
			String nullLabel = registries.getStructNullRegistry(struct.getPrototype());
			MemAddress nullAddress = new MemAddress(new LabelAddress(nullLabel));
			instructions.comment("init " + struct.name + " null");
			for(VariableDeclaration member : struct.members) {
				ConstructorDefaultValue nullField = struct.getNullField(member.name);
				Expression nullMemberValue = nullField == null ? member.getDefaultValue() : nullField.getValue();
				Address fieldAddress = nullAddress.addOffset(concreteType.getOffset(member.name));
				initFunctionWriter.mem.writeTo(fieldAddress, nullMemberValue, errors);
			}
		}
		
		for(VariableDeclaration var : initializableVariables) {
			instructions.comment("init " + var.name);
			Address address = new MemAddress(new LabelAddress(registries.getRegistry(var.getPrototype())));
			if(var.modifiers.hasModifier(Modifier.NATIVE)) {
				String nativeLabel = var.modifiers.getModifier(NativeModifier.class).nativeRef;
				instructions.mov(address, nativeLabel);
			} else {
				Expression defaultVal = var.getDefaultValue();
				if(defaultVal == null)
					defaultVal = new NoneExp();
				initFunctionWriter.mem.writeTo(address, defaultVal, errors);
			}
		}
		
		instructions.endStackFrame();
		instructions.ret();
		instructions.skip();
		
		for(FunctionSection func : unit.functions) {
			if(project.manifest.DEBUG_SYMBOLS) {
				instructions.comment("-".repeat(60));
				instructions.comment(func.toString());
				instructions.comment("-".repeat(60));
			}
			instructions.label(RegistryManager.getGlobalRegistry(func.getPrototype()));

			if(func.modifiers.hasModifier(Modifier.NATIVE)) {
				instructions.jmp(func.modifiers.getModifier(NativeModifier.class).nativeRef);
			} else {
				FunctionWriter writer = new FunctionWriter(this, func);
				writer.writeFunction(errors);
			}
			
			instructions.skip();
		}
		
		for(SimpleLambda lambda : unit.lambdas) {
			if(project.manifest.DEBUG_SYMBOLS) {
				instructions.comment("-".repeat(60));
				instructions.comment(lambda.toString());
				instructions.comment("-".repeat(60));
			}
			instructions.label(registries.getLambdaRegistry(lambda));
			LambdaWriter writer = new LambdaWriter(this, lambda);
			writer.writeLambda(errors);
			instructions.skip();
		}
		
		instructions.skip(2);
	}
	
	private void finalizeSegments() {
		dataSectionWriter.insertExternDeclarations();
	}
	
	// --------------------------- special calls ---------------------------
	
	public String getSpecialLabel() {
		return ".special_" + (specialCallCount++);
	}
	
	public void testThrowError() {
		String label = getSpecialLabel();
		instructions.test(Register.RAX, Register.RAX);
		instructions.add(OpCode.JNZ, label);
		instructions.call(requireExternLabel(GlobalLabels.SPECIAL_THROW));
		instructions.label(label);
	}
	
	public void callAlloc(int byteCount) {
		callAlloc(new ImmediateValue(byteCount, MemSize.QWORD));
	}
	
	public void callAlloc(OperationParameter size) {
		instructions.push(size);
		instructions.call(requireExternLabel(GlobalLabels.SPECIAL_ALLOC));
		// FUTURE implement other calling conventions (__stdcall currently)
		testThrowError();
	}
	
}
