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
	public static final String JAVA_OR = "||";
	public static final String JAVA_AND = "&&";
	public static final String JAVA_EQUALS_TO = "==";
	public static final String JAVA_STRING_EQUALS = ".equals";
	public ArrayList<String> aggregateFunctions;
	public String lhs;
	public String operator;
	public String rhs;
	private String javaString = "";
	
	public Condition(String sqlCondition)
	{
		aggregateFunctions = new ArrayList<String>();
		String[] tokens = sqlCondition.split(" ");
		if(!tokens[0].equals(""))
		{
			this.lhs = tokens[0];
			if(!InputParser.isAggregate(this.lhs) && this.lhs.contains("_"))
			{
				this.lhs = this.lhs.substring(0,this.lhs.lastIndexOf('_'));
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
					tokens[i] = tokens[i].replace("'", "\"");
				}
				else
				{
					if(InputParser.isAggregate(tokens[i]))
					{
						aggregateFunctions.add(tokens[i]);
						tokens[i] = "curStruct.groupVars["+0+"].get(\""+tokens[i]+"\")";
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
	
	public String getJavaString()
	{
		return javaString;
	}

}
