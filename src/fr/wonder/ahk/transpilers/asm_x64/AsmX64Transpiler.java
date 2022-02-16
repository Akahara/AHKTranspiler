package fr.wonder.ahk.transpilers.asm_x64;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.asm_x64.natives.CallingConvention;
import fr.wonder.ahk.transpilers.asm_x64.natives.ProcessFiles;
import fr.wonder.ahk.transpilers.asm_x64.units.ConcreteTypesTable;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.systems.process.ProcessUtils;
import fr.wonder.commons.utils.ArrayOperator;

public class AsmX64Transpiler implements Transpiler {
	
	private static final String ASM_PATH = "asm/", OBJ_PATH = "obj/";
	
	private final Logger logger;
	
	public AsmX64Transpiler(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public String getName() {
		return "AHK_Default//Assembly_x64";
	}

	@Override
	public ExecutableHandle exportProject(LinkedHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException {
		validateProject(handle, errors);
		errors.assertNoErrors();
		
		File asmDir = new File(dir, ASM_PATH);
		
		if(handle.manifest.callingConvention != CallingConvention.__stdcall) {
			throw new UnimplementedException("Unimplemented calling convention " +
					handle.manifest.callingConvention);
		}
		
		String[] files = new String[handle.units.length];
		
		ConcreteTypesTable types = new ConcreteTypesTable();
		
		for(int i = 0; i < handle.units.length; i++) {
			Unit unit = handle.units[i];
			files[i] = writeUnit(handle, unit, types, asmDir, errors);
		}
		
		errors.assertNoErrors();
		
		String[] processFiles = ProcessFiles.writeFiles(handle, asmDir, errors);
		errors.assertNoErrors();
		
		files = ArrayOperator.add(files, processFiles);
		
		runExternalCompiler(handle, dir, files, errors);
		
		return new ExecutableHandleImpl(handle);
	}

	private static class ExecutableHandleImpl extends ExecutableHandle {
		
		private final AHKManifest manifest;
		
		private ExecutableHandleImpl(LinkedHandle handle) {
			this.manifest = handle.manifest;
		}
		
	}
	
	private void validateProject(LinkedHandle handle, ErrorWrapper errors) {
		handle.manifest.validate(handle, errors.subErrors("Invalid manifest"), false);
		handle.manifest.validateAsm(errors.subErrors("Invalid manifest"));
		
		for(Unit u : handle.units)
			validateUnit(handle, u, errors.subErrors("Unable to validate unit " + u.fullBase));
	}

	private void validateUnit(LinkedHandle handle, Unit unit, ErrorWrapper errors) {
		for(FunctionSection func : unit.functions) {
			if(func.modifiers.hasModifier(Modifier.NATIVE)) {
				if(!func.modifiers.getModifier(Modifier.NATIVE).validateArgs(func.getPrototype(), (f,m) -> NativeModifier.parseModifier(m)))
					errors.add("Invalid native modifier syntax" + func.getErr());
			}
		}
		for(VariableDeclaration var : unit.variables) {
			if(var.modifiers.hasModifier(Modifier.NATIVE)) {
				if(!var.modifiers.getModifier(Modifier.NATIVE).validateArgs(var.getPrototype(), (v,m) -> NativeModifier.parseModifier(m)))
					errors.add("Invalid native modifier syntax" + var.getErr());
			}
		}
	}

	private static String writeUnit(LinkedHandle handle, Unit unit, ConcreteTypesTable types, File dir, ErrorWrapper errors) throws IOException {
		String file = unit.fullBase.replaceAll("\\.", "/")+".asm";
		InstructionSet instructions = UnitWriter.writeUnit(handle, unit, types, errors);
		File f = new File(dir, file);
		if(!f.isFile()) { f.getParentFile().mkdirs(); f.createNewFile(); }
		FilesUtils.write(f, instructions.toString());
		return file;
	}
	
	private void runExternalCompiler(LinkedHandle handle, File dir, String[] files, ErrorWrapper errors) throws IOException, WrappedException {
		if(files.length == 0)
			return;
		
		new File(dir, OBJ_PATH).mkdirs();
		
		String nasm = handle.manifest.NASM_PATH + " -f " + handle.manifest.BUILD_ARCHITECTURE + " -i " + ASM_PATH + " ";
		if(handle.manifest.DEBUG_SYMBOLS) nasm += "-g -F dwarf ";
		
		boolean compiled = true;
		
		for(String f : files) {
			new File(dir, OBJ_PATH + f).getParentFile().mkdirs();
			compiled &= runCommand(nasm + ASM_PATH + f + " -o " + OBJ_PATH + f + ".o", dir) == 0;
		}
		
		if(!compiled) {
			errors.add("Unable to compile all source files");
			errors.assertNoErrors();
		}
		
		String ld = "ld -o " + handle.manifest.OUTPUT_NAME + " ";
		if(!handle.manifest.LINKER_OPTIONS.isBlank())
			ld += handle.manifest.LINKER_OPTIONS + " ";
		for(String f : files)
			ld += OBJ_PATH + f + ".o ";
		if(runCommand(ld, dir) != 0)
			errors.add("Unable to link all source files");
		
		errors.assertNoErrors();
	}
	
	private int runCommand(String cmd, File dir) throws IOException {
		logger.info("Running command || " + cmd);
		ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
		Process p = pb.directory(dir).start();
		Thread redirect = ProcessUtils.redirectOutput(p, logger);
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
		logger.info(" || exit code 0x" + Integer.toHexString(ec) + " = " + ec + " " + signal);
		return ec;
	}

	@Override
	public Process runProject(ExecutableHandle handle, File dir, ErrorWrapper errors)
			throws IOException, WrappedException {
		runCommand("./" + ((ExecutableHandleImpl) handle).manifest.OUTPUT_NAME + " arg1 arg2", dir);
		return null;
	}

}
