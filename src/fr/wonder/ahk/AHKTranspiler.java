package fr.wonder.ahk;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiler.Compiler;
import fr.wonder.ahk.transpilers.Transpiler;
import fr.wonder.ahk.transpilers.asm_x64.AsmX64Transpiler;
import fr.wonder.ahk.utils.CompilationError;
import fr.wonder.ahk.utils.ErrorWrapper;
import fr.wonder.commons.files.FilesUtils;
import fr.wonder.commons.files.Manifest;
import fr.wonder.commons.utils.ManifestUtils;

public class AHKTranspiler {
	
	public static AHKProjectHandle createProject(File dir) throws IOException {
		List<File> files = FilesUtils.listFilesRecur(dir, f->f.isFile() && f.getName().endsWith(".ahk"));
		UnitSource[] sources = new UnitSource[files.size()];
		for(int i = 0; i < files.size(); i++) {
			File f = files.get(i);
			sources[i] = new UnitSource(f.getName(), FilesUtils.read(f));
		}
		Manifest sourceManifest = ManifestUtils.parseManifest(new File(dir, "manifest.txt"));
		AHKManifest manifest = ManifestUtils.createManifest2(sourceManifest, AHKManifest.class,
				ManifestUtils.CONVENTION_SCREAMING_SNAKE_CASE);
		return new AHKProjectHandle(sources, manifest);
	}
	
	public static AHKCompiledHandle compile(AHKProjectHandle project) {
		ErrorWrapper errors = new ErrorWrapper("Cannot compile project");
		try {
			return Compiler.compile(project, errors);
		} catch (CompilationError x) {
			errors.dump();
			return null;
		}
	}
	
	public static boolean exportProject(AHKCompiledHandle project, File dir, Transpiler transpiler) {
		ErrorWrapper errors = new ErrorWrapper("Cannot export project using transpiler " + transpiler.getName());
		try {
			transpiler.exportProject(project, dir, errors);
			errors.assertNoErrors();
			return true;
		} catch (CompilationError e) {
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
		} catch (CompilationError e) {
			errors.dump();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static void main(String[] args) throws IOException {
		AHKProjectHandle project = createProject(new File("code"));
		AHKCompiledHandle handle = compile(project);
//		Transpiler transpiler = new PythonTranspiler();
//		File dir = new File("exported_py");
		Transpiler transpiler = new AsmX64Transpiler();
		File dir = new File("exported_x64");
		if(handle != null) {
			if(exportProject(handle, dir, transpiler))
				runProject(handle, dir, transpiler);
		}
		
	}
	
}
