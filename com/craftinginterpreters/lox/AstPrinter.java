package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Ternary;
import com.craftinginterpreters.lox.Expr.Unary;

public class AstPrinter implements Expr.Visitor<String> {
	
	public String print(Expr expr) {
		return expr.accept(this);
	}
	
	@Override
	public String visitTernaryExpr(Ternary expr) {
		return parenthesize("ternary", expr.comparison, expr.truthy, expr.falsey);
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}
	
	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesize("group", expr.expression);
	}
	
	@Override
	public String visitLiteralExpr(Literal expr) {
		if(expr.value == null) {
			return "nil";
		}
		return expr.value.toString();
	}
	
	@Override
	public String visitUnaryExpr(Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}
	
	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("(");
		builder.append(name);
		for(Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this)); // recursively call AstPrinter on non-terminal expressions
		}
		builder.append(")");
		
		return builder.toString();
	}
}
