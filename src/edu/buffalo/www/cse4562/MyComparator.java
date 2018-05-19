package edu.buffalo.www.cse4562;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.sf.jsqlparser.eval.Eval;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.expression.PrimitiveValue.InvalidPrimitive;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;

public class MyComparator implements Comparator<ArrayList<PrimitiveValue>>{
	private List<OrderByElement> order;
	private ArrayList<ColumnWithType> column;
	
	public MyComparator(List<OrderByElement> order,ArrayList<ColumnWithType> column){
		this.order = order;
		this.column = column;
	}
	@Override
	public int compare(ArrayList<PrimitiveValue> l1, ArrayList<PrimitiveValue> l2) {
		for(int i = 0;i != order.size();++i){
			boolean isasc = order.get(i).isAsc();
			int index = 0;
			for(int j = 0;j != column.size();++j){
				String o = order.get(i).getExpression().toString();
				String cn = column.get(j).getColumn().getColumnName();
				String whole_cn = column.get(j).getColumn().getWholeColumnName();
				if(o.equals(whole_cn) || o.equals(cn)){
					index = j;
					break;
				}
			}
			String type = column.get(index).getType().getDataType();
			int return_val = 0;
			
			if(type.equals("INT") || type.equals("INTEGER") || type.equals("DECIMAL") || type.equals("DOUBLE") || type.equals("LONG")){
				if(isasc){
					try {
						if(l1.get(index).toDouble() > l2.get(index).toDouble())
							return_val = 1;
						else if(l1.get(index).toDouble() < l2.get(index).toDouble())
							return_val = -1;
						else if(l1.get(index).toDouble() == l2.get(index).toDouble())
							continue;
					} catch (InvalidPrimitive e) {
						e.printStackTrace();
					}
				}
				else{
					try {
						if(l1.get(index).toDouble() > l2.get(index).toDouble())
							return_val = -1;
						else if(l1.get(index).toDouble() < l2.get(index).toDouble())
							return_val = 1;
						else if(l1.get(index).toDouble() == l2.get(index).toDouble())
							continue;
					} catch (InvalidPrimitive e) {
						e.printStackTrace();
					}
				}
			}
			else if(type.equals("STRING")|| type.equals("VARCHAR")|| type.equals("CHAR")){
				PrimitiveValue pv1 = l1.get(index);
				PrimitiveValue pv2 = l2.get(index);
				if(isasc){
					if(pv1.toRawString().compareTo(pv2.toRawString()) > 0)
						return_val = 1;
					else if(pv1.toRawString().compareTo(pv2.toRawString()) < 0)
						return_val = -1;
					else if(pv1.toRawString().compareTo(pv2.toRawString()) == 0)
						continue;
				}
				else{
					if(pv1.toRawString().compareTo(pv2.toRawString()) > 0)
						return_val = -1;
					else if(pv1.toRawString().compareTo(pv2.toRawString()) < 0)
						return_val = 1;
					else if(pv1.toRawString().compareTo(pv2.toRawString()) == 0)
						continue;
				}
			}
			else if(type.equals("DATE")){
				PrimitiveValue pv1 = l1.get(index);
				PrimitiveValue pv2 = l2.get(index);
				Expression exp = new MinorThan(pv1,pv2);
				Expression exp2 = new GreaterThan(pv1,pv2);
				Eval eval = new Eval(){
					@Override
					public PrimitiveValue eval(Column arg0) throws SQLException {
						return new LongValue(0);
					}		
				};
				PrimitiveValue ret = null;
				PrimitiveValue ret2 = null;
				boolean r = false;
				boolean r2 = false;
				try {
					ret = eval.eval(exp);
					ret2 = eval.eval(exp2);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				try {
					r = ret.toBool();
					r2 = ret2.toBool();
				} catch (InvalidPrimitive e) {
					e.printStackTrace();
				}
				
				
				if(isasc){
					if(!r && r2)
						return_val = 1;
					else if(r && !r2)
						return_val = -1;
					else if(!r && !r2){
						continue;
					}
				}
				else{
					if(!r && r2)
						return_val = -1;
					else if(r && !r2)
						return_val = 1;
					else if(!r && !r2)
						continue;
				}
			}
			return return_val;
		}
		return 0;
	}
}

