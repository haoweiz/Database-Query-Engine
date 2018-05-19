package edu.buffalo.www.cse4562;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;



public class FromScanner implements Operator {
	private BufferedReader input = null;
	private HashSet<Column> need_column;
	private HashSet<Integer> index_of_need_column;
	private List<ColumnDefinition> columndefinition;
	
	private ArrayList<ColumnWithType> column;
	private Operator source;
	
	public FromScanner(HashMap<String,File> dataDir,HashMap<String,CreateTable> tables,String table_name,String table_alias,Operator child,HashSet<Column> need_column){
		this.need_column = need_column;
		column = new ArrayList<ColumnWithType>();
		index_of_need_column = new HashSet<Integer>();
		setSchema(dataDir,tables,table_name,table_alias,child);
		if(source == null)
			columndefinition = tables.get(table_name).getColumnDefinitions();
	}
	
	private void setSchema(HashMap<String,File> dataDir,HashMap<String,CreateTable> tables,String table_name,String table_alias,Operator child){
		if(child == null){
			CreateTable t = tables.get(table_name);
			List<ColumnDefinition> cd = t.getColumnDefinitions();
			for(int i = 0;i != cd.size();++i){
				Table tab = new Table();
				if(table_alias == null)
					tab.setName(table_name);
				else
					tab.setName(table_alias);
				Column col = new Column();
				col.setColumnName(cd.get(i).getColumnName());
				col.setTable(tab);
				if(need_column.contains(col)){
				    column.add(new ColumnWithType(cd.get(i).getColDataType(),col));
				    index_of_need_column.add(i);
				}
			}
			
			File f = dataDir.get(table_name);
			FileReader fd = null;
			try {
				fd = new FileReader(f);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			input = new BufferedReader(fd);
			source = null;
		}
		else{
			source = child;
			ArrayList<ColumnWithType> child_cwt = child.getColumnWithType();
			for(int i = 0;i != child_cwt.size();++i){
				Table tab = new Table();
                tab.setName(table_alias);
				Column col = new Column();
				col.setColumnName(child_cwt.get(i).getColumn().getColumnName());
				col.setTable(tab);
				ColumnWithType t = new ColumnWithType(child_cwt.get(i).getType(),col);
				if(need_column.contains(col)){
				    column.add(t);
				    index_of_need_column.add(i);
				}
			}
		}
	}
	
	@Override
	public ArrayList<PrimitiveValue> readOneTuple() {
		ArrayList<PrimitiveValue> res;
		if(source == null){
			String line = null;
			res = new ArrayList<PrimitiveValue>();
			try {
				line = input.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(line == null) return null;
			String[] cols = line.split("\\|");

			for (int i = 0; i != columndefinition.size(); ++i) {
				if(!index_of_need_column.contains(i))
					continue;
				
				String type = columndefinition.get(i).getColDataType().getDataType();
				if (type.equals("INT") || type.equals("INTEGER")
						|| type.equals("LONG"))
					res.add(new LongValue(cols[i]));
				else if (type.equals("STRING") || type.equals("VARCHAR")
						|| type.equals("CHAR")) {
					res.add(new StringValue(cols[i]));
				} else if (type.equals("DATE")) {
					res.add(new DateValue(cols[i]));
				} else if (type.equals("DECIMAL") || type.equals("DOUBLE")) {
					res.add(new DoubleValue(cols[i]));
				}
			}
		}
		else{
			ArrayList<PrimitiveValue> tuple = source.readOneTuple();
			if(tuple == null) return null;
			res = new ArrayList<PrimitiveValue>();
			for(int i = 0;i != tuple.size();++i){
				if(index_of_need_column.contains(i)){
					res.add(tuple.get(i));
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
