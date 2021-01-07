package player;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import static javafx.scene.media.MediaPlayer.Status.*;

/**
 * Controller class for {@code player.fxml}.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 3.1
 * @since 04.01.2021
 */
public class Player {
    Media media;
    MediaPlayer mediaPlayer;

    @FXML
    MediaView mediaView;

    @FXML
    Region regionAlbumBackground;

    @FXML
    Button btnPlay, btnStepBack, btnStop, btnStepForward;

    @FXML
    FontAwesomeIconView iconBtnPlay;

    @FXML
    ImageView imageAlbum;

    @FXML
    Label labelCurrentTime, labelTotalDuration;

    @FXML
    Slider sliderVolume, sliderSeek;

    @FXML
    StackPane paneMediaView;

    @FXML
    TableView<player.Media> viewTableMedia;

    @FXML
    TableColumn<player.Media, String> columnTitle, columnArtist, columnDuration;

    @FXML
    TabPane paneTab;

    @FXML
    void initialize() {
        updateMediaTable();

        sliderVolume.setValue(50);

        /* Listeners */

        setControlListeners();

        paneTab.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                updateMediaTable();
            } else {
                // Todo: Update playlists
            }
        });
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

        if (imageAlbum.getImage() != null) {
            imageAlbum.setImage(null);
            regionAlbumBackground.setBackground(Background.EMPTY);
        }

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
        viewTableMedia.getItems().removeAll(viewTableMedia.getItems());

        ArrayList<player.Media> mediaList = Main.updateDatabase();

        columnTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        columnArtist.setCellValueFactory(new PropertyValueFactory<>("artist"));
        columnDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));

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
            labelCurrentTime.setText(player.Media.formatSeconds((int) Math.round(newValue.toSeconds())));
            sliderSeek.setValue(newValue.toSeconds());
        });

        mediaPlayer.setOnReady(() -> {
            labelTotalDuration.setText(player.Media.formatSeconds((int) Math.round(media.getDuration().toSeconds())));
            sliderSeek.setMax(media.getDuration().toSeconds());

            Map<String, Object> metadata = media.getMetadata();

            if (metadata.containsKey("image")) {
                imageAlbum.setImage((Image) metadata.get("image"));

                BackgroundImage image = new BackgroundImage(imageAlbum.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true));

                regionAlbumBackground.setBackground(new Background(image));
                regionAlbumBackground.setEffect(new GaussianBlur(50));
            }

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