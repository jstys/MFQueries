import java.util.ArrayList;

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
	private Condition[] sigma;	//Selection conditions on grouped tuples
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
	
	public Condition[] getSigma()
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
	
	public void setN(int n)
	{
		this.n = n;
	}
	
	public void setV(String string)
	{
		this.V = string.split(",");
	}
	
	public void setF(String string)
	{
		this.F = string.split(",");
	}
	
	public void setSigma(String string)
	{
		String[] split = string.split("\n");
		this.sigma = new Condition[split.length];
		for(int i = 0; i < split.length; i++)
		{
			this.sigma[i] = new Condition(split[i]);
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
	
	/*****************************
	 * toString
	 * 
	 * @return a string representation of 
	 * this InputParser object.
	 *****************************/
	public String toString()
	{
		String result = "";
		String[] sigmaArray = new String[this.sigma.length];
		for(int i = 0; i < this.sigma.length; i ++)
		{
			sigmaArray[i] = this.sigma[i].getJavaString();
		}
		
		result += arrayToString(S, "S");
		result += "n: " + n + "\n";
		result += arrayToString(V, "V");
		result += arrayToString(F, "F");
		result += arrayToString(sigmaArray, "sigma");
		result += "G: " + G.getJavaString() + "\n";
		return result;
	}
	
	public static boolean isAggregate(String str)
	{
		String[] split = str.split("_");
		if(split.length >= 2)
		{
			return split[0].equalsIgnoreCase("max") || split[0].equalsIgnoreCase("min") || split[0].equalsIgnoreCase("avg") || split[0].equalsIgnoreCase("max");
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
		for(int i = 0; i < sigma.length; i++)
		{
			for(int j = 0; j < sigma[i].aggregateFunctions.size(); j++)
			{
				if(!aggregateFunctions.contains(sigma[i].aggregateFunctions.get(j)))
				{
					aggregateFunctions.add(sigma[i].aggregateFunctions.get(j));
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
	}

}
