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
}
