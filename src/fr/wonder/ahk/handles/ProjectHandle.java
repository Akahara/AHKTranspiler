package fr.wonder.ahk.handles;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiler.Compiler;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class ProjectHandle {
	
	public final UnitSource[] units;
	public final AHKManifest manifest;
	
	public ProjectHandle(UnitSource[] units, AHKManifest manifest) {
		this.units = units;
		this.manifest = manifest;
	}
	
	public CompiledHandle compile(ErrorWrapper errors) throws WrappedException {
		return Compiler.compile(this, errors);
	}
	
}
