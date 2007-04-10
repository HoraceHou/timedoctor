/*******************************************************************************
 * Copyright (c) 2007 NXP Semiconductors B.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NXP Semiconductors B.V. - initial API and implementation
 *******************************************************************************/
package com.nxp.timedoctor.ui.statistics;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;

import com.nxp.timedoctor.core.model.SampleLine;
import com.nxp.timedoctor.core.model.TraceModel;
import com.nxp.timedoctor.core.model.ZoomModel;
import com.nxp.timedoctor.core.model.statistics.StatisticsTimeModel;
import com.nxp.timedoctor.core.model.statistics.TraceStatistic;

public class TraceStatViewer implements Observer, IStatisticsViewPage {
	private ZoomModel zoomModel;
	private StatisticsTimeModel timeModel;
	
	private TraceStatTableViewer tableViewer;
	private TraceStatistic traceStat;
	
	private Composite topComposite;
	
	/**
	 * The constructor.
	 */
	public TraceStatViewer() {		
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable o, Object arg) {
		if (o instanceof ZoomModel) {
			timeModel.setTimes(zoomModel.getStartTime(), zoomModel.getEndTime());
		} else {
			// Show statistics for the current zoom range
			traceStat.calculate(timeModel.getStartTime(), timeModel.getEndTime());
			tableViewer.refresh();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.nxp.timedoctor.ui.statistics.IStatisticsViewPage#setModels(com.nxp.timedoctor.core.model.ZoomModel, com.nxp.timedoctor.core.model.TraceModel)
	 */
	public void setModels(final ZoomModel zoomModel, final TraceModel traceModel) {
		this.zoomModel = zoomModel;		
		this.timeModel = new StatisticsTimeModel();
		
		traceStat = new TraceStatistic(traceModel);
		
		this.zoomModel.addObserver(this);
		this.timeModel.addObserver(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		topComposite = new Composite(parent, SWT.NONE);
		
		GridLayout parentLayout = new GridLayout(1, false);
		parentLayout.marginHeight = 0;
		parentLayout.marginWidth = 0;
		parentLayout.verticalSpacing = 0;
		topComposite.setLayout(parentLayout);
		
		StatTimeViewer timeViewer = new StatTimeViewer(topComposite, timeModel);
		timeViewer.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
		
		tableViewer = new TraceStatTableViewer(topComposite);
		tableViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));		
		tableViewer.setInput(traceStat);
		
		timeModel.setTimes(zoomModel.getStartTime(), zoomModel.getEndTime());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#getControl()
	 */
	public Control getControl() {
		return topComposite;
	}

	/* (non-Javadoc)
	 * @see com.nxp.timedoctor.ui.statistics.IStatisticsViewPage#selectLine(com.nxp.timedoctor.core.model.SampleLine)
	 */
	public void selectLine(SampleLine line) {
		//Do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setActionBars(org.eclipse.ui.IActionBars)
	 */
	public void setActionBars(IActionBars actionBars) {
		//TODO: button to select between seconds, cycles, or %
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setFocus()
	 */
	public void setFocus() {
		//Do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#dispose()
	 */
	public void dispose() {
		zoomModel.deleteObserver(this);
		timeModel.deleteObserver(this);
	}
}
