package org.erlide.core.preferences;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public final class PathSerializer {

	public static final String SEP = ";";

	public static String packList(final Iterable<IPath> list) {
		final StringBuilder result = new StringBuilder();
		for (final IPath s : list) {
			result.append(s.toPortableString()).append(SEP);
		}
		return result.toString();
	}

	public static List<IPath> unpackList(final String string) {
		return unpackList(string, SEP);
	}

	public static String packArray(final IPath[] strs) {
		final StringBuilder result = new StringBuilder();
		for (final IPath s : strs) {
			result.append(s.toPortableString()).append(SEP);
		}
		return result.toString();
	}

	public static IPath[] unpackArray(final String str) {
		return unpackList(str).toArray(new IPath[0]);
	}

	public static List<String> readFile(final String file) {
		final List<String> res = new ArrayList<String>();
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(
					file));
			String line;
			while ((line = reader.readLine()) != null) {
				res.add(line);
			}
		} catch (final IOException e) {
		}
		return res;
	}

	private PathSerializer() {
	}

	public static List<IPath> unpackList(final String string, final String sep) {
		final String[] v = string.split(sep);
		final List<String> sresult = new ArrayList<String>(Arrays.asList(v));
		final List<IPath> result = new ArrayList<IPath>();
		for (String s : sresult) {
			result.add(new Path(s));
		}
		return result;
	}

}
