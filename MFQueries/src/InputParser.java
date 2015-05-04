import java.util.ArrayList;

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
	
	private String[] S;			//Projected attributes
	private int n;				//Number of grouping variables
	private String[] V;			//Grouping attributes
	private String[] F;			//Array of aggregate functions
	private ArrayList<ArrayList<Condition>> sigma;	//Selection conditions on grouped tuples
	private Condition G;		//Having condition
	private ArrayList<String> aggregateFunctions;
	
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
	 * MUTATOR METHODS
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
		this.sigma = new ArrayList<ArrayList<Condition>>(this.n);
		for(int i = 0; i < this.n; i++)
		{
			this.sigma.add(new ArrayList<Condition>());
		}
		for(int i = 0; i < split.length; i++)
		{
			int varNum = Integer.parseInt(split[i].split("_")[2]);
			this.sigma.get(varNum).add(new Condition(split[i]));
		}
	}
	
	public void setSigma(JSONArray strings)
	{
		this.sigma = new ArrayList<ArrayList<Condition>>(this.n);
		for(int i = 0; i < this.n; i++)
		{
			this.sigma.add(new ArrayList<Condition>());
		}
		for(int i = 0; i < strings.size(); i++)
		{
			int varNum;
			String var = ((String)strings.get(i)).split(" ")[0];
			if(isAggregate((String)strings.get(i)))
			{
				varNum = Integer.parseInt(var.split("_")[2]);
			}
			else
			{
				varNum = Integer.parseInt(var.split("_")[1]);
			}
			varNum--;
			this.sigma.get(varNum).add(new Condition(((String)strings.get(i))));
		}
	}
	
	public void setG(String string)
	{
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
	
	public static boolean isAggregate(String str)
	{
		String[] split = str.split("_");
		if(split.length >= 2)
		{
			return split[0].equalsIgnoreCase("max") || split[0].equalsIgnoreCase("min") || split[0].equalsIgnoreCase("avg") || split[0].equalsIgnoreCase("sum");
		}
		return false;
	}
	
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
	
	private void jsonArrayToArray(String[] arr, JSONArray json)
	{
		for(int i = 0; i < json.size(); i++)
		{
			arr[i] = (String)json.get(i);
		}
	}

}


