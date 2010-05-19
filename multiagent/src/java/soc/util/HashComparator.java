package soc.util;

import java.util.Comparator;
import java.util.Map;

public class HashComparator implements Comparator{

	public int compare(Object obj1, Object obj2){

	int result=0;
	
	Map.Entry e1 = (Map.Entry)obj1 ;

	Map.Entry e2 = (Map.Entry)obj2 ;//Sort based on values.

	Integer value1 = (Integer)e1.getValue();
	Integer value2 = (Integer)e2.getValue();

	if(value1.compareTo(value2)==0){

	Integer hex1=(Integer)e1.getKey();
	Integer hex2=(Integer)e2.getKey();

//	Sort keys in order
	result=hex1.compareTo(hex2);

	} else{
//	Sort values in a descending order
	result=value2.compareTo( value1 );
	}

	return result;
	}

}
