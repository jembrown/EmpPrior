import javax.swing.JFrame;


/*
 * Main class that will query TreeBase database for a gene name
 */

public class EmpPriorSearch
{
	public static void main(String[] args) 
	{
		BranchLengthProgress frame = new BranchLengthProgress();
		frame.setTitle("EmpPriorSearch");      // Added Search to name - JMB
		frame.setSize(1000,550);               // JMB
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);  
	}
}
