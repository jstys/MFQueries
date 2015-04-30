import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class MFQueryExecutor {

    private ArrayList<HashMap<java.lang.String, MFStruct>> mftable;
    private ArrayList<java.lang.String> mfkeys;
    private final static String usr = "postgres";
    private final static String pwd = "anycolor92";
    private final static String url = "jdbc:postgresql://localhost:5432/postgres";

    public MFQueryExecutor() {
        mfkeys = new ArrayList<String>();
        mftable = new ArrayList<HashMap<String,MFStruct>>(3);
        for (int i = 0; (i< 3); i += 1) {
            mftable.set(i, new HashMap<String, MFStruct>());
        }
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

            while (more) {
                int mftableIndex = -1;
                int count = -1;
                int quant = rs.getInt(7);
                String state = rs.getString(6);
                int month = rs.getInt(4);
                int year = rs.getInt(5);
                int day = rs.getInt(3);
                String prod = rs.getString(2);
                String cust = rs.getString(1);
                !mfkeys.contains(cust+"_"+prod) ? (mfkeys.add(cust+"_"+prod);
                if ((mftable.get(cust+"_"+prod) == null)) {
                    mftable.put(cust+"_"+prod, new MFStruct(cust,prod,0));
                }
                if ((year==1996)) {
                    if ((mftable.get(cust+"_"+prod).initialized == false)) {
                        mftable.get(cust+"_"+prod).initialized = true;
                        mftable.get(cust+"_"+prod).sum_quant_1 = 0;
                    }
                    mftable.get(cust+"_"+prod).count++;
                    System.out.println(mftable.get(cust+"_"+prod).count);
                }
                more = rs.next();
            }
        } catch (SQLException _x) {
            System.out.println("Connection URL or username or password errors!");
        _x.printStackTrace();

        }
    }

    public class MFStruct {

        public String cust;
        public String prod;
        public int sum_quant_1;
        public boolean initialized;
        public int count;

        public MFStruct(String cust, String prod, int sum_quant_1) {
            this.cust = cust;
            this.prod = prod;
            this.sum_quant_1 = sum_quant_1;
            this.initialized = false;
            this.count = 0;
        }

    }

}
