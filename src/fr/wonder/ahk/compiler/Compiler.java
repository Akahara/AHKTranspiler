package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.handles.CompiledHandle;
import fr.wonder.ahk.handles.ProjectHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class Compiler {

	/**
	 * @param unitaryComp whether the project should be compiled as a whole or as a
	 *                    collection of units.
	 */
	public static CompiledHandle compile(ProjectHandle project, ErrorWrapper errors) throws WrappedException {
		Unit[] units = new Unit[project.units.length];
		for (int i = 0; i < project.units.length; i++) {
			try {
				UnitSource source = project.units[i];
				units[i] = UnitParser.parseUnit(source, errors.subErrrors("Parsing errors in unit " + source.name));
			} catch (WrappedException x) {
				// catching there only results in skipping a part of
				// the compilation of the current source, errors will
				// still be reported in the error wrapper
				// TODO search for uses of this function and make sure that errors are handled
			}
		}
		errors.assertNoErrors();

		return new CompiledHandle(units);
	}

}
