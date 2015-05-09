import java.util.ArrayList;

/************************************
 * Author: Jim Stys
 * Date: 4/7/2015
 * Class: Condition
 * 
 * Description:  
 * This class is responsible for parsing SQL
 * conditions and representing them as JAVA conditions
 * instead.
 ************************************/
public class Condition {
	
	//Constants for any tokens that need to be converted to Java equivalents
	public static final String JAVA_OR = "||";
	public static final String JAVA_AND = "&&";
	public static final String JAVA_EQUALS_TO = "==";
	public static final String JAVA_STRING_EQUALS = ".equals";
	
	private static final String TOKEN_SEPARATOR = " ";
	
	public ArrayList<String> aggregateFunctions;	//Holds list of aggregateFunctions found in this condition
	public String lhs;								//Left side of condition
	public String operator;							//Operator in condition
	public String rhs;								//Right hand side of condition
	private String javaString = "";					//String to hold resulting java equivalent
	
	public Condition(String sqlCondition)
	{
		aggregateFunctions = new ArrayList<String>();
		
		if(sqlCondition.equals("true")) //Case of an empty 'having' condition just return true
		{
			javaString = "true";
		}
		else
		{
			String[] tokens = sqlCondition.split(TOKEN_SEPARATOR); //Whitespace mandatory between tokens in order for parsing to work
			
			if(!tokens[0].equals(""))
			{
				this.lhs = tokens[0];
				if(!InputParser.isAggregate(this.lhs) && this.lhs.contains(InputParser.NAME_SEPARATOR))
				{
					//Remove grouping variable number from non-aggregate functions
					this.lhs = this.lhs.substring(0,this.lhs.lastIndexOf(InputParser.NAME_SEPARATOR));
				}
				this.rhs = "";
				this.operator = "";
				boolean isOperator = false;
				for(int i = 0; i < tokens.length; i++)
				{
					if(tokens[i].equals("="))
					{
						tokens[i] = JAVA_EQUALS_TO;
						isOperator = true;
					}
					else if(tokens[i].equalsIgnoreCase("or"))
					{
						tokens[i] = JAVA_OR;
					}
					else if(tokens[i].equalsIgnoreCase("and"))
					{
						tokens[i] = JAVA_AND;
					}
					else if(tokens[i].contains("'"))
					{
						//Java requires double quotes for strings
						tokens[i] = tokens[i].replace("'", "\"");
					}
					else
					{
						if(InputParser.isAggregate(tokens[i]))
						{
							//Add the aggregate function to the list
							aggregateFunctions.add(tokens[i]);
							//Replace the token with the Java source necessary to retrieve it from the grouping variable
							tokens[i] = "curStruct.groupVars["+InputParser.getVarNum(tokens[i])+"].get(\""+tokens[i]+"\")";
						}
					}
					
					javaString += tokens[i];
					
					if(isOperator)
					{
						this.operator = tokens[i];
						isOperator = false;
					}
					else if(i > 0)
					{
						this.rhs += tokens[i];
					}
				}
			}
		}
	}
	
	public String getJavaString()
	{
		return javaString;
	}

}
