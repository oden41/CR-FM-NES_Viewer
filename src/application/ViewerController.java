package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;

/**
 * @author shumpmita
 *
 */
public class ViewerController {
	// 評価回数のラベル
	@FXML
	private Label noOfEvalLabel;
	// 最良評価値のラベル
	@FXML
	private Label bestEvalLabel;
	// データを表示するチャート
	@FXML
	private ScatterChart<Double, Double> chart;
	// x軸
	@FXML
	private NumberAxis xAxis;
	// y軸
	@FXML
	private NumberAxis yAxis;
	// 最初の世代のデータを表示するボタン
	@FXML
	private Button startButton;
	// 次世代のデータを表示するボタン
	@FXML
	private Button nextButton;
	// 前世代のデータを表示するボタン
	@FXML
	private Button backButton;
	// 最終世代を表示するボタン
	@FXML
	private Button lastButton;
	// チャートをpng形式で保存するボタン
	@FXML
	private Button saveButton;
	// チャートの範囲を決定するスライダー
	@FXML
	private Slider slider;

	private ArrayList<Long> noOfEvalList;
	private ArrayList<Double> bestEvalList;
	private ArrayList<double[][]> dataList;

	// 世代数
	private static int index;
	// チャートを画像保存する際に選択するオブジェクト
	private FileChooser fileChooser;

	@FXML
	public void initialize() {
		fileChooser = new FileChooser();
		fileChooser.setTitle("チャート保存");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));

		// x軸，y軸の設定
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

		// logフォルダにあるデータを読み込み
		File file = new File("./log/BestValue.csv");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// オブジェクトに落とし込み
			String str = "";
			while ((str = br.readLine()) != null) {
				String[] split = str.split(",");
				noOfEvalList.add(Long.parseLong(split[0]));
				bestEvalList.add(Double.parseDouble(split[1]));
			}
		}
		catch (Exception ex) {
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
		}
		catch (Exception ex) {
			System.out.println("Population読み込みでエラー");
		}

		assert noOfEvalList.size() == bestEvalList.size();
		assert noOfEvalList.size() == dataList.size();

		// 1e-5と1(倍率)をeの非線形で結ぶ
		// スライダーの変更と同時にチャート範囲を変更するイベントを追加
		slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				double magValue = 1e-5 * Math.exp(new_val.doubleValue() * 5 * Math.log(10));
				xAxis.setLowerBound(-1 * magValue * 5);
				xAxis.setUpperBound(magValue * 5);
				yAxis.setLowerBound(-1 * magValue * 5);
				yAxis.setUpperBound(magValue * 5);
			}
		});
		// 初期集団を表示
		onActionStartButton(null);
	}

	// チャート保存ボタン押下時に呼び出されるメソッド
	@FXML
	protected void onSnapShotButtonAction(ActionEvent e) {
		File importFile = fileChooser.showSaveDialog(saveButton.getScene().getWindow());
		// 保存場所を設定していればnullでない
		if (importFile != null) {
			String path = importFile.getPath().toString();
			WritableImage image = chart.snapshot(new SnapshotParameters(), null);
			File file = new File(path);
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
			}
			catch (IOException e1) {
				// TODO: handle exception here
			}
		}
	}

	@FXML
	public void onActionStartButton(ActionEvent event) {
		startButton.setDisable(true);
		backButton.setDisable(true);
		nextButton.setDisable(false);
		lastButton.setDisable(false);

		index = 0;
		SetData(index);
	}

	@FXML
	public void onActionNextButton(ActionEvent event) {
		index++;
		if (index == noOfEvalList.size() - 1) {
			onActionLastButton(event);
			return;
		}
		SetData(index);
		startButton.setDisable(false);
		backButton.setDisable(false);
		nextButton.setDisable(false);
		lastButton.setDisable(false);
	}

	@FXML
	public void onActionBackButton(ActionEvent event) {
		index--;
		if (index == 0) {
			onActionStartButton(event);
			return;
		}
		SetData(index);
		startButton.setDisable(false);
		backButton.setDisable(false);
		nextButton.setDisable(false);
		lastButton.setDisable(false);
	}

	@FXML
	public void onActionLastButton(ActionEvent event) {
		startButton.setDisable(false);
		backButton.setDisable(false);
		nextButton.setDisable(true);
		lastButton.setDisable(true);

		index = noOfEvalList.size() - 1;
		SetData(index);
	}

	/**
	 * ラベルおよびチャートにデータを表示
	 *
	 * @param index
	 *                世代
	 */
	private void SetData(int index) {
		noOfEvalLabel.setText(String.valueOf(noOfEvalList.get(index)));
		bestEvalLabel.setText(String.valueOf(bestEvalList.get(index)));

		chart.getData().clear();
		XYChart.Series series = new XYChart.Series();
		series.setName("個体");
		double[][] data = dataList.get(index);
		for (int i = 0; i < data.length; i++) {
			double[] ds = data[i];

			series.getData().add(new XYChart.Data<Double, Double>(ds[0], ds[1]));
		}
		chart.getData().add(series);

		// ToolTipを表示
		// データをchartにセットした後でないと正しく表示されなかったため，この場所での処理
		for (Series<Double, Double> s : chart.getData()) {
			for (Data<Double, Double> point : s.getData()) {
				double x = point.getXValue().doubleValue();
				double y = point.getYValue().doubleValue();
				Tooltip.install(point.getNode(), new Tooltip(String.format("(%1.3e, %1.3e), Eval:%1.3e", x, y, ktablet(new double[] { x, y }))));
			}
		}
	}

	private static double ktablet(double[] x) {
		int k = (int) ((double) x.length / 4.0); // k=n/4
		double result = 0.0; // 評価値を初期化
		for (int i = 0; i < x.length; ++i) {
			double xi = x[i]; // i番目の次元の要素
			if (i < k) {
				result += xi * xi;
			}
			else {
				result += 10000.0 * xi * xi;
			}
		}
		return result;
	}
}
