//#**************************************************************************
//#
//#    Copyright (C) 2004-2006  Wolfram Diestel
//#
//#    This program is free software; you can redistribute it and/or modify
//#    it under the terms of the GNU General Public License as published by
//#    the Free Software Foundation; either version 2 of the License, or
//#    (at your option) any later version.
//#
//#    This program is distributed in the hope that it will be useful,
//#    but WITHOUT ANY WARRANTY; without even the implied warranty of
//#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//#    GNU General Public License for more details.
//#
//#    You should have received a copy of the GNU General Public License
//#    along with this program; if not, write to the Free Software
//#    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//#
//#    Send comments and bug fixes to diestel@steloj.de
//#
//#**************************************************************************

package net.katsstuff.arbaro.mesh;

import net.katsstuff.arbaro.transformation.Vector;

/**
 * A class holding a point and a normal vector
 *
 * @author Wolfram Diestel
 */
public final class Vertex {

	public Vector point;
	public Vector normal;
	public UVVector uv; // uv-coordinate

	Vertex(Vector pt, Vector norm, UVVector uv_) {
		point = pt;
		normal = norm;
		uv = uv_;
	}
}