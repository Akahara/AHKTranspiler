package fr.wonder.ahk.transpilers.python;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.AHKCompiledHandle;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.UnitImportation;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.commons.exceptions.AssertionException;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.files.FilesUtils;

public class PythonTranspiler implements Transpiler {
	
	@Override
	public String getName() {
		return "AHK_Default//Python";
	}
	
	@Override
	public void exportProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException, AssertionException {
		ErrorWrapper[] unitErrors = new ErrorWrapper[handle.units.length+handle.nativeRequirements.length];
		for(int i = 0; i < handle.units.length; i++) {
			unitErrors[i] = errors.subErrrors("Unable to compile unit " + handle.units[i].getFullBase());
			refactorUnit(handle, handle.units[i], unitErrors[i]);
		}
		for(int i = 0; i < handle.nativeRequirements.length; i++) {
			unitErrors[handle.units.length+i] = errors.subErrrors("Unable to compile native unit " +
					handle.nativeRequirements[i].getFullBase());
			refactorUnit(handle, handle.nativeRequirements[i], errors);
		}
		for(int i = 0; i < handle.units.length; i++) {
			if(!unitErrors[i].noErrors())
				continue;
			File unitFile = new File(dir, handle.units[i].getFullBase().replaceAll("\\.", "_")+".py");
			exportUnit(handle, handle.units[i], unitFile, unitErrors[i]);
		}
		for(int i = 0; i < handle.nativeRequirements.length; i++) {
			if(!unitErrors[handle.units.length+i].noErrors())
				continue;
			File unitFile = new File(dir, handle.nativeRequirements[i].getFullBase().replaceAll("\\.", "_")+".py");
			exportUnit(handle, handle.nativeRequirements[i], unitFile, unitErrors[handle.units.length+i]);
		}
		errors.assertNoErrors();
	}

	@Override
	public void exportAPI(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException {
		throw new IOException("Unsupported in python");
	}
	
	private static void refactorUnit(AHKCompiledHandle handle, Unit unit, ErrorWrapper errors) {
		for(int i = unit.functions.length-1; i >= 0; i--) {
			FunctionSection func = unit.functions[i];
			if(func.modifiers.hasModifier(Modifier.NATIVE)) {
				if(!NativeFunctions.isKnownNative(func, handle.typesTable.conversions))
					errors.add("Unknown native function " + func.getErr());
			} else {
				int funcDuplicates = 0;
				for(int j = 0; j < i; j++)
					if(func.name.equals(unit.functions[j].name))
						funcDuplicates++;
				if(funcDuplicates != 0)
					func.name += "_"+funcDuplicates;
			}
		}
	}
	
	private static void exportUnit(AHKCompiledHandle handle, Unit unit, File file, ErrorWrapper errors) throws IOException {
		file.createNewFile();
		
		StringBuilder sb = new StringBuilder();
		
		for(UnitImportation imported : unit.importations)
			sb.append("from " + imported.unitBase.replaceAll("\\.", "_") + " import *\n");
		if(unit.importations.length != 0)
			sb.append('\n');
		
		sb.append("class " + unit.name + ":\n\n");
		
		for(FunctionSection func : unit.functions) {
			if(!func.modifiers.hasModifier(Modifier.NATIVE)) {
				FunctionWriter.writeFunction(unit, func, sb, errors);
			} else {
				NativeFunctions.writeNative(func, sb, handle.typesTable.conversions);
			}
			sb.append("\n\n");
		}
		
		for(VariableDeclaration decl : unit.variables) {
			FunctionWriter.writeVarDeclaration(unit, decl, sb, errors);
			sb.append('\n');
		}
		
		if(handle.manifest.ENTRY_POINT.equals(unit.getFullBase())) {
			sb.append("\n");
			sb.append("if __name__ == '__main__':\n");
			sb.append("  exit(" + handle.manifest.entryPointFunction.declaringUnit.name + "." +
					handle.manifest.entryPointFunction.getUnitSignature() + "())\n");
		}
		
		FilesUtils.write(file, sb.toString());
	}
	
	@Override
	public int runProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("python3", handle.manifest.ENTRY_POINT.replaceAll("\\.", "_")+".py");
		pb.directory(dir);
		pb.inheritIO();
		Process process = pb.start();
		try {
			return process.waitFor();
		} catch (InterruptedException x) {
			throw new IOException("The python process got interrupted");
		}
	}
	
}
