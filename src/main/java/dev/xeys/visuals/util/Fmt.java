package dev.xeys.visuals.util;

/** Allocation-free number formatting into reusable StringBuilders (HUD text updates every frame). */
public final class Fmt {
	private Fmt() {
	}

	public static StringBuilder fixed(StringBuilder out, double value, int decimals) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return out.append('-');
		}
		if (value < 0) {
			double scale = Math.pow(10, decimals);
			if (Math.round(-value * scale) != 0) {
				out.append('-');
			}
			value = -value;
		}
		long scale = 1;
		for (int i = 0; i < decimals; i++) {
			scale *= 10;
		}
		long scaled = Math.round(value * scale);
		out.append(scaled / scale);
		if (decimals > 0) {
			out.append('.');
			long frac = scaled % scale;
			for (long d = scale / 10; d > 0; d /= 10) {
				out.append((char) ('0' + (frac / d) % 10));
			}
		}
		return out;
	}

	/** Zero-padded two digits (clock fields). */
	public static StringBuilder pad2(StringBuilder out, int v) {
		if (v < 10) {
			out.append('0');
		}
		return out.append(v);
	}

	public static StringBuilder duration(StringBuilder out, long seconds) {
		long h = seconds / 3600;
		long m = (seconds / 60) % 60;
		long s = seconds % 60;
		if (h > 0) {
			out.append(h).append(':');
			pad2(out, (int) m);
		} else {
			out.append(m);
		}
		out.append(':');
		return pad2(out, (int) s);
	}

	public static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			case 6 -> "VI";
			case 7 -> "VII";
			case 8 -> "VIII";
			case 9 -> "IX";
			case 10 -> "X";
			default -> Integer.toString(n);
		};
	}
}
