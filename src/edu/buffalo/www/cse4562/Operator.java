package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import net.sf.jsqlparser.expression.PrimitiveValue;


public interface Operator {
	public ArrayList<PrimitiveValue> readOneTuple();
	public ArrayList<ColumnWithType> getColumnWithType();
}
