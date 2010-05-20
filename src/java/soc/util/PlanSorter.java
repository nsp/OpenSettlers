/**
*	This class is a Vector wrapper class which is used for sorting the plan of actions
*/

package soc.util;

import java.util.*;

public class PlanSorter implements Comparator {
	
	public int compare(Object obj1, Object obj2) {
		
		Vector temp1 = (Vector)obj1;
		Vector temp2 = (Vector)obj2;
		
		Double num1 = (Double)temp1.get(2);
		Double num2 = (Double)temp2.get(2);
		
		if(num1.doubleValue() < num2.doubleValue())
			return -1;
		else if(num1.doubleValue() > num2.doubleValue())
			return 1;
		else if(num1.doubleValue() == num2.doubleValue())
			return 0;
		
		return Integer.MAX_VALUE;
		
		}
	}
	