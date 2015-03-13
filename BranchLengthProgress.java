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
	private JLabel label = new JLabel("Enter a gene name with variations in quotes (e.g., \"cytb\" \"cytochrome_b\"):");     // JMB
	private JLabel minTaxLabel = new JLabel("Minimum Number of Taxa: ");		// JMB
	private JTextField minTaxField = new JTextField(3);
	private JLabel maxTaxLabel = new JLabel("Maximum Number of Taxa: ");	// JMB
	private JTextField maxTaxField = new JTextField(3);

	public BranchLengthProgress()
	{
		jpb.setStringPainted(true);
		jpb.setValue(0);
		jpb.setMaximum(100);
		
		jtaResult.setWrapStyleWord(false);
		jtaResult.setLineWrap(false);
		jtaResult.setEditable(false);
		
		JPanel bottomPanel = new JPanel();

		// Layout in next section modified to use GridLayout    // JMB

		bottomPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
		GridLayout inputLayout = new GridLayout(0,2);
		bottomPanel.setLayout(inputLayout);
		bottomPanel.add(label);
		// bottomPanel.add(label, BorderLayout.NORTH);
		bottomPanel.add(jtfGene);
		// bottomPanel.add(jtfGene, BorderLayout.NORTH);
		bottomPanel.add(minTaxLabel);
		// bottomPanel.add(taxaLabel, BorderLayout.WEST);
		bottomPanel.add(minTaxField);
		// bottomPanel.add(taxaField, BorderLayout.WEST);
		bottomPanel.add(maxTaxLabel);     // JMB
		bottomPanel.add(maxTaxField);     // JMB
		bottomPanel.add(jbtStart);
		// bottomPanel.add(jbtStart, BorderLayout.SOUTH);

		add(jpb, BorderLayout.NORTH);
		add(new JScrollPane(jtaResult), BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);
		
		
		jbtStart.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				TreeBaseConnectionMT task = null;
				try
				{
					if ( !minTaxField.getText().equals("") && maxTaxField.getText().equals("") )
						task = new TreeBaseConnectionMT(jtfGene.getText(), minTaxField.getText(), "10000", jtaResult);					// JMB
					else if ( minTaxField.getText().equals("") && !maxTaxField.getText().equals("") )																// JMB
						task = new TreeBaseConnectionMT(jtfGene.getText(), "0", maxTaxField.getText(), jtaResult);						// JMB
					else if ( !minTaxField.getText().equals("") && !maxTaxField.getText().equals("") )																// JMB
						task = new TreeBaseConnectionMT(jtfGene.getText(), minTaxField.getText(), maxTaxField.getText(), jtaResult);	// JMB
					else
						task = new TreeBaseConnectionMT(jtfGene.getText(), "0", "10000", jtaResult);									// JMB
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
				task.execute();
			}
		});
	}
}
