base fr.wonder.main;

import ahk.Kernel;

unit Main;

alias Func = func int(int);

func int foo(int i) {
	return i+5;
}

global func int main() {
	part1();
	part2(50);
	return 0;
}

func void part1() {
	int j = 10;
	Func f = (int x)[j]:int => x+j+1;
	Func ff = (int x)[j, f]:int => j;
	Func fooAsFunc = foo;

	Kernel.out << "Expected: 111 10 3x20\n";
	Kernel.out << f(100) << "\n";
	Kernel.out << ff(100) << "\n";
	Kernel.out << foo(15) << "\n";
	Kernel.out << (foo)(15) << "\n"; // calls the function normally, not with a closure
	Kernel.out << fooAsFunc(15) << "\n";
}

func void part2(int i) {
	Func f = (int x)[i]:int => i*x;
	Func ff = (int x)[f]:int => f(x);
	
	Kernel.out << "Expected: 150 250\n";
	Kernel.out << f(3) << "\n";
	Kernel.out << ff(5) << "\n";
}
