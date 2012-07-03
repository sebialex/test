import java.util.List;

import org.apache.zookeeper.KeeperException.Code;


/**
 * @author Alexandru Stanisor
 *
 */
public class ZKOpResponse {
	
	private Code code = null;
	
	private String newPath = null;
	
	private List<String> children = null;
	
	private String data = null;
	
	
	public ZKOpResponse(Code code) {
		this.code = code;	
	}
	
	public ZKOpResponse(Code code, String newPath) {
		this.code = code;
		this.newPath = newPath;
	}
	
	public ZKOpResponse(Code code, List<String> children) {
		this.code = code;
		this.children = children;
	}
	
	public ZKOpResponse(String data, Code code) {
		this.code = code;
		this.data = data;
	}
	
	public String printString(){
		String s_code = "NULL";
		String s_newPath = "NULL";
		StringBuffer s_children = new StringBuffer();
		
		String s_data = "NULL";
		
		if (this.code != null) {
			s_code = this.code.toString();
		}
		
		if (this.newPath != null) {
			s_newPath = this.newPath;
		}
		
		if (this.children != null) {
			s_children.append("|"); 
			for (String child : this.children) {
				s_children.append(child); 
				s_children.append("|"); 
			}
		}
		else {
			s_children.append("NULL");
		}
		
		if (this.data != null) {
			s_data = this.data;
		}
		
		return ("ZKOpResponse=Code:" + s_code + ", newPath:" + s_newPath + ", children:" + s_children + ", data:" + s_data);
	}
	
	/**
	 * @return the code
	 */
	public Code getCode() {
		return code;
	}
	/**
	 * @param code the code to set
	 */
	public void setCode(Code code) {
		this.code = code;
	}
	/**
	 * @return the newPath
	 */
	public String getNewPath() {
		return newPath;
	}
	/**
	 * @param newPath the newPath to set
	 */
	public void setNewPath(String newPath) {
		this.newPath = newPath;
	}

	/**
	 * @return the children
	 */
	public List<String> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(List<String> children) {
		this.children = children;
	}

	/**
	 * @return the data
	 */
	public String getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(String data) {
		this.data = data;
	}
	
}
