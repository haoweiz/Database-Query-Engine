package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import java.util.HashMap;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;

public class OneExpHashJoinOperator implements Operator{
	private ArrayList<ColumnWithType> column;
	private Operator parser;
	private Operator subparser;
	private Column left;
	private Column right;
	private int l;
	private int r;
	private int index;
	
	private ArrayList<PrimitiveValue> tuple1;
	private HashMap<PrimitiveValue,ArrayList<ArrayList<PrimitiveValue>>> hash;
	
	private ArrayList<PrimitiveValue> res;
	
	public OneExpHashJoinOperator(Expression where,Operator parser,Operator subparser){
		this.parser = parser;
		this.subparser = subparser;
		res = new ArrayList<PrimitiveValue>();
		index = 0;
		left = (Column)((EqualsTo)where).getLeftExpression();
		right = (Column)((EqualsTo)where).getRightExpression();
		l = 0;
		r = 0;
		
		hash = new HashMap<PrimitiveValue,ArrayList<ArrayList<PrimitiveValue>>>();
		column = new ArrayList<ColumnWithType>();
		ArrayList<ColumnWithType> column1 = parser.getColumnWithType();
		ArrayList<ColumnWithType> column2 = subparser.getColumnWithType();
		column.addAll(column1);
		column.addAll(column2);
		
		tuple1 = parser.readOneTuple();
		this.setData();
	}
	private void setData(){
		ArrayList<ColumnWithType> leftcolumn = parser.getColumnWithType();
		for(int i = 0;i != leftcolumn.size();++i){
			if(leftcolumn.get(i).getColumn().equals(left) || leftcolumn.get(i).getColumn().equals(right)){
				l = i;
				break;
			}
		}
		ArrayList<ColumnWithType> rightcolumn = subparser.getColumnWithType();
		for(int i = 0;i != rightcolumn.size();++i){
			if(rightcolumn.get(i).getColumn().equals(left) || rightcolumn.get(i).getColumn().equals(right)){
				r = i;
				break;
			}
		}
			
		ArrayList<PrimitiveValue> tuple = subparser.readOneTuple();
		while(tuple != null){
			PrimitiveValue key = tuple.get(r);
			if(hash.containsKey(key)){
				hash.get(key).add(tuple);
			}
			else{
				ArrayList<ArrayList<PrimitiveValue>> new_tuple = new ArrayList<ArrayList<PrimitiveValue>>();
				new_tuple.add(tuple);
			    hash.put(key, new_tuple);
			}
			tuple = subparser.readOneTuple();
		}
	}
	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		res.clear();
		while(true){
			if(!hash.containsKey(tuple1.get(l))){
				tuple1 = parser.readOneTuple();
				if(tuple1 == null) return null;
				continue;
			}
		    if(hash.get(tuple1.get(l)).size() != index){
			    res.addAll(tuple1);
			    res.addAll(hash.get(tuple1.get(l)).get(index));
			    index++;
			    break;
		    }
		    else{
			    tuple1 = parser.readOneTuple();
			    index = 0;
			    if(tuple1 == null) return null;
			    continue;
		    }
		}
		return res;
	}

	@Override
	public ArrayList<ColumnWithType> getColumnWithType() {
		return column;
	}

}
