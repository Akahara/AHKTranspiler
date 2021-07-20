package fr.wonder.ahk.compiled.units;

public enum UnitCompilationState {
	
	/**
	 * The unit' source was tokenized but nothing was parsed yet. The unit does not
	 * properly-said exist yet, you should never see this state as the parser
	 * immediately changes it to PARSED(WITH_ERRORS)
	 */
	FRESH (0),
	
	/**
	 * The unit' sections have been parsed, including variables, structures,
	 * functions and their bodies / default values...<br>
	 * Functions' types are not computed yet, the unit needs to be prelinked
	 * first.<br>
	 * Signatures and prototypes are not set either as they need types to be
	 * computed.<br>
	 * The unit may contain duplicate variables and such errors that will be
	 * reported during prelinkage.
	 */
	PARSED (1),
	/** See {@link #PARSED}, but with an error */
	PARSED_WITH_ERRORS (-1),
	
	/**
	 * Functions and structures types are set, prototypes and signatures as
	 * well.<br>
	 * The unit' structure was validated, no duplicate variable exist, functions
	 * have valid types... However expressions and statements are not, this will be
	 * done during linkage.
	 */
	PRELINKED (2),
	/** See {@link #PRELINKED}, but with an error */
	PRELINKED_WITH_ERRORS (-2),
	
	/**
	 * Statements and expressions have been validated ; functions calls and
	 * variables are bound to their declarations' prototypes.<br>
	 * The code is ready to be run.
	 */
	LINKED (3),
	/** See {@link #LINKED}, but with an error */
	LINKED_WITH_ERRORS (-3);
	
	/**
	 * The step in the compilation process, negative values indicate that an error
	 * occurred and higher values are further in the compilation order
	 */
	public final int step;
	
	private UnitCompilationState(int step) {
		this.step = step;
	}
	
}
