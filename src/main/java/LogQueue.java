package main.java;

public class LogQueue {
	
	private String [] array;
    
    int start;
    int end;
    int capacity;
   
    public LogQueue(int capacity) {
        array = new String[capacity];
        this.capacity = capacity;
        start = 0;
        end = 0 ;
    }
   
    public void add(String object) {
    	   	
        if( end == capacity ) {
        	array[start] = object;
        	start = (start+1) % capacity;
        } else {
             array[end++] = object;
        }
    }
   
    public String get(int i) {
    	if (i > end)
    		throw new IndexOutOfBoundsException("Index " + i + " out of bounds.");
    	
        if( end == capacity ) {
            //offset i using start
            return array[(i + start) % capacity];
        } else
            return array[i];
    }

    public String[] getAll() {
    	int size = end < capacity ? end : capacity; 
    	final String[] out = new String[size];
    	for (int i = 0; i < size; i++)
    		out[i] = array[(i + start) % capacity];
    	return out;
    }
}
