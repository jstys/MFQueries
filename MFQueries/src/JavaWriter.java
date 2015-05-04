/************************************
 * Author: Jim Stys
 * Date: 4/7/2015
 * Class: JavaWriter
 * 
 * Description:  
 * This class is responsible for taking
 * the contents of the parsed input and, 
 * using the logic necessary to execute EMF
 * queries, output JAVA source that will return
 * the results of arbitary EMF queries.
 ************************************/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.javatuples.Pair;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JVar;


public class JavaWriter {
	//Class names
	private static final String RUNNER_CLASS = "MFQueryExecutor";
	private static final String STRUCT_CLASS = "MFStruct";
	
	//SQL data type strings
	private static final String DBTYPE_CHARACTER = "character";
	private static final String DBTYPE_VARCHAR = "character varying";
	private static final String DBTYPE_INTEGER = "integer";
	
	//SQL aggregate function strings
	private static final String MAX_FUNCTION = "max";
	private static final String MIN_FUNCTION = "min";
	private static final String AVG_FUNCTION = "avg";
	private static final String SUM_FUNCTION = "sum";
	private static final String COUNT_FUNCTION = "count";
	
	//Positions of data in aggregate variable names i.e. max_quant_1
	private static final int AGGREGATE_FUNCTION = 0;
	private static final int AGGREGATE_COLUMN = 1;
	private static final int AGGREGATE_INDEX = 2;
	
	private static final int MF_NAME = 0;
	private static final int MF_TYPE = 1;
	private static final int MF_POS = 2;
	
	private InputParser parsed;
	private DBConnector dbconnector;
	private String[][] mfdata;
	private JCodeModel codeModel;
	private JDefinedClass runnerClass;
	private JDefinedClass mfstructClass;
	private String mainTemplate;
	private String connectTemplate;
	private String retrieveTemplate1;
	private String retrieveTemplate2;
	private String groupVarClassTemplate;
	private String initStructTemplate;
	private String mainFunctionsTemplate;
	private String scan0Template;
	
	public JavaWriter(InputParser parsed)
	{
		this.parsed = parsed;
		this.mainTemplate = "";
		this.connectTemplate = "";
		this.retrieveTemplate1 = "";
		this.retrieveTemplate2 = "";
		
		dbconnector = new DBConnector();
		dbconnector.connect();
		dbconnector.retreive();
		getMFData();
		parseTemplates();
	}
	
	private void parseTemplates()
	{
		Scanner input;
		try {
			input = new Scanner(new File("templates.txt"));
			String line = input.nextLine();
			while(!line.equals("END_TEMPLATE"))
			{
				switch(line)
				{
					case "MAIN_START":
						mainTemplate = readTemplate("MAIN_END", input);
					case "CONNECT_START":
						input.nextLine();
						connectTemplate = readTemplate("CONNECT_END", input);
					case "RETRIEVE_START1":
						input.nextLine();
						retrieveTemplate1 = readTemplate("RETRIEVE_END", input);
					case "RETRIEVE_START2":
						input.nextLine();
						retrieveTemplate2 = readTemplate("RETRIEVE_END", input);
					case "GROUPVARCLASS_START":
						input.nextLine();
						groupVarClassTemplate = readTemplate("GROUPVARCLASS_END", input);
					case "INITSTRUCT_START":
						input.nextLine();
						initStructTemplate = readTemplate("INITSTRUCT_END", input);
					case "GROUPVARFCNS_START":
						input.nextLine();
						mainFunctionsTemplate = readTemplate("GROUPVARFCNS_END", input);
					case "SCAN0_START":
						input.nextLine();
						scan0Template = readTemplate("SCAN0_END", input);
						
				}
				line = input.nextLine();
			}
			input.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	private String readTemplate(String done, Scanner input)
	{
		String dest = "";
		String line = input.nextLine();
		while(!line.equals(done))
		{
			dest += line + "\n";
			line = input.nextLine();
		}
		return dest;
	}
	
	private void getMFData()
	{
		this.mfdata = new String[parsed.getV().length][2];
		for(int i = 0; i < parsed.getV().length; i++)
		{
			String name = parsed.getV()[i];
			String type = dbconnector.columndata.get(name).getValue0();
			
			this.mfdata[i][MF_NAME] = name;
			this.mfdata[i][MF_TYPE] = type;
		}
	}
	
	public void generateJavaSource()
	{
		codeModel = new JCodeModel();

		try {
		    generateRunnerClass();
		    codeModel.build(new File("."));
		}catch (IOException ex) {
			  // report
		}catch (JClassAlreadyExistsException e) {
			   // ...
		}catch (ClassNotFoundException e) {
			//...
		}finally {
		}
		  
	}
	
	private void generateRunnerClass() throws IOException, JClassAlreadyExistsException, ClassNotFoundException
	{
		runnerClass = codeModel._class(RUNNER_CLASS);
		
		JClass Hashmap = codeModel.ref(java.util.HashMap.class).narrow(String.class).narrow(codeModel.parseType("MFStruct"));
		JClass AtomInt = codeModel.ref(java.util.concurrent.atomic.AtomicInteger.class);
		JClass Map = codeModel.ref(java.util.Map.class);
		runnerClass.field(JMod.PRIVATE, AtomInt, "test");
		runnerClass.field(JMod.PRIVATE, Hashmap, "mftable");
		runnerClass.field(JMod.PRIVATE, Map, "maptest");
		runnerClass.field(JMod.PRIVATE+JMod.STATIC+JMod.FINAL, codeModel.parseType("String"), "usr").init(JExpr.lit("postgres"));
		runnerClass.field(JMod.PRIVATE+JMod.STATIC+JMod.FINAL, codeModel.parseType("String"), "pwd").init(JExpr.lit("anycolor92"));
		runnerClass.field(JMod.PRIVATE+JMod.STATIC+JMod.FINAL, codeModel.parseType("String"), "url").init(JExpr.lit("jdbc:postgresql://localhost:5432/postgres"));
		
		JMethod construct = runnerClass.constructor(JMod.PUBLIC);
		construct.body().directStatement("mftable = new HashMap<String,MFStruct>();");
		
		JMethod main = runnerClass.method(JMod.PUBLIC+JMod.STATIC, void.class, "main");
		main.param(String[].class, "args");
		main.body().directStatement(mainTemplate);
		
		JMethod connect = runnerClass.method(JMod.PRIVATE, void.class, "connect");
		connect.body().directStatement(connectTemplate);
		
		generateRetrieveFunction();
		runnerClass.direct(mainFunctionsTemplate);
		runnerClass.direct(groupVarClassTemplate);
		generateStructClass();
	}
	
	private void generateRetrieveFunction()
	{
		//Generate import statements for external references
		JClass connectionClass = codeModel.ref(java.sql.Connection.class);
		JClass driverManagerClass = codeModel.ref(java.sql.DriverManager.class);
		JClass resultSetClass = codeModel.ref(java.sql.ResultSet.class);
		JClass sqlExceptionClass = codeModel.ref(java.sql.SQLException.class);
		JClass statementClass = codeModel.ref(java.sql.Statement.class);
		
		//Generate method header
		JMethod retrieve = runnerClass.method(JMod.PRIVATE, void.class, "retrieve");
		
		//Wrap DB connection in try block
		JTryBlock tryBlock = retrieve.body()._try();
		JBlock tryBody = tryBlock.body();
		
		//Setup DB connection
		JVar con = tryBody.decl(connectionClass, "con", driverManagerClass.staticInvoke("getConnection").arg(runnerClass.fields().get("url")).arg(runnerClass.fields().get("usr")).arg(runnerClass.fields().get("pwd")));
		tryBody.decl(resultSetClass, "rs");
		tryBody.decl(statementClass, "st", con.invoke("createStatement").arg(JExpr.direct("ResultSet.TYPE_SCROLL_SENSITIVE")).arg(JExpr.direct("ResultSet.CONCUR_READ_ONLY")));
		tryBody.directStatement(retrieveTemplate1);
		JVar numScans = tryBody.decl(codeModel.INT, "numScans", JExpr.direct(""+(parsed.getN()+1)));
		JVar scan = tryBody.decl(codeModel.INT, "scan", JExpr.direct("0"));
		String aggArray = "String [] aggregates = {";
		for(int i = 0; i < this.parsed.getAggregateFunctions().size(); i++)
		{
			if(i != 0)
			{
				aggArray += ",";
			}
			aggArray += "\""+this.parsed.getAggregateFunctions().get(i)+"\"";
		}
		aggArray += "};";
		tryBody.directStatement(aggArray);
		
		//While loop through entire resultset
		JBlock whileBody = tryBody._while(JExpr.direct("more && scan < numScans")).body();
		Iterator it = dbconnector.columndata.entrySet().iterator();
		
		whileBody.directStatement("HashMap<String, Integer> intVals = new HashMap<String, Integer>();");
		
		
		String key = "";	//Dynamically create the key from grouping attributes
		
		//Fetch values by column number
		while(it.hasNext())
        { 
        	Map.Entry entry = (Map.Entry)it.next();
        	String name = (String)entry.getKey();
        	String type = ((Pair<String,Integer>)entry.getValue()).getValue0();
        	int index = ((Pair<String,Integer>)entry.getValue()).getValue1().intValue();
        	whileBody.directStatement(getJavaType(type) + " " + name + " = rs." + getResultSetType(type) + "("+index+");");
        	if(getJavaType(type).equals("int"))
        	{
        		whileBody.directStatement("intVals.put(\""+name+"\","+name+");");
        	}
        	index++;
        }
		
		String groupingAttrb = "";	//Hold a string of grouping attributes for building the MFstruct
		
		//Loop through grouping attributes to concatenate mftable key
		for(int i = 0; i < parsed.getV().length; i++)
		{
			String name = parsed.getV()[i];
    		if(!key.equals(""))
    		{
    			key += "+\"_\"+";
    		}
    		key += name;
			if(i != 0)
			{
				groupingAttrb += ",";
			}
			groupingAttrb += name;
		}
		groupingAttrb += ",aggregates,numScans";
		
		//Build string for initializing mfstruct
		String buildMFStruct = "mftable.put(" + key + ", new MFStruct(" + groupingAttrb;
		String initAggregates = "";
		for(int i = 0; i < parsed.getF().length; i++)
		{
			buildMFStruct += ",0";
			initAggregates += "mftable.get(" + key + ")."+parsed.getF()[i]+"=";
		}
		buildMFStruct += "));";
		
		//Run mfstruct initialization if mftable doesn't have key yet
		JBlock ifBody = whileBody._if(JExpr.direct("mftable.get(" + key + ") == null"))._then();
		ifBody.directStatement(buildMFStruct);
		
		String conditionString = "scan == 0";
		JBlock conditionBody = whileBody._if(JExpr.direct(conditionString))._then();
		conditionBody.directStatement("MFStruct curStruct = mftable.get("+key+");");
		conditionBody.directStatement(scan0Template);
		
		//Generate catch block for db connections
		JBlock catchBody = tryBlock._catch(sqlExceptionClass).body();
		catchBody.directStatement(retrieveTemplate2);
	}
	
	private void generateStructClass() throws IOException, JClassAlreadyExistsException, ClassNotFoundException
	{
		mfstructClass = runnerClass._class(STRUCT_CLASS);
		generateMFStructProperties();
		generateMFStructConstructor();
		mfstructClass.direct(initStructTemplate);
	}
	
	private void generateMFStructProperties() throws IOException, ClassNotFoundException
	{
		for(int i = 0; i < mfdata.length; i++)
		{
			String[] column = mfdata[i];
			mfstructClass.field(JMod.PUBLIC, codeModel.parseType(getJavaType(column[MF_TYPE])), column[MF_NAME]);
		}
		mfstructClass.field(JMod.PUBLIC, codeModel.parseType("GroupingVariable[]"), "groupVars");
		mfstructClass.field(JMod.PUBLIC, codeModel.parseType("String[]"), "aggregateFunctions");
	}
	
	private void generateMFStructConstructor() throws IOException, ClassNotFoundException
	{
		JMethod construct = mfstructClass.constructor(JMod.PUBLIC);
		for(int i = 0; i < mfdata.length; i++)
		{
			String[] column = mfdata[i];
			construct.param(codeModel.parseType(getJavaType(column[MF_TYPE])), column[MF_NAME]);
		}
		construct.param(codeModel.parseType("String[]"), "aggFcns");
		construct.param(codeModel.INT, "numGroupVars");
		
		//Initialize properties
		JBlock body = construct.body();
		for(int i = 0; i < mfdata.length; i++)
		{
			String[] column = mfdata[i];
			body.assign(JExpr._this().ref(mfstructClass.fields().get(column[MF_NAME])), JExpr.ref(construct.params().get(i).name()));
		}
		body.directStatement("this.aggregateFunctions = aggFcns;");
		body.directStatement("groupVars = new GroupingVariable[numGroupVars+1];");
		body.directStatement("for(int i = 0; i < groupVars.length; i++){");
		body.directStatement("    groupVars[i] = new GroupingVariable();");
		body.directStatement("}");
		body.directStatement("initStruct();");
	}
	
	private String getJavaType(String dbType)
	{
		switch(dbType)
		{
			case DBTYPE_VARCHAR:
			case DBTYPE_CHARACTER:
				return "String";
			case DBTYPE_INTEGER:
				return "int";
			default:
				return null;
		}
	}
	
	private String getResultSetType(String dbType)
	{
		switch(dbType)
		{
			case DBTYPE_VARCHAR:
			case DBTYPE_CHARACTER:
				return "getString";
			case DBTYPE_INTEGER:
				return "getInt";
			default:
				return null;
		}
	}
	
	private class DBConnector
	{
		private String usr ="postgres";
	    private String pwd ="anycolor92";
	    private String url ="jdbc:postgresql://localhost:5432/postgres";
	    
	    public HashMap<String, Pair<String,Integer>> columndata;
	    
		//Function to connect to the database
	   public void connect(){
	        try {
	        Class.forName("org.postgresql.Driver");     //Loads the required driver
	        System.out.println("Success loading Driver!");
	        } catch(Exception exception) {
	        System.out.println("Fail loading Driver!");
	        exception.printStackTrace();
	        }
	    }
	   
	 //Function to retreive from the database and process on the resultset received

	   public void retreive(){

	        try {
		        Connection con = DriverManager.getConnection(url, usr, pwd);    //connect to the database using the password and username
		        System.out.println("Success connecting server!");
		        columndata = new HashMap<String, Pair<String,Integer>>();
		        ResultSet rs;          			 //resultset object gets the set of values retreived from the database
		        boolean more;
		        Statement st = con.createStatement();   //statement created to execute the query
		        
		        String ret = "select column_name, data_type, ordinal_position from information_schema.columns where table_name = 'sales'";
		        rs = st.executeQuery(ret);              //executing the query 
		        more=rs.next();                         //checking if more rows availabl
		        
		        while(more)
		        {
		        	//Fetch all the columns from the row
		        	columndata.put(rs.getString(MF_NAME + 1), new Pair(rs.getString(MF_TYPE + 1), new Integer(rs.getInt(MF_POS + 1))));
		        	
		        	more=rs.next();
		        }
	        
	        } catch(SQLException e) {
	         System.out.println("Connection URL or username or password errors!");
	        e.printStackTrace();
	        }
	    }
	}

}
