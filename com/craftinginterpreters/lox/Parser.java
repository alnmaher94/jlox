package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

public class Parser {
	
	private static class ParseError extends RuntimeException{}
	private final boolean isREPL;
	private List<Token> tokens;
	private int current = 0;
	Parser(List<Token> tokens, boolean isREPL){
		this.tokens = tokens;
		this.isREPL = isREPL;
	}
	
	Parser(List<Token> tokens){
		this.tokens = tokens;
		this.isREPL = false;
	}
	
	// program -> declaration* EOF
	public List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while(!isAtEnd()) {
			statements.add(declaration());
		}
		return statements;
	}
	
	// declaration -> "var" IDENTIFIER ("=" expression)? | statement
	private Stmt declaration() {
		try {
			if(match(TokenType.VAR)) {
				return varDeclaration();
			}
			
			return statement();
		} catch(ParseError error) {
			synchronize();
			return null;
		}

	}
	
	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect variable name");
		Expr intializer = null;
		if(match(TokenType.EQUAL)) {
			intializer = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Var(name, intializer);
	}
	
	// statement -> exprStmt | printStmt | block
	private Stmt statement() {
		if(match(TokenType.PRINT)) {
			return printStatement();
		}
		if(match(TokenType.LEFT_BRACE)) {
			return new Stmt.Block(block());
		}
		
		return expressionStatement();
	}
	
	// printStmt -> "print" expression ";"
	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}
	
	//block -> "{" declarations* "}"
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();
		
		while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		
		consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}
	
	// exprStmt -> expression ";"
	private Stmt expressionStatement() {
		Expr value = expression();
		// Allow expressions without semicolons in REPL
		if(!this.isREPL) {
			consume(TokenType.SEMICOLON, "Expect ';' after value.");
		} else {
			match(TokenType.SEMICOLON);
		}
		return new Stmt.Expression(value);
	}
	
	//expression -> equality
	private Expr expression() {
		return assignment();
	}
	
	private Expr assignment() {
		Expr expr = equality();

		if( match(TokenType.EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			if(expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name, value);
			}
			error(equals, "Invalid assignment target");
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
		
		if(match(TokenType.IDENTIFIER)) {
			return new Expr.Variable(previous());
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
