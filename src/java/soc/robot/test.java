
package soc.robot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class test {

public static void main(String[] args) {

//Create a hashtable
Hashtable myhash=new Hashtable();

//Put things in Hashtable
myhash.put(10, 2);
myhash.put(11, 2);
myhash.put(12, 3); 

//Put keys and values in to an arraylist using entryset
ArrayList myArrayList=new ArrayList(myhash.entrySet()); 

//Sort the values based on values first and then keys.
Collections.sort(myArrayList, new MyComparator()); 

//Show sorted results
Iterator itr=myArrayList.iterator();
String key="";
int value=0;
int cnt=0;
while(itr.hasNext()){ 

cnt++;
Map.Entry e=(Map.Entry)itr.next(); 

key = (String)e.getKey();
value = ((Integer)e.getValue()).intValue();

System.out.println(key+", "+value);
}

}

static class MyComparator implements Comparator{

public int compare(Object obj1, Object obj2){

int result=0;Map.Entry e1 = (Map.Entry)obj1 ;

Map.Entry e2 = (Map.Entry)obj2 ;//Sort based on values.

Integer value1 = (Integer)e1.getValue();
Integer value2 = (Integer)e2.getValue();

if(value1.compareTo(value2)==0){

Integer word1=(Integer)e1.getKey();
Integer word2=(Integer)e2.getKey();

//Sort String in an alphabetical order
result=word1.compareTo(word2);

} else{
//Sort values in a descending order
result=value2.compareTo( value1 );
}

return result;
}

}

}

