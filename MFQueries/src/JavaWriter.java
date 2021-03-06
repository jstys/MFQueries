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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.javatuples.Pair;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
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
	
	//Java data type strings
	private static final String JAVA_STRING = "String";
	private static final String JAVA_INT = "int";
	
	//Array indices of mfdata
	private static final int MF_NAME = 0;
	private static final int MF_TYPE = 1;
	private static final int MF_POS = 2;
	
	//Strings to hold templated code
	private String mainTemplate;
	private String connectTemplate;
	private String retrieveTemplate1;
	private String retrieveTemplate2;
	private String groupVarClassTemplate;
	private String initStructTemplate;
	private String mainFunctionsTemplate;
	
	private InputParser parsed;
	private DBConnector dbconnector;
	private String[][] mfdata;
	private JCodeModel codeModel;
	private JDefinedClass runnerClass;
	private JDefinedClass mfstructClass;
	
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
		this.mfdata = new String[dbconnector.columndata.size()][2];
		Iterator<Entry<String, Pair<String, Integer>>> it = dbconnector.columndata.entrySet().iterator();
		int index = 0;
		
		while(it.hasNext())
        { 
        	Map.Entry<String, Pair<String, Integer>> entry = (Map.Entry<String, Pair<String, Integer>>)it.next();
        	String name = (String)entry.getKey();
        	String type = ((Pair<String,Integer>)entry.getValue()).getValue0();
        	this.mfdata[index][MF_NAME] = name;
        	this.mfdata[index][MF_TYPE] = type;
        	index++;
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
		//Generate class header
		runnerClass = codeModel._class(RUNNER_CLASS);
		
		//Generate import statements for external references
		JClass Hashmap = codeModel.ref(java.util.HashMap.class).narrow(String.class).narrow(codeModel.parseType("MFStruct"));
		JClass AtomInt = codeModel.ref(java.util.concurrent.atomic.AtomicInteger.class);
		JClass Map = codeModel.ref(java.util.Map.class);
		runnerClass.field(JMod.PRIVATE, AtomInt, "test");
		runnerClass.field(JMod.PRIVATE, Hashmap, "mftable");
		runnerClass.field(JMod.PRIVATE, Map, "maptest");
		runnerClass.field(JMod.PRIVATE+JMod.STATIC+JMod.FINAL, codeModel.parseType(JAVA_STRING), "usr").init(JExpr.lit("postgres"));
		runnerClass.field(JMod.PRIVATE+JMod.STATIC+JMod.FINAL, codeModel.parseType(JAVA_STRING), "pwd").init(JExpr.lit("anycolor92"));
		runnerClass.field(JMod.PRIVATE+JMod.STATIC+JMod.FINAL, codeModel.parseType(JAVA_STRING), "url").init(JExpr.lit("jdbc:postgresql://localhost:5432/postgres"));
		
		//Generate Constructor
		JMethod construct = runnerClass.constructor(JMod.PUBLIC);
		construct.body().directStatement("mftable = new HashMap<String,MFStruct>();");
		
		//Generate main method for running the source
		JMethod main = runnerClass.method(JMod.PUBLIC+JMod.STATIC, void.class, "main");
		main.param(String[].class, "args");
		main.body().directStatement(mainTemplate);
		
		//Generate connect to DB function
		JMethod connect = runnerClass.method(JMod.PRIVATE, void.class, "connect");
		connect.body().directStatement(connectTemplate);
		
		generateRetrieveFunction();					//Generate retrieve DB rows function
		generateOutputMethod();						//Generate function to output EMF query results
		runnerClass.direct(mainFunctionsTemplate);	//Generate main functions to aggregate / initialize grouping variables
		runnerClass.direct(groupVarClassTemplate);	//Generate Grouping Variable inner class
		generateStructClass();						//Generate MFStruct inner class
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
		tryBody.decl(codeModel.INT, "numScans", JExpr.direct(""+(parsed.getN()+1)));
		tryBody.decl(codeModel.INT, "scan", JExpr.direct("0"));
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
		Iterator<Entry<String, Pair<String, Integer>>> it = dbconnector.columndata.entrySet().iterator();
		
		//Initialize HashMap to store any integer values referenced by their column name
		whileBody.directStatement("HashMap<String, Integer> intVals = new HashMap<String, Integer>();");
		
		
		String key = "";	//Dynamically create the key from grouping attributes
		
		//Fetch values by column number
		while(it.hasNext())
        { 
        	Map.Entry<String, Pair<String, Integer>> entry = (Map.Entry<String, Pair<String, Integer>>)it.next();
        	String name = (String)entry.getKey();
        	String type = ((Pair<String,Integer>)entry.getValue()).getValue0();
        	int index = ((Pair<String,Integer>)entry.getValue()).getValue1().intValue();
        	whileBody.directStatement(getJavaType(type) + " " + name + " = rs." + getResultSetType(type) + "("+index+");");
        	if(getJavaType(type).equals(JAVA_INT))
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
		}
		for(int i = 0; i < this.mfdata.length; i++)
		{
			if(i != 0)
			{
				groupingAttrb += ",";
			}
			groupingAttrb += this.mfdata[i][MF_NAME];
		}
		groupingAttrb += ",aggregates,numScans";
		
		//Build string for initializing mfstruct
		String buildMFStruct = "mftable.put(" + key + ", new MFStruct(" + groupingAttrb;
		buildMFStruct += "));";
		
		//Run mfstruct initialization if mftable doesn't have key yet
		JBlock ifBody = whileBody._if(JExpr.direct("mftable.get(" + key + ") == null"))._then();
		ifBody.directStatement(buildMFStruct);
		
		//Condition for 0th scan
		String conditionString = "scan == 0";
		JConditional condition = whileBody._if(JExpr.direct(conditionString));
		JBlock conditionBody = condition._then();
		
		//Condition for nth scan > 0
		JBlock conditionElse = condition._else();
		JBlock foreachLoop = conditionElse.forEach(codeModel.directClass("MFStruct"), "curStruct", JExpr.direct("mftable.values()")).body();
		List<String> groupingAttr = Arrays.asList(this.parsed.getV());
		
		//Set non-grouping attributes in this MFStruct row to the current row from DB
		String setNonAttrString = "curStruct.setNonGroupingAttrib(";
		int index = 0;
		for(int i = 0; i < mfdata.length; i++)
		{
			String[] column = mfdata[i];
			if(!groupingAttr.contains(column[MF_NAME]))
			{
				if(index != 0)
					setNonAttrString += ",";
				setNonAttrString += column[MF_NAME];
				index++;
			}
		}
		setNonAttrString += ");";
		if(index > 0)
			foreachLoop.directStatement(setNonAttrString);
		
		//Generate 'Such That' condition for nth scan > 0
		foreachLoop.directStatement("GroupingVariable curVariable = curStruct.groupVars[scan];");
		JConditional suchThatConditions = null;
		for(int i = 1; i <= this.parsed.getN(); i++) //Loop through each grouping variable (excluding 0th)
		{
			String suchThat = "scan == " + i;
			ArrayList<Condition> sigmas = new ArrayList<Condition>();
			
			//Combine 0th sigma conditions (where condition) with ith conditions
			sigmas.addAll(this.parsed.getSigma().get(0));
			sigmas.addAll(this.parsed.getSigma().get(i));
			
			for(int j = 0; j < sigmas.size(); j++) //Loop through each sigma condition associated with grouping variable i
			{
				Condition curCondition = sigmas.get(j);
				suchThat += " && ";
				if(InputParser.isAggregate(curCondition.lhs))  //Fetch aggregate variables from ith grouping variable
				{
					suchThat += "curVariable.get("+curCondition.lhs+")"+curCondition.rhs;
				}
				else //Fetch value from current entry in mftable
				{
					String field = curCondition.lhs;
					for(int k = 0; k < mfdata.length; k++)
					{
						if(mfdata[k][MF_NAME].equals(field))
						{
							//Replace == in String comparison with .equals
							if(curCondition.operator.equals(Condition.JAVA_EQUALS_TO) && getJavaType(mfdata[k][MF_TYPE]).equals(JAVA_STRING))
								curCondition.operator = Condition.JAVA_STRING_EQUALS;
							sigmas.set(j, curCondition);
							break;
						}
					}
					//Separate logic for .equals and any other comparison operator
					if(curCondition.operator.equals(Condition.JAVA_STRING_EQUALS))
					{
						suchThat += "curStruct."+curCondition.lhs+curCondition.operator+"("+curCondition.rhs+")";
					}
					else
					{
						suchThat += "curStruct."+curCondition.lhs+curCondition.operator+curCondition.rhs;
					}
					
				}
			}
			//Separate logic for if vs. else if brackets
			if(i == 1)
			{
				suchThatConditions = foreachLoop._if(JExpr.direct(suchThat));
				JBlock suchThatBody = suchThatConditions._then();
				suchThatBody.directStatement("aggregateGroupingVariable(curVariable, intVals);");
			}
			else
			{
				JBlock suchThatBody = suchThatConditions._elseif(JExpr.direct(suchThat))._then();
				suchThatBody.directStatement("aggregateGroupingVariable(curVariable, intVals);");
			}
		}
		conditionBody.directStatement("MFStruct curStruct = mftable.get("+key+");");
		conditionBody.directStatement("GroupingVariable curVariable = curStruct.groupVars[0];");
		String suchThat0 = "";
		for(int i = 0; i < this.parsed.getSigma().get(0).size(); i++) //Loop through sigma conditions for 0th grouping variable
		{
			if(!suchThat0.equals(""))
			{
				suchThat0 += " && ";
			}
			Condition curCondition = this.parsed.getSigma().get(0).get(i);
			String field = curCondition.lhs;
			for(int k = 0; k < mfdata.length; k++)
			{
				if(mfdata[k][MF_NAME].equals(field))
				{
					//Replace == in String comparison with .equals
					if(curCondition.operator.equals(Condition.JAVA_EQUALS_TO) && getJavaType(mfdata[k][MF_TYPE]).equals(JAVA_STRING))
						curCondition.operator = Condition.JAVA_STRING_EQUALS;
					this.parsed.getSigma().get(0).set(i, curCondition);
					break;
				}
			}
			//Separate logic for .equals and any other comparison operator
			if(curCondition.operator.equals(Condition.JAVA_STRING_EQUALS))
			{
				suchThat0 += "curStruct."+curCondition.lhs+curCondition.operator+"("+curCondition.rhs+")";
			}
			else
			{
				suchThat0 += "curStruct."+curCondition.lhs+curCondition.operator+curCondition.rhs;
			}
		}
		JBlock scan0Body = conditionBody._if(JExpr.direct(suchThat0))._then();
		scan0Body.directStatement("aggregateGroupingVariable(curVariable, intVals);");
		
		//Logic for incrementing the scan number when end of resultSet reached
		whileBody.directStatement("more = rs.next();");
		whileBody.directStatement("if(!more)");
		whileBody.directStatement("{");
		whileBody.directStatement("    more = rs.first();");
		whileBody.directStatement("    scan++;");
		whileBody.directStatement("}");
		
		//Logic for outputting results at end of all scans
		tryBody.directStatement("outputResults();");

		
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
		generateMFStructNonGroupingAttrib();
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
	
	private void generateMFStructNonGroupingAttrib() throws IOException, ClassNotFoundException
	{
		JMethod function = mfstructClass.method(JMod.PUBLIC, void.class, "setNonGroupingAttrib");
		List<String> groupingAttr = Arrays.asList(parsed.getV());
		for(int i = 0; i < mfdata.length; i++)
		{
			String[] column = mfdata[i];
			if(!groupingAttr.contains(column[MF_NAME]))
			{
				function.param(codeModel.parseType(getJavaType(column[MF_TYPE])), column[MF_NAME]);
			}
		}
		JBlock body = function.body();
		for(int i = 0; i < function.params().size(); i++)
		{
			String name = function.params().get(i).name();
			body.assign(JExpr._this().ref(mfstructClass.fields().get(name)), JExpr.ref(function.params().get(i).name()));
		}
	}
	
	private void generateOutputMethod()
	{
		//Generate method header
		JMethod outputMethod = runnerClass.method(JMod.PRIVATE, void.class, "outputResults");
		JBlock methodBody = outputMethod.body();
		
		//Generate printf statements for column headers
		for(int i = 0; i < this.parsed.getS().length; i++)
		{
			methodBody.directStatement("System.out.printf(\"%-"+(this.parsed.getS()[i].length()+10)+"s\", \"" + this.parsed.getS()[i] + "\");"); 
		}
		methodBody.directStatement("System.out.println();");
		
		//Generate foreach loop to loop through mftable entries
		JBlock foreachLoop = methodBody.forEach(codeModel.directClass("MFStruct"), "curStruct", JExpr.direct("mftable.values()")).body();
		
		//Generate if statement for the 'having' condition
		JBlock havingBody = foreachLoop._if(JExpr.direct(this.parsed.getG().getJavaString()))._then();
		
		for(int i = 0; i < this.parsed.getS().length; i++) //Loop through projected attributes
		{
			String projected = this.parsed.getS()[i];
			
			//Separate logic for generating printf statements for aggregates or normal attributes respectively
			if(InputParser.isAggregate(projected))
			{
				if(InputParser.isCount(projected))
				{
					havingBody.directStatement("System.out.printf(\"%-"+(this.parsed.getS()[i].length()+10)+"d\", curStruct.groupVars[" + InputParser.getVarNum(projected) + "].count);");
				}
				else
				{
					havingBody.directStatement("System.out.printf(\"%-"+(this.parsed.getS()[i].length()+10)+"d\", curStruct.groupVars[" + InputParser.getVarNum(projected) + "].get(\""+projected+"\"));");
				}
			}
			else
			{
				if(lookupType(projected).equals(JAVA_STRING))
				{
					havingBody.directStatement("System.out.printf(\"%-"+(this.parsed.getS()[i].length()+10)+"s\", curStruct."+projected+");"); 
				}
				else
				{
					havingBody.directStatement("System.out.printf(\"%-"+(this.parsed.getS()[i].length()+10)+"d\", curStruct."+projected+");"); 
				}
			}
		}
		havingBody.directStatement("System.out.println();");
	}
	
	private String getJavaType(String dbType)
	{
		switch(dbType)
		{
			case DBTYPE_VARCHAR:
			case DBTYPE_CHARACTER:
				return JAVA_STRING;
			case DBTYPE_INTEGER:
				return JAVA_INT;
			default:
				return null;
		}
	}
	
	private String lookupType(String name)
	{
		for(int i = 0; i < mfdata.length; i++)
		{
			if(mfdata[i][MF_NAME].equals(name))
			{
				return getJavaType(mfdata[i][MF_TYPE]);
			}
		}
		return null;
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
		        	//Store column data into the hashmap
		        	columndata.put(rs.getString(MF_NAME + 1), new Pair<String,Integer>(rs.getString(MF_TYPE + 1), new Integer(rs.getInt(MF_POS + 1))));
		        	
		        	more=rs.next();
		        }
	        
	        } catch(SQLException e) {
	         System.out.println("Connection URL or username or password errors!");
	        e.printStackTrace();
	        }
	    }
	}

}
