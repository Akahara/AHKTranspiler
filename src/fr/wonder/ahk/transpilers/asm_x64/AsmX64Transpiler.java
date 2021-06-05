package fr.wonder.ahk.transpilers.asm_x64;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.AHKTranspiler;
import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.TranspilableHandle;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.asm_x64.natives.ProcessFiles;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.utils.ArrayOperator;
import fr.wonder.commons.utils.ProcessUtils;

public class AsmX64Transpiler implements Transpiler {
	
	/*
	 * Fixes:
	 *  - make a type table that knows the size of a data type
	 */
	
	@Override
	public String getName() {
		return "AHK_Default//Assembly_x64";
	}

	@Override
	public ExecutableHandle exportProject(TranspilableHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException {
		validateProject(handle, errors);
		errors.assertNoErrors();
		
		String[] files = new String[handle.units.length];
		
		for(int i = 0; i < handle.units.length; i++) {
			Unit unit = handle.units[i];
			files[i] = writeUnit(handle, unit, dir, errors);
		}
		
		errors.assertNoErrors();
		
		String[] processFiles = ProcessFiles.writeFiles(handle, dir, errors);
		files = ArrayOperator.add(files, processFiles);
		
		errors.assertNoErrors();
		
		runExternalCompiler(handle, dir, files, errors);
		
		return new ExecutableHandleImpl(handle);
	}

	private static class ExecutableHandleImpl extends ExecutableHandle {
		
		private final AHKManifest manifest;
		
		private ExecutableHandleImpl(TranspilableHandle handle) {
			this.manifest = handle.manifest;
		}
		
	}
	
	@Override
	public void exportAPI(TranspilableHandle handle, File dir, ErrorWrapper errors) throws IOException {
		// TODO export as api
	}
	
	private void validateProject(TranspilableHandle handle, ErrorWrapper errors) {
		handle.manifest.validate(handle, errors.subErrrors("Invalid manifest"), false);
		handle.manifest.validateAsm(errors.subErrrors("Invalid manifest"));
		
		for(Unit u : handle.units)
			validateUnit(handle, u, errors.subErrrors("Unable to validate unit " + u.fullBase));
		for(Unit u : handle.nativeRequirements)
			validateUnit(handle, u, errors.subErrrors("Unable to validate native unit " + u.fullBase));
	}

	private void validateUnit(TranspilableHandle handle, Unit unit, ErrorWrapper errors) {
		for(FunctionSection func : unit.functions) {
			if(func.modifiers.hasModifier(Modifier.NATIVE)) {
				if(!func.modifiers.getModifier(Modifier.NATIVE).validateArgs(func, NativeModifier::parseModifier))
					errors.add("Invalid native modifier syntax" + func.getErr());
			}
		}
	}

	private static String writeUnit(TranspilableHandle handle, Unit unit, File dir, ErrorWrapper errors) throws IOException {
		String file = unit.fullBase.replaceAll("\\.", "/");
		InstructionSet instructions = UnitWriter.writeUnit(handle, unit, errors);
		File f = new File(dir, file+".asm");
		if(!f.isFile()) { f.getParentFile().mkdirs(); f.createNewFile(); }
		FilesUtils.write(f, instructions.toString());
		return file;
	}
	
	private static void runExternalCompiler(TranspilableHandle handle, File dir, String[] files, ErrorWrapper errors) throws IOException, WrappedException {
		if(files.length == 0)
			return;
		
		new File(dir, "obj_files").mkdirs();
		
		String nasm = handle.manifest.NASM_PATH + " -f " + handle.manifest.BUILD_ARCHITECTURE + " ";
		if(handle.manifest.DEBUG_SYMBOLS) nasm += "-g -F dwarf ";
		String asme = ".asm -o obj_files/";
		
		boolean compiled = true;
		
		for(String f : files) {
			new File(dir, "obj_files/"+f).getParentFile().mkdirs();
			compiled &= runCommand(nasm + f + asme + f + ".o", dir) == 0;
		}
		
		if(!compiled) {
			errors.add("Unable to compile all source files");
			errors.assertNoErrors();
		}
		
		String ld = "ld -o " + handle.manifest.OUTPUT_NAME + " ";
		if(!handle.manifest.LINKER_OPTIONS.isBlank())
			ld += handle.manifest.LINKER_OPTIONS + " ";
		for(String f : files)
			ld += "obj_files/" + f + ".o ";
		if(runCommand(ld, dir) != 0)
			errors.add("Unable to link all source files");
		
		errors.assertNoErrors();
	}
	
	private static int runCommand(String cmd, File dir) throws IOException {
		AHKTranspiler.logger.info("Running command || " + cmd);
		ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
		Process p = pb.directory(dir).start();
		Thread redirect = ProcessUtils.redirectOutput(p, AHKTranspiler.logger);
		int split = cmd.indexOf(' ');
		cmd = split == -1 ? cmd : cmd.substring(0, split);
		cmd = cmd.startsWith("/") ? cmd.substring(cmd.lastIndexOf('/', split)+1) : cmd;
		redirect.setName("> " + cmd);
		redirect.start();
		int ec = -1;
		try { ec = p.waitFor(); } catch (InterruptedException x) { }
		String signal = ProcessUtils.getErrorSignal(ec);
		if(signal == null) signal = "";
		try { redirect.join(); } catch (InterruptedException e) { }
		AHKTranspiler.logger.info(" || exit code 0x" + Integer.toHexString(ec) + " = " + ec + " " + signal);
		return ec;
	}

	@Override
	public Process runProject(ExecutableHandle handle, File dir, ErrorWrapper errors)
			throws IOException, WrappedException {
		runCommand("./" + ((ExecutableHandleImpl) handle).manifest.OUTPUT_NAME, dir);
		return null;
	}

}
