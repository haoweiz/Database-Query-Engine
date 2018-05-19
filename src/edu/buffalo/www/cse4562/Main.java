package edu.buffalo.www.cse4562;

import java.io.File;
import java.util.HashMap;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.Union;


public class Main {
	private static HashMap<String,File> dataDir = new HashMap<String,File>();
	private static HashMap<String,CreateTable> tables = new HashMap<String,CreateTable>();
	static String prompt = "$> ";
	
	public static void main(String[] args) throws ParseException{
		System.out.println(prompt);
        System.out.flush();
		CCJSqlParser parser = new CCJSqlParser(System.in);
		Statement statement = parser.Statement();
		while(statement != null){
			//long startTime=System.currentTimeMillis(); 
			if(statement instanceof CreateTable){
				CreateTable ct = (CreateTable) statement;
				tables.put(ct.getTable().getName(), ct);
				String name = ct.getTable().getName();
				File f = new File("data/"+ct.getTable().getName()+".dat");
				dataDir.put(name, f);
			}else if(statement instanceof Select){
				GenerateParserTree generateparsertree = new GenerateParserTree(dataDir,tables);
				Operator parse = null;
				SelectBody select = ((Select) statement).getSelectBody();
				if(select instanceof PlainSelect){
					PlainSelect pselect = (PlainSelect) select;
					parse = generateparsertree.generateParsertree(pselect);
				}else if(select instanceof Union){
					
				}
				if(parse != null){
					PrintOperator.Print(parse);
				}
			}
            //long endTime=System.currentTimeMillis();
            //System.out.println("Time = " + (endTime-startTime));
			System.out.println(prompt);
            System.out.flush();
			statement = parser.Statement();
		}
	}
}
