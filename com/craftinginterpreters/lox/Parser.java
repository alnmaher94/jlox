package com.craftinginterpreters.lox;

import java.util.List;

public class Parser {
	
	private static class ParseError extends RuntimeException{}

	private List<Token> tokens;
	private int current = 0;
	Parser(List<Token> tokens){
		this.tokens = tokens;
	}
	
	public Expr parse() {
		try {
			return expression();
		} catch(ParseError e) {
			return null;
		}
	}
	
	//expression -> equality
	private Expr expression() {
		return ternary();
	}
	
	// equality -> comparison ((== | !=) comparison)*
	private Expr ternary() {
		Expr expr = equality();
		while(match(TokenType.QUESTION_MARK)) {
			Expr truthy = equality();
			consume(TokenType.COLON, "Expect ':' after expression.");
			Expr falsey = equality();
			return new Expr.Ternary(expr, truthy, falsey);
		}
		return expr;
	}

	// equality -> comparison ((== | !=) comparison)*
	private Expr equality() {
		Expr expr = comparison();
		while(match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}
	
	// comparison -> term ((> | < | <= | >=) term)*
	private Expr comparison() {
		Expr expr = term();
		while(match(TokenType.GREATER, TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	// term -> factor ((+ | -) factor)*
	private Expr term() {
		Expr expr = factor();
		while(match(TokenType.PLUS, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	// factor -> unary ((* | /) unary)*
	private Expr factor() {
		Expr expr = unary();
		while(match(TokenType.STAR, TokenType.SLASH)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	// unary -> (! | -) unary | primary
	private Expr unary() {
		while(match(TokenType.BANG, TokenType.MINUS)) {
			return new Expr.Unary(previous(), unary());
		}
		
		return primary();
	}
	
	// primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
	private Expr primary() {
		if(match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().literal);
		}
		
		if(match(TokenType.TRUE)) {
			return new Expr.Literal(TokenType.TRUE);
		}
		
		if(match(TokenType.FALSE)) {
			return new Expr.Literal(TokenType.FALSE);
		}
		
		if(match(TokenType.NIL)) {
			return new Expr.Literal(TokenType.NIL);
		}
		
		if(match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		
		throw error(peek(), "Expect expression.");
	}
	
	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if(check(type)) {
				advance();
				return true;
			}
		}
		
		return false;
	}
	
	private boolean check(TokenType type) {
		if(isAtEnd()) {
			return false;
		}
		
		return peek().type == type;
	}
	
	private Token advance() {
		if(!isAtEnd()) {
			current++;
		}
		return previous();
	}
	
	private boolean isAtEnd() {
		return peek().type == TokenType.EOF;
	}
	
	private Token peek() {
		return this.tokens.get(this.current);
	}
	
	private Token previous() {
		return this.tokens.get(this.current - 1);
	}
	
	private Token consume(TokenType type, String message) {
		if(check(type)) {
			return advance();
		}
		
		throw error(peek(), message);
	}
	
	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}
	
	@SuppressWarnings("incomplete-switch")
	private void synchronize() {
		advance();
		while(!isAtEnd()) {
			if(previous().type == TokenType.SEMICOLON) {
				return;
			}
			
			switch(peek().type) {
				case CLASS: case FOR: case FUN: case IF: case PRINT:
				case RETURN: case VAR: case WHILE:
					return;
			}
		}
		
		advance();
	}

}
