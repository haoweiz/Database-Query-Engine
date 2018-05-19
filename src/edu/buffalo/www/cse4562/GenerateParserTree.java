package edu.buffalo.www.cse4562;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;

class EXP{
	public EXP(Expression others,Expression equalsto){
		this.others = others;
		this.equalsto = equalsto;
	}
	public Expression getExpression(){
		return others;
	}
	public Expression getEqualsTo(){
		return equalsto;
	}
	private Expression others;
	private Expression equalsto;
};

public class GenerateParserTree {
	private HashMap<String,File> dataDir;
	private HashMap<String,CreateTable> tables;
	public GenerateParserTree(HashMap<String,File> dataDir,HashMap<String,CreateTable> tables) {
		this.dataDir = dataDir;
		this.tables = tables;
	}
	
	public Operator generateParsertree(PlainSelect pselect){
		Operator parser = null;
		if (pselect.getFromItem() instanceof Table) {
			String table_name = ((Table)pselect.getFromItem()).getName();
			String table_alias = pselect.getFromItem().getAlias();
			ProjectionPushDown ppd = new ProjectionPushDown(setTotalColumn(tables,table_name,table_alias),pselect.getSelectItems(),pselect.getWhere(),pselect.getGroupByColumnReferences(),table_alias == null ? table_name : table_alias);
			parser = new FromScanner(dataDir,tables,table_name,table_alias,null,ppd.getColumn());
			
			Optimizer opt = new Optimizer(parser,pselect.getWhere());
			parser = opt.Optimize();
			
			Expression project = opt.getExpression();
			
			if(pselect.getJoins() != null){
				List<Join> joins = pselect.getJoins();
				Iterator<Join> iter = joins.iterator();
				while(iter.hasNext()){
					Join j = (Join) iter.next();
					Operator subparser = null;
					if(j.getRightItem() instanceof Table){
						String join_name = ((Table)j.getRightItem()).getName();
						String join_alias = ((Table)j.getRightItem()).getAlias();
						
						ProjectionPushDown jppd = new ProjectionPushDown(setTotalColumn(tables,join_name,join_alias),pselect.getSelectItems(),pselect.getWhere(),pselect.getGroupByColumnReferences(),join_alias == null ? join_name : join_alias);
						subparser = new FromScanner(dataDir,tables,join_name,join_alias,null,jppd.getColumn());
						opt = new Optimizer(subparser,project);
						subparser = opt.Optimize();
						EXP expr = CanHashJoin(project = opt.getExpression(),subparser.getColumnWithType(),parser.getColumnWithType());
						if(expr.getEqualsTo() != null){
							project = expr.getExpression();
							if(expr.getEqualsTo() instanceof AndExpression)
							    parser = new HashJoinOperator(expr.getEqualsTo(),parser,subparser);
							else
								parser = new OneExpHashJoinOperator(expr.getEqualsTo(),parser,subparser);
						}
						else
						    parser = new JoinOperator(parser,subparser);
					}
					else if(j.getRightItem() instanceof SubSelect){
						String join_alias = j.getRightItem().getAlias();
						SelectBody b = ((SubSelect)j.getRightItem()).getSelectBody();
						subparser = generateParsertree((PlainSelect)(b));
						ProjectionPushDown jppd = new ProjectionPushDown(subparser.getColumnWithType(),pselect.getSelectItems(),pselect.getWhere(),pselect.getGroupByColumnReferences(),join_alias);
						subparser = new FromScanner(dataDir,tables,null,join_alias,subparser,jppd.getColumn());
						opt = new Optimizer(subparser,project);
						subparser = opt.Optimize();
						EXP expr = CanHashJoin(project = opt.getExpression(),subparser.getColumnWithType(),parser.getColumnWithType());
						if(expr.getEqualsTo() != null){
							project = expr.getExpression();
							if(expr.getEqualsTo() instanceof AndExpression)
							    parser = new HashJoinOperator(expr.getEqualsTo(),parser,subparser);
							else
								parser = new OneExpHashJoinOperator(expr.getEqualsTo(),parser,subparser);
						}
						else
						    parser = new JoinOperator(parser,subparser);
					}
					
					if(j.getOnExpression() != null){
						
					}
				}
			}
			
			if(project != null){
				parser = new SelectionOperator(parser,project);
			}

			if(pselect.getGroupByColumnReferences() != null || hasAggregate(pselect.getSelectItems())){
		        parser = new GroupByOperator(parser,pselect.getGroupByColumnReferences(),pselect.getSelectItems());
			}
			else{
			    parser = new ProjectOperator(parser,pselect.getSelectItems());
			}
			
			if(pselect.getOrderByElements() != null){
				parser = new OrderbyOperator(parser,pselect.getOrderByElements());
			}
			
			if(pselect.getLimit() != null){
				parser = new LimitOperator(parser,pselect.getLimit());
			}
			
		}
		else if(pselect.getFromItem() instanceof SubSelect){
			FromItem fi = pselect.getFromItem();
			SelectBody body = ((SubSelect) fi).getSelectBody();
			String table_alias = fi.getAlias();
			if(body instanceof PlainSelect){
				parser = generateParsertree((PlainSelect)body);
				ProjectionPushDown ppd = new ProjectionPushDown(parser.getColumnWithType(),pselect.getSelectItems(),pselect.getWhere(),pselect.getGroupByColumnReferences(),table_alias);
				parser = new FromScanner(dataDir,tables,null,table_alias,parser,ppd.getColumn());
				
				Optimizer opt = new Optimizer(parser,pselect.getWhere());
				parser = opt.Optimize();
				
				Expression project = opt.getExpression();
				
				if(pselect.getJoins() != null){
					List<Join> joins = pselect.getJoins();
					Iterator<Join> iter = joins.iterator();
					while (iter.hasNext()) {
						Join j = (Join) iter.next();
						Operator subparser = null;
						if (j.getRightItem() instanceof Table) {
							String join_name = ((Table) j.getRightItem()).getName();
							String join_alias = ((Table) j.getRightItem()).getAlias();
							ProjectionPushDown jppd = new ProjectionPushDown(setTotalColumn(tables,join_name,join_alias),pselect.getSelectItems(),pselect.getWhere(),pselect.getGroupByColumnReferences(),join_alias == null ? join_name : join_alias);
							subparser = new FromScanner(dataDir, tables,join_name, join_alias, null,jppd.getColumn());
							opt = new Optimizer(subparser,project);
							subparser = opt.Optimize();
							EXP expr = CanHashJoin(project = opt.getExpression(),subparser.getColumnWithType(),parser.getColumnWithType());
							if(expr.getEqualsTo() != null){
								project = expr.getExpression();
								if(expr.getEqualsTo() instanceof AndExpression)
								    parser = new HashJoinOperator(expr.getEqualsTo(),parser,subparser);
								else
									parser = new OneExpHashJoinOperator(expr.getEqualsTo(),parser,subparser);
							}
							else
							    parser = new JoinOperator(parser, subparser);
						} else if (j.getRightItem() instanceof SubSelect) {
							String join_alias = j.getRightItem().getAlias();
							SelectBody b = ((SubSelect) j.getRightItem()).getSelectBody();
							subparser = generateParsertree((PlainSelect) (b));
							ProjectionPushDown jppd = new ProjectionPushDown(subparser.getColumnWithType(),pselect.getSelectItems(),pselect.getWhere(),pselect.getGroupByColumnReferences(),join_alias);
							subparser = new FromScanner(dataDir, tables, null,join_alias, subparser,jppd.getColumn());
							opt = new Optimizer(subparser,project);
							subparser = opt.Optimize();
							EXP expr = CanHashJoin(project = opt.getExpression(),subparser.getColumnWithType(),parser.getColumnWithType());
							if(expr.getEqualsTo() != null){
								project = expr.getExpression();
								if(expr.getEqualsTo() instanceof AndExpression)
								    parser = new HashJoinOperator(expr.getEqualsTo(),parser,subparser);
								else
									parser = new OneExpHashJoinOperator(expr.getEqualsTo(),parser,subparser);
							}
							else
							    parser = new JoinOperator(parser, subparser);
						}

						if (j.getOnExpression() != null) {

						}
					}
				}
				
				
				if(project != null){
					parser = new SelectionOperator(parser,project);
				}

				if(pselect.getGroupByColumnReferences() != null || hasAggregate(pselect.getSelectItems())){
			        parser = new GroupByOperator(parser,pselect.getGroupByColumnReferences(),pselect.getSelectItems());
				}
				else{
				    parser = new ProjectOperator(parser,pselect.getSelectItems());
				}
				
				if(pselect.getOrderByElements() != null){
					parser = new OrderbyOperator(parser,pselect.getOrderByElements());
				}
				
				if(pselect.getLimit() != null){
					parser = new LimitOperator(parser,pselect.getLimit());
				}
			}
			else if(body instanceof Union){
				
			}
		}
		return parser;
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
	
	
	private ArrayList<ColumnWithType> setTotalColumn(HashMap<String,CreateTable> tables,String table_name,String table_alias){
		CreateTable t = tables.get(table_name);
		List<ColumnDefinition> cd = t.getColumnDefinitions();
		ArrayList<ColumnWithType> total_column = new ArrayList<ColumnWithType>();
		for(ColumnDefinition c : cd){
			Table tab = new Table();
			if(table_alias == null)
				tab.setName(table_name);
			else
				tab.setName(table_alias);
			Column col = new Column();
			col.setColumnName(c.getColumnName());
			col.setTable(tab);
			total_column.add(new ColumnWithType(c.getColDataType(),col));
		}
		return total_column;
	}

	
	private EXP CanHashJoin(Expression expression,ArrayList<ColumnWithType> left,ArrayList<ColumnWithType> right) {
		ArrayList<Expression> AndExp = Seperate(expression);
		Expression HashExpression = null;
		Expression others = null;
		for(int i = 0;i != AndExp.size();++i){
			if(AndExp.get(i) instanceof EqualsTo){
				EqualsTo et = (EqualsTo)AndExp.get(i);
				if(et.getLeftExpression() instanceof Column && et.getRightExpression() instanceof Column){
					Column l = (Column) et.getLeftExpression();
					Column r = (Column) et.getRightExpression();
					boolean flagl = false;
					boolean flagr = false;
					for(int j = 0;j != left.size();++j){
						if(l.equals(left.get(j).getColumn()) || r.equals(left.get(j).getColumn())){
							flagl = true;
							break;
						}
					}
					for(int k = 0;k != right.size();++k){
						if(l.equals(right.get(k).getColumn()) || r.equals(right.get(k).getColumn())){
							flagr = true;
							break;
						}
					}
					if(flagl && flagr) {
						if(HashExpression == null)
						    HashExpression = et;
						else
							HashExpression = new AndExpression(HashExpression,et);
					}
					else{
						if(others == null)
							others = AndExp.get(i);
						else
						    others = new AndExpression(others,AndExp.get(i));
					}
				}
				else{
					if(others == null)
						others = AndExp.get(i);
					else
					    others = new AndExpression(others,AndExp.get(i));
				}
			}
			else{
				if(others == null)
					others = AndExp.get(i);
				else
				    others = new AndExpression(others,AndExp.get(i));
			}
		}
		EXP exp = new EXP(others,HashExpression);
		return exp;
	}
	
	private boolean hasAggregate(List<SelectItem> si){
		for(int i = 0;i != si.size();++i){
			SelectItem s = si.get(i);
			if(s instanceof SelectExpressionItem){
				if(((SelectExpressionItem) s).getExpression() instanceof Function)
					return true;
			}
		}
		return false;
	}
}
