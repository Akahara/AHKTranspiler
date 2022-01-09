
base fr.wonder.main;

import ahk.Kernel;

unit Main;

struct Struct {
	str s;
	constructor(str s);
}

global func int main() {
	alloc1();
	Struct s3 = Struct("s3");
	Kernel.runGarbageCollector();
	Struct s4 = Struct("s4");
	return 5;
}

func void alloc1() {
	Struct s1 = Struct("s1");
	Struct s2 = Struct("s2");
}
