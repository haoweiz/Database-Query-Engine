package edu.buffalo.www.cse4562;

import java.util.ArrayList;

import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.statement.select.Limit;

public class LimitOperator implements Operator {
	private ArrayList<ColumnWithType> column;
	private Operator input;
	private static long number;
	
	public LimitOperator(Operator input,Limit limit){
		column = input.getColumnWithType();
		this.input = input;
		number = limit.getRowCount();
	}

	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		ArrayList<PrimitiveValue> tuple = input.readOneTuple();
		if(number != 0){
			number--;
			return tuple;
		}
		return null;
	}


	@Override
	public ArrayList<ColumnWithType> getColumnWithType() {
		// TODO Auto-generated method stub
		return column;
	}

}
