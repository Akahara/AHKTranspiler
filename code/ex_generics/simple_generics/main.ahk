
base fr.wonder.main;

import ahk.Kernel;

unit Main;

struct Struct {
	int a;
	
	constructor(int a);
}

func Struct addStructs(Struct s1, Struct s2) {
	return Struct(s1.a + s2.a);
}

struct Gen<[X]> {
	X x;
	
	constructor(X x);
}

func <[T]> Gen<[T]> addGens(Gen<[T]> g1, Gen<[T]> g2) {
	return Gen<[T]>(g1.x);
}

func Gen<[int]> addIntGens(Gen<[int]> g1, Gen<[int]> g2) {
	return Gen<[int]>(g1.x+g2.x);
}

func <[T]> Gen<[Gen<[T]>]> genGen(T t) {
	return Gen<[Gen<[T]>]>(Gen<[T]>(t));
}

func <[T,R]> void consume(T t, R r) {}

global func int main() {
	Kernel.println("------ Expected: 4 43 43 111 1 2 3");
	Kernel.printlni(4);
	
	Struct s = Struct(43);
	Gen<[Struct]> g = Gen<[Struct]>(s);
	Struct ss = g.x;
	Kernel.printlni(ss.a);
	Kernel.printlni(addGens<[Struct]>(g, g).x.a); // "addGen" does not actually add Gens
	Kernel.printlni(addIntGens(Gen<[int]>(100), Gen<[int]>(11)).x);
	Kernel.printlni(genGen<[int]>(1).x.x);
	Kernel.printlni(genGen<[int[][]]>([[2]]).x.x[0][0]);
	Kernel.printlni(genGen<[Gen<[int[]]>]>(Gen<[int[]]>([3])).x.x.x[0]);
	consume<[int, float]>(4, 5.5);
	
	return 5;
}
