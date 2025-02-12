//  #**************************************************************************
//  #
//  #    Copyright (C) 2003-2006  Wolfram Diestel
//  #
//  #    This program is free software; you can redistribute it and/or modify
//  #    it under the terms of the GNU General Public License as published by
//  #    the Free Software Foundation; either version 2 of the License, or
//  #    (at your option) any later version.
//  #
//  #    This program is distributed in the hope that it will be useful,
//  #    but WITHOUT ANY WARRANTY; without even the implied warranty of
//  #    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  #    GNU General Public License for more details.
//  #
//  #    You should have received a copy of the GNU General Public License
//  #    along with this program; if not, write to the Free Software
//  #    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  #
//  #    Send comments and bug fixes to diestel@steloj.de
//  #
//  #**************************************************************************/

package net.katsstuff.arbaro.params;

public class IntParam extends AbstractParam {

	private final int min;
	private final int max;
	private final int deflt;
	private int value;

	IntParam(
		String nam, int mn, int mx, int def, String grp, int lev,
		int ord, String sh, String lng
	) {
		super(nam, grp, lev, ord, sh, lng);
		min = mn;
		max = mx;
		deflt = def;
		value = Integer.MIN_VALUE;
	}

	public String getDefaultValue() {
		return Integer.toString(deflt);
	}

	public void clear() {
		value = Integer.MIN_VALUE;
		fireStateChanged();
	}

	public void setValue(String val) throws ParamException {
		int i;
		try {
			i = Integer.parseInt(val);
		} catch (NumberFormatException e) {
			throw new ParamException("Error setting value of " + name + ". \"" + val
									 + "\" isn't a valid integer.");
		}

		if (i < min) {
			throw new ParamException("Value of " + name + " should be greater or equal to " + min);
		}

		if (i > max) {
			throw new ParamException("Value of " + name + " should be greater or equal to " + max);
		}

		value = i;
		fireStateChanged();
	}

	public String getValue() {
		return Integer.toString(value);
	}

	public boolean empty() {
		return value == Integer.MIN_VALUE;
	}

	public int intValue() {
		if (empty()) {
			warn(name + " not given, using default value(" + deflt + ")");
			// set value to default, i.e. don't warn again
			value = deflt;
			fireStateChanged();
		}
		return value;
	}

	public String getLongDesc() {
		String desc = super.getLongDesc();
		desc += "<br><br>";
		if (min != Integer.MIN_VALUE) {
			desc += "Minimum: " + min + "\n";
		}
		if (max != Integer.MAX_VALUE) {
			desc += "Maximum: " + max + "\n";
		}
		if (deflt != Integer.MIN_VALUE) {
			desc += "Default: " + deflt + "\n";
		}
		return desc;
	}
}












