package fr.wonder.ahk.handles;

import fr.wonder.ahk.transpilers.Transpiler;

/**
 * An executable project handle is created by a specific {@link Transpiler} that
 * will be forwarded to its
 * {@link Transpiler#runProject(ExecutableHandle, java.io.File, fr.wonder.commons.exceptions.ErrorWrapper)
 * runProject()} method. Implementations may include any amount of data required
 * to run the exported project.
 */
public abstract class ExecutableHandle {

}
