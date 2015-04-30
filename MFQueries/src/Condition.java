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
	private static final String JAVA_OR = "||";
	private static final String JAVA_AND = "&&";
	private static final String JAVA_EQUALS_TO = "==";
	public ArrayList<String> aggregateFunctions;
	private String javaString = "";
	
	public Condition(String sqlCondition)
	{
		aggregateFunctions = new ArrayList<String>();
		String[] tokens = sqlCondition.split(" ");
		for(int i = 0; i < tokens.length; i++)
		{
			if(tokens[i].equals("="))
			{
				javaString += JAVA_EQUALS_TO;
			}
			else if(tokens[i].equalsIgnoreCase("or"))
			{
				javaString += JAVA_OR;
			}
			else if(tokens[i].equalsIgnoreCase("and"))
			{
				javaString += JAVA_AND;
			}
			else
			{
				if(InputParser.isAggregate(tokens[i]))
				{
					aggregateFunctions.add(tokens[i]);
				}
				javaString += tokens[i];
			}
		}
	}
	
	public String getJavaString()
	{
		return javaString;
	}

}
