package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();
	
	Environment() {
		this.enclosing = null;
	}
	
	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}
	
	void define(String name, Object value) {
		values.put(name, value);
	}
	
	Object get(Token name) {
		if(values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}

		// Check parent scope for variable
		if(this.enclosing != null) {
			return this.enclosing.get(name);
		}

		throw new RuntimeError(name, "Undefined variable'" + name.lexeme +"'.");
	}
	
	void assign(Token name, Object value) {
		if(values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		
		// if variable doesn't exist check parent scope
		if(this.enclosing != null) {
			this.enclosing.assign(name, value);
			return;
		}
		
		throw new RuntimeError(name, "Undefined variable'" + name.lexeme +"'.");
	}
}
