import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;

// BranchLength GUI
public class BranchLengthProgress extends JFrame
{
	private JProgressBar jpb = new JProgressBar();
	private JTextArea jtaResult = new JTextArea();
	private JTextField jtfGene = new JTextField(30);
	private JButton jbtStart = new JButton("Start!");
	private JLabel label = new JLabel("Enter a gene name with variations in quotes (e.g., \"cytb\" \"cytochrome_b\"):");
	private JLabel minTaxLabel = new JLabel("Minimum Number of Taxa: ");
	private JTextField minTaxField = new JTextField(3);
	private JLabel maxTaxLabel = new JLabel("Maximum Number of Taxa: ");
	private JTextField maxTaxField = new JTextField(3);
	private JButton jbtCancel = new JButton("Cancel");
	private JRadioButton extractButton = new JRadioButton("Attempt to extract gene from Nexus file.");
	private JLabel emptyLabel = new JLabel("");

	public BranchLengthProgress()
	{
		jpb.setStringPainted(true);
		jpb.setValue(0);
		jpb.setMaximum(100);
		
		jtaResult.setWrapStyleWord(false);
		jtaResult.setLineWrap(false);
		jtaResult.setEditable(false);
		
		JPanel bottomPanel = new JPanel();

		// Layout in next section modified to use GridLayout

		bottomPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
		GridLayout inputLayout = new GridLayout(0,2);
		bottomPanel.setLayout(inputLayout);
		bottomPanel.add(label);
		bottomPanel.add(jtfGene);
		bottomPanel.add(minTaxLabel);
		bottomPanel.add(minTaxField);
		bottomPanel.add(maxTaxLabel);
		bottomPanel.add(maxTaxField);
		bottomPanel.add(extractButton);
		bottomPanel.add(emptyLabel);
		bottomPanel.add(jbtStart);
		bottomPanel.add(jbtCancel);

		add(jpb, BorderLayout.NORTH);
		add(new JScrollPane(jtaResult), BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);
		
		final TreeBaseConnectionMT task = new TreeBaseConnectionMT();
		
		jbtStart.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String geneString = jtfGene.getText();
				try
				{
					if ( !minTaxField.getText().equals("") && maxTaxField.getText().equals("") )
						task.set(geneString, minTaxField.getText(), "10000", jtaResult, extractButton.isSelected());
					else if ( minTaxField.getText().equals("") && !maxTaxField.getText().equals("") )
						task.set(geneString, "0", maxTaxField.getText(), jtaResult, extractButton.isSelected());
					else if ( !minTaxField.getText().equals("") && !maxTaxField.getText().equals("") )
						task.set(geneString, minTaxField.getText(), maxTaxField.getText(), jtaResult, extractButton.isSelected());
					else
						task.set(geneString, "0", "10000", jtaResult, extractButton.isSelected());
						
				}
				catch (IOException e1) 
				{
					e1.printStackTrace();
				}
				task.addPropertyChangeListener(new PropertyChangeListener()
				{
					public void propertyChange(PropertyChangeEvent e)
					{
						if("progress".equals(e.getPropertyName()))
						{
							jpb.setValue((Integer)e.getNewValue());
						}
					}
				});
				
				// Dummy checking for quotation marks. Simple check that just asks if there is: (1) at least one mark and (2) an even number of marks
				int quoteCount = geneString.length() - geneString.replace("\"", "").length(); // Clever method found online to count characters in a string
				if ( quoteCount == 0 || quoteCount % 2 != 0 ){
					task.quoteWarning();
				} else {
					task.execute();
				}
			}
		});
		
		jbtCancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				task.cancel(true);
			}
		});
		
	}
}
