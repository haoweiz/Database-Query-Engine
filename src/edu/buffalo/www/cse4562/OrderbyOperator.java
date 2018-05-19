package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.statement.select.OrderByElement;

public class OrderbyOperator implements Operator{

	private ArrayList<ColumnWithType> column;
	private Operator input;
	private int index;
	
	private List<OrderByElement> order;
	private ArrayList<ArrayList<PrimitiveValue>> data;
	
	public OrderbyOperator(Operator input,List<OrderByElement> orderby){
		this.input = input;
		this.column = input.getColumnWithType();
		order = orderby;
		
		data = new ArrayList<ArrayList<PrimitiveValue>>();
		this.sort();
		index = 0;
	}

	private void sort(){
		ArrayList<PrimitiveValue> t = input.readOneTuple();
		while(t != null){
			data.add(t);
			t = input.readOneTuple();
		}
		
		Collections.sort(data,new MyComparator(order,column));
	}
	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		ArrayList<PrimitiveValue> tuple = null;
		if(index != data.size()){
		     tuple = data.get(index);
		     index++;
		}
		else{
			index = 0;
			return null;
		}
		return tuple;
	}


	@Override
	public ArrayList<ColumnWithType> getColumnWithType() {
		// TODO Auto-generated method stub
		return column;
	}
}
