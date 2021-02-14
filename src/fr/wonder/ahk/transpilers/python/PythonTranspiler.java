package fr.wonder.ahk.transpilers.python;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.TranspilableHandle;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.files.FilesUtils;

public class PythonTranspiler implements Transpiler {
	
	@Override
	public String getName() {
		return "AHK_Default//Python";
	}
	
	@Override
	public ExecutableHandle exportProject(TranspilableHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException {
		handle.manifest.validate(handle, errors, false);
		errors.assertNoErrors();
		ErrorWrapper[] unitErrors = new ErrorWrapper[handle.units.length+handle.nativeRequirements.length];
		for(int i = 0; i < handle.units.length; i++) {
			unitErrors[i] = errors.subErrrors("Unable to compile unit " + handle.units[i].fullBase);
			refactorUnit(handle, handle.units[i], unitErrors[i]);
		}
		for(int i = 0; i < handle.nativeRequirements.length; i++) {
			unitErrors[handle.units.length+i] = errors.subErrrors("Unable to compile native unit " +
					handle.nativeRequirements[i].fullBase);
			refactorUnit(handle, handle.nativeRequirements[i], errors);
		}
		for(int i = 0; i < handle.units.length; i++) {
			if(!unitErrors[i].noErrors())
				continue;
			File unitFile = new File(dir, handle.units[i].fullBase.replaceAll("\\.", "_")+".py");
			exportUnit(handle, handle.units[i], unitFile, unitErrors[i]);
		}
		for(int i = 0; i < handle.nativeRequirements.length; i++) {
			if(!unitErrors[handle.units.length+i].noErrors())
				continue;
			File unitFile = new File(dir, handle.nativeRequirements[i].fullBase.replaceAll("\\.", "_")+".py");
			exportUnit(handle, handle.nativeRequirements[i], unitFile, unitErrors[handle.units.length+i]);
		}
		errors.assertNoErrors();
		return new PythonExecutable(handle.manifest.ENTRY_POINT.replaceAll("\\.", "_")+".py");
	}
	
	private static class PythonExecutable extends ExecutableHandle {
		
		private final String mainFile;
		
		private PythonExecutable(String mainFile) {
			this.mainFile = mainFile;
		}
		
	}

	@Override
	public void exportAPI(TranspilableHandle handle, File dir, ErrorWrapper errors) throws IOException {
		throw new IOException("Unsupported in python");
	}
	
	private static void refactorUnit(TranspilableHandle handle, Unit unit, ErrorWrapper errors) {
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
	
	private static void exportUnit(TranspilableHandle handle, Unit unit, File file, ErrorWrapper errors) throws IOException, WrappedException {
		FilesUtils.create(file);
		
		StringBuilder sb = new StringBuilder();
		
		for(String imported : unit.importations)
			sb.append("from " + imported.replaceAll("\\.", "_") + " import *\n");
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
		
		if(handle.manifest.ENTRY_POINT.equals(unit.fullBase)) {
			sb.append("\n");
			sb.append("if __name__ == '__main__':\n");
			sb.append("  exit(" + handle.manifest.entryPointUnit.name + "." +
					handle.manifest.entryPointFunction.getSignature().computedSignature + "())\n");
		}
		
		FilesUtils.write(file, sb.toString());
	}
	
	@Override
	public Process runProject(ExecutableHandle handle, File dir, ErrorWrapper errors) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("python3", ((PythonExecutable) handle).mainFile);
		pb.directory(dir);
		pb.inheritIO();
		return pb.start();
	}
	
}
