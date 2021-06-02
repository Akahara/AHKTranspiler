base fr.wonder.main;

import ahk.Kernel;

unit Unit;

func int main() {
	int c = 2;
	int i = 4;
	int j = 3;
	if(true) {
		int k = 5;
		Kernel.print(i);
	}
	Kernel.println();
}
