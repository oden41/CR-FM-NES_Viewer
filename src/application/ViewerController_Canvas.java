package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

/**
 * @author shumpmita
 *
 */
public class ViewerController_Canvas {

	@FXML
	private Label noOfEvalLabel;
	@FXML
	private Label bestEvalLabel;
	@FXML
	private Label rangeLabel;

	@FXML
	private Canvas chart;

	@FXML
	private Button startButton;
	@FXML
	private Button nextButton;
	@FXML
	private Button backButton;
	@FXML
	private Button lastButton;
	@FXML
	private Button saveButton;

	@FXML
	private Slider slider;

	private ArrayList<Long> noOfEvalList;
	private ArrayList<Double> bestEvalList;
	private ArrayList<double[][]> dataList;
	private ArrayList<double[]> meanList;
	private ArrayList<double[]> covList;

	private static int index;

	private FileChooser fileChooser;
	private static final double defaultRange = 5;
	private static double maxRange = defaultRange;

	@FXML
	public void initialize() {
		fileChooser = new FileChooser();
		fileChooser.setTitle("チャート保存");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));

		noOfEvalList = new ArrayList<>();
		bestEvalList = new ArrayList<>();
		dataList = new ArrayList<>();
		meanList = new ArrayList<>();
		covList = new ArrayList<>();

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

		file = new File("./log/Mean.csv");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// オブジェクトに落とし込み
			String str = "";
			while ((str = br.readLine()) != null) {
				double[] array = new double[2];
				String[] split = str.split(",");
				array = Stream.of(split).mapToDouble(e -> Double.parseDouble(e)).toArray();
				meanList.add(array);
			}
		} catch (Exception ex) {
			System.out.println("Population読み込みでエラー");
		}

		file = new File("./log/Cov.csv");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			// オブジェクトに落とし込み
			String str = "";
			while ((str = br.readLine()) != null) {
				double[] array = new double[3];
				String[] split = str.split(",");
				array = Stream.of(split).mapToDouble(e -> Double.parseDouble(e)).toArray();
				covList.add(array);
			}
		} catch (Exception ex) {
			System.out.println("Population読み込みでエラー");
		}

		assert noOfEvalList.size() == bestEvalList.size();
		assert noOfEvalList.size() == dataList.size();
		assert noOfEvalList.size() == meanList.size();
		assert noOfEvalList.size() == covList.size();

		// 1e-5と1(倍率)をeの非線形で結ぶ
		slider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
				maxRange = (1e-5 + Math.pow(new_val.doubleValue(), 2) * (1 - 1e-5)) * defaultRange;
				SetData(index);
			}
		});

		onActionStartButton(null);
	}

	@FXML
	protected void onSnapShotButtonAction(ActionEvent e) {
		File importFile = fileChooser.showSaveDialog(saveButton.getScene().getWindow());
		if (importFile != null) {
			String path = importFile.getPath().toString();
			WritableImage image = chart.snapshot(new SnapshotParameters(), null);

			// TODO: probably use a file chooser here
			File file = new File(path);

			try {
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
			} catch (IOException e1) {
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
		bestEvalLabel.setText(String.format("%.3e", bestEvalList.get(index)));
		rangeLabel.setText(String.format("%.2e", maxRange));

		GraphicsContext gc = chart.getGraphicsContext2D();
		// クリア
		gc.clearRect(0, 0, chart.getWidth(), chart.getHeight());
		// 高低図
		drawHeatMap(gc);
		// 外枠を描画
		drawBorderAndAxis(gc);
		// データを描画
		double[][] data = dataList.get(index);
		gc.setFill(Color.ORANGE);
		drawPoint(gc, data);
		// 楕円
		gc.setStroke(Color.GREENYELLOW);
		gc.setLineWidth(3);
		drawEllipsoid(gc, meanList.get(index), covList.get(index));
	}

	private void drawPoint(GraphicsContext gc, double[][] data) {
		for (int i = 0; i < data.length; i++) {
			double r = 8;
			gc.fillOval(transformX(data[i][0]) - r / 2.0, transformY(data[i][1]) - r / 2.0, r, r);
		}
	}

	private void drawBorderAndAxis(GraphicsContext g) {
		final double canvasWidth = g.getCanvas().getWidth();
		final double canvasHeight = g.getCanvas().getHeight();

		g.setStroke(Color.BLACK);
		g.setLineWidth(4);
		g.strokeRect(0, 0, canvasWidth, canvasHeight);

		g.setLineWidth(1);
		g.strokeLine(0, canvasHeight / 2, canvasWidth, canvasHeight / 2);
		g.strokeLine(canvasWidth / 2, 0, canvasWidth / 2, canvasHeight);
	}

	private void drawHeatMap(GraphicsContext gc) {
		final int width = (int) gc.getCanvas().getWidth();
		final int height = (int) gc.getCanvas().getHeight();
		double[][] map = new double[width][height];
		double min = Double.MAX_VALUE;
		double max = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				map[i][j] = ktablet(new double[] { inverseX(i), inverseY(j) });
				if (min > map[i][j])
					min = map[i][j];
				if (max < map[i][j])
					max = map[i][j];
			}
		}
		WritableImage dest = new WritableImage(width, height);
		PixelWriter writer = dest.getPixelWriter();

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				writer.setColor(x, y, Color.gray(Math.pow((map[x][y] - min) / (max - min), 0.25)));
			}
		}
		gc.drawImage(dest, 0, 0);
	}

	private void drawEllipsoid(GraphicsContext gc, double[] mean, double[] cov) {
		double sigmaU = (cov[0] + cov[2] + Math.sqrt((cov[0] - cov[2]) * (cov[0] - cov[2]) + 4 * cov[1] * cov[1])) / 2.0
				/ 2.0;
		double sigmaV = (cov[0] + cov[2] - Math.sqrt((cov[0] - cov[2]) * (cov[0] - cov[2]) + 4 * cov[1] * cov[1])) / 2.0
				/ 2.0;
		double rad = Math.atan((sigmaU - cov[0]) / cov[1]);
		double alpha = rad * 180 / Math.PI;
		double axis1 = 2.146 * Math.sqrt(sigmaU) * gc.getCanvas().getWidth() / maxRange;
		double axis2 = 2.146 * Math.sqrt(sigmaV) * gc.getCanvas().getHeight() / maxRange;
		//x,y:Canvas座標
		double x = transformX(mean[0]);
		double y = transformY(mean[1]);
		gc.translate(x, y);
		// 楕円描画
		gc.rotate(-alpha);
		gc.strokeOval(-axis1, -axis2, 2 * axis1, 2 * axis2);
		gc.rotate(alpha);
		gc.translate(-x, -y);
	}

	private double transformX(double data) {
		double range = maxRange;
		return (data + range) * chart.getWidth() * 0.5 / range;
	}

	private double inverseX(double width) {
		double range = maxRange;
		return 2.0 * range * width / chart.getWidth() - range;
	}

	private double transformY(double data) {
		double range = maxRange;
		return (-data + range) * chart.getHeight() * 0.5 / range;
	}

	private double inverseY(double height) {
		double range = maxRange;
		return range - 2.0 * range * height / chart.getHeight();
	}

	private static double ktablet(double[] x) {
		int k = (int) ((double) x.length / 4.0); // k=n/4
		double result = 0.0; // 評価値を初期化
		for (int i = 0; i < x.length; ++i) {
			double xi = x[i]; // i番目の次元の要素
			if (i < k) {
				result += xi * xi;
			} else {
				result += 10000.0 * xi * xi;
			}
		}
		return result;
	}
}
