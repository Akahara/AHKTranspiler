
base fr.wonder.main;

import ahk.Kernel;

unit Main;

alias IntGen = func int(int, int, int);

blueprint #Blueprint {
	operator Self + Self = Self;
}

struct Struct : #Blueprint {
	int a;
	
	constructor(int a);
	
	operator addStructs : Struct + Struct = Struct;
}

struct NGen<[X]> {
	X x;
	constructor(X x);
}

struct Gen<[X : #Blueprint]> {
	X x;
	constructor(X x);
}

func Struct addStructs(Struct s1, Struct s2) {
	return Struct(s1.a + s2.a);
}

func int intGen(int x, int y, int z) {
	return x+1;
}

func <[T : #Blueprint]> void testGIP() {}

func NGen<[int]> addIntNGens(NGen<[int]> g1, NGen<[int]> g2) {
	return NGen<[int]>(g1.x+g2.x);
}

func <[T : #Blueprint]> Gen<[T]> addGens(Gen<[T]> g1, Gen<[T]> g2) {
	return Gen<[T]>(g1.x + g2.x);
}


func <[T,R]> void consume(T t, R r) {}

global func int main() {
	Kernel.println("------ Expected: 4 43 2 6 7");
	Kernel.printlni(4);
	
	Kernel.printlni(Gen<[Struct]>(Struct(43)).x.a);
	consume<[int, float]>(4, 5.5);
	IntGen ig = intGen;
	Kernel.out << ig(1, 2, 3) << "\n";
	testGIP<[Struct]>();
	Kernel.out << addIntNGens(NGen<[int]>(2), NGen<[int]>(4)).x << "\n";
	Gen<[Struct]> g1 = Gen<[Struct]>(Struct(3));
	Gen<[Struct]> g2 = Gen<[Struct]>(Struct(4));
	Gen<[Struct]> res = addGens<[Struct]>(g1, g2);
	Kernel.out << res.x.a;
	
	return 5;
}