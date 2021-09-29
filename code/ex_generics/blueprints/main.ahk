
base fr.wonder.main;

import ahk.Kernel;

unit Main;

struct Struct : #Blueprint {
	int a;
	
	constructor(int a);
	
	operator addStructs : Struct + Struct = Struct;
}

func Struct addStructs(Struct s1, Struct s2) {
	return Struct(s1.a + s2.a);
}

struct Gen<[X : #Blueprint]> {
	X x;
	
	constructor(X x);
}

blueprint #Blueprint {
	operator Self + Self = Self;
}


func <[T : #Blueprint]> Gen<[T]> addGens(Gen<[T]> g1, Gen<[T]> g2) {
	return Gen<[T]>(g1.x + g2.x);
}

/*
func Gen<[int]> addIntGens(Gen<[int]> g1, Gen<[int]> g2) {
	return Gen<[int]>(g1.x+g2.x);
}

func <[T]> Gen<[Gen<[T]>]> genGen(T t) {
	return Gen<[Gen<[T]>]>(Gen<[T]>(t));
}
*/

func <[T,R]> void consume(T t, R r) {}

global func int main() {
	Kernel.println("------ Expected: 4");
	Kernel.printlni(4);
	
	Kernel.printlni(Gen<[Struct]>(Struct(43)).x.a);
	consume<[int, float]>(4, 5.5);
	
	return 5;
}
