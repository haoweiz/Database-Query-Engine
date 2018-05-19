package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

public class ProjectionPushDown {
	private HashSet<Column> column;
	private ArrayList<ColumnWithType> total_column;
	private String table_name;
	public ProjectionPushDown(ArrayList<ColumnWithType> total_column,List<SelectItem> selectitem,Expression where,List<Column> groupby,String table_name){
		this.total_column = total_column;
		this.table_name = table_name;
		
		column = new HashSet<Column>();
		if(selectitem != null)
		    column.addAll(this.GetSelectItem(selectitem));
		if(where != null)
		    column.addAll(this.GetWhere(where));
		if(groupby != null)
		    column.addAll(this.GetGroupBy(groupby));
	}
	private ArrayList<Column> Seperate(Expression e){
		ArrayList<Column> res = new ArrayList<Column>();
		if(e instanceof Column){
			res.add((Column)e);
			return res;
		}
		if(e instanceof BinaryExpression){
			ArrayList<Column> left = Seperate(((BinaryExpression) e).getLeftExpression());
			ArrayList<Column> right = Seperate(((BinaryExpression) e).getRightExpression());
			res.addAll(left);
			res.addAll(right);
			return res;
		}
		return res;
	}

	private HashSet<Column> GetSelectItem(List<SelectItem> selectitem){
		HashSet<Column> res = new HashSet<Column>();
		for(int index = 0;index != selectitem.size();++index){
			SelectItem si = selectitem.get(index);
			if (si instanceof SelectExpressionItem) {
				SelectExpressionItem sei = (SelectExpressionItem) selectitem.get(index);
				String alias = sei.getAlias();
				Expression e = sei.getExpression();
				if(alias == null){
					if(e instanceof Function){
						Expression exp = ((Function) e).getParameters().getExpressions().get(0);
						res.addAll(this.Seperate(exp));
					}
					else{
						res.add((Column) e);
					}
				}
				else{
					if(e instanceof Function){
						Expression exp = ((Function) e).getParameters().getExpressions().get(0);
						res.addAll(this.Seperate(exp));
					}
					else{
						res.addAll(this.Seperate(e));
					}
				}
			} else if (si instanceof AllColumns) {
				for(int i = 0;i != total_column.size();++i)
					res.add(total_column.get(i).getColumn());
			} else if (si instanceof AllTableColumns) {
				if(((AllTableColumns)si).getTable().getName().equals(table_name)){
					for(int i = 0;i != total_column.size();++i)
						res.add(total_column.get(i).getColumn());
				}
			}
		}
		return res;
	}
	private HashSet<Column> GetWhere(Expression where){
		HashSet<Column> res = new HashSet<Column>();
		res.addAll(this.Seperate(where));
		return res;
	}
	private HashSet<Column> GetGroupBy(List<Column> groupby){
		HashSet<Column> res = new HashSet<Column>();
		res.addAll(groupby);
		return res;
	}
	public HashSet<Column> getColumn(){
		return column;
	}
}
