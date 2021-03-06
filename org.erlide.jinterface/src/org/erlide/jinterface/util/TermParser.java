/*******************************************************************************
 * Copyright (c) 2008 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.jinterface.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangException;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpFormatPlaceholder;
import com.ericsson.otp.erlang.OtpPatternVariable;

public class TermParser {

	private TermParser() {
	}

	private static Map<String, OtpErlangObject> cache = new HashMap<String, OtpErlangObject>();

	public static OtpErlangObject parse(final String s) throws ParserException {
		OtpErlangObject value = cache.get(s);
		if (value == null) {
			value = parse(scan(s));
			cache.put(s, value);
		}
		return value;
	}

	private static OtpErlangObject parse(final List<Token> tokens)
			throws ParserException {
		if (tokens.size() == 0) {
			return null;
		}
		OtpErlangObject result = null;
		final Token t = tokens.remove(0);
		switch (t.kind) {
		case ATOM:
			result = new OtpErlangAtom(t.text);
			break;
		case VARIABLE:
			result = new OtpPatternVariable(t.text);
			break;
		case STRING:
			result = new OtpErlangString(t.text);
			break;
		case INTEGER:
			result = new OtpErlangLong(Long.parseLong(t.text));
			break;
		case PLACEHOLDER:
			result = new OtpFormatPlaceholder(t.text);
			break;
		case TUPLESTART:
			result = parseTuple(tokens, new Stack<OtpErlangObject>());
			break;
		case TUPLEEND:
			throw new ParserException("unexpected " + t.toString());
		case LISTSTART:
			result = parseList(tokens, new Stack<OtpErlangObject>(), null);
			break;
		case LISTEND:
			throw new ParserException("unexpected " + t.toString());
		case COMMA:
			throw new ParserException("unexpected " + t.toString());
		default:
			throw new ParserException("unknown token" + t.toString());
		}
		return result;
	}

	private static OtpErlangObject parseList(final List<Token> tokens,
			final Stack<OtpErlangObject> stack, OtpErlangObject tail)
			throws ParserException {
		if (tokens.size() == 0) {
			return null;
		}
		final Token t = tokens.get(0);
		if (t.kind == TokenKind.LISTEND) {
			tokens.remove(0);
			try {
				return new OtpErlangList(stack.toArray(new OtpErlangObject[0]),
						tail);
			} catch (final OtpErlangException e) {
				e.printStackTrace();
				// can't happen
				return null;
			}
		} else {
			OtpErlangObject atail = tail;
			if (t.kind == TokenKind.CONS) {
				tokens.remove(0);
				atail = parse(tokens);
			} else {
				stack.push(parse(tokens));
				if (tokens.get(0).kind == TokenKind.COMMA) {
					tokens.remove(0);
				}
			}
			return parseList(tokens, stack, atail);
		}
	}

	private static OtpErlangObject parseTuple(final List<Token> tokens,
			final Stack<OtpErlangObject> stack) throws ParserException {
		if (tokens.size() == 0) {
			return null;
		}
		final Token t = tokens.get(0);
		if (t.kind == TokenKind.TUPLEEND) {
			tokens.remove(0);
			return new OtpErlangTuple(stack.toArray(new OtpErlangObject[0]));
		} else {
			if (t.kind == TokenKind.CONS) {
				throw new ParserException("cons is invalid in tuple");
			} else {
				stack.push(parse(tokens));
				if (tokens.get(0).kind == TokenKind.COMMA) {
					tokens.remove(0);
				}
			}
			return parseTuple(tokens, stack);
		}
	}

	private static enum TokenKind {
		ATOM, VARIABLE, STRING, INTEGER, PLACEHOLDER, TUPLESTART, TUPLEEND, LISTSTART, LISTEND, COMMA, CONS, UNKNOWN;
	}

	private static class Token {

		TokenKind kind;
		int start;
		int end;
		String text;

		@Override
		public String toString() {
			return "<" + this.kind.toString() + ": !" + this.text + "!>";
		}

		public static Token nextToken(final String s) {
			if (s == null || s.length() == 0) {
				return null;
			}
			final Token result = new Token();
			char c;
			int i = 0;
			do {
				c = s.charAt(i++);
				if (i >= s.length()) {
					return null;
				}
			} while ((c == ' ' || c == '\t' || c == '\n' || c == '\r'));
			i--;

			result.start = i;
			result.end = i;
			if (c <= 'z' && c >= 'a') {
				result.kind = TokenKind.ATOM;
				while (result.end < s.length() && (c >= 'a' && c <= 'z')
						|| (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
						|| c == '_') {
					c = s.charAt(result.end++);
				}
				result.end--;
			} else if (c == '\'') {
				result.kind = TokenKind.ATOM;
				c = s.charAt(++result.end);
				// TODO add escape!
				while (result.end < s.length() && c != '\'') {
					c = s.charAt(result.end++);
				}
			} else if (c == '"') {
				result.kind = TokenKind.STRING;
				c = s.charAt(++result.end);
				// TODO add escape!
				while (result.end < s.length() && c != '"') {
					c = s.charAt(result.end++);
				}
			} else if ((c >= 'A' && c <= 'Z') || c == '_') {
				result.kind = TokenKind.VARIABLE;
				while (result.end < s.length() && (c >= 'a' && c <= 'z')
						|| (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
						|| c == '_' || c == ':') {
					c = s.charAt(result.end++);
				}
				result.end--;
			} else if (c <= '9' && c >= '0') {
				result.kind = TokenKind.INTEGER;
				while (result.end < s.length() && (c >= '0' && c <= '9')) {
					c = s.charAt(result.end++);
				}
				result.end--;
			} else if (c == '~') {
				result.kind = TokenKind.PLACEHOLDER;
				c = s.charAt(++result.end);
				while (result.end <= s.length()
						&& ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) {
					c = s.charAt(result.end++);
				}
				result.end--;
			} else if (c == '{') {
				result.kind = TokenKind.TUPLESTART;
				result.end = result.start + 1;
			} else if (c == '}') {
				result.kind = TokenKind.TUPLEEND;
				result.end = result.start + 1;
			} else if (c == '[') {
				result.kind = TokenKind.LISTSTART;
				result.end = result.start + 1;
			} else if (c == ']') {
				result.kind = TokenKind.LISTEND;
				result.end = result.start + 1;
			} else if (c == ',') {
				result.kind = TokenKind.COMMA;
				result.end = result.start + 1;
			} else if (c == '|') {
				result.kind = TokenKind.CONS;
				result.end = result.start + 1;
			} else {
				result.kind = TokenKind.UNKNOWN;
				result.end = result.start + 1;
			}
			result.text = s.substring(result.start, result.end);
			final char ch = result.text.charAt(0);
			if (ch == '~') {
				result.text = result.text.substring(1);
			} else if (ch == '"' || ch == '\'') {
				result.text = result.text
						.substring(1, result.text.length() - 1);
			}
			return result;
		}
	}

	private static List<Token> scan(String s) {
		String ss = s + " ";
		final List<Token> result = new ArrayList<Token>();
		Token t = Token.nextToken(ss);
		while (t != null) {
			result.add(t);
			ss = ss.substring(t.end);
			t = Token.nextToken(ss);
		}
		return result;
	}

}
