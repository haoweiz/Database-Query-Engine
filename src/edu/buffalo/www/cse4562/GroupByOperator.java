package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.expression.PrimitiveValue.InvalidPrimitive;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

public class GroupByOperator implements Operator{
	private ArrayList<ColumnWithType> oldColumn;
	private ArrayList<ColumnWithType> column;
	private List<Column> GroupByColumnReference;
	private List<SelectItem> selectitem;
	private Operator input;
	private Evaluator evaluator;
	
	private ArrayList<Expression> KeyValue;
	private ArrayList<Integer> index;
	private HashMap<ArrayList<PrimitiveValue>,ArrayList<PrimitiveValue>> aggregate;
	private static Iterator<?> iter;

	public GroupByOperator(Operator input,List<Column> gbcr, List<SelectItem> si) {
		this.oldColumn = input.getColumnWithType();
		this.evaluator = new Evaluator(oldColumn);
		this.input = input;
		GroupByColumnReference = gbcr;
		selectitem = si;
		column = new ArrayList<ColumnWithType>();
		KeyValue = new ArrayList<Expression>();
		index = new ArrayList<Integer>();
		if(GroupByColumnReference != null)
		    KeyValue.addAll(GroupByColumnReference);
		aggregate = new HashMap<ArrayList<PrimitiveValue>,ArrayList<PrimitiveValue>>();

		setColumn();
		setAggregate();
		setIndex();
		iter = aggregate.entrySet().iterator();
	}

	private void setIndex(){
		for(int i = 0;i != selectitem.size();++i){
			for(int j = 0;j != KeyValue.size();++j){
				String ColumnName = ((SelectExpressionItem) selectitem.get(i)).getExpression().toString();
				String KeyValueName = (KeyValue.get(j)).toString();
				if(ColumnName.equals(KeyValueName)){
					index.add(j);
					break;
				}
			}
		}
	}
	
	private void setColumn(){
		for (int index = 0; index != selectitem.size(); ++index) {
			SelectItem si = selectitem.get(index);
			if (si instanceof SelectExpressionItem) {
				SelectExpressionItem sei = (SelectExpressionItem) selectitem.get(index);
				String alias = sei.getAlias();
				Expression e = sei.getExpression();
				
				if(alias == null){
					if(e instanceof Function){
						Table tab = new Table();
						Column col = new Column();
						col.setColumnName(e.toString());
						col.setTable(tab);
						ColDataType cdt = new ColDataType();
						if(((Function) e).getName().equals("COUNT"))
							cdt.setDataType("INTEGER");
						else
							cdt.setDataType("DOUBLE");
						column.add(new ColumnWithType(cdt,col));
						KeyValue.add(e);
					}
					else{
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
				}
				else{
					if(e instanceof Function){
						Table tab = new Table();
						Column col = new Column();
						col.setColumnName(alias);
						col.setTable(tab);
						ColDataType cdt = new ColDataType();
						if(((Function) e).getName().equals("COUNT"))
							cdt.setDataType("INTEGER");
						else
							cdt.setDataType("DOUBLE");
						column.add(new ColumnWithType(cdt,col));
						KeyValue.add(e);
					}
					else{
						Table tab = new Table();
						Column col = new Column();
						col.setColumnName(alias);
						col.setTable(tab);
						ColDataType cdt = new ColDataType();
						column.add(new ColumnWithType(cdt,col));
					}
				}
				
			} else if (si instanceof AllColumns) {
				
			} else if (si instanceof AllTableColumns) {
				
			}
		}
	}
	private void setAggregate(){
		ArrayList<PrimitiveValue> tuple = input.readOneTuple();
		ArrayList<Integer> IndexOfGroupBy = new ArrayList<Integer>();
		if(GroupByColumnReference != null){
		    for(int i = 0;i != GroupByColumnReference.size();++i){
			    for(int j = 0;j != oldColumn.size();++j){
				    String gbcrWholeName = GroupByColumnReference.get(i).getWholeColumnName();
				    String oldWholeName = oldColumn.get(j).getColumn().getWholeColumnName();
				    String oldName = oldColumn.get(j).getColumn().getColumnName();
				    if(gbcrWholeName.equals(oldWholeName) || gbcrWholeName.equals(oldName)){
					    IndexOfGroupBy.add(new Integer(j));
				    }
			    }
		    }
		}
		while(tuple != null){
			ArrayList<PrimitiveValue> key = new ArrayList<PrimitiveValue>();
			for(int i = 0;i != IndexOfGroupBy.size();++i){
				key.add(tuple.get(IndexOfGroupBy.get(i)));
			}
			if(aggregate.containsKey(key)){
				ArrayList<PrimitiveValue> value = new ArrayList<PrimitiveValue>();
				int IndexOfAggregate = 0;
				boolean hasAVG = false;
				for(int j = 0;j != selectitem.size();++j){
					if(selectitem.get(j) instanceof SelectExpressionItem){
						Expression exp = ((SelectExpressionItem)selectitem.get(j)).getExpression();
						if(exp instanceof Function){
							if(((Function)exp).getName().equals("MIN")){
								PrimitiveValue OldValue = aggregate.get(key).get(IndexOfAggregate);
								IndexOfAggregate++;
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue res = evaluator.calculate(e);
								try {
									if(res.toDouble() < OldValue.toDouble())
										value.add(res);
									else
										value.add(OldValue);
								} catch (InvalidPrimitive e1) {
									e1.printStackTrace();
								}
							}
							else if(((Function)exp).getName().equals("MAX")){
								PrimitiveValue OldValue = aggregate.get(key).get(IndexOfAggregate);
								IndexOfAggregate++;
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue res = evaluator.calculate(e);
								try {
									if(res.toDouble() > OldValue.toDouble())
										value.add(res);
									else
										value.add(OldValue);
								} catch (InvalidPrimitive e1) {
									e1.printStackTrace();
								}
							}
							else if(((Function)exp).getName().equals("SUM")){
								PrimitiveValue OldValue = aggregate.get(key).get(IndexOfAggregate);
								IndexOfAggregate++;
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue res = evaluator.calculate(e);
								double d = 0;
								try {
									d = OldValue.toDouble()+res.toDouble();
								} catch (InvalidPrimitive e1) {
									e1.printStackTrace();
								}
								value.add(new DoubleValue(Double.toString(d)));
							}
							else if(((Function)exp).getName().equals("AVG")){
								PrimitiveValue OldValue = aggregate.get(key).get(IndexOfAggregate);
								IndexOfAggregate++;
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue res = evaluator.calculate(e);
								double d = 0;
								try {
									d = OldValue.toDouble()+res.toDouble();
								} catch (InvalidPrimitive e1) {
									e1.printStackTrace();
								}
								value.add(new DoubleValue(Double.toString(d)));
								hasAVG = true;
							}
							else if(((Function)exp).getName().equals("COUNT")){
								PrimitiveValue OldValue = aggregate.get(key).get(IndexOfAggregate);
								IndexOfAggregate++;
								long res = 0;
								try {
									res = OldValue.toLong() + 1;
								} catch (InvalidPrimitive e1) {
									e1.printStackTrace();
								}
								value.add(new LongValue(res));
							}
						}
					}
				}
				
				if(hasAVG){
					PrimitiveValue OldCount = aggregate.get(key).get(value.size());
					long NewCount = 0;
					try {
						NewCount = OldCount.toLong() + 1;
					} catch (InvalidPrimitive e1) {
						e1.printStackTrace();
					}
					value.add(new LongValue(NewCount));
				}
				aggregate.put(key, value);
			}
			else{
				ArrayList<PrimitiveValue> value = new ArrayList<PrimitiveValue>();
				boolean hasAVG = false;
				for(int j = 0;j != selectitem.size();++j){
					if(selectitem.get(j) instanceof SelectExpressionItem){
						Expression exp = ((SelectExpressionItem)selectitem.get(j)).getExpression();
						if(exp instanceof Function){
							if(((Function)exp).getName().equals("MIN")){
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue res = evaluator.calculate(e);
								value.add(res);
							}
							else if(((Function)exp).getName().equals("MAX")){
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue res = evaluator.calculate(e);
								value.add(res);
							}
							else if(((Function)exp).getName().equals("SUM")){
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue res = evaluator.calculate(e);
								try {
									value.add(new DoubleValue(res.toDouble()));
								} catch (InvalidPrimitive e1) {
									e1.printStackTrace();
								}
							}
							else if(((Function)exp).getName().equals("AVG")){
								Expression e = ((Function)exp).getParameters().getExpressions().get(0);
								evaluator.setTuple(tuple);
								PrimitiveValue sum = evaluator.calculate(e);
								try {
									value.add(new DoubleValue(sum.toDouble()));
								} catch (InvalidPrimitive e1) {
									e1.printStackTrace();
								}
								hasAVG = true;
							}
							else if(((Function)exp).getName().equals("COUNT")){
								PrimitiveValue res = new LongValue(1);
								value.add(res);
							}
						}
					}
				}
				if(hasAVG)
					value.add(new LongValue(1));
				aggregate.put(key, value);
			}
			tuple = input.readOneTuple();
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public ArrayList<PrimitiveValue> readOneTuple() {
		ArrayList<PrimitiveValue> tuple = new ArrayList<PrimitiveValue>();
		ArrayList<PrimitiveValue> result = new ArrayList<PrimitiveValue>();
		if(iter.hasNext()){
			Entry<?, ?> entry = (Entry<?, ?>)iter.next();
			tuple.addAll((ArrayList<PrimitiveValue>)entry.getKey());
			tuple.addAll((ArrayList<PrimitiveValue>)entry.getValue());
			for(int i = 0;i != index.size();++i){
				String KeyValueName = (KeyValue.get(i)).toString();
				if(KeyValueName.length() > 3 && KeyValueName.substring(0, 3).equals("AVG")){
					PrimitiveValue total = tuple.get(index.get(i));
					PrimitiveValue count = tuple.get(KeyValue.size());
					double res = 0;
					try {
						res = total.toDouble()/count.toDouble();
					} catch (InvalidPrimitive e1) {
						e1.printStackTrace();
					}
					result.add(new DoubleValue(Double.toString(res)));
				}
				else{
					result.add(tuple.get(index.get(i)));
				}
			}
			return result;
		}
		return null;
	}


	@Override
	public ArrayList<ColumnWithType> getColumnWithType() {
		// TODO Auto-generated method stub
		return column;
	}

}

