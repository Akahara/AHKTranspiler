base fr.wonder.main;

import ahk.Kernel;

unit Unit;

func int gcd(int x, int y) {
	if(y == 0)
		return x;
	else
		return gcd(y, x%y);
}

func int main() {
	int c = 2;
	int i = 4;
	int j = 3;
	if(true) {
		int k = 5;
		Kernel.print(i);
	}
	Kernel.println();
	Kernel.print(gcd(24, 18));
	Kernel.println();
	Kernel.print(7/3);
	Kernel.print(7%3);
	Kernel.println();
}
