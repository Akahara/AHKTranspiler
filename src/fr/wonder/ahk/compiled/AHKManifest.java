package fr.wonder.ahk.compiled;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.types.FunctionArguments;
import fr.wonder.ahk.handles.TranspilableHandle;
import fr.wonder.ahk.transpilers.asm_x64.natives.CallingConvention;
import fr.wonder.ahk.transpilers.asm_x64.natives.OSInstrinsic;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class AHKManifest {

	/**
	 * @param unitaryComp if false, some checks will not be made like finding the
	 *                    entry point of the program
	 */
	public void validate(TranspilableHandle handle, ErrorWrapper errors, boolean unitaryComp) {
		if (!unitaryComp) {
			if (ENTRY_POINT == null || ENTRY_POINT.isBlank()) {
				errors.add("The entry point must be specified");
			} else {
				searchEntryPoint(handle.units, errors);
			}
		}
	}

	private void searchEntryPoint(Unit[] units, ErrorWrapper errors) {
		for (Unit u : units) {
			if (u.fullBase.equals(ENTRY_POINT)) {
				this.entryPointUnit = u;
				for (FunctionSection f : u.functions) {
					if (f.name.equals("main")) {
						this.entryPointFunction = f;
						if (f.modifiers.hasModifier(Modifier.NATIVE))
							errors.add("The main function cannot be native" + f.getErr());
						if (f.returnType != VarType.INT)
							errors.add("The main function must return an integer" + f.getErr());
						if (!FunctionArguments.matchNoConversions(f.getArgumentTypes(), new VarType[] {}))
							errors.add("The main function has an invalid signature, expecting int:main(void)" + f.getErr());
						if (f.modifiers.visibility != DeclarationVisibility.GLOBAL)
							errors.add("The main function must be global" + f.getErr());
						break;
					}
				}
				if (entryPointFunction == null)
					errors.add("The main function cannot be found in unit " + u.fullBase);
				break;
			}
		}
		if (entryPointFunction == null)
			errors.add("The entry point unit does not exist " + ENTRY_POINT);
	}

	/* -------------------------- Common section -------------------------- */
	public String ENTRY_POINT;
	public boolean DEBUG_SYMBOLS;

	/* set by #checkEntryPoint */
	public Unit entryPointUnit;
	public FunctionSection entryPointFunction;

	/* -------------------------- Python section -------------------------- */

	/* ------------------------- Assembly section ------------------------- */
	public void validateAsm(ErrorWrapper errors) {
		if (BUILD_ARCHITECTURE == null || BUILD_ARCHITECTURE.isBlank())
			errors.add("The build architecture must be specified");
		if (OUTPUT_NAME == null || OUTPUT_NAME.isBlank())
			OUTPUT_NAME = "process";
		if (LINKER_OPTIONS == null)
			LINKER_OPTIONS = "";
		if (NASM_PATH == null || NASM_PATH.isBlank())
			NASM_PATH = "/usr/local/bin/nasm";
		if (OSInstrinsic.getOS(BUILD_TARGET) == null)
			errors.add("Invalid build target '" + BUILD_TARGET + "' valid ones are "
					+ Arrays.toString(OSInstrinsic.values()));
		if (CALLING_CONVENTION == null)
			callingConvention = CallingConvention.__stdcall;
		else if ((callingConvention = CallingConvention.getConvention(CALLING_CONVENTION)) == null)
			errors.add("Invalid calling convention '" + BUILD_TARGET + "' valid ones are "
					+ Arrays.toString(CallingConvention.values()));
	}

	public String BUILD_ARCHITECTURE;
	public String LINKER_OPTIONS;
	public String OUTPUT_NAME;
	public String NASM_PATH;
	public String BUILD_TARGET;
	public String CALLING_CONVENTION;
	public CallingConvention callingConvention;

}
