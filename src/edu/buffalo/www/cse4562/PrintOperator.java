package edu.buffalo.www.cse4562;

import java.util.ArrayList;

import net.sf.jsqlparser.expression.PrimitiveValue;

public class PrintOperator {
	public static void Print(Operator input){
		ArrayList<PrimitiveValue> tuple = input.readOneTuple();
		while(tuple != null){
			String s = "";
			for(PrimitiveValue t : tuple){
				s += t.toString()+"|";
			}
			System.out.println(s.substring(0, s.length()-1));
			tuple = input.readOneTuple();
		}
	}
}
