package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiler.linker.Linker;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class CompiledHandle {

	public final Unit[] units;
	public final AHKManifest manifest;
	
	public CompiledHandle(Unit[] units, AHKManifest manifest) {
		this.units = units;
		this.manifest = manifest;
	}
	
	public LinkedHandle link(ErrorWrapper errors) throws WrappedException {
		return new Linker(this).link(errors);
	}

}
