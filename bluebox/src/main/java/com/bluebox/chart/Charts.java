package com.bluebox.chart;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.Rotation;

import com.bluebox.smtp.storage.StorageFactory;


public class Charts {
	public static final String CHART_ROOT = "rest/chart";

	private  PieDataset createPieDataset(JSONObject jo) {
		DefaultPieDataset result = new DefaultPieDataset();
		@SuppressWarnings("rawtypes")
		Iterator keys = jo.keys();
		String key;
		while (keys.hasNext()){
			try {
				key = keys.next().toString();
				result.setValue(key, jo.getLong(key));
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return result;

	}

	private IntervalXYDataset createIntervalXYDataset(JSONObject jo) {
		final XYSeries series = new XYSeries("Random Data");
		for (int i = 1; i < 32; i++) {
			try {
				series.add(i, jo.getInt(i+""));
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		final XYSeriesCollection dataset = new XYSeriesCollection(series);
		return dataset;
	}

	public void renderDailyCountChart(OutputStream os, int width, int height) throws IOException {
		IntervalXYDataset dataset = createIntervalXYDataset(StorageFactory.getInstance().getCountByDay());
		JFreeChart chart = ChartFactory.createXYBarChart(
				"",
				"", 
				false,
				"", 
				dataset,
				PlotOrientation.VERTICAL,
				false,
				false,
				false
				);
		
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlineVisible(false);
		
		XYItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0,new Color(0x107bbb));
		XYBarRenderer renderer2 = (XYBarRenderer) plot.getRenderer();
		renderer2.setShadowVisible(false);
		
		// y-axis
		ValueAxis yAxis = plot.getRangeAxis();
		yAxis.setAxisLineVisible(false);
		yAxis.setTickMarksVisible(true);
		yAxis.setTickLabelsVisible(true);
		yAxis.setVisible(true);
		yAxis.setAutoRange(true);
		yAxis.setAutoRangeMinimumSize(1);
		NumberAxis rangeaxis = (NumberAxis) plot.getRangeAxis(); 
		rangeaxis.setAutoRangeStickyZero(false);
		
		// x-axis
		ValueAxis xAxis = plot.getDomainAxis();
		xAxis.setAxisLineVisible(false);
		xAxis.setAutoRange(true);
		xAxis.setRange(1, 31);

		
		ChartUtilities.writeChartAsPNG(os, chart, width, height);
	}

	private CategoryDataset createDataset(JSONObject jo) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		String series1 = "Day of month";
		String day;
		for (int i = 1; i < 32; i++) {
			day = i+"";
			try {
				dataset.addValue(jo.getInt(day), series1, day);
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return dataset;
	}

	public void renderChart(final HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("image/png");
		int width=200, height=200;
		try {
			width = Integer.parseInt(req.getParameter("width"));
			height = Integer.parseInt(req.getParameter("height"));
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		renderDailyCountChart(resp.getOutputStream(),width,height);
		resp.flushBuffer();
	}

	public void renderDailyCountChartOld(OutputStream os, int width, int height) throws IOException {
		CategoryDataset dataset = createDataset(StorageFactory.getInstance().getCountByDay());
		JFreeChart chart = ChartFactory.createLineChart("", "", "", dataset, PlotOrientation.VERTICAL, false, false, false);
		chart.setBorderVisible(false);
		chart.removeLegend();
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlineVisible(false);

		CategoryItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0,new Color(0x107bbb));

		CategoryAxis categoryAxis = (CategoryAxis) plot.getDomainAxis();
		categoryAxis.setAxisLineVisible(false);
		categoryAxis.setTickMarksVisible(false);
		categoryAxis.setTickLabelsVisible(true);
		categoryAxis.setVisible(true);
		categoryAxis.setMinorTickMarksVisible(false);//.s.setTickUnit(new NumberTickUnit(31));

		ValueAxis valueAxis = plot.getRangeAxis();
		valueAxis.setAxisLineVisible(true);
		valueAxis.setTickMarksVisible(false);
		valueAxis.setTickLabelsVisible(false);
		valueAxis.setVisible(false);

		ChartUtilities.writeChartAsPNG(os, chart, width, height);
	}

	public void renderHistoryChart(OutputStream os, int width, int height) throws IOException {
		JFreeChart chart = ChartFactory.createPieChart3D("",          // chart title
				createPieDataset(StorageFactory.getInstance().getCountByDay()),                // data
				true,                   // include legend
				true,
				false);


		PiePlot3D plot = (PiePlot3D) chart.getPlot();
		plot.setStartAngle(290);
		plot.setDirection(Rotation.CLOCKWISE);
		plot.setForegroundAlpha(0.5f);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setBackgroundAlpha(0.2f); 
		plot.setOutlineVisible(false);
		ChartUtilities.writeChartAsPNG(os, chart, width, height);
	}
}
