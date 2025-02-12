// #**************************************************************************
// #
// #    Copyright (C) 2008  Moritz Moeller
// #
// #    This program is free software; you can redistribute it and/or modify
// #    it under the terms of the GNU General Public License as published by
// #    the Free Software Foundation; either version 2 of the License, or
// #    (at your option) any later version.
// #
// #    This program is distributed in the hope that it will be useful,
// #    but WITHOUT ANY WARRANTY; without even the implied warranty of
// #    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// #    GNU General Public License for more details.
// #
// #    You should have received a copy of the GNU General Public License
// #    along with this program; if not, write to the Free Software
// #    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
// #
// #    Send comments and bug fixes to arbaro@virtualritz.com or
// #    diestel@steloj.de
// #
// #**************************************************************************/

package net.katsstuff.arbaro.export;

/**
 * Helps with hierachically indenting text strings
 */
class StringIndenter {

	String indent;
	int indentLevel = 0;

	public StringIndenter(String indent) {
		this.indent = indent;
	}

	public void increase() {
		++indentLevel;
	}

	public void decrease() {
		if (0 < indentLevel) {
			--indentLevel;
		}
	}

	public String getIndent() {
		return String.valueOf(indent).repeat(Math.max(0, indentLevel));
	}
}
