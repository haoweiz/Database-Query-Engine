package edu.buffalo.www.cse4562;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.create.table.ColDataType;

public class ColumnWithType {
	private ColDataType type;
	private Column column;
	
	public ColumnWithType(ColDataType t,Column c){
		this.type = t;
		this.column = c;
	}
	public ColDataType getType(){
		return type;
	}
	public Column getColumn(){
		return column;
	}
}
