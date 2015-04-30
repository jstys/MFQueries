import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;


public class MFQueryExecutor {

    private HashMap<String, MFStruct> mftable;
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
            Connection con = DriverManager.getConnection(url, usr, pwd);    //connect to the database using the password and username
            System.out.println("Success connecting server!");
            ResultSet rs;                    //resultset object gets the set of values retreived from the database
            boolean more;
            Statement st = con.createStatement();   //statement created to execute the query
            
            String ret = "select * from sales";
            rs = st.executeQuery(ret);              //executing the query 
            more=rs.next();                         //checking if more rows available

        while (more) {
            int quant = rs.getInt(7);
            String state = rs.getString(6);
            int month = rs.getInt(4);
            int year = rs.getInt(5);
            int day = rs.getInt(3);
            String prod = rs.getString(2);
            String cust = rs.getString(1);
            if ((!mftable.get(prod_cust))) {
                mftable.put(prod_cust, new MFStruct(cust,prod,sum));
            }
        }
    }

    public class MFStruct {

        private String cust;
        private String prod;
        private int sum;

        public MFStruct(String cust, String prod, int sum) {
            this.cust = cust;
            this.prod = prod;
            this.sum = sum;
        }

    }

}
