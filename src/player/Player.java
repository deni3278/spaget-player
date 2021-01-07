package player;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.nio.file.Paths;
import java.util.ArrayList;

import static javafx.scene.media.MediaPlayer.Status.*;

/**
 * Controller class for {@code player.fxml}.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 3.0
 * @since 04.01.2021
 */
public class Player {
    Media media;
    MediaPlayer mediaPlayer;

    @FXML
    MediaView mediaView;

    @FXML
    Button btnPlay, btnStepBack, btnStop, btnStepForward;

    @FXML
    FontAwesomeIconView iconBtnPlay;

    @FXML
    Label labelCurrentTime, labelTotalDuration;

    @FXML
    Slider sliderVolume, sliderSeek;

    @FXML
    TableView<player.Media> viewTableMedia;

    @FXML
    TableColumn<player.Media, String> columnTitle, columnArtist, columnLength;

    @FXML
    void initialize() {
        updateMediaTable();

        sliderVolume.setValue(50);

        setControlListeners();
    }

    /**
     * Seeks {@code MediaPlayer} to a playback time specified by {@link #sliderSeek}.
     */
    @FXML
    void handleSeek() {
        mediaPlayer.seek(javafx.util.Duration.seconds(sliderSeek.getValue()));
    }

    /**
     * Creates new instances of {@link #media} and {@link #mediaPlayer} using the argument.
     *
     * @param path absolute {@code URI} path of a media file as {@code String}
     */
    private void playMedia(String path) {
        media = new Media(Paths.get(path).toUri().toString());

        if (mediaPlayer == null) {
            btnPlay.setDisable(false);
            btnStepBack.setDisable(false);
            btnStop.setDisable(false);
            btnStepForward.setDisable(false);
            sliderVolume.setDisable(false);
            sliderSeek.setDisable(false);
        } else {
            mediaPlayer.dispose();
        }

        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        setMediaPlayerListeners();
    }

    /**
     * Plays the selected media file if it's double clicked in the table.
     *
     * @param event event which indicates that a mouse action occurred
     * @see #viewTableMedia
     */
    @FXML
    void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            player.Media media = viewTableMedia.getSelectionModel().getSelectedItem();

            if (media != null) {
                playMedia(media.getPath());
            }
        }
    }

    /**
     * Retrieves an up-to-date {@code ArrayList} of local media files. The files are added as columns in the library table.
     */
    @FXML
    void updateMediaTable() {
        ArrayList<player.Media> mediaList = Main.updateDatabase();

        columnTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        columnArtist.setCellValueFactory(new PropertyValueFactory<>("artist"));
        columnLength.setCellValueFactory(new PropertyValueFactory<>("length"));

        for (player.Media media : mediaList) {
            viewTableMedia.getItems().add(media);
        }
    }

    /**
     * Implements various {@code Listener}s for the {@link #mediaPlayer} object.
     * <p>
     * {@link #sliderVolume} detects a change in value and applies the new value to {@link #mediaPlayer}'s volume.
     * <p>
     * {@link #mediaPlayer} changes {@link #btnPlay}'s icon depending on whether or not the current media is playing.
     * {@link #labelCurrentTime}'s text and {@link #sliderSeek}'s value are changed as the {@code currentTimeProperty}
     * of the {@code MediaPlayer} changes. {@link #labelTotalDuration}'s text and {@link #sliderSeek}'s max value are
     * set to the total duration of {@link #media}.
     */
    private void setMediaPlayerListeners() {
        sliderVolume.valueProperty().addListener((observable, oldValue, newValue) -> {
            mediaPlayer.setVolume(newValue.doubleValue() / 100.0);
        });

        mediaPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer.getStatus() == PLAYING) {
                iconBtnPlay.setGlyphName("PAUSE");
            } else {
                iconBtnPlay.setGlyphName("PLAY");
            }
        });

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            labelCurrentTime.setText(Double.toString(newValue.toSeconds()));
            sliderSeek.setValue(newValue.toSeconds());
        });

        mediaPlayer.setOnReady(() -> {
            labelTotalDuration.setText(String.valueOf(mediaPlayer.getTotalDuration().toSeconds()));
            sliderSeek.setMax(media.getDuration().toSeconds());

            mediaPlayer.setVolume(sliderVolume.getValue() / 100.0);
            mediaPlayer.play();
        });
    }

    /**
     * Implements an appropriate {@code Listener} to each of the control buttons and sliders.
     */
    private void setControlListeners() {
        btnPlay.setOnAction(e -> {
            if (mediaPlayer.getStatus() == PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        });

        btnStepBack.setOnAction(e -> System.out.println("Step Backward"));

        btnStop.setOnAction(e -> mediaPlayer.stop());

        btnStepForward.setOnAction(e -> System.out.println("Step Forward"));
    }
}