package updateAttachment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.JPanel;



public class UpdateAttachmentFinal
{
  static Connection myConn;
  static PreparedStatement myStmt;
  static PreparedStatement myStmt1;
  static String id = null;
  static String app_type = null;
  static String category = null;
  static String applicant_type = null;
  static String l_union = null;
  static FileInputStream input = null;
  
  static String doc_id = null;
  
  static String URL;
  static String User;
  static String Password;
  final JPanel panel = new JPanel();
  
  public UpdateAttachmentFinal(String URL, String User, String Password)
  {
    URL = URL;
    User = User;
    Password = Password;
  }
  
  public void setAttachment(String reference_no) throws SQLException, IOException
  {
    try
    {
      myConn = DriverManager.getConnection(URL, User, Password);
    } catch (SQLException e) {
      JOptionPane.showMessageDialog(panel, e.getMessage(), "Error", 
        0);
    }
    
    try
    {
      String sql1 = "select id,application_type_id,category,applicant_type,isLabourUnionMember from application_form where pin=? and status!=?";
      myStmt1 = myConn.prepareStatement(sql1);
      myStmt1.setString(1, reference_no);
      myStmt1.setString(2, "Submitted");
      
      try
      {
        ResultSet rs = myStmt1.executeQuery();
        if (rs.isBeforeFirst())
        {
          while (rs.next()) {
            id = rs.getString("id");
            app_type = rs.getString("application_type_id");
            category = rs.getString("category");
            applicant_type = rs.getString("applicant_type");
            l_union = rs.getString("isLabourUnionMember");
          }
          
        }
        else {
          JOptionPane.showMessageDialog(panel, "Application Not Found For: " + reference_no, "Warning", 
            2);
        }
      }
      catch (SQLException e)
      {
        JOptionPane.showMessageDialog(panel, e.getMessage(), "Error", 
          0);
      }
      finally {
        if (myStmt1 != null) {
          myStmt1.close();
        }
      }
      




      try
      {
        if (app_type.equals("NEW"))
        {
          if (l_union.equals("1"))
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            setLabourUnion(reference_no);
            
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          
        }
        else if (app_type.equals("RENEWAL"))
        {
          if ((category.equals("PROFESSIONAL")) && (applicant_type.equals("GENERAL")))
          {
            if (l_union.equals("1"))
            {
              setApplicationForm(reference_no);
              setDL(reference_no);
              setMoneyReceipt(reference_no);
              setTest(reference_no);
              setLabourUnion(reference_no);
              JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
            }
            else
            {
              setApplicationForm(reference_no);
              setDL(reference_no);
              setMoneyReceipt(reference_no);
              setTest(reference_no);
              JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
            }
            
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          
        }
        else if (app_type.equals("DUPLICATE"))
        {
          if (l_union.equals("1"))
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setLabourUnion(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          
        }
        else if (app_type.equals("VEHICLE_CHANGE"))
        {
          if (l_union.equals("1"))
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            setLabourUnion(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          
        }
        else if (app_type.equals("VEHICLE_ADD"))
        {
          if (l_union.equals("1"))
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            setLabourUnion(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          
        }
        else if (app_type.equals("TYPE_CHANGE"))
        {
          if (l_union.equals("1"))
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            setLabourUnion(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setTest(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          
        }
        else if (app_type.equals("CORRECTION"))
        {
          if (l_union.equals("1"))
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setLabourUnion(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          

        }
        else if (app_type.equals("PSV"))
        {
          if (l_union.equals("1"))
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setLabourUnion(reference_no);
            setRTC(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          else
          {
            setApplicationForm(reference_no);
            setDL(reference_no);
            setMoneyReceipt(reference_no);
            setRTC(reference_no);
            JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
          }
          

        }
        else if (app_type.equals("ENDORSEMENT"))
        {
          setApplicationForm(reference_no);
          setDL(reference_no);
          setMoneyReceipt(reference_no);
          JOptionPane.showMessageDialog(panel, "Completed", reference_no, 1, null);
        }
      }
      catch (SQLException e) {
        JOptionPane.showMessageDialog(panel, e.getMessage(), "Error", 
          0);

      }
      finally
      {
        if (input != null)
        {
          input.close();
        }
        if (myStmt != null)
        {
          close(myConn, myStmt);
        }
      }
    }
    catch (SQLException e)
    {
      JOptionPane.showMessageDialog(panel, e.getMessage(), "Error", 
        0);
    }
  }
  












































































































































































































  public void setDL(String reference_no)
    throws SQLException
  {
    doc_id = "DL";
    try {
      String sql = "insert into attach_docs (id,pin,app_id, doc_id, data) values(?,?,?,?,?)";
      
      myStmt = myConn.prepareStatement(sql);
      


      myStmt.setString(1, id);
      myStmt.setString(2, reference_no);
      myStmt.setString(3, app_type);
      myStmt.setString(4, doc_id);
      
      File theFile_data = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data);
      myStmt.setBinaryStream(5, input);
      
      myStmt.executeUpdate();
      
      System.out.println("\n" + reference_no + " Completed successfully Most Recent DL!");

    }
    catch (Exception localException) {}finally
    {

      if (myStmt != null) {
        myStmt.close();
      }
    }
  }
  
  public void setMoneyReceipt(String reference_no) throws SQLException
  {
    doc_id = "MONEY_RECEIPT";
    try {
      String sql = "insert into attach_docs (id,pin,app_id, doc_id, data) values(?,?,?,?,?)";
      
      myStmt = myConn.prepareStatement(sql);
      


      myStmt.setString(1, id);
      myStmt.setString(2, reference_no);
      myStmt.setString(3, app_type);
      myStmt.setString(4, doc_id);
      
      File theFile_data = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data);
      myStmt.setBinaryStream(5, input);
      
      myStmt.executeUpdate();
      
      System.out.println("\n" + reference_no + " Completed successfully Money Receipt!");

    }
    catch (Exception localException) {}finally
    {

      if (myStmt != null) {
        myStmt.close();
      }
    }
  }
  
  public void setApplicationForm(String reference_no) throws SQLException, FileNotFoundException
  {
    doc_id = "FORM";
    try {
      String sql = "insert into attach_docs (id,pin,app_id,doc_id,data,data2,data3,data4) values (?,?,?,?,?,?,?,?)";
      myStmt = myConn.prepareStatement(sql);
      


      myStmt.setString(1, id);
      myStmt.setString(2, reference_no);
      myStmt.setString(3, app_type);
      myStmt.setString(4, doc_id);
      
      File theFile_data = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data);
      myStmt.setBinaryStream(5, input);
      
      File theFile_data2 = new File("data_image\\signature.jpg");
      input = new FileInputStream(theFile_data2);
      myStmt.setBinaryStream(6, input);
      
      File theFile_data3 = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data3);
      myStmt.setBinaryStream(7, input);
      
      File theFile_data4 = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data4);
      myStmt.setBinaryStream(8, input);
      
      myStmt.executeUpdate();
      
      System.out.println("\n" + reference_no + " Completed successfully Application Form Doc!");
    }
    catch (Exception exc) {
      JOptionPane.showMessageDialog(panel, exc.getMessage(), "Error", 
        0);
    }
    finally {
      if (myStmt != null) {
        myStmt.close();
      }
    }
  }
  
  public void setTest(String reference_no) throws SQLException
  {
    doc_id = "TEST";
    try {
      String sql = "insert into attach_docs (id,pin,app_id, doc_id, data) values(?,?,?,?,?)";
      
      myStmt = myConn.prepareStatement(sql);
      
      myStmt.setString(1, id);
      myStmt.setString(2, reference_no);
      myStmt.setString(3, app_type);
      myStmt.setString(4, doc_id);
      
      File theFile_data = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data);
      myStmt.setBinaryStream(5, input);
      
      myStmt.executeUpdate();





    }
    catch (Exception localException) {}finally
    {




      if (myStmt != null) {
        myStmt.close();
      }
    }
  }
  
  public void setLabourUnion(String reference_no) throws SQLException
  {
    doc_id = "LABOUR_UNION";
    try {
      String sql = "insert into attach_docs (id,pin,app_id, doc_id, data) values(?,?,?,?,?)";
      
      myStmt = myConn.prepareStatement(sql);
      
      myStmt.setString(1, id);
      myStmt.setString(2, reference_no);
      myStmt.setString(3, app_type);
      myStmt.setString(4, doc_id);
      
      File theFile_data = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data);
      myStmt.setBinaryStream(5, input);
      
      myStmt.executeUpdate();





    }
    catch (Exception localException) {}finally
    {




      if (myStmt != null) {
        myStmt.close();
      }
    }
  }
  
  public void setRTC(String reference_no) throws SQLException
  {
    doc_id = "RTC_APPROVAL";
    try {
      String sql = "insert into attach_docs (id,pin,app_id, doc_id, data) values(?,?,?,?,?)";
      
      myStmt = myConn.prepareStatement(sql);
      
      myStmt.setString(1, id);
      myStmt.setString(2, reference_no);
      myStmt.setString(3, app_type);
      myStmt.setString(4, doc_id);
      
      File theFile_data = new File("data_image\\photo.jpg");
      input = new FileInputStream(theFile_data);
      myStmt.setBinaryStream(5, input);
      
      myStmt.executeUpdate();





    }
    catch (Exception localException) {}finally
    {




      if (myStmt != null) {
        myStmt.close();
      }
    }
  }
  
  private static void close(Connection myConn, Statement myStmt) throws SQLException
  {
    if (myStmt != null) {
      myStmt.close();
    }
    
    if (myConn != null) {
      myConn.close();
    }
  }
}
