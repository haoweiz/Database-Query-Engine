package edu.buffalo.www.cse4562;

import java.util.ArrayList;
import java.util.Iterator;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.schema.Column;

public class Optimizer {
	private Operator input;
	ArrayList<Expression> expression;
	public Optimizer(Operator input,Expression exp){
		this.input = input;
		expression = this.Seperate(exp);
	}
	public ArrayList<Expression> Seperate(Expression e){
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
	public Operator Optimize(){
		Iterator<Expression> iter = expression.iterator();
		Expression local;
		while(iter.hasNext()){
			local = iter.next();
			if(local instanceof BinaryExpression){
				Column c = null;
				if(((BinaryExpression) local).getLeftExpression() instanceof Column && !(((BinaryExpression) local).getRightExpression() instanceof Column)){
					c = (Column)((BinaryExpression) local).getLeftExpression();

				}
				else if(((BinaryExpression) local).getRightExpression() instanceof Column && !(((BinaryExpression) local).getLeftExpression() instanceof Column)){
					c = (Column)((BinaryExpression) local).getRightExpression();
				}
				if(c == null)
					continue;
				ArrayList<ColumnWithType> cwt = input.getColumnWithType();
				for(int i = 0;i != cwt.size();++i){
					if(cwt.get(i).getColumn().getColumnName().equals(c.getWholeColumnName()) ||
							cwt.get(i).getColumn().getWholeColumnName().equals(c.getWholeColumnName())){
						input = new SelectionOperator(input,local);
						iter.remove();
						break;
					}
				}
			}
		}
		return input;
	}
	public Expression getExpression(){
		Expression result = null;
		for(Expression e : expression){
			if(result == null){
				result=e;
			}
			else{
				result=new AndExpression(result,e);
			}
		}
		return result;
	}
}
