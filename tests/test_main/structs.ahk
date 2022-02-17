base fr.wonder.main;

unit Structs;

global struct Structure {
  
  null (a=65);
  
  global int a;
  
  global constructor(int a);
  
  operator opStructPlus : Structure + Structure = Structure;
  
}

global func int fff(Structure a) {
	return a.a;
}

global func Structure opStructPlus(Structure a, Structure b) {
	return Structure(a.a+b.a);
}

global struct Cyclic1 {
	
	global Cyclic2 other;
	global int a = 32;
	
}

global struct Cyclic2 {

	global int b = 29;
	global Cyclic1 other;

}
