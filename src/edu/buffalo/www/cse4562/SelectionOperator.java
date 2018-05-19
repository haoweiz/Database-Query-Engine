package edu.buffalo.www.cse4562;
import java.util.ArrayList;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.expression.PrimitiveValue.InvalidPrimitive;


public class SelectionOperator implements Operator{
	private ArrayList<ColumnWithType> column;
	private Operator input;
	private Expression condition;
	private Evaluator evaluator;
	
	public SelectionOperator(Operator input,Expression condition){
		this.column = input.getColumnWithType();
		this.input = input;
		this.condition = condition;
		this.evaluator = new Evaluator(column);
	}
	

	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		ArrayList<PrimitiveValue> tuple = null;
		do{
			tuple = input.readOneTuple();
			if(tuple == null) return null;
			
			evaluator.setTuple(tuple);
			try {
				if(!evaluator.evaluate(condition))
					tuple = null;
			} catch (InvalidPrimitive e) {
				e.printStackTrace();
			}
		}while(tuple == null);
		return tuple;
	}



	@Override
	public ArrayList<ColumnWithType> getColumnWithType() {
		// TODO Auto-generated method stub
		return column;
	}
}