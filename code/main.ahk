base fr.wonder.main;

import ahk.Kernel;

unit Unit;

int ii = -3;
	
int gi = 8;
str x = "str";
int[] array = [56, 42, 37];

func int gcd(int x, int y) {
	if(y == 0)
		return x;
	else
		return gcd(y, x%y);
}
func int gcd2(int x, int y) {
	while(y) {
		int t = x;
		x = y;
		y = t%y;
	}
	return x;
}

func int main() {
	Kernel.println(Kernel.argv[0]);
	Kernel.println("Expected: 4 6 6 2 1 10");
	int c = 2;
	int l = 4;
	int j = 3;
	if(true) {
		int k = 5;
		Kernel.print(l);
	}
	Kernel.println();
	Kernel.println(gcd(24, 18));
	Kernel.println(gcd2(24, 18));
	Kernel.println(7/3);
	Kernel.println(7%3);
	Kernel.println(gi+c);
	Kernel.println("Expected str 56 42 37 3 32");
	Kernel.println(x);
	Kernel.println(array[0]);
	Kernel.println(array[1]);
	Kernel.println(array[2]);
	Kernel.println(sizeof(array));
	array[1] = 32;
	Kernel.println(array[1]);
	for(int i : -10..10) {
		Kernel.print(i);
		Kernel.print(" / 5 = ");
		Kernel.print(i/5);
		Kernel.print("   ");
		Kernel.print(i);
		Kernel.print(" % 5 = ");
		Kernel.print(i%5);
		Kernel.println();
	}
	Kernel.println(-2/2);
	return 5;
}
