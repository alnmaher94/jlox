package com.craftinginterpreters.lox;

import java.util.List;

import com.craftinginterpreters.lox.Expr.Assign;
import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Unary;
import com.craftinginterpreters.lox.Expr.Variable;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
	private Environment environment = new Environment();
	private final boolean isREPL;
	
	Interpreter() {
		this.isREPL = false;
	}
	
	Interpreter(boolean isREPL) {
		this.isREPL = isREPL;
	}

	public void interpret(List<Stmt> statements) {
		try {
			for(Stmt statement : statements) {
				execute(statement);
			}
		} catch(RuntimeError error) {
			Lox.runtimeError(error);
		}
		
	}
	
	// Stmts
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		Object value = evaluate(stmt.expression);
		if(this.isREPL) {
			System.out.println(stringify(value));
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}
	
	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(this.environment));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if(stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		environment.define(stmt.name.lexeme, value);
		return null;
	}
	
	private void execute(Stmt stmt) {
		stmt.accept(this);
	}
	
	private void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;
			for(Stmt stmt: statements) {
				execute(stmt);
			}
		} finally {
			this.environment = previous;
		}
	}

	// Expressions
	@Override
	public Object visitBinaryExpr(Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);
		
		switch(expr.operator.type) {
			case GREATER:
				checkNumberOperands(expr.operator, left, right);
				return (double)left > (double)right;
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double)left >= (double)right;
			case LESS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left < (double)right;
			case LESS_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double)left <= (double)right;
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			case MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left - (double)right;
			case PLUS:
				if (left instanceof Double && right instanceof Double) {
					return (double)left + (double)right;	
				}
				if(left instanceof String && right instanceof String) {
					return (String)left + (String)right;
				}
				if(left instanceof String && right instanceof Double) {
					return (String)left + stringify(right);
				}
				if(left instanceof Double && right instanceof String) {
					return stringify(left) + (String)right;
				}
				throw new RuntimeError(expr.operator, "Operands must be two numbers of two strings");
			case SLASH:
				checkNumberOperands(expr.operator, left, right);
				if((double) right == 0) {
					throw new RuntimeError(expr.operator, "Cannot divide by 0");
				}
				return (double)left / (double)right;
			case EQUAL:
				Expr.Variable variable = (Expr.Variable)expr.left;
				this.environment.assign(variable.name, right);
			case STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double)left * (double)right;
			default:
				return null;
		}
	}

	@Override
	public Object visitGroupingExpr(Grouping expr) {
		// TODO Auto-generated method stub
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitUnaryExpr(Unary expr) {
		Object right = evaluate(expr.right);
		switch(expr.operator.type) {
			case BANG:
				return !isTruthy(right);
			case MINUS:
				checkNumberOperand(expr.operator, right);
				return -(double)right;
			default:
				// unreachable
				return null;
		}
	}
	
	@Override
	public Object visitVariableExpr(Variable expr) {
		return environment.get(expr.name);
	}
	
	@Override
	public Object visitAssignExpr(Assign expr) {
		Object value = evaluate(expr.value);
		environment.assign(expr.name, value);
		return value;
	}
	
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}
	
	/*
	 * Null and false return false,
	 * everything else is true
	 */
	private boolean isTruthy(Object obj) {
		if(obj == null) {
			return false;
		}
		if(obj instanceof Boolean) {
			return (boolean)obj;
		}
		return true;
	}
	
	private boolean isEqual(Object a, Object b) {
		if(a == null && b == null) {
			return true;
		}
		if(a == null) {
			return false;
		}
		return a.equals(b);
	}
	
	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operand must be a number.");
	}
	
	private void checkNumberOperands(Token operator, Object left, Object right) {
		if(left instanceof Double && right instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operands must be numbers.");
	}
	
	private String stringify(Object obj) {
		if(obj == null) {
			return "nil";
		}
		if(obj instanceof Double) {
			String text = obj.toString();
			if(text.endsWith(".0")) {
				return text.substring(0, text.length() - 2);
			}
		}
		
		return obj.toString();
	}

}
