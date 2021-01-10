package fr.wonder.ahk.transpilers.asm_x64;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import fr.wonder.ahk.AHKCompiledHandle;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.asm_x64.natives.ProcessFiles;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;
import fr.wonder.ahk.transpilers.asm_x64.writers.TextBuffer;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.utils.ErrorWrapper;
import fr.wonder.commons.files.FilesUtils;

public class AsmX64Transpiler implements Transpiler {
	
	@Override
	public String getName() {
		return "AHK_Default//Assembly_x64";
	}

	@Override
	public void exportProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException {
		validateProject(handle, errors);
		errors.assertNoErrors();
		
		if(dir.isDirectory())
			FilesUtils.deleteRecur(dir);
		dir.mkdirs();
		
		String[] files = new String[handle.units.length + handle.nativeRequirements.length];
		
		for(int i = 0; i < handle.units.length; i++) {
			Unit unit = handle.units[i];
			files[i] = writeUnit(handle, unit, dir, errors);
		}
		
		for(int i = 0; i < handle.nativeRequirements.length; i++) {
			Unit unit = handle.nativeRequirements[i];
			files[i+handle.units.length] = writeUnit(handle, unit, dir, errors);
		}
		
		errors.assertNoErrors();
		
		String[] processFiles = ProcessFiles.writeFiles(handle, dir, errors);
		files = Arrays.copyOf(files, files.length+processFiles.length);
		for(int i = 0; i < processFiles.length; i++)
			files[files.length-processFiles.length+i] = processFiles[i];
		
		errors.assertNoErrors();
		
		runCompiler(handle, dir, files, errors);
	}
	
	private void validateProject(AHKCompiledHandle handle, ErrorWrapper errors) {
		handle.manifest.validateAsm(errors.subErrrors("Invalid manifest"));
		
		for(Unit u : handle.units)
			validateUnit(handle, u, errors.subErrrors("Unable to validate unit " + u.getFullBase()));
		for(Unit u : handle.nativeRequirements)
			validateUnit(handle, u, errors.subErrrors("Unable to validate native unit " + u.getFullBase()));
	}

	private void validateUnit(AHKCompiledHandle handle, Unit unit, ErrorWrapper errors) {
		for(FunctionSection func : unit.functions) {
			if(func.modifiers.hasModifier(Modifier.NATIVE)) {
				if(!func.modifiers.getModifier(Modifier.NATIVE).validateArgs(func, NativeModifier::parseModifier))
					errors.add("Invalid native modifier syntax" + func.getErr());
			}
		}
	}

	private static String writeUnit(AHKCompiledHandle handle, Unit unit, File dir, ErrorWrapper errors) throws IOException {
		String file = unit.getFullBase().replaceAll("\\.", "/");
		TextBuffer tb = new TextBuffer();
		UnitWriter.writeUnit(handle, unit, tb, errors);
		File f = new File(dir, file+".asm");
		if(!f.isFile()) { f.getParentFile().mkdirs(); f.createNewFile(); }
		FilesUtils.write(f, tb.toString());
		return file;
	}
	
	private static void runCompiler(AHKCompiledHandle handle, File dir, String[] files, ErrorWrapper errors) throws IOException {
		if(files.length == 0)
			return;
		
		new File(dir, "obj_files").mkdirs();
		
		String nasm = handle.manifest.NASM_PATH + " -f " + handle.manifest.BUILD_ARCHITECTURE + " ";
		if(handle.manifest.DEBUG_SYMBOLS) nasm += "-g -F dwarf ";
		String asme = ".asm -o obj_files/";
		
		boolean compiled = true;
		
		for(String f : files) {
			new File(dir, "obj_files/"+f).getParentFile().mkdirs();
			compiled &= runCommand(nasm + f + asme + f + ".o", dir);
		}
		
		if(!compiled) {
			errors.add("Unable to compile all source files");
			return;
		}
		
		String ld = "ld -o " + handle.manifest.OUTPUT_NAME + " ";
		if(!handle.manifest.LINKER_OPTIONS.isBlank())
			ld += handle.manifest.LINKER_OPTIONS + " ";
		for(String f : files)
			ld += "obj_files/" + f + ".o ";
		if(!runCommand(ld, dir))
			errors.add("Unable to link all source files");
	}
	
	private static boolean runCommand(String cmd, File dir) throws IOException {
		System.out.print("Running command || " + cmd);
		ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
		Process p = pb.directory(dir).start();
		int ec = -1;
		try { ec = p.waitFor(); } catch (InterruptedException x) { }
		System.out.print(" || exit code 0x" + Integer.toHexString(ec) + " = " + ec);
		if((ec & 0x80) == 0 || ((ec-1) ^ 0x80) > 16)
			System.out.println();
		else
			System.out.println(" signal " + SIGNALS[(ec-1) ^ 0x80]);
		System.out.flush();
		System.err.flush();
		System.out.print(new String(p.getInputStream().readAllBytes()));
		System.err.print(new String(p.getErrorStream().readAllBytes()));
		System.out.flush();
		System.err.flush();
		return ec == 0;
	}
	
	private static final String[] SIGNALS = {
			"SIGHUP hangup", "SIGINT interrupt", "SIGQUIT terminal quit", "SIGILL illegal instruction",
			"SIGTRAP trap", "SIGABRT abort", null, "SIGFPE arithmetic error",
			"SIGKILL kill", null, "SIGSEGV segmentation fault", null,
			"SIGPIPE invalid pipe", "SIGALRM alarm clock", "SIGTERM termination", null
	};

	@Override
	public void runProject(AHKCompiledHandle handle, File dir, ErrorWrapper errors) throws IOException {
		runCommand("./" + handle.manifest.OUTPUT_NAME, dir);
	}
	
}
