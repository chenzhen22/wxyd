package com.ghbank.ifp.data;

public class SessionNameElement extends SessionElement {
	public String name;

	public SessionNameElement() {
	}

	public SessionNameElement(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
}