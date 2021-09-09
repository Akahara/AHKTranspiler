package fr.wonder.ahk;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.ahk.handles.ProjectHandle;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.asm_x64.AsmX64Transpiler;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.loggers.AnsiLogger;
import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.systems.process.Manifest;
import fr.wonder.commons.systems.process.ManifestUtils;
import fr.wonder.commons.systems.process.ProcessUtils;

public class AHKTranspiler {
	
	/*
	 * TODO > x64 files
	 * TODO > lambdas
	 * TODO > generics
	 * TODO > create a new x64 transpiler, based on an abstract intermediate language
	 * TODO > rework project handles
	 */
	
//	public static Logger logger = new SimpleLogger(null, Logger.LEVEL_DEBUG);
	public static Logger logger = new AnsiLogger(null, Logger.LEVEL_DEBUG);
	
	public static ProjectHandle createProject(File dir, File manifestFile) throws IOException {
		List<File> files = FilesUtils.listFiles(dir, f->f.isFile() && f.getName().endsWith(".ahk"));
		files.removeIf(f -> f.getAbsolutePath().contains("ex_"));
		UnitSource[] sources = new UnitSource[files.size()];
		for(int i = 0; i < files.size(); i++) {
			File f = files.get(i);
			sources[i] = new UnitSource(f.getName(), FilesUtils.read(f));
		}
		Arrays.sort(sources, (s1, s2) -> s2.name.compareTo(s1.name)); // compile the Kernel files last for debugging purposes
		Manifest man = ManifestUtils.parseManifest(manifestFile);
		AHKManifest manifest = ManifestUtils.buildManifestFromValues(man, AHKManifest.class,
				ManifestUtils.CONVENTION_SCREAMING_SNAKE_CASE);
		return new ProjectHandle(sources, manifest);
	}
	
	public static void main(String[] args) throws IOException {
		File codeDir = new File("code");
		ProjectHandle project = createProject(codeDir, new File(codeDir, "manifest.txt"));
		Transpiler transpiler;
		
		transpiler = new AsmX64Transpiler();
//		transpiler = new AHLTranspiler();
		
		File dir = new File("exported/exported_x64");
		dir.mkdirs();
		FilesUtils.deleteContents(dir);
		try {
			logger.warn("Exporting with " + transpiler.getName());
			LinkedHandle handle = project
				.compile(new ErrorWrapper("Unable to compile", true))
				.link(new ErrorWrapper("Unable to link", true));
			ExecutableHandle exec = transpiler.exportProject(handle, dir, new ErrorWrapper("Unable to export", true));
			if(exec == null)
				return;
			logger.warn("Running with " + transpiler.getName());
			Process process = transpiler.runProject(exec, dir, new ErrorWrapper("Unable to run"));
			if(process != null)
				ProcessUtils.redirectOutputToStd(process).start();
		} catch (WrappedException e) {
			e.errors.dump();
		}
	}
	
}
