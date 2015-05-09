import java.util.ArrayList;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.simple.JSONArray;

/************************************
 * Author: Jim Stys
 * Date: 4/7/2015
 * Class: InputParser
 * 
 * Description:  
 * This class is responsible for parsing the
 * user input from the command line passed from
 * the MFQueryGenerator main class and storing them
 * in an InputParser object which can be accessed
 * by the JavaWriter class.
 ************************************/
public class InputParser {
	
	//SQL aggregate function strings
	private static final String MAX_FUNCTION = "max";
	private static final String MIN_FUNCTION = "min";
	private static final String AVG_FUNCTION = "avg";
	private static final String SUM_FUNCTION = "sum";
	private static final String COUNT_FUNCTION = "count";
	
	public static final String NAME_SEPARATOR = "_";
	
	private String[] S;									//Projected attributes
	private int n;										//Number of grouping variables
	private String[] V;									//Grouping attributes
	private String[] F;									//Array of aggregate functions
	private ArrayList<ArrayList<Condition>> sigma;		//Selection conditions on grouped tuples
	private Condition G;								//Having condition
	private ArrayList<String> aggregateFunctions;		//Comprehensive collection of aggregate functions appearing in query
	
	/*****************************
	 * DEFAULT CONSTRUCTOR
	 *****************************/
	public InputParser()
	{
		aggregateFunctions = new ArrayList<String>();
	}
	
	/*****************************
	 * ACCESSOR METHODS
	 *****************************/
	public String[] getS()
	{
		return S;
	}
	
	public int getN()
	{
		return n;
	}
	
	public String[] getV()
	{
		return V;
	}
	
	public String[] getF()
	{
		return F;
	}
	
	public ArrayList<ArrayList<Condition>> getSigma()
	{
		return sigma;
	}
	
	public Condition getG()
	{
		return G;
	}

	public ArrayList<String> getAggregateFunctions()
	{
		return aggregateFunctions;
	}
	
	/*****************************
	 * MUTATOR METHODS (overloaded for commandline input or JSON)
	 *****************************/
	public void setS(String string)
	{
		this.S = string.split(",");
	}
	
	public void setS(JSONArray strings)
	{
		this.S = new String[strings.size()];
		jsonArrayToArray(this.S, strings);
	}
	
	public void setN(int n)
	{
		this.n = n;
	}
	
	public void setV(String string)
	{
		this.V = string.split(",");
	}
	
	public void setV(JSONArray strings)
	{
		this.V = new String[strings.size()];
		jsonArrayToArray(this.V, strings);
	}
	
	public void setF(String string)
	{
		this.F = string.split(",");
	}
	
	public void setF(JSONArray strings)
	{
		this.F = new String[strings.size()];
		jsonArrayToArray(this.F, strings);
	}
	
	public void setSigma(String string)
	{
		String[] split = string.split("\n");
		this.sigma = new ArrayList<ArrayList<Condition>>(this.n+1);
		for(int i = 0; i < this.n+1; i++)
		{
			this.sigma.add(new ArrayList<Condition>());
		}
		for(int i = 0; i < split.length; i++)
		{
			String var = split[i];
			int varNum = getVarNum(var.split(" ")[0]);
			
			this.sigma.get(varNum).add(new Condition(var));
		}
	}
	
	public void setSigma(JSONArray strings)
	{
		this.sigma = new ArrayList<ArrayList<Condition>>(this.n+1);
		for(int i = 0; i <= this.n+1; i++)
		{
			this.sigma.add(new ArrayList<Condition>());
		}
		for(int i = 0; i < strings.size(); i++)
		{
			String var = (String)strings.get(i);
			int varNum = getVarNum(var.split(" ")[0]);
			
			this.sigma.get(varNum).add(new Condition(var));
		}
	}
	
	public void setG(String string)
	{
		if(string.equals(""))
			string = "true";
		this.G = new Condition(string);
	}
	
	/*****************************
	 * arrayToString
	 * 
	 * Helper function for printing out array contents.
	 * 
	 * @param arr the array that is being
	 * converted to a string
	 * @param name the name of the field that 
	 * is stored in arr
	 * 
	 * @return a string representation of the
	 * arr param
	 *****************************/
	public String arrayToString(String[] arr, String name)
	{
		String result = "";
		result += name + ": [";
		for(int i = 0; i < arr.length; i++)
		{
			if(i != 0)
			{
				result += ",";
			}
			result += arr[i];
		}
		result += "]\n";
		
		return result;
	}
	
	/*****************************
	 * arrayListToString
	 * 
	 * Helper function for printing out array contents.
	 * 
	 * @param arr the ArrayList that is being
	 * converted to a string
	 * @param name the name of the field that 
	 * is stored in arr
	 * 
	 * @return a string representation of the
	 * arr param
	 *****************************/
	public String arrayListToString(ArrayList<String> arr, String name)
	{
		String result = "";
		result += name + ": [";
		for(int i = 0; i < arr.size(); i++)
		{
			if(i != 0)
			{
				result += ",";
			}
			result += arr.get(i);
		}
		result += "]\n";
		
		return result;
	}
	
	/*****************************
	 * toString
	 * 
	 * @return a string representation of 
	 * this InputParser object.
	 *****************************/
	public String toString()
	{
		String result = "";
		ArrayList<String> sigmaArray = new ArrayList<String>();
		for(int i = 0; i < sigma.size(); i++)
		{
			for(int j = 0; j < sigma.get(i).size(); j++)
			{
				sigmaArray.add(sigma.get(i).get(j).getJavaString());
			}
		}
		
		result += arrayToString(S, "S");
		result += "n: " + n + "\n";
		result += arrayToString(V, "V");
		result += arrayToString(F, "F");
		result += arrayListToString(sigmaArray, "sigma");
		result += "G: " + G.getJavaString() + "\n";
		return result;
	}
	
	/*****************************
	 * isAggregate
	 * 
	 * @param str the string being tested
	 * for aggregate function keyword.
	 * 
	 * @return true if the string contains
	 * an aggregate keyword, false otherwise
	 *****************************/
	public static boolean isAggregate(String str)
	{
		String[] split = str.split(NAME_SEPARATOR);
		if(split.length >= 2)
		{
			return split[0].equalsIgnoreCase(MAX_FUNCTION) || split[0].equalsIgnoreCase(MIN_FUNCTION) || split[0].equalsIgnoreCase(AVG_FUNCTION) || split[0].equalsIgnoreCase(SUM_FUNCTION) || split[0].equalsIgnoreCase(COUNT_FUNCTION);
		}
		return false;
	}
	
	/*****************************
	 * isGlobalCondition
	 * 
	 * @param str the string being tested
	 * for belonging to the 0th grouping variable.
	 * 
	 * @return true if the string is a 
	 * condition on the 0th grouping variable.
	 *****************************/
	public static boolean isGlobalCondition(String str)
	{
		String[] split = str.split(NAME_SEPARATOR);
		return split.length < 2;
	}
	
	/*****************************
	 * isGlobalCondition
	 * 
	 * @param str the string being tested
	 * for belonging to the 0th grouping variable.
	 * 
	 * @return true if the string is a 
	 * condition on the 0th grouping variable.
	 *****************************/
	public static boolean isCount(String str)
	{
		String[] split = str.split(NAME_SEPARATOR);
		return split[0].equalsIgnoreCase(COUNT_FUNCTION);
	}
	
	/*****************************
	 * getVarNum
	 * 
	 * @param str the string to extract
	 * the grouping variable number from
	 * 
	 * @return an integer representing the grouping
	 * variable number.
	 *****************************/
	public static int getVarNum(String str)
	{
		String[] split = str.split(NAME_SEPARATOR);
		if(split.length >= 2)
		{
			if(NumberUtils.isNumber(split[split.length-1]))
				return Integer.parseInt(split[split.length-1]);
		}
		return 0;
	}
	
	/*****************************
	 * findAggregates
	 * 
	 * Finds all aggregates in any of the input fields
	 * and adds them to the aggregateFunctions arrayList.
	 *****************************/
	public void findAggregates()
	{
		for(int i = 0; i < this.S.length; i++)
		{
			if(isAggregate(this.S[i]) && !aggregateFunctions.contains(this.S[i]))
			{
				aggregateFunctions.add(this.S[i]);
			}
		}
		for(int i = 0; i < this.F.length; i++)
		{
			if(!aggregateFunctions.contains(this.F[i]))
			{
				aggregateFunctions.add(this.F[i]);
			}
		}
		for(int i = 0; i < sigma.size(); i++)
		{
			for(int j = 0; j < sigma.get(i).size(); j++)
			{
				for(int k = 0; k < sigma.get(i).get(j).aggregateFunctions.size(); k++)
				{
					if(!aggregateFunctions.contains(sigma.get(i).get(j).aggregateFunctions.get(k)))
					{
						aggregateFunctions.add(sigma.get(i).get(j).aggregateFunctions.get(k));
					}
				}
			}
		}
		for(int i = 0; i < G.aggregateFunctions.size(); i++)
		{
			if(!aggregateFunctions.contains(G.aggregateFunctions.get(i)))
			{
				aggregateFunctions.add(G.aggregateFunctions.get(i));
			}
		}
		System.out.println(aggregateFunctions.size());
	}
	
	/*****************************
	 * jsonArrayToArray
	 * 
	 * @param arr the array whose elements originate
	 * in the JSONArray
	 * @param json the json array being converted
	 *****************************/
	private void jsonArrayToArray(String[] arr, JSONArray json)
	{
		for(int i = 0; i < json.size(); i++)
		{
			arr[i] = (String)json.get(i);
		}
	}

}


