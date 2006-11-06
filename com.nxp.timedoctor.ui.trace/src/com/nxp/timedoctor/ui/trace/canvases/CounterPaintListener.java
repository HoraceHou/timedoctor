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

package com.nxp.timedoctor.ui.trace.canvases;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.nxp.timedoctor.core.model.SampleLine;
import com.nxp.timedoctor.core.model.ZoomModel;

/**
 * Contains the code to paint a value,cycle or memory cycle.
 */
public class CounterPaintListener implements PaintListener {

	/**
	 * The color to use in painting the line.
	 */
	private Color color;

	/**
	 * The color to use in filling the line.
	 */
	private Color fillColor;

	/**
	 * <code>Observable</code> containing zoom and scroll data.
	 */
	private ZoomModel zoom;

	/**
	 * The line containing data to visualize.
	 */
	private SampleLine line;

	/**
	 * The minimum allowed x-value, for use in the <code>boundedInt</code>
	 * function.
	 */
	private static final int X_MIN = -100;

	/**
	 * The maximum allowed x-value, for use in the <code>boundedInt</code>
	 * function.
	 */
	private static final int X_MAX = 100000;

	/**
	 * Vertical padding value on the bottom of trace lines.
	 */
	private static final int VERTICAL_PADDING = 2;

	/**
	 * The starting time of the part of the line currently displayed, based on
	 * scroll data.
	 */
	private double timeOffset;

	/**
	 * Sapcing in pixels between grid lines.
	 */
	private static final int GRID_SPACING = 10;

	private double frequency;
	
	/**
	 * Constructs a new <code>CounterPaintListener</code> with the given
	 * color,filling color, sample line, and source of zoom/scroll data.
	 * 
	 * @param col
	 *            the color with which to paint the line
	 * @param fillCol
	 *            the color used to fill the line
	 * @param sampleLine
	 *            contains the data to be displayed
	 * @param zoomData
	 *            contains data on the zoom/scroll state of the system
	 */

	public CounterPaintListener(final Color col, final Color fillCol,
			final SampleLine sampleLine, final ZoomModel zoomData) {
		this.color = col;
		this.fillColor = fillCol;
		this.line = sampleLine;
		this.zoom = zoomData;
		
		if (line.getType() == SampleLine.LineType.MEM_CYCLES) {
			this.frequency = line.getCPU().getMemClocksPerSec();
		}
		else {
			this.frequency = line.getCPU().getClocksPerSec();
		}		
	}

	/**
	 * Sent when a paint event occurs for the control. Repaints the affected
	 * section of the line.
	 * 
	 * @param e
	 *            an event containing information about the paint
	 * 
	 * @see PaintListener#paintControl(PaintEvent)
	 */
	public final void paintControl(final PaintEvent e) {
		if (zoom.getStartTime() != zoom.getEndTime()) {
			Canvas canvas = ((Canvas) e.widget);
			Composite section = canvas.getParent();
			Composite rightPane = section.getParent();
			Composite scroll = rightPane.getParent();

			// guarantees trace drawing is unaffected by appearance of vertical
			// scrollbar.
			int fullWidth = scroll.getBounds().width;
			// TODO calculate height for proportional counters
			int fullHeight = canvas.getBounds().height - VERTICAL_PADDING;
			double startTime = zoom.getStartTime();
			double endTime = zoom.getEndTime();
			double pixelsPerSec = fullWidth / (endTime - startTime);
			double drawStartTime = startTime + (((double)e.x) / pixelsPerSec);
			double drawEndTime = drawStartTime + (((double)e.width) / pixelsPerSec);
			double maxFilling = line.getMaxSampleValue();
			
			e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WHITE));
			e.gc.fillRectangle(e.x, e.y, e.width, e.height);
			
			// Draw the bottom line
			e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_BLACK));
			e.gc.drawLine(e.x, fullHeight, e.x + e.width, fullHeight);
			
			drawGridLines(e, fullHeight);

			e.gc.setForeground(color);
			e.gc.setBackground(fillColor);

			int index = Math.max(1, line.binarySearch(drawStartTime));
			double curMaxFilling = 0;
			double curMinFilling = 0;
			for (; index < line.getCount(); index++) {
				int xCur = boundedInt((line.getSample(index - 1).time - startTime) * pixelsPerSec);
				int xNext = boundedInt((line.getSample(index).time - startTime) * pixelsPerSec);

				double timeDifference = line.getSample(index).time
					- line.getSample(index - 1).time;
				double valueDifference = line.getSample(index).val
					- line.getSample(index - 1).val;
				
				double curFilling;
				if (line.getType() == SampleLine.LineType.CYCLES
						|| line.getType() == SampleLine.LineType.MEM_CYCLES) {
					curFilling = valueDifference / (timeDifference * frequency);					
				} else {
					curFilling = valueDifference / maxFilling;
				}
				
				// Compute maximum and minimum over the last events that fall within the same pixel
				// Always at least include the previous event to ensure the contour line is
				// continuous (the event typically occurs somewhere within a pixel, 
				// not on the exact pixel boundary).
				curMaxFilling = Math.max(curMaxFilling, curFilling);
				curMinFilling = Math.min(curMinFilling, curFilling);
				
				if (xNext == xCur && index < line.getCount()) {
					continue;
				}
				
				// Get current filling in pixels
				int curFillHeight = 0;
				if (curFilling > 0) {
					// Show at least one pixel if there is something in the counter
					curFillHeight = Math.max(1, (int) (fullHeight * curFilling));
				}

				// Get min filling in pixels
				int minFillHeight = 0;
				if (curMinFilling > 0) {
					// Show at least one pixel if there is something in the counter
					minFillHeight = Math.max(1, (int) (fullHeight * curMinFilling));
				}

				// Get max buffer filling in pixels
				int maxFillHeight = (int) (fullHeight * curMaxFilling);

				// Note: fillRectangle is drawn (verified for MS Windows) from the left-upper origin
				// including the origin, up to (excluding) width, height
				// Note that the origin stays upper-left, even when height is negative
				// Lines are drawn including the start and end point
				// A line with the same start and end point draws a point
				
				e.gc.setForeground(color);
				e.gc.setBackground(fillColor);
					
				// Draw rectangle with actual value
				// Set height origin to fullHeight + 1 to include drawing at fullHeight
				e.gc.fillRectangle(xCur, fullHeight + 1, 
						xNext - xCur, - curFillHeight);
				// Draw top line on top of rectangle
				e.gc.drawLine(xCur, fullHeight - curFillHeight, 
						xNext, fullHeight - curFillHeight);

				// Draw min line
				e.gc.setForeground(fillColor);
				e.gc.drawLine(xCur, fullHeight, 
						xCur, fullHeight - minFillHeight);

				// Draw max line on top of min line
				// Drawing after the drawing of min line ensures that if max=min=0
				// only the contour is drawn (one pixel for the max line is visible)
				e.gc.setForeground(color);
				e.gc.drawLine(xCur, fullHeight - minFillHeight, 
						xCur, fullHeight - maxFillHeight);

				if (line.getSample(index).time > drawEndTime) {
					break;
				}

				// Include the previouse sample in the min/max computations
				// for the next sample
				curMaxFilling = curFilling;
				curMinFilling = curFilling;
			}
		}			
	}

	private void drawGridLines(final PaintEvent e, int height) {
		for (int i = 0, y = height; y >= 0; i++, y -= GRID_SPACING) {
			if (i == 0) {
				e.gc.setForeground(e.display
						.getSystemColor(SWT.COLOR_BLACK));
			} else {
				e.gc
						.setForeground(e.display
								.getSystemColor(SWT.COLOR_GRAY));
			}
			e.gc.drawLine(e.x, y, e.x + e.width, y);
		}
	}
	
	/**
	 * Ensures the given value is within the valid x-values and casts it to an
	 * int. If the value is too low, returns <code>X_MIN</code>. If it's too
	 * high, returns <code>X_MAX</code>.
	 * 
	 * @param val
	 *            the value to be checked and casted
	 * @return <code>value</code>, <code>X_MIN</code>, or
	 *         <code>X_MAX</code>
	 */
	private int boundedInt(final double val) {
		return (int) Math.min(X_MAX, Math.max(X_MIN, val));
	}
}
