base fr.wonder.main;

import ahk.Kernel;
import fr.wonder.main.Structs;

unit Main;

alias Afunction = func int(int);
alias IntAlias = int;
alias IntArray = IntAlias[];
alias ComplexAlias = func Afunction(int, IntArray)[];

int ii = 3;

int gi = 8;
str x = "str";
int[] array = [56, 42, 37];
Structure structure = null;

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
	Kernel.println("------ Expected: argc argv[1]");
	Kernel.println(sizeof(Kernel.argv));
	Kernel.println(Kernel.argv[1]);
	Kernel.println("------ Expected: 4 6 6 2 1 10");
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
	Kernel.println("------ Expected str 56 42 37 3 32");
	Kernel.println(x);
	Kernel.println(array[0]);
	Kernel.println(array[1]);
	Kernel.println(array[2]);
	Kernel.println(sizeof(array));
	array[1] = 32;
	Kernel.println(array[1]);
	Kernel.println("------ Expected a division/modulus table");
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
	Kernel.println("------ Expected -1 3 -2 -3 63 12 12 10 2 1");
	Kernel.println(-2/2);
	Kernel.println(int:(3.8));
	Kernel.println(int:(2+(-3.5)));
	Kernel.println(int:(-(3.+0.)));
	Kernel.println(255>>2);
	Kernel.println(3<<2);
	Kernel.println(int:(8*1.5));
	Kernel.println(int:(32./3.));
	Kernel.println(int:(32.%3.));
	Kernel.println("------ Expected F T T");
	Kernel.println(3==0);
	Kernel.println(!false);
	Kernel.println(3 != true);
	Kernel.println("------ Expected composed strings:");
	Kernel.println("a composed " + "string");
	Kernel.println("");
	Kernel.println("------ Expected 2 37");
	Cyclic1 cyclic1 = null;
	cyclic1.a = 2;
	Kernel.println(cyclic1.other.other.a);
	Kernel.println(array[-1]);
	IntAlias aliasedInt = 5;
	Kernel.println(aliasedInt);
	IntArray aliasedIntArray = [0, 1, 2];
	Kernel.println(aliasedIntArray[1]);
	ComplexAlias complex = null;
	Kernel.println(int:(complex));
	
	return 5;
}
