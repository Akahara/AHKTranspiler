base fr.wonder.main;

import ahk.Kernel;

unit Unit;

str x = "str";
int gi = 4;
int[] array = [56, 42, 37];

func int gcd(int x, int y) {
	if(y == 0)
		return x;
	else
		return gcd(y, x%y);
}

func int a(int a, int b) {
	for(int i = -10..10) {
		Kernel.print(i);
		Kernel.print(" / 5 = ");
		Kernel.print(i/5);
		Kernel.print("   ");
		Kernel.print(i);
		Kernel.print(" % 5 = ");
		Kernel.print(i%5);
		Kernel.println();
	}
	return 0;
}

func int a(int a) {
	return a+1;
}

func int main() {
	for(int i =  0 : i < sizeof(array) : i++) {
		Kernel.print(array[i]);
		Kernel.println();
	}
	return 0;
}