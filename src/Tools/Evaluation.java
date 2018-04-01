package Tools;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Evaluation {
	int numOfRulesGold=0;
	int numOfRulesMatcher=0;
	int numOfRulesCorrect=0;
	public Evaluation(ArrayList<String> mapping, ArrayList<String> reference) { 
		ArrayList<String> correct=new ArrayList<String>();
		ArrayList<String> revised=new ArrayList<String>();
		for (String r : reference) 
		{
			String rParts[]=r.split(",");
			for (String m : mapping) 
			{
				String mParts[]=m.split(",");
				//Revised Ҳ��Ϊ����ȷ��
				if (rParts[0].equals(mParts[0])&& rParts[1].equals(mParts[1])) 
				{
					correct.add(r);
				}
				else if(rParts[0].equals(mParts[1])&& rParts[1].equals(mParts[0]))			
				{
					correct.add(r);
				}		
				else if(rParts[0].equals(mParts[1])&& rParts[1].equals(mParts[0])&&mParts[2].equals("?"))			
				{
					revised.add(r);
				}	
				else if (rParts[0].equals(mParts[0])&& rParts[1].equals(mParts[1])&&mParts[2].equals("?")) 
				{
					revised.add(r);
				}
		
			}
		}		
		this.numOfRulesGold = reference.size();		
		this.numOfRulesMatcher = mapping.size()-revised.size();
		this.numOfRulesCorrect = correct.size();
		
	}	
	
	public String toShortDesc() {
		double precision = this.getPrecision();
		double recall = this.getRecall();
		double f = this.getFMeasure();

		return toDecimalFormat(precision) + "  " + toDecimalFormat(recall) + "  " + toDecimalFormat(f);
	}
	
	public double getPrecision() {
		return (double)this.numOfRulesCorrect /  (double)this.numOfRulesMatcher;
	}
	
	public double getRecall() {
		return (double)this.numOfRulesCorrect /  (double)this.numOfRulesGold;
	}
	
	public double getFMeasure() {
		if ((this.getPrecision() == 0.0f) || (this.getRecall() == 0.0f)) { return 0.0f; }
		return (2 * this.getPrecision() * this.getRecall()) / (this.getPrecision() + this.getRecall());
	}
	
	public int getMatcherAlignment() {
		return numOfRulesMatcher;
	}
	
	public int getCorrectAlignment() {
		return numOfRulesCorrect;
	}
	
	
	private static String toDecimalFormat(double precision) {
		DecimalFormat df = new DecimalFormat("0.000");
		return df.format(precision).replace(',', '.');
	}
		
}
