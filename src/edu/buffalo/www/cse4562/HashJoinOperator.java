package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import java.util.HashMap;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;

public class HashJoinOperator implements Operator{
	private ArrayList<ColumnWithType> column;
	private Operator parser;
	private Operator subparser;
	private ArrayList<Column> left;
	private ArrayList<Column> right;
	private ArrayList<Integer> l;
	private ArrayList<Integer> r;
	private int index;
	
	private ArrayList<PrimitiveValue> tuple1;
	private HashMap<ArrayList<PrimitiveValue>,ArrayList<ArrayList<PrimitiveValue>>> hash;
	
	private ArrayList<PrimitiveValue> res;
	private ArrayList<PrimitiveValue> key;
	
	public HashJoinOperator(Expression where,Operator parser,Operator subparser){
		this.parser = parser;
		this.subparser = subparser;
		index = 0;
		left = new ArrayList<Column>();
		right = new ArrayList<Column>();
		res = new ArrayList<PrimitiveValue>();
		key = new ArrayList<PrimitiveValue>();
		ArrayList<Expression> exp = this.Seperate(where);
		for(int i = 0;i != exp.size();++i) {
			Expression e = exp.get(i);
			left.add((Column)((EqualsTo)e).getLeftExpression());
			right.add((Column)((EqualsTo)e).getRightExpression());
		}
		l = new ArrayList<Integer>();
		r = new ArrayList<Integer>();
		
		hash = new HashMap<ArrayList<PrimitiveValue>,ArrayList<ArrayList<PrimitiveValue>>>();
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
		for(int i = 0;i != left.size();++i) {
			for(int j = 0;j != leftcolumn.size();++j) {
				if(leftcolumn.get(j).getColumn().equals(left.get(i)) || leftcolumn.get(j).getColumn().equals(right.get(i))) {
					l.add(j);
				}
			}
		}
		ArrayList<ColumnWithType> rightcolumn = subparser.getColumnWithType();
		for(int i = 0;i != right.size();++i) {
			for(int j = 0;j != rightcolumn.size();++j) {
				if(rightcolumn.get(j).getColumn().equals(left.get(i)) || rightcolumn.get(j).getColumn().equals(right.get(i))) {
					r.add(j);
				}
			}
		}
			
		ArrayList<PrimitiveValue> tuple = subparser.readOneTuple();
		while(tuple != null){
			ArrayList<PrimitiveValue> key = new ArrayList<PrimitiveValue>();
			for(int i = 0;i != r.size();++i)
				key.add(tuple.get(r.get(i)));
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
	private ArrayList<Expression> Seperate(Expression e){
		ArrayList<Expression> result = new ArrayList<Expression>();
		ArrayList<Expression> left = null;
		ArrayList<Expression> right = null;
		if(e instanceof AndExpression){
			right = Seperate(((AndExpression) e).getRightExpression());
			if(right != null)
				result.addAll(right);
			left = Seperate(((AndExpression) e).getLeftExpression());
			if(left != null)
				result.addAll(left);
		}
		else
		    result.add(e);
		return result;
	}
	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		while(true){
			res.clear();
			key.clear();
			for(int i = 0;i != l.size();++i)
				key.add(tuple1.get(l.get(i)));
			if(!hash.containsKey(key)){
				tuple1 = parser.readOneTuple();
				if(tuple1 == null) return null;
				continue;
			}
		    if(hash.get(key).size() != index){
			    res.addAll(tuple1);
			    res.addAll(hash.get(key).get(index));
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
