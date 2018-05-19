package edu.buffalo.www.cse4562;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import net.sf.jsqlparser.eval.Eval;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.expression.PrimitiveValue.InvalidPrimitive;
import net.sf.jsqlparser.schema.Column;


public class Evaluator{
	private ArrayList<PrimitiveValue> tuple;
	private HashMap<Column,Integer> ColumnToIndex;
	
	public Evaluator(ArrayList<ColumnWithType> column){
		ColumnToIndex = new HashMap<Column,Integer>();
		for(int i = 0;i != column.size();++i) {
			ColumnToIndex.put(column.get(i).getColumn(), i);
		}
	}

	public void setTuple(ArrayList<PrimitiveValue> tuple) {
		this.tuple = tuple;
	}
	public boolean evaluate(Expression condition) throws InvalidPrimitive{
		Eval eval = new Eval(){
			@Override
			public PrimitiveValue eval(Column arg0) throws SQLException {
				return tuple.get(ColumnToIndex.get(arg0));
			}
		};
		PrimitiveValue res = null;
		try {
			res = eval.eval(condition);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		boolean result = res.toBool();
		return result;
	}
	
	public PrimitiveValue calculate(Expression condition){
		Eval eval = new Eval(){
			@Override
			public PrimitiveValue eval(Column arg0) throws SQLException {
				return tuple.get(ColumnToIndex.get(arg0));
			}
		};
		PrimitiveValue res = null;
		try {
			res = eval.eval(condition);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;
	}

}
