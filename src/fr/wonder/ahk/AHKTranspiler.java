package fr.wonder.ahk;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiler.Compiler;
import fr.wonder.ahk.compiler.linker.Linker;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.python.PythonTranspiler;
import fr.wonder.commons.exceptions.AssertionException;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.files.Manifest;
import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.loggers.SimpleLogger;
import fr.wonder.commons.utils.ManifestUtils;

public class AHKTranspiler {
	
	public static Logger logger = new SimpleLogger(null, Logger.LEVEL_DEBUG);
	
	public static AHKProjectHandle createProject(File dir) throws IOException {
		List<File> files = FilesUtils.listFilesRecur(dir, f->f.isFile() && f.getName().endsWith(".ahk"));
		UnitSource[] sources = new UnitSource[files.size()];
		for(int i = 0; i < files.size(); i++) {
			File f = files.get(i);
			sources[i] = new UnitSource(f.getName(), FilesUtils.read(f));
		}
		Manifest sourceManifest = ManifestUtils.parseManifest(new File(dir, "manifest.txt"));
		AHKManifest manifest = ManifestUtils.buildManifestFromValues(sourceManifest, AHKManifest.class,
				ManifestUtils.CONVENTION_SCREAMING_SNAKE_CASE);
		return new AHKProjectHandle(sources, manifest);
	}
	
	public static AHKProjectHandle createUnitaryProject(File manifest, File file) throws IOException {
		UnitSource[] sources = { new UnitSource(file.getName(), FilesUtils.read(file)) };
		Manifest sourceManifest = ManifestUtils.parseManifest(manifest);
		AHKManifest man = ManifestUtils.buildManifestFromValues(sourceManifest, AHKManifest.class,
				ManifestUtils.CONVENTION_SCREAMING_SNAKE_CASE);
		return new AHKProjectHandle(sources, man);
	}
	
	public static AHKCompiledHandle compileAndLink(AHKProjectHandle project) {
		ErrorWrapper errors = null;
		try {
			errors = new ErrorWrapper("Cannot compile project");
			AHKCompiledHandle handle = Compiler.compile(project, errors);
			errors = new ErrorWrapper("Cannot link project");
			Linker.link(handle, errors);
			return handle;
		} catch (AssertionException x) {
			errors.dump();
			return null;
		}
	}
	
	public static boolean exportProject(AHKCompiledHandle project, File dir, Transpiler transpiler) {
		ErrorWrapper errors = new ErrorWrapper("Cannot export project using transpiler " + transpiler.getName());
		
		if(dir.isDirectory()) {
			try {
				FilesUtils.deleteContents(dir);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			if(!dir.mkdirs()) {
				new IOException("Unable to create directory " + dir).printStackTrace();
				return false;
			}
		}
		try {
			transpiler.exportProject(project, dir, errors);
			errors.assertNoErrors();
			return true;
		} catch (AssertionException e) {
			errors.dump();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean runProject(AHKCompiledHandle project, File dir, Transpiler transpiler) {
		ErrorWrapper errors = new ErrorWrapper("Cannot export project using transpiler " + transpiler.getName());
		try {
			transpiler.runProject(project, dir, errors);
			errors.assertNoErrors();
			return true;
		} catch (AssertionException e) {
			errors.dump();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static void main(String[] args) throws IOException {
		AHKProjectHandle project = createProject(new File("code"));
		AHKCompiledHandle handle = compileAndLink(project);
		Transpiler transpiler = new PythonTranspiler();
		File dir = new File("exported_py");
//		Transpiler transpiler = new AsmX64Transpiler();
//		File dir = new File("exported_x64");
		if(handle != null) {
			if(exportProject(handle, dir, transpiler))
				runProject(handle, dir, transpiler);
		}
		
	}
	
}
