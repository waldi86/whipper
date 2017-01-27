	package org.whipper;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whipper.Query.QueryResult;
import org.whipper.connection.ConnectionFactory;
import org.whipper.exceptions.DbNotAvailableException;
import org.whipper.exceptions.ServerNotAvailableException;
import org.whipper.resultmode.ResultHolder;
import org.whipper.xml.XmlHelper;

public class SQLHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(Query.class);
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    
    private long startTime = -1;
    private long endTime = -1;

	public List<List<Object>> getResults(ExpectedResultHolder ehr, String sql) throws Exception{
		LOG.info("Running query {} - {}");
        Statement ps = null;
        Exception exception = null;
        boolean valid = true;
        List<List<Object>> rows = null; 
        startTime = System.currentTimeMillis();
        try{
            ps = ehr.getConnection().createStatement();
            ps.execute(sql);
        } catch (Exception ex) {
        	exception = ex;
        	LOG.error("Error while reading sql results: " + ex.getMessage());
		}
        endTime = System.currentTimeMillis();
        if(exception != null){
        	Whipper.close(ps);
        	throw exception;
        } else {
            try{
                rows = buildResult(ps);
            } catch (SQLException ex){
            	throw ex;
            } finally {
                Whipper.close(ps);
            }
        }
        return rows;
	}
	
	 public List<List<Object>> buildResult(Statement s) throws SQLException, IllegalArgumentException{
		 	List<List<Object>> rows = null; 
	        if(s == null){
	            throw new IllegalArgumentException("Statement cannot be null.");
	        }
	        if(s.isClosed()){
	            throw new IllegalArgumentException("Statement is closed.");
	        }
	        ResultSet rs = s.getResultSet();
	        if(rs == null){
	        	return null;
	        } else {
	            ResultSetMetaData md = rs.getMetaData();
	            int columnCount = md.getColumnCount();
	            List<String> columnLabels;
	            List<String> columnTypeNames;
	            columnLabels = new ArrayList<>(columnCount);
	            columnTypeNames = new ArrayList<>(columnCount);
	            for(int i = 1; i <= columnCount; i++){
	                columnLabels.add(md.getColumnLabel(i));
	                columnTypeNames.add(md.getColumnTypeName(i));
	            }
	            rows = new LinkedList<>();
	            while(rs.next()){
	                List<Object> row = new ArrayList<>(columnCount);
	                for(int i = 1; i <= columnCount; i++){
	                    Object o = rs.getObject(i);
	                    if(o instanceof Clob){
	                        o = ((Clob) o).getSubString(1l, (int)((Clob) o).length());
	                    } else if(o instanceof Blob){
	                        Blob b = (Blob)o;
	                        o = XmlHelper.encode(b.getBytes(1l, (int)b.length()));
	                    } else if(o instanceof SQLXML){
	                        o = ((SQLXML)o).getString();
	                    } else if(o instanceof byte[]){
	                        o = XmlHelper.encode((byte[]) o);
	                    } else if(o instanceof Byte[]){
	                        o = XmlHelper.encode(toPrimitive((Byte[]) o));
	                    }	                    
	                    row.add(o);
	                }
	                rows.add(row);
	            }
	        }
	        return rows;
	    }
	 
	 /**
	     * Converts array to its primitive equivalent.
	     *
	     * @param in input array
	     * @return primitive array
	     */
	    public static byte[] toPrimitive(Byte[] in){
	        byte[] out = new byte[in.length];
	        for(int i = 0; i < in.length; i++){
	            out[i] = in[i].byteValue();
	        }
	        return out;
	    }
	
}
