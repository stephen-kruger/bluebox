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
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.bluebox.smtp.storage.StorageFactory;


public class Charts {
	public static final String CHART_ROOT = "rest/chart";

	//	private  PieDataset createPieDataset(JSONObject jo) {
	//		DefaultPieDataset result = new DefaultPieDataset();
	//		@SuppressWarnings("rawtypes")
	//		Iterator keys = jo.keys();
	//		String key;
	//		while (keys.hasNext()){
	//			try {
	//				key = keys.next().toString();
	//				result.setValue(key, jo.getLong(key));
	//			}
	//			catch (Throwable t) {
	//				t.printStackTrace();
	//			}
	//		}
	//		return result;
	//
	//	}

	private IntervalXYDataset createIntervalXYDataset(JSONObject jo) {
		final XYSeries series = new XYSeries("Random Data");
		@SuppressWarnings("rawtypes")
		Iterator keys = jo.keys();
		String key;
		while (keys.hasNext()) {
			key = keys.next().toString();
			try {
				series.add(Double.parseDouble(key), jo.getInt(key));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		final XYSeriesCollection dataset = new XYSeriesCollection(series);
		return dataset;
	}

	//	private TimeSeries createDailyDataset(JSONObject jo) {
	//		TimeSeries series = new TimeSeries("Daily");
	//		int day;
	//		Date now = new Date();
	//		for (int i = 1; i < 32; i++) {
	//			day = i;
	//			try {
	//				series.add(new Day(day,now.getMonth(),1900), jo.getInt(day+""));
	//			} 
	//			catch (JSONException e) {
	//				e.printStackTrace();
	//			}
	//		}
	//
	//		return series;
	//	}

	public void renderDailyCountChart(OutputStream os, int width, int height) throws IOException {
		boolean thumbnail = false;
		if (width<=300) {
			thumbnail = true;
		}
		IntervalXYDataset dataset = createIntervalXYDataset(StorageFactory.getInstance().getCountByDay());
		//		TimeSeries series = createDailyDataset(StorageFactory.getInstance().getCountByDay());
		//		TimeSeriesCollection dataset= new TimeSeriesCollection();
		//		dataset.addSeries(series);
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
		if (thumbnail) {
			renderer2.setShadowVisible(false);
		}
		// y-axis
		ValueAxis yAxis = plot.getRangeAxis();
		if (thumbnail) {
			yAxis.setAxisLineVisible(false);
			yAxis.setTickMarksVisible(true);
			yAxis.setTickLabelsVisible(true);
			yAxis.setVisible(true);
		}
		yAxis.setAutoRange(true);
		//		yAxis.setAutoRangeMinimumSize(1);
		//		NumberAxis rangeaxis = (NumberAxis) plot.getRangeAxis(); 
		//		rangeaxis.setAutoRangeStickyZero(false);

		// x-axis
		ValueAxis xAxis = plot.getDomainAxis();
		if (thumbnail) {
			xAxis.setAxisLineVisible(false);
		}
		xAxis.setAutoRange(true);
		//		xAxis.setRange(1, 31);


		ChartUtilities.writeChartAsPNG(os, chart, width, height);
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
		if (req.getParameter("chart").equals("daily")) {
			renderDailyCountChart(resp.getOutputStream(),width,height);
		}
		if (req.getParameter("chart").equals("hourly")) {
			renderHourlyCountChart(resp.getOutputStream(),width,height);
		}
		if (req.getParameter("chart").equals("weekly")) {
			renderWeeklyCountChart(resp.getOutputStream(),width,height);
		}
		resp.flushBuffer();
	}

	private TimeSeries createHourlyDataset(JSONObject jo) {
		TimeSeries series = new TimeSeries("Hourly");
		int hour;
		final Day today = new Day();
		for (int i = 1; i < 24; i++) {
			hour = i;
			try {
				series.add(new Hour(hour,today), jo.getInt(hour+""));
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return series;
	}

	private DefaultPieDataset createWeeklyDataset(JSONObject jo) {
		DefaultPieDataset result = new DefaultPieDataset();
		// TODO localize these strings
		String[] key = new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
		for (int i = 1; i < 8; i++) {
			try {
				result.setValue(key[i-1], jo.getInt(i+""));

			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	public void renderHourlyCountChart(OutputStream os, int width, int height) throws IOException {
		boolean thumbnail = false;
		if (width<300) {
			thumbnail = true;
		}
		TimeSeries dataset = createHourlyDataset(StorageFactory.getInstance().getCountByHour());
		TimeSeriesCollection my_data_series= new TimeSeriesCollection();
		my_data_series.addSeries(dataset);
		JFreeChart chart = ChartFactory.createTimeSeriesChart("", "", "", my_data_series, false, false, false);
		chart.setBorderVisible(false);
		chart.removeLegend();
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlineVisible(false);

		XYItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0,new Color(0x107bbb));

		ValueAxis categoryAxis = (ValueAxis) plot.getDomainAxis();
		if (thumbnail) {
			categoryAxis.setAxisLineVisible(false);
			categoryAxis.setTickMarksVisible(false);
			categoryAxis.setTickLabelsVisible(true);
			categoryAxis.setVisible(true);
			categoryAxis.setMinorTickMarksVisible(false);//.s.setTickUnit(new NumberTickUnit(31));
		}

		ValueAxis valueAxis = plot.getRangeAxis();
		if (thumbnail) {
			valueAxis.setAxisLineVisible(true);
			valueAxis.setTickMarksVisible(false);
			valueAxis.setTickLabelsVisible(false);
			valueAxis.setVisible(false);
		}

		ChartUtilities.writeChartAsPNG(os, chart, width, height);
	}

	public void renderWeeklyCountChart(OutputStream os, int width, int height) throws IOException {
		boolean thumbnail = false;
		if (width<300) {
			thumbnail = true;
		}
		DefaultPieDataset dataset = createWeeklyDataset(StorageFactory.getInstance().getCountByDayOfWeek());
		JFreeChart chart = ChartFactory.createPieChart("", dataset, false, false, false);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(Color.white);
		Plot plot = chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setOutlineVisible(false);
		if (thumbnail)
			chart.removeLegend();


		ChartUtilities.writeChartAsPNG(os, chart, width, height);
	}

	//	public void renderHistoryChart(OutputStream os, int width, int height) throws IOException {
	//		JFreeChart chart = ChartFactory.createPieChart3D("",          // chart title
	//				createPieDataset(StorageFactory.getInstance().getCountByDay()),                // data
	//				true,                   // include legend
	//				true,
	//				false);
	//
	//
	//		PiePlot3D plot = (PiePlot3D) chart.getPlot();
	//		plot.setStartAngle(290);
	//		plot.setDirection(Rotation.CLOCKWISE);
	//		plot.setForegroundAlpha(0.5f);
	//		plot.setBackgroundPaint(Color.WHITE);
	//		plot.setBackgroundAlpha(0.2f); 
	//		plot.setOutlineVisible(false);
	//		ChartUtilities.writeChartAsPNG(os, chart, width, height);
	//	}
}
