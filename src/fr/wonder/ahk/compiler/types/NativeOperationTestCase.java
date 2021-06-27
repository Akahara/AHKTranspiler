package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;

class NativeOperationTestCase {

	public static void main(String[] args) {
		VarType[] types = { VarType.INT, VarType.BOOL, VarType.FLOAT, VarType.STR };
		
		System.out.println("order: (i)nt (b)ool (f)loat (s)tr");
		
		for(Operator o : Operator.values()) {
			if(o == Operator.NOT)
				continue;
			
			System.out.println("\n");
			System.out.println(o);
			for(VarType r : types) {
				for(VarType l : types) {
					NativeOperation op = NativeOperation.getOperation(l, r, o, true);
					if(op == null)
						System.out.print("_ ");
					else
						System.out.print(op.getResultType().getName().charAt(0) + " ");
//						System.out.print(op + " ");
				}
				System.out.println();
			}
		}
		
		System.out.println("\nnull_SUBSTRACT");
		for(VarType r : types) {
			NativeOperation op = NativeOperation.getOperation(null, r, Operator.SUBSTRACT, true);
			if(op == null)
				System.out.println("_ ");
			else
				System.out.println(op.getResultType().getName().charAt(0) + " ");
		}
	}
	
}
