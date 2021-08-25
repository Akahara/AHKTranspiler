package fr.wonder.ahk.transpilers;

import java.io.File;
import java.io.IOException;

import fr.wonder.ahk.handles.ExecutableHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public interface Transpiler {
	
	public String getName();
	public ExecutableHandle exportProject(LinkedHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException;
	public Process runProject(ExecutableHandle handle, File dir, ErrorWrapper errors) throws IOException, WrappedException;
	
}
