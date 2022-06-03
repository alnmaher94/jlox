package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
	
	static boolean hadError = false;
	static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage jlox [script]");
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}
	
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()), false);
		
		// Indicate error an exit
		if(hadError) {
			System.exit(65);
		}
		if(hadRuntimeError) {
			System.exit(70);
		}
	}
	
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		for(;;) { // infinite loop
			System.out.print("> ");
			String line = reader.readLine();
			if(line == null) {
				break;
			}
			run(line, true);
			hadError = false; // Reset flag in interactive loop
		}
	}
	
	private static void run(String source, boolean isREPL) {
		List<Token> tokens = new Scanner(source).scanTokens();
		List<Stmt> statments = new Parser(tokens, isREPL).parse();
		if(hadError) {
			return;
		}
		new Interpreter(isREPL).interpret(statments);
	}
	
	static void error(int line, String message) {
		report(line, "", message);
	}
	
	static void error(Token token, String message) {
		if(token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}
	
	static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() +
		"\n[line " + error.token.line + "]");
		hadRuntimeError = true;
		
	}
	
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}

}
