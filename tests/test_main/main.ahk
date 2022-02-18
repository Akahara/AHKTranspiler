base fr.wonder.main;

import ahk.Kernel;
import fr.wonder.main.Structs;

unit Main;

alias Afunction = func Structure(int);
alias IntAlias = int;
alias IntArray = IntAlias[];
alias ComplexAlias = func Afunction(int, IntArray)[];
alias GenFunc = func int(int);
alias SGenFunc = func Structure(int);

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
		x, y = y, x%y;
	}
	return x;
}

func int gen1(int x) {
	return x+1;
}
func int gen2(int x) {
	return x*2;
}
func bool genb(bool b) {
	return !b;
}

func Structure structGen(int i) {
	return Structure(i);
}

global func int main() {
	Kernel.println("------ Expected: argc argv[1]");
	Kernel.printlni(sizeof(Kernel.argv));
	Kernel.println(Kernel.argv[1]);
	Kernel.println("------ Expected: 4 6 6 2 1 10");
	int c = 2;
	int l = 4;
	int j = 3;
	if(true) {
		int k = 5;
		Kernel.out << l;
	}
	Kernel.out << "\n";
	Kernel.printlni(gcd(24, 18));
	Kernel.printlni(gcd2(24, 18));
	Kernel.printlni(7/3);
	Kernel.printlni(7%3);
	Kernel.printlni(gi+c);
	Kernel.println("------ Expected str 56 42 37 3 32");
	Kernel.println(x);
	Kernel.printlni(array[0]);
	Kernel.printlni(array[1]);
	Kernel.printlni(array[2]);
	Kernel.printlni(sizeof(array));
	array[1] = 32;
	Kernel.printlni(array[1]);
	Kernel.println("------ Expected a division/modulus table");
	for(int i : -10..10) {
		Kernel.out << i << " / 5 = " << i/5 << "   " << i << " % 5 = " << i%5 << "\n";
	}
	Kernel.println("------ Expected -1 3 -1 -3 63 12 12 10 2");
	Kernel.printlni(-2/2);
	Kernel.printlni(int:(3.8));
	Kernel.printlni(int:(2+(-3.5)));
	Kernel.printlni(int:(-(3.+0.)));
	Kernel.printlni(255>>2);
	Kernel.printlni(3<<2);
	Kernel.printlni(int:(8*1.5));
	Kernel.printlni(int:(32./3.));
	Kernel.printlni(int:(32.%3.));
	Kernel.println("------ Expected F T T");
	Kernel.printlnb(3==0);
	Kernel.printlnb(!false);
	Kernel.printlnb(3 != true);
	Kernel.println("------ Expected composed strings:");
	Kernel.println("a composed " + "string");
	Kernel.println(""); // TODO str + int
	Kernel.println("------ Expected 2 37 5 1 65 42 22 65");
	Cyclic1 cyclic1 = null;
	cyclic1.a = 2;
	Kernel.printlni(cyclic1.other.other.a);
	Kernel.printlni(array[-1]);
	IntAlias aliasedInt = 5;
	Kernel.printlni(aliasedInt);
	IntArray aliasedIntArray = [0, 1, 2];
	Kernel.printlni(aliasedIntArray[1]);
	Afunction function = null;
	Kernel.printlni(function(0).a);
	function = structGen;
	Kernel.printlni(function(42).a);
	Kernel.printlni((function(10)+function(12)).a);
	Kernel.printlni(Structs.fff(structure));
	Kernel.println("------ Expected 9 7 0");
	Kernel.printlni(3^2);
	Kernel.printlni((gen1 << gen2)(3)); // apply gen2 first
	GenFunc gen = null;
	Kernel.printlni(gen(3));
	Kernel.println("------ Expected 65, 130 four times, 9 -6 -4 true");
	SGenFunc sgen = null;
	Kernel.printlni(sgen(0).a);				// simply call the function
	Kernel.printlni(sgen(0).a+sgen(0).a);	// simple call the function twice and add results members
	Kernel.printlni((sgen(0)+sgen(0)).a);	// add results and retrieve member
	Kernel.printlni((sgen+sgen)(0).a);		// add functions and then call, and retrieve member
	Kernel.printlni((sgen+sgen(0))(0).a);	// add function and structure, call and retrieve member
	Kernel.printlni((gen1+5)(3)); // gen1 adds 1, gen1(3)+5 = 9
	Kernel.printlni((-gen1)(5));
	Kernel.printlnb((!genb)(true)); // genb returns the oposite of the argument
	
	Kernel.println("----- Expected 4");
	Kernel.printlni(l); // if the stack was not messed up, this should print 4
	
	Kernel.out << "----- Expected 45 3.5 ~2.3 0.\n";
	Kernel.out << 4 << 5 << "\n";
	Kernel.out << 3.5 << "\n" << 2.3 << "\n";
	Kernel.out << 0. << "\n";
	
	return 5;
}
