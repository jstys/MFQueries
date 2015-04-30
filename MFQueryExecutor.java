import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MFQueryExecutor {

    private HashMap<java.lang.String, MFStruct> mftable;
    private final static String usr = "postgres";
    private final static String pwd = "anycolor92";
    private final static String url = "jdbc:postgresql://localhost:5432/postgres";

    public MFQueryExecutor() {
        mftable = new HashMap<String,MFStruct>();
    }

    public static void main(java.lang.String[] args) {
        MFQueryExecutor dbmsass1 = new MFQueryExecutor();
        dbmsass1.connect();
        dbmsass1.retrieve();

    }

    private void connect() {
        try {
            Class.forName("org.postgresql.Driver");     //Loads the required driver
            System.out.println("Success loading Driver!");
        } catch(Exception exception) {
            System.out.println("Fail loading Driver!");
            exception.printStackTrace();
        }

    }

    private void retrieve() {
        try {
            Connection con = DriverManager.getConnection(url, usr, pwd);
            ResultSet rs;
            Statement st = con.createStatement();
            boolean more;
            
            String ret = "select * from sales";
            rs = st.executeQuery(ret);              //executing the query 
            more=rs.next();                         //checking if more rows available
            int numScans = 3;
            int scan = 0;

            while (more && scan < numScans) {
                HashMap<String, Integer> intVals = new HashMap<String, Integer>();
                int quant = rs.getInt(7);
                intVals.put("quant", quant);
                String state = rs.getString(6);
                int month = rs.getInt(4);
                intVals.put("month", month);
                int year = rs.getInt(5);
                intVals.put("year", year);
                int day = rs.getInt(3);
                intVals.put("day", day);
                String prod = rs.getString(2);
                String cust = rs.getString(1);

                //InputParser.getAggregateFunctions()
                String [] aggregates = {"max_quant","avg_quant_1"};

                //Initialize total MFStruct for the grouping
                if ((mftable.get(cust+"_"+prod) == null)) {
                    mftable.put(cust+"_"+prod, new MFStruct(cust,prod,aggregates,numScans));
                }

                if(scan == 0)
                {
                    MFStruct curStruct = mftable.get(cust+"_"+prod);
                    GroupingVariable curVariable = curStruct.groupVars[0];

                    if(!curStruct.groupVars[0].used)
                    {
                        //Reset result set to beginning
                        more = rs.first();
                        more = rs.next();
                        scan++;
                        continue;
                    }
                    
                    initGroupingVariable(curVariable, intVals);
                    
                    curVariable.count++;
                    aggregateGroupingVariable(curVariable, intVals);
                }
                else
                {
                   for (MFStruct curStruct : mftable.values()) 
                    {
                        if(true)//conditions on curStruct
                        {
                            GroupingVariable curVariable = curStruct.groupVars[scan];
                            initGroupingVariable(curVariable, intVals);

                            curVariable.count++;
                            aggregateGroupingVariable(curVariable, intVals);
                        }
                    } 
                }

                more = rs.next();
                if(!more)
                {
                    //Reset result set to beginning
                    more = rs.first();
                    more = rs.next();
                    scan++;
                }
            }
            //TODO: call function to output results
            outputResults();
            
        } catch (SQLException _x) {
            System.out.println("Connection URL or username or password errors!");
        _x.printStackTrace();

        }
    }

    public void initGroupingVariable(GroupingVariable curVariable, HashMap<String, Integer> intVals)
    {
        if (curVariable.initialized == false)
        {
            for (String key : curVariable.aggMap.keySet()) 
            {
                String[] split = key.split("_");
                switch(split[0])
                {
                    case "max":
                    case "min":
                        curVariable.aggMap.get(key).getAndSet(intVals.get(split[1]));
                        break;
                    default:
                        curVariable.aggMap.get(key).getAndSet(0);
                        break;
                }
            }
            curVariable.initialized = true;
        }
    }

    public void aggregateGroupingVariable(GroupingVariable curVariable, HashMap<String, Integer> intVals)
    {
        for (Map.Entry<String, AtomicInteger> entry : curVariable.aggMap.entrySet()) 
        {
            String key = entry.getKey();
            AtomicInteger value = entry.getValue();
            String[] split = key.split("_");
            switch(split[0])
            {
                case "max":
                    if(intVals.get(split[1]).intValue() >= value.intValue())
                        value.set(intVals.get(split[1]).intValue());
                    break;
                case "min":
                    if(intVals.get(split[1]).intValue() <= value.intValue())
                        value.set(intVals.get(split[1]).intValue());
                    break;
                case "sum":
                    value.getAndAdd(intVals.get(split[1]).intValue());
                    break;
                case "avg":
                    int prevSum = value.get() * (curVariable.count - 1);
                    int newAvg = (prevSum + intVals.get(split[1]).intValue()) / curVariable.count;
                    value.set(newAvg);
                    break;
            }
        }
    }
    
    //TODO: create this function
    public void outputResults()
    {
        System.out.printf("%-8s  ", "column1");            //left aligned
        System.out.printf("%.2f  ", "column2");            //left aligned
        System.out.printf("%.2f  ", "column3");             //right aligned
        System.out.printf("%.2f  ", "column4");
        System.out.println();
        
        for(MFStruct curStruct : mftable.values())
        {   
            if(true) //TODO: test having conditions here
            {
                //TODO: print results here
            }
        }
    }

    public class GroupingVariable {
        public HashMap<String, AtomicInteger> aggMap;
        public boolean initialized;
        public boolean used;
        public int count;

        public GroupingVariable()
        {
            count = 0;
            initialized = false;
            used = false;
            aggMap = new HashMap<String, AtomicInteger>();
        }

        public void addAggregate(String agg)
        {
            if(this.used == false)
            {
                this.used = true;
            }
            aggMap.put(agg, new AtomicInteger());
        }
    }

    public class MFStruct {
        //These are grouping attributes
        public String cust;
        public String prod;

        public GroupingVariable [] groupVars;

        private String[] aggregateFunctions;

        public MFStruct(String cust, String prod, String[] aggFcns, int numGroupVars) 
        {
            this.cust = cust;
            this.prod = prod;
            this.aggregateFunctions = aggFcns;
            groupVars = new GroupingVariable[numGroupVars+1];
            for(int i = 0; i < groupVars.length; i++)
            {
                groupVars[i] = new GroupingVariable();
            }
            initStruct();
        }

        private void initStruct() 
        {
            for(int i = 0; i < aggregateFunctions.length; i++)
            {
                String[] split = aggregateFunctions[i].split("_");
                switch(split.length)
                {
                    case 2:
                        groupVars[0].addAggregate(split[0]+"_"+split[1]);
                        break;
                    case 3:
                        groupVars[Integer.parseInt(split[2])].addAggregate(split[0]+"_"+split[1]);
                        break;
                }
            }
        }
    }

}
