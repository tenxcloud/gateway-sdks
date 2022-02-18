package com.apigateway.sdk.model;

import java.io.Serializable;
import java.util.Arrays;

public class User implements Serializable {

	private static final long serialVersionUID = -2731598327208972274L;

	private Long id;
	private String name;
	private Integer age;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public static void main(String[] args) {
		String test = "test|12111yuyuyayayya";
		
		int index0 = test.indexOf("|",0);
		if (index0 == -1) {
			System.out.println(test);
		} else {
			String test1 = test.substring(0, test.indexOf("|"));
			System.out.println(test1);
			String test2 = test.substring((test.indexOf("|")+1),test.length());
			System.out.println(test2);
			int index1 = test2.indexOf("|",0);
			if(index1 == -1) {//只有一层
				System.out.println(test2);
			} else {
				String test3 = test2.substring(0, test2.indexOf("|"));
				System.out.println(test3);
				String test4 = test2.substring((test2.indexOf("|")+1),test2.length());
				System.out.println(test4);
			}
		}
//		String[] testStr = test.split("\\|");
//		int len = testStr.length;
//		if(len>=2) {
//			System.out.println(testStr[0]);
//			System.out.println(testStr[1]);
//			testStr.
//		} else if (len==2) {
//			System.out.println(testStr[0]);
//		} else if (len==1){
//			System.out.println(testStr[0]);
//		}
	}
}
