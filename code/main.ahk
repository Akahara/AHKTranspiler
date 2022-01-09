base fr.wonder.main;

import ahk.Kernel;

unit Main;

struct Struct {
	str s;
	constructor(str s);
}

global func int main() {
	Kernel.out << 5 << "\n";
	Kernel.out << -5 << "\n";
	Kernel.out << 7 << "\n";
	Kernel.out << -3.7 << "\n";
	Kernel.out << -3.123456789123456789123456789 << "\n";
	Kernel.out << 5.2 << "\n";
	Kernel.out << int:(-3.6) << "\n";
	Kernel.out << int:(-3.2) << "\n";
	Kernel.out << int:(3.6) << "\n";
	Kernel.out << int:(3.2) << "\n";
}
