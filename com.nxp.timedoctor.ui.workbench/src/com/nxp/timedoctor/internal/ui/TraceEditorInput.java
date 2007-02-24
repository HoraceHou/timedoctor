/*******************************************************************************
 * Copyright (c) 2006 Royal Philips Electronics NV.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Royal Philips Electronics NV. - initial API and implementation
 *******************************************************************************/
package com.nxp.timedoctor.internal.ui;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;

public class TraceEditorInput implements IPathEditorInput {
	private File file;

	public TraceEditorInput(final File file) {
		this.file = file;
	}
	
	public TraceEditorInput(final String name, final String path) {
		this.file = new File(path + File.separator + name);
	}

	public boolean exists() {
		return file.exists();
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	
	public String getName() {
		return file.getName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return file.getAbsolutePath();
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(final Class adapter) {
		return null;
	}

	/**
	 * Used for checking if an editor for this file is already open
	 */
	@Override
	public boolean equals(final Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (!(obj instanceof TraceEditorInput)) {
			return false;
		}
		TraceEditorInput other = (TraceEditorInput) obj;
		return file.getAbsolutePath().equals(other.file.getAbsolutePath());
	}

	public IPath getPath() {
		return new Path(file.getAbsolutePath());
	}
}
