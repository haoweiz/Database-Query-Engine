package edu.buffalo.www.cse4562;

import java.util.ArrayList;

import net.sf.jsqlparser.expression.PrimitiveValue;

public class JoinOperator implements Operator{
	private ArrayList<ColumnWithType> column;
	private ArrayList<ArrayList<PrimitiveValue>> data;
	
	private Operator input;
	private Operator subparser;
	private int index;
	
	private ArrayList<PrimitiveValue> tuple1;
	
	public JoinOperator(Operator input,Operator subparser){
		index = 0;
		column = new ArrayList<ColumnWithType>();
		data = new ArrayList<ArrayList<PrimitiveValue>>();
		ArrayList<ColumnWithType> column1 = input.getColumnWithType();
		ArrayList<ColumnWithType> column2 = subparser.getColumnWithType();
		column.addAll(column1);
		column.addAll(column2);

		tuple1 = input.readOneTuple();
		this.input = input;
		this.subparser = subparser;
		setData();
	}
	
	private void setData(){
		ArrayList<PrimitiveValue> tuple = subparser.readOneTuple();
		while(tuple != null){
			data.add(tuple);
			tuple = subparser.readOneTuple();
		}
	}
	
	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		ArrayList<PrimitiveValue> res = new ArrayList<PrimitiveValue>();
		if(index < data.size()){
			ArrayList<PrimitiveValue> tuple2 = data.get(index);
		    res.addAll(tuple1);
			res.addAll(tuple2);
			index++;
		}
		else{
			tuple1 = input.readOneTuple();
			index = 0;
			ArrayList<PrimitiveValue> tuple2 = data.get(index);
			index++;
			if(tuple1 == null) return null;
			res.addAll(tuple1);
			res.addAll(tuple2);
		}
		if(res.size() != 0)
		    return res;
		else 
			return null;
	}



	@Override
	public ArrayList<ColumnWithType> getColumnWithType() {
		// TODO Auto-generated method stub
		return column;
	}

}
