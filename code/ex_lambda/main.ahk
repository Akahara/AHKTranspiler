base fr.wonder.main;

import ahk.Kernel;

unit Main;

alias Generator = func int(int);

func int identity(int x) {
	return x;
}

global func int main() {
	Generator i = identity;
	Kernel.out << i(4) << "\n";
	Generator j = (int x):int => x;
	Kernel.out << j(4) << "\n";
	Kernel.out << ((int x):int => x+2)(4) << "\n";
	Kernel.out << ((int x):int => identity(x)*3+2)(3) << "\n";
}
