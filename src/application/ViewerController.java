package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.stream.Stream;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * @author shumpmita
 *
 */
public class ViewerController {

	@FXML
	private Label noOfEvalLabel;

	@FXML
	private Label bestEvalLabel;

	@FXML
	private ScatterChart<Double, Double> chart;
	@FXML
	private NumberAxis xAxis;
	@FXML
	private NumberAxis yAxis;

	@FXML
	private Button nextButton;

	private ArrayList<Long> noOfEvalList;
	private ArrayList<Double> bestEvalList;
	private ArrayList<double[][]> dataList;

	private static int index;

	@FXML
	public void initialize() {
		xAxis.setAutoRanging(false);
		xAxis.setLowerBound(-5);
		xAxis.setUpperBound(5);
		xAxis.setTickUnit(1);

		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(-5);
		yAxis.setUpperBound(5);
		yAxis.setTickUnit(1);

		noOfEvalList = new ArrayList<>();
		bestEvalList = new ArrayList<>();
		dataList = new ArrayList<>();

		//logフォルダにあるデータを読み込み
		File file = new File("./log/BestValue.csv");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// オブジェクトに落とし込み
			String str = "";
			while ((str = br.readLine()) != null) {
				String[] split = str.split(",");
				noOfEvalList.add(Long.parseLong(split[0]));
				bestEvalList.add(Double.parseDouble(split[1]));
			}
		} catch (Exception ex) {
			System.out.println("BestValue読み込みでエラー");
		}

		file = new File("./log/Population.csv");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// オブジェクトに落とし込み
			int noOfPop = Integer.parseInt(br.readLine());
			String str = "";
			while ((str = br.readLine()) != null) {
				double[][] array = new double[noOfPop][];
				for (int i = 0; i < noOfPop; i++) {
					String[] split = str.split(",");
					array[i] = Stream.of(split).mapToDouble(e -> Double.parseDouble(e)).toArray();
					if (i != noOfPop - 1)
						str = br.readLine();
				}
				dataList.add(array);
			}
		} catch (Exception ex) {
			System.out.println("Population読み込みでエラー");
		}

		assert noOfEvalList.size() == bestEvalList.size();
		assert noOfEvalList.size() == dataList.size();

		index = 0;
		SetData(index);
	}

	@FXML
	public void onActionNextButton(ActionEvent event) {
		index++;
		SetData(index);
		if (index == noOfEvalList.size() - 1)
			nextButton.setDisable(true);
	}

	/**
	 * ラベルおよびチャートにデータを表示
	 * @param index 世代
	 */
	private void SetData(int index) {
		noOfEvalLabel.setText(String.valueOf(noOfEvalList.get(index)));
		bestEvalLabel.setText(String.valueOf(bestEvalList.get(index)));

		chart.getData().clear();
		XYChart.Series series = new XYChart.Series();
		series.setName("Individual");
		double[][] data = dataList.get(index);
		for (int i = 0; i < data.length; i++) {
			double[] ds = data[i];

			series.getData().add(new XYChart.Data<Double, Double>(ds[0], ds[1]));
		}
		chart.getData().add(series);
	}
}
