package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;


public class ProjectOperator implements Operator{
	private ArrayList<ColumnWithType> oldColumn;
	private ArrayList<ColumnWithType> column;
	private Operator source;
	private List<SelectItem> selectitem;
	private boolean flag;
	private Evaluator evaluator;
	
	
	public ProjectOperator(Operator input,List<SelectItem> selectitem){
		this.source = input;
		this.selectitem = selectitem;
		oldColumn = input.getColumnWithType();
		evaluator = new Evaluator(oldColumn);
		column = new ArrayList<ColumnWithType>();
		this.setNewNameType();
		flag = false;
	}
	
	public void setNewNameType() {
		for (int index = 0; index != selectitem.size(); ++index) {
			SelectItem si = selectitem.get(index);
			if (si instanceof SelectExpressionItem) {
				SelectExpressionItem sei = (SelectExpressionItem) selectitem.get(index);
				String alias = sei.getAlias();
				Expression e = sei.getExpression();

				if (alias == null) {
					for (int j = 0; j != oldColumn.size(); ++j) {
						Column col = oldColumn.get(j).getColumn();
						if (col.getWholeColumnName().equals(e.toString()) || col.getColumnName().equals(e.toString())) {
							Table tab = new Table();
							tab.setName(oldColumn.get(j).getColumn().getTable().getName());
							Column c = new Column();
							c.setColumnName(oldColumn.get(j).getColumn().getColumnName());
							c.setTable(tab);
							ColDataType cdt = new ColDataType();
							cdt.setDataType(oldColumn.get(j).getType().getDataType());
							ColumnWithType t = new ColumnWithType(cdt, c);
							column.add(t);
							break;
						}
					}
				} 
				else {
					Table tab = new Table();
					Column col = new Column();
					col.setColumnName(alias);
					col.setTable(tab);
					ColDataType cdt = new ColDataType();
					column.add(new ColumnWithType(cdt,col));
				}
			} else if (si instanceof AllColumns) {
				for (int j = 0; j != oldColumn.size(); ++j) {
					Table tab = new Table();
					tab.setName(oldColumn.get(j).getColumn().getTable().getName());
					Column c = new Column();
					c.setColumnName(oldColumn.get(j).getColumn().getColumnName());
					c.setTable(tab);
					ColDataType cdt = new ColDataType();
					cdt.setDataType(oldColumn.get(j).getType().getDataType());
					ColumnWithType t = new ColumnWithType(cdt,c);
					column.add(t);
				}
			} else if (si instanceof AllTableColumns) {
				for (int j = 0; j != oldColumn.size(); ++j) {
					Table left = oldColumn.get(j).getColumn().getTable();
					Table right = ((AllTableColumns)si).getTable();
					if (left.getName().equals(right.getName())) {
						Table tab = new Table();
						tab.setName(oldColumn.get(j).getColumn().getTable().getName());
						Column c = new Column();
						c.setColumnName(oldColumn.get(j).getColumn().getColumnName());
						c.setTable(tab);
						ColDataType cdt = new ColDataType();
						cdt.setDataType(oldColumn.get(j).getType().getDataType());
						ColumnWithType t = new ColumnWithType(cdt,c);
						column.add(t);
					}
				}

			}
		}
	}
	

	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		ArrayList<PrimitiveValue> tuple = source.readOneTuple();
		if(tuple == null) {
			flag = false;
			return null;
		}
		ArrayList<PrimitiveValue> res = new ArrayList<PrimitiveValue>();
		
		for (int index = 0; index != selectitem.size(); ++index) {
			SelectItem si = selectitem.get(index);
			if (si instanceof SelectExpressionItem) {
				SelectExpressionItem sei = (SelectExpressionItem)selectitem.get(index);
				String alias = sei.getAlias();
				Expression e = sei.getExpression();
				
				if(alias == null){
					for(int j = 0;j != oldColumn.size();++j){
						Column col = oldColumn.get(j).getColumn();
						if(col.getWholeColumnName().equals(e.toString()) || col.getColumnName().equals(e.toString())){
							res.add(tuple.get(j));
						}
					}
				}
				else{
					evaluator.setTuple(tuple);
					PrimitiveValue value = evaluator.calculate(e);
					res.add(value);
					if(!flag){
						flag = true;
					    column.get(index).getType().setDataType(value.getType().toString());
					}
				}
			}
			else if (si instanceof AllColumns){
				res.addAll(tuple);
			}
			else if (si instanceof AllTableColumns){
				for(int j = 0;j != oldColumn.size();++j){
					Table left = oldColumn.get(j).getColumn().getTable();
					Table right = ((AllTableColumns)si).getTable();
					if (left.getName().equals(right.getName())) {
						res.add(tuple.get(j));
					}
				}
			}
		}
		
		return res;
	}

	@Override
	public ArrayList<ColumnWithType> getColumnWithType() {
		// TODO Auto-generated method stub
		return column;
	}

}
