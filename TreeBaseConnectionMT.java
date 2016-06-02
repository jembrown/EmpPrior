import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.swing.*;

public class TreeBaseConnectionMT extends SwingWorker<String, String>
{
	private URL input;
	private BufferedReader br;
	private String[] gene;
	private int minInd;
	private int maxInd;
	private JTextArea result; 
	public String baseUrl = "https://treebase.org/treebase-web/phylows/study/TB2:";
	private Vector<String> fileNames = new Vector<String>();
	private Vector<String> warnings = new Vector<String>();
	private Vector<String> delGeneNames = new Vector<String>();
	
	private boolean extractFlag;
	
	/*
	 * Take text from text field and split into an array. Each item in the array 
	 * should be some gene
	 */
	 
	public TreeBaseConnectionMT(){
	};
	 
	public void set(String gene, String minInd, String maxInd, JTextArea result, boolean extractFlag) throws IOException
	{
		
		// Splits based on "s followed by some amount of whitespace
		this.gene = gene.split("\"\\s+");
		this.minInd = Integer.parseInt(minInd);
		this.maxInd = Integer.parseInt(maxInd);
		this.result = result;
		this.extractFlag = extractFlag;
	}
	
	public void quoteWarning(){
		publish("WARNING: You did not surround your gene names with the proper number of quotation marks.");
		publish("Please re-enter gene names and try again.");
		publish("");
	}
	
	protected String doInBackground()
	{
		
		try
		{
			
			if (extractFlag)
				publish("\nWill attempt to extract gene from Nexus file. Results may be unreliable, especially when gene names are common strings.\n");
		
			for (int gCount=0; gCount < gene.length; gCount++){						// Added iterator over all gene names
																					// Changed all instances of gene[0] to gene[gCount]

				gene[gCount] = gene[gCount].replace("\"","");						// Strips off parentheses

				setProgress(100 * gCount / gene.length);

				ExecutorService executor = Executors.newFixedThreadPool(10);		// Moved this line inside loop across gene names

				// Query TreeBase with first array item (first gene entered).
				publish("Searching for genes with name "+gene[gCount]+"...");
				input = new URL("https://treebase.org/treebase-web/search/studySearch.html?query=dcterms.abstract=\""+gene[gCount]+"\"");
				
				br = new BufferedReader(new InputStreamReader(input.openStream()));
				
				String line;
				Vector<String> urlVector = new Vector<String>();

				// Read through source code of query and store each unique nexus url
				while ((line = br.readLine()) != null)
				{
					if (line.indexOf("?format=nexus") > -1)
						urlVector.add(line.split(":|\"")[3]);
				}

				br.close();

				if(urlVector.size() == 0)
				{
					publish("No results found for gene "+gene[gCount]+".");
				}

				if (urlVector.size() > 0){

					// Assign a thread to each nexus file and send to GetANexusFile class
					for (int i = 0; i < urlVector.size(); i++)
					{
						if (!isCancelled()){
							fileNames.add( urlVector.elementAt(i).replace("?format=nexus","")+"_"+gene[gCount]+".nex" );
							executor.execute(new GetANexusFile(urlVector.elementAt(i), gene, gCount, i, minInd, maxInd, fileNames, warnings, extractFlag, delGeneNames));
							// setProgress now accounts for >1 gene
							setProgress(((100 * i / (urlVector.size()*gene.length)) + (100 * gCount / gene.length)));
							publish("Retrieving nexus"+i+": https://treebase.org/treebase-web/phylows/study/TB2:"+urlVector.elementAt(i));
							try{
								Thread.sleep(100);
							}
							catch (InterruptedException ie) {}
						}
					}

					if (isCancelled()){
						publish("");
						publish("Nexus retrieval cancelled by the user. All files not retrieved. Skipping downstream processing. Attempting to quit elegantly.");
						publish("");
					}
					
					if (!isCancelled())
						publish("\nParsing nexus files...\n");

				}

				executor.shutdown();

				while(!executor.isTerminated() && !isCancelled()) {

					if (!warnings.isEmpty()){
						Iterator<String> warnItr = warnings.iterator();
						while (warnItr.hasNext())
							publish(warnItr.next());
						warnings.removeAllElements();
					}
					
					if (!delGeneNames.isEmpty()){
						Iterator<String> delGeneItr = delGeneNames.iterator();
						while (delGeneItr.hasNext())
							publish("File "+delGeneItr.next()+" was deleted because it did not meet requirements.");
						delGeneNames.removeAllElements();
					}
					
				}

				if (isCancelled())
					publish("\nFile parsing not complete due to user cancellation.\n");

				

				publish("");
			}

			// This code block looks for duplicate files with the same study ID

			if (!isCancelled()){
				publish("Looking for duplicate files...");
				Vector<String> baseNames = new Vector<String>();
				Iterator<String> fileNameItr = fileNames.iterator();
				while (fileNameItr.hasNext()) {
					if (!isCancelled()){
						String fileToCheck = fileNameItr.next();

						// Extract base name from fileToCheck
						for (int gCount=0; gCount < gene.length; gCount++){
							if (fileToCheck.indexOf(gene[gCount]) > -1){
								int geneNameIndex = fileToCheck.indexOf("_"+gene[gCount]);
								String currBase = fileToCheck.substring(0,geneNameIndex);
								if ( baseNames.indexOf(currBase) > -1 ) {
									try {
										new File(fileToCheck).delete();
										publish(fileToCheck+" was deleted.");
									} catch (Exception noFile) {
										System.err.println("Problem deleting file: "+fileToCheck);
									}
								}
								else
									baseNames.add( currBase );
							}
						}
					}
				}
				if (isCancelled()){
					publish("\nDuplicate file deletion cancelled by the user. Exiting...\n");
				}
			}

			publish("");
			publish("Finished.");
			setProgress(100);
		
		}
		catch (IOException ie) 
		{
			System.out.println("IOException: " + ie);
		}
		catch (Exception e)
		{
			System.out.println("Exception: " + e);
		}
		
		return "url";
	}
	
	// What is printed in the JTextArea GUI
	protected void process(java.util.List<String> list)
	{
		for (int i = 0; i < list.size(); i++)
			result.append(list.get(i) + "\n");
	}
}

// Creates and formats nexus files
class GetANexusFile implements Runnable
{
	private String url;
	private String[] gene;

	private int geneIndex;
	private int count;
	private int minInd;
	private int maxInd;
	private int numberTaxa = 0;
	int[] geneSequence = new int[2];
	private Vector<String> fileNames = new Vector<String>();
	private Vector<String> warnings = new Vector<String>();
	private boolean extractFlag;
	private Vector<String> delGeneNames = new Vector<String>();
	
	public GetANexusFile(String url, String[] gene, int geneIndex, int count, int minInd, int maxInd, Vector<String> fileNames, Vector<String> warnings, boolean extractFlag, Vector<String> delGeneNames) // Added gene Index -- JMB
	{
		this.url = url;
		this.gene = gene;
		this.geneIndex = geneIndex;
		this.count = count;
		this.minInd = minInd;
		this.maxInd = maxInd;
		this.fileNames = fileNames;
		this.warnings = warnings;
		this.extractFlag = extractFlag;
		this.delGeneNames = delGeneNames;
	}
	public void run()
	{
		try
		{
			if (extractFlag)
				getNexusAndExtract(url);
			else
				getNexus(url);
			isGeneThere(count, minInd, maxInd);
		}
		catch (Exception e)
		{
			warnings.add("There was a problem parsing file "+url.replace("?format=nexus","")+" with gene name "+gene[geneIndex]+"!");
			System.out.println("\nThere was a problem parsing file "+url.replace("?format=nexus","")+" with gene name "+gene[geneIndex]+"!\n");
		}
	}
	
	public void getNexus(String goodUrl) throws IOException
	{
		URL input = new URL("https://treebase.org/treebase-web/phylows/study/TB2:"+goodUrl);
		BufferedReader br = new BufferedReader(new InputStreamReader(input.openStream()));
		StringBuffer stringCat = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null)
			stringCat.append(line + "\n");
		br.close();
		BufferedWriter writer = new BufferedWriter(new FileWriter(goodUrl.replace("?format=nexus","")+"_"+gene[geneIndex]+".nex"));		
		String[] in = new String(stringCat).split("\n");
				
		// Formats nexus file -- Don't touch
		for (int i = 0; i < in.length; i++)
		{		
			String str = in[i];
			writer.write(nucSub(str)+"\n");
			if (str.toLowerCase().indexOf("DIMENSIONS NTAX".toLowerCase()) > -1)
			{
				str = str.split("=")[1];
				int tempNumTaxa = Integer.parseInt(str.substring(0,str.length()-1));
				if (tempNumTaxa >= minInd && tempNumTaxa <= maxInd)
					numberTaxa = tempNumTaxa;
			}
		}
		
		writer.close();
	}

	public void getNexusAndExtract(String goodUrl) throws IOException
	{
		URL input = new URL("https://treebase.org/treebase-web/phylows/study/TB2:"+goodUrl);

		// System.out.println(geneIndex);		// Useful for debugging

		BufferedReader br = new BufferedReader(new InputStreamReader(input.openStream()));
		
		StringBuffer stringCat = new StringBuffer();
		String line;
		String charSetName = "charSetName not initialized.";
		
		while ((line = br.readLine()) != null)
			stringCat.append(line + "\n");
		
		br.close();
		
		boolean start = false;
		boolean title = false;
		boolean parseGeneBounds = false;
		
		Vector<String> taxa = new Vector<String>();
		Vector<Integer> ntaxa = new Vector<Integer>();
		Map<String, Integer> d = new HashMap<String, Integer>();
				
		// BufferedWriter writer = new BufferedWriter(new FileWriter("nexus" + count + ".nex", true));
		BufferedWriter writer = new BufferedWriter(new FileWriter(goodUrl.replace("?format=nexus","")+"_"+gene[geneIndex]+".nex"));    // JMB
		writer.write("#NEXUS\n\n");
		String str;
				
		// First check if alignments are concatenated genes. If so, get coordinates of gene of interest
		String[] in = new String(stringCat).split("\n");
		try
		{
			// Iterates across lines in nexus file
			for (int a = 0; a < in.length; a++)
			{
				// Looks for line with character sets
				// if(in[a].indexOf("CHARSET") > -1 && in[a].split("\\s+")[2].toLowerCase().indexOf(gene[geneIndex].substring(1,3).toLowerCase()) > -1)
				if(in[a].indexOf("CHARSET") > -1) // && in[a].split("\\s+")[2].toLowerCase().indexOf(gene[geneIndex].toLowerCase()) > -1)	// JMB
				{
				
					for (int geneCharIndex = 0; geneCharIndex < gene.length; geneCharIndex++)						// JMB
					{
						if (in[a].split("\\s+")[2].toLowerCase().indexOf(gene[geneCharIndex].toLowerCase()) > -1)	// JMB
							parseGeneBounds = true;																	// JMB
					}
					
					if (parseGeneBounds)																			// JMB
					{
						// Splits string around "="s and ";"s, then extracts sequence range limits for gene 		// JMB
						String seqRange = in[a].split("=|;")[2].trim();
						geneSequence[0] = Integer.parseInt(seqRange.split("-")[0]);
						geneSequence[1] = Integer.parseInt(seqRange.split("-")[1]);
					
						// Extracts character set name	// JMB
						charSetName = in[a].split("=")[1].replace(")","").replace("\'","").trim();					// JMB
					
						title = true;
						break;
					}
				}
			}
		}
		catch (NumberFormatException nfe)
		{
			geneSequence[0] = 0;
			geneSequence[1] = 0;
		}
				
		// Formats nexus file -- Don't touch
		for (int i = 0; i < in.length; i++)
		{		
			str = in[i];

			// Stores taxon set names (e.g., Taxa1, Taxa2, ...) // JMB
			if (str.toLowerCase().indexOf("TITLE  Taxa".toLowerCase()) > -1)
			{
				str = str.split("\\s+")[2];
				taxa.add(str.substring(0,str.length()-1));
			}
			// Stores taxon number // JMB
			else if (str.toLowerCase().indexOf("DIMENSIONS NTAX".toLowerCase()) > -1)
			{
				str = str.split("=")[1];
				ntaxa.add(Integer.parseInt(str.substring(0,str.length()-1)));
			}
			// Checks to see if gene name is found and NO charset definition was already processed (unpartitioned)
			else if (geneNameFound(str) && title == false)
			{
				writer.write("BEGIN DATA;\n\n");
				writer.write(str+"\n");
				start = true;
			
				// Stores taxon set names and sizes in d
				for (int j = 0; j < taxa.size(); j++)
					d.put(taxa.elementAt(j), ntaxa.elementAt(j));
			}
			// Checks to see if gene name is found and charset definition WAS already processed (partitioned)
			//else if ((geneNameFound(str) || charSetNameFound(str,charSetName) ) && title == true)						// JMB
			else if (charSetNameFound(str,charSetName) && title == true)						// JMB
			{
				writer.write("BEGIN DATA;\n\n");
				writer.write("\tTITLE \'"+gene[geneIndex].substring(0,gene[geneIndex].length())+"(partitioned)\';\n");	// JMB
				start = true;

				// Stores taxon set names and sizes in d
				for (int j = 0; j < taxa.size(); j++)
					d.put(taxa.elementAt(j), ntaxa.elementAt(j));
			}
			// Uses linked taxon set to find the corresponding # of taxa
			else if ((str.indexOf("LINK") > -1) && start)
			{
				str = str.split("=")[1];
				String subStr = str.substring(0, str.length()-1).trim();
				numberTaxa = d.get(subStr);
			}
			else if (fileStop(str) && start)
			{
				writer.write(str+"\n");
				break;
			}
			else if (start)
			{				
				if (str.toLowerCase().indexOf("DIMENSIONS NCHAR".toLowerCase()) > -1 && geneSequence[0] == 0)
				{
					writer.write(str.substring(0, str.length()-1) + " NTAX="+numberTaxa+";\n");
					continue;
				}
				else if (str.toLowerCase().indexOf("DIMENSIONS NCHAR".toLowerCase()) > -1 && geneSequence[0] > 0)
				{
					writer.write("\tDIMENSIONS NCHAR="+(geneSequence[1]-(geneSequence[0]-1))+" NTAX="+numberTaxa+";\n");
					continue;
				}
			
				if(str.split("\\s+").length == 2 && geneSequence[0] != 0)
				{	
					String alignment = nucSub(str);
					String[] parts = alignment.split("\\s+");
					String formatStr = "%-50s %s%n";
					writer.write(String.format(formatStr, parts[0], parts[1].substring(geneSequence[0]-1, geneSequence[1]).replaceAll("\\?", "-")));
				}
				else if (str.split("\\s+").length == 2 && geneSequence[0] == 0)
				{
					String alignment = nucSub(str);
					String[] parts = alignment.split("\\s+");
					String formatStr = "%-50s %s%n";
					writer.write(String.format(formatStr, parts[0], parts[1]));
				}
				else if(str.split("\'\\s+").length == 2 && geneSequence[0] != 0)
				{	
					String alignment = nucSub(str);
					String[] parts = alignment.split("\'\\s+");
					String formatStr = "%-50s %s%n";
					writer.write(String.format(formatStr, parts[0].replaceAll("\\s+", ""), parts[1].substring(geneSequence[0]-1, geneSequence[1]).replaceAll("\\?", "-")));
				}
				else if (str.split("\'\\s+").length == 2 && geneSequence[0] == 0)
				{
					String alignment = nucSub(str);
					String[] parts = alignment.split("\'\\s+");
					String formatStr = "%-50s %s%n";
					writer.write(String.format(formatStr, parts[0].replaceAll("\\s+", ""), parts[1]));
				}
				else
					writer.write(str+"\n");
			} 
			else
				continue;
		}

		writer.close();
	}
	
	// Checks to see if gene name is found
	private boolean geneNameFound(String s)
	{
		String lowerCaseAndSubGene;
		for (int i = 0; i < gene.length; i++)
		{	
			/*if (gene.length == 1)													// JMB commented out
				lowerCaseAndSubGene = gene[i].substring(1,gene[i].length()).trim();    
			else if (i == gene.length-1)
				lowerCaseAndSubGene = gene[i].substring(1,gene[i].length()-1).trim();
			else
				lowerCaseAndSubGene = gene[i].substring(1,gene[i].length()).trim();
			
			if (s.toLowerCase().indexOf(lowerCaseAndSubGene.toLowerCase()) > -1)	
				return true;
			else
				continue;*/
				
			if (s.toLowerCase().indexOf(gene[i].toLowerCase()) > -1)				// JMB
				return true;														// JMB
			else																	// JMB
				continue;															// JMB
		}
		return false;
	}

	// Looks to see if name of relevant character set is found 						// JMB
	private boolean charSetNameFound(String s, String charSetName)					// JMB
	{
		if (s.indexOf(charSetName) > -1)											// JMB
			return true;															// JMB
		else																		// JMB
			return false;															// JMB
	}

	// Looks for end of taxa block and then dna matrix
	private boolean fileStop(String s)
	{
		 return s.toUpperCase().indexOf("END;") > -1;
	}
	
	// Substitutes for ambiguous nucleotides (e.g., {AG} = R)
	private String nucSub(String line)
	{
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("AG", "R");
		tokens.put("CT", "Y");
		tokens.put("AC", "M");
		tokens.put("GT", "K");
		tokens.put("CG", "S");
		tokens.put("AT", "W");
		tokens.put("ACT", "H");
		tokens.put("CGT", "B");
		tokens.put("ACG", "V");
		tokens.put("AGT", "D");
		tokens.put("ACGT", "N");

		//String patternString = "\\{(" + StringUtils.join(tokens.keySet(), "|") + ")\\}";
		String patternString = "\\{(AG|CT|AC|GT|CG|AT|ACT|CGT|ACG|AGT|ACGT)\\}";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(line);
		
		StringBuffer sb = new StringBuffer();
		
		while(matcher.find())
		{
			matcher.appendReplacement(sb, tokens.get(matcher.group(1)));
		}
		matcher.appendTail(sb);
		
		return sb.toString();
	}
	
	// Checks to see if: (1) there is a data matrix, and (3) taxon number meets requirements
	// Deletes file if either scenario is violated
	private void isGeneThere(int num, int taxaNum, int maxTaxNum)
	{
		try
		{
			// String infile = "nexus"+num+".nex";
			String infile = url.replace("?format=nexus","")+"_"+gene[geneIndex]+".nex";

			FileInputStream file = new FileInputStream(infile);
			DataInputStream in = new DataInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line;
			boolean matrixFlag = false;
			boolean taxNumFlag = true;
			
			String patternString = ".*MATRIX.*";
			Pattern pattern = Pattern.compile(patternString);
			
			while((line = br.readLine()) != null)
			{
				Matcher matcher = pattern.matcher(line);
				if(matcher.matches())
				{
					matrixFlag = true;
				}
			}
			
			if (taxaNum > numberTaxa || numberTaxa > maxTaxNum)
				taxNumFlag = false;
			
			br.close();
			
			if (!matrixFlag || !taxNumFlag)
			{
				File file1 = new File(url.replace("?format=nexus","")+"_"+gene[geneIndex]+".nex");
				delGeneNames.add(url.replace("?format=nexus","")+"_"+gene[geneIndex]+".nex");
				file1.delete();
				fileNames.remove( url.replace("?format=nexus","")+"_"+gene[geneIndex]+".nex" );
			}
		}
		catch(IOException e) 
		{}
	}
}