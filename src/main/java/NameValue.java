package main.java;

public class NameValue implements Comparable<NameValue> {
	
	public String id, name;
	
	public NameValue() {
		
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + " (" + id + ")";
	}
	
	@Override
	public int compareTo(NameValue o) {
		if (o == null)
			return 0;
		
		if (this.name == null && o.name == null)
			return 0;
		else if (o.name == null)
			return -1;
		else if (this.name == null)
			return 1;
		else
			return this.name.toLowerCase().compareTo(o.name.toLowerCase());
	}
}
