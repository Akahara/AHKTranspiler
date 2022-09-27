base fr.wonder.main;

import ahk.Kernel;

unit Main;

enum @enum {
	foo,
	bar,
}

enum @enum2 {
	foo,
	baz
}

global func int main() {
	@enum a = @enum::foo;
	@enum2 b = @enum2::foo;
	
	Kernel.out << (a == @enum::bar) << Kernel.endl;
	Kernel.out << (a == @enum::foo) << Kernel.endl;
	Kernel.out << (b != @enum2::baz) << Kernel.endl;
	
	return 0;
}
