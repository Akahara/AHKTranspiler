package fr.wonder.ahk.transpilers.ahl;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.ahl.raw.FunctionWriter;
import fr.wonder.ahk.transpilers.ahl.raw.UnitWriter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class AHLTranspiler implements Transpiler {

	@Override
	public String getName() {
		return "AHK_Default//AHL";
	}

	@Override
	public ExecutableHandle exportProject(LinkedHandle handle, File dir, ErrorWrapper errors)
			throws IOException, WrappedException {
		
		for(Unit unit : handle.units) {
			UnitWriter writer = new UnitWriter(unit, errors);
			for(FunctionSection func : unit.functions) {
				InstructionSequence is = FunctionWriter.writeFunction(writer, func);
				System.out.println(is);
			}
		}
		
		return null;
	}

	@Override
	public Process runProject(ExecutableHandle handle, File dir, ErrorWrapper errors)
			throws IOException, WrappedException {
		throw new IllegalAccessError("The AHL transpiler does not produce ");
	}
	
}
