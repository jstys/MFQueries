/************************************
 * Author: Jim Stys
 * Date: 4/7/2015
 * Class: MFQueryGenerator
 * 
 * Description:  
 * This is the main class that runs the project
 * and interfaces directly with the user to get
 * the input and then relies on other classes for
 * parsing and outputing additional java source.
 ************************************/
import java.util.Scanner;


public class MFQueryGenerator {
	
	private static InputParser inputParser;
	private static JavaWriter javaWriter;
	
	/*****************************
	 * main
	 * 
	 * Responsible for running the project.
	 * 
	 * @param args String array holding
	 * command-line arguments when executing
	 * this program
	 *****************************/
	public static void main(String[] args)
	{
		inputParser = new InputParser();
		getUserInput();
		System.out.println(inputParser);
		
		javaWriter = new JavaWriter(inputParser);
		javaWriter.generateJavaSource();
	}
	
	/*****************************
	 * getUserInput
	 * 
	 * Prompts user to enter necessary
	 * data for query and uses that to
	 * modify the static instance of InputParser.
	 *****************************/
	public static void getUserInput()
	{
		Scanner input = new Scanner(System.in);
		
		System.out.println("Enter list of projected attributes:");
		inputParser.setS(input.nextLine());
		
		System.out.println("Enter number of grouping variables:");
		inputParser.setN(input.nextInt());
		input.nextLine();
		
		System.out.println("Enter list of grouping attributes:");
		inputParser.setV(input.nextLine());
		
		System.out.println("Enter list of aggregate functions:");
		inputParser.setF(input.nextLine());
		
		System.out.println("Enter list of predicates (enter blank line to finish):");
		String result = input.nextLine();
		String concat = "";
		while(!result.isEmpty())
		{
			concat += result + "\n";
			result = input.nextLine();
		}
		concat = concat.trim();
		inputParser.setSigma(concat);
		
		System.out.println("Enter predicate for the having clause:");
		inputParser.setG(input.nextLine());
		
		input.close();
		inputParser.findAggregates();
	}

}
