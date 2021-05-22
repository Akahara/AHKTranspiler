package fr.wonder.ahk;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.ProjectHandle;
import fr.wonder.ahk.handles.TranspilableHandle;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.asm_x64.AsmX64Transpiler;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.files.Manifest;
import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.loggers.SimpleLogger;
import fr.wonder.commons.utils.ManifestUtils;
import fr.wonder.commons.utils.ProcessUtils;

public class AHKTranspiler {
	
	public static Logger logger = new SimpleLogger(null, Logger.LEVEL_DEBUG);
	
	public static ProjectHandle createProject(File dir) throws IOException {
		List<File> files = FilesUtils.listFiles(dir, f->f.isFile() && f.getName().endsWith(".ahk"));
		UnitSource[] sources = new UnitSource[files.size()];
		for(int i = 0; i < files.size(); i++) {
			File f = files.get(i);
			sources[i] = new UnitSource(f.getName(), FilesUtils.read(f));
		}
		return new ProjectHandle(sources);
	}
	
	public static void main(String[] args) throws IOException {
		File codeDir = new File("code");
		ProjectHandle project = createProject(codeDir);
//		Transpiler transpiler = new PythonTranspiler();
//		File dir = new File("exported_py");
		Transpiler transpiler = new AsmX64Transpiler();
		File dir = new File("exported_x64");
		Manifest man = ManifestUtils.parseManifest(new File(codeDir, "manifest.txt"));
		AHKManifest manifest = ManifestUtils.buildManifestFromValues(man, AHKManifest.class,
				ManifestUtils.CONVENTION_SCREAMING_SNAKE_CASE);
		try {
			TranspilableHandle handle = project
				.compile(new ErrorWrapper("Unable to compile"))
				.link(new ErrorWrapper("Unable to link"))
				.prepare(manifest);
			ExecutableHandle exec = transpiler.exportProject(handle, dir, new ErrorWrapper("Unable to export"));
			Process process = transpiler.runProject(exec, dir, new ErrorWrapper("Unable to run"));
			if(process != null)
				ProcessUtils.redirectOutputToStd(process);
		} catch (WrappedException e) {
			e.errors.dump();
		}
	}
	
}
