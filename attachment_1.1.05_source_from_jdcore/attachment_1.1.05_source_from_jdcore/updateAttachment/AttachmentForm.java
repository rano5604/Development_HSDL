package updateAttachment;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class AttachmentForm
{
  static final JPanel panel = new JPanel();
  
  public AttachmentForm() {}
  
  public static void main(String[] args) { JFrame frame = new JFrame("Attachment");
    frame.setSize(400, 200);
    frame.setDefaultCloseOperation(3);
    
    Container container = frame.getContentPane();
    container.setLayout(new GridLayout(0, 2));
    
    JTextField db_url = new JTextField("192.168.1.201");
    db_url.setPreferredSize(new Dimension(100, 10));
    
    final JTextField db_name = new JTextField("brta_old");
    db_name.setPreferredSize(new Dimension(100, 10));
    
    final JTextField db_user = new JTextField("root");
    db_user.setPreferredSize(new Dimension(100, 10));
    
    final JTextField db_pass = new JPasswordField();
    db_pass.setPreferredSize(new Dimension(100, 10));
    
    final JTextField reference_no = new JTextField();
    reference_no.setPreferredSize(new Dimension(100, 10));
    


    JLabel DB_URL = new JLabel("DB URL");
    JLabel DB_NAME = new JLabel("DB NAME");
    JLabel DB_USER = new JLabel("DB USER");
    JLabel DB_PASS = new JLabel("DB PASSWORD");
    JLabel Reference = new JLabel("Reference No");
    
    JButton okButton = new JButton("OK");
    okButton.addActionListener(new ActionListener()
    {

      public void actionPerformed(ActionEvent e)
      {
        String url = getText();
        String db = db_name.getText();
        url = "jdbc:mysql://" + url + ":3306/" + db;
        String user = db_user.getText();
        String pass = db_pass.getText();
        String ref = reference_no.getText().toUpperCase();
        

        UpdateAttachmentFinal2 ua = new UpdateAttachmentFinal2(url, user, pass);
        try
        {
          ua.setAttachment(ref);
        } catch (SQLException e1) {
          JOptionPane.showMessageDialog(AttachmentForm.panel, e1.getMessage(), "Error", 
            0);
        }
        catch (IOException e1) {
          JOptionPane.showMessageDialog(AttachmentForm.panel, e1.getMessage(), "Error", 
            0);
        }
        
      }
    });
    container.add(DB_URL);
    container.add(db_url);
    container.add(DB_NAME);
    container.add(db_name);
    container.add(DB_USER);
    container.add(db_user);
    container.add(DB_PASS);
    container.add(db_pass);
    container.add(Reference);
    container.add(reference_no);
    container.add(okButton);
    
    frame.setVisible(true);
  }
}
