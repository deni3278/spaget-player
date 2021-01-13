package spaget;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static javafx.scene.media.MediaPlayer.Status.*;

/**
 * Controller class for {@code player.fxml}.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 3.4
 * @since 04.01.2021
 */
public class PlayerController {
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
    ListView<Playlist> viewListPlaylists;

    @FXML
    Slider sliderVolume, sliderSeek;

    @FXML
    StackPane paneMediaView;

    @FXML
    TableView<spaget.Media> viewTableMedia;

    @FXML
    TableColumn<spaget.Media, String> columnTitle, columnArtist, columnDuration;

    @FXML
    TabPane paneTab;

    @FXML
    TextField fieldSearch;

    @FXML
    void initialize() {
        updateMediaTable();

        /* Default values */

        viewTableMedia.setPlaceholder(new Label("No media files in local folder"));
        sliderVolume.setValue(50.0);

        /* Listeners */

        setControlListeners();

        paneTab.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                updateMediaTable();
            } else {
                updatePlaylistList();
            }
        });

        fieldSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                viewTableMedia.getItems().stream()
                        .filter(media -> media.toString().toLowerCase().contains(newValue.toLowerCase()))
                        .findAny()
                        .ifPresent(media -> {
                            viewTableMedia.getSelectionModel().select(media);
                            viewTableMedia.scrollTo(media);
                        });
            } else {
                viewTableMedia.getSelectionModel().select(null);
            }
        });

        fieldSearch.setOnAction(e -> playMedia(viewTableMedia.getSelectionModel().getSelectedItem().getPath()));

        /* Context Menu for Library ListView */

        MenuItem refresh = new MenuItem("Refresh");
        refresh.setOnAction(e -> updateMediaTable());

        ContextMenu menuMedia = new ContextMenu();
        menuMedia.getItems().add(refresh);

        viewTableMedia.setContextMenu(menuMedia);

        /* Context Menu for Playlist ListView */

        MenuItem newPlaylist = new MenuItem("New Playlist");
        newPlaylist.setOnAction(e -> handleNewPlaylist());

        ContextMenu menuPlaylist = new ContextMenu();
        menuPlaylist.getItems().add(newPlaylist);

        viewListPlaylists.setContextMenu(menuPlaylist);
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
     * <p>
     * Control buttons are disabled by default since no media file is played upon initialization.
     * They are enabled the first time a media file is played.
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

        /* Remove MP3 album image from the player if it exists */

        if (imageAlbum.getImage() != null) {
            imageAlbum.setImage(null);
            regionAlbumBackground.setBackground(Background.EMPTY);
        }

        setMediaPlayerListeners();
    }

    /**
     * Plays the selected {@link spaget.Media} if it's double clicked in the table.
     *
     * @param event event which indicates that a mouse action occurred
     * @see #viewTableMedia
     */
    @FXML
    void handleTableClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            spaget.Media media = viewTableMedia.getSelectionModel().getSelectedItem();

            if (media != null) {
                playMedia(media.getPath());
            }
        }
    }

    /**
     * Creates a new empty {@link Playlist} and adds it to the database.
     */
    @FXML
    void handleNewPlaylist() {
        Image icon = new Image(this.getClass().getResourceAsStream("/images/spaghetti.png")); // Image object containing the icon for the dialog window

        /* Create, initialize, and display an input dialog for the playlist name */

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Playlist");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setContentText("Name");

        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(icon);

        Optional<String> input = dialog.showAndWait();

        input.ifPresent(name -> {
            for (Playlist playlist : Main.getPlaylists()) {

                /* Display an error alert if there's already a playlist with the same name as the inputted name */

                if (playlist.getName().equals(name)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText(null);
                    alert.setGraphic(null);
                    alert.setContentText("Playlist already exists!");
                    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
                    alert.showAndWait();

                    return;
                }
            }

            DB.insertSQL("INSERT INTO tblPlaylist (fldName) VALUES ('" + name + "')");

            updatePlaylistList();
        });
    }

    /**
     * Plays the selected {@link Playlist} if it's double clicked in the list.
     * <p>
     * A new {@code Tab} is created with a {@code ListView} of media files in the {@link Playlist}.
     *
     * @param event event which indicates that a mouse action occurred
     * @see #viewListPlaylists
     */
    @FXML
    void handleListClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Playlist playlist = viewListPlaylists.getSelectionModel().getSelectedItem(); // Object of the selected playlist

            /* If there already isn't a tab for it, a tab is created for the chosen playlist */

            if (playlist != null) {
                for (Tab tab : paneTab.getTabs()) {
                    if (tab.getText().equals(playlist.getName())) {
                        paneTab.getSelectionModel().select(tab);

                        startPlaylist(tab);

                        return;
                    }
                }

                /* Setup the table for the playlist's media contents */

                TableColumn<spaget.Media, String> playlistColumnTitle = new TableColumn<>("Title");
                TableColumn<spaget.Media, String> playlistColumnArtist = new TableColumn<>("Artist");
                TableColumn<spaget.Media, String> playlistColumnDuration = new TableColumn<>("");

                MaterialDesignIconView icon = new MaterialDesignIconView();
                icon.setGlyphName("CLOCK");
                icon.setSize("16");
                playlistColumnDuration.setGraphic(icon);

                playlistColumnTitle.setMinWidth(columnTitle.getMinWidth());
                playlistColumnArtist.setMinWidth(columnArtist.getMinWidth());
                playlistColumnDuration.setMinWidth(columnDuration.getMinWidth());

                playlistColumnTitle.setMaxWidth(columnTitle.getMaxWidth());
                playlistColumnArtist.setMaxWidth(columnArtist.getMaxWidth());
                playlistColumnDuration.setMaxWidth(columnDuration.getMaxWidth());

                playlistColumnTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
                playlistColumnArtist.setCellValueFactory(new PropertyValueFactory<>("artist"));
                playlistColumnDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));

                TableView<spaget.Media> viewTablePlaylist = new TableView<>();
                viewTablePlaylist.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                viewTablePlaylist.getColumns().addAll(playlistColumnTitle, playlistColumnArtist, playlistColumnDuration);
                viewTablePlaylist.getItems().addAll(playlist.getMediaList());

                Tab tabPlaylist = new Tab(playlist.getName());
                tabPlaylist.setContent(viewTablePlaylist);

                paneTab.getTabs().add(tabPlaylist);

                paneTab.getSelectionModel().select(tabPlaylist);
                startPlaylist(tabPlaylist);
            }
        }
    }

    /**
     * Retrieves an up-to-date {@code ArrayList} of local media files. The files are added as rows in the library table.
     */
    @FXML
    void updateMediaTable() {
        viewTableMedia.getItems().removeAll(viewTableMedia.getItems()); // Remove any items in the table to avoid duplicate records

        columnTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        columnArtist.setCellValueFactory(new PropertyValueFactory<>("artist"));
        columnDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));

        viewTableMedia.getItems().addAll(Main.updateMedia());

        /* Add context menus to each record in the table for adding media files to playlists */

        viewTableMedia.setRowFactory(view -> {
            TableRow<spaget.Media> row = new TableRow<>();

            row.emptyProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    ContextMenu menu = new ContextMenu();
                    Menu addToPlaylist = new Menu("Add To Playlist");

                    /* Add a menu item for each playlist */

                    for (Playlist playlist : viewListPlaylists.getItems()) {
                        MenuItem item = new MenuItem(playlist.getName());
                        item.setOnAction(e -> {
                            ArrayList<spaget.Media> mediaList = playlist.getMediaList();

                            for (spaget.Media media : mediaList) {
                                if (media.getPath().equals(row.getItem().getPath())) {

                                    /* Display an error alert if the media file already is in the playlist */

                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setHeaderText(null);
                                    alert.setGraphic(null);
                                    alert.setContentText("Media is already in the playlist!");
                                    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(this.getClass().getResourceAsStream("/images/spaghetti.png")));
                                    alert.showAndWait();

                                    return;
                                }
                            }

                            mediaList.add(row.getItem());

                            DB.insertSQL("INSERT INTO tblPlaylistMedia (fldPlaylistName, fldMediaPath) VALUES ('" + playlist.getName() + "', '" + row.getItem().getPath() + "')");
                        });

                        addToPlaylist.getItems().add(item);
                    }

                    menu.getItems().add(addToPlaylist);
                    row.setContextMenu(menu);
                }
            });

            return row;
        });
    }

    /**
     * Retrieves an up-to-date {@code ArrayList} of playlists and {@link Playlist}s added as rows in the playlist list.
     * <p>
     * {@code Context Menu}s are added to each row in the list with items: rename, delete, and new playlist.
     */
    @FXML
    void updatePlaylistList() {
        viewListPlaylists.getItems().removeAll(viewListPlaylists.getItems());
        viewListPlaylists.getItems().addAll(Main.getPlaylists());

        /* Add context menus to each record in the table for renaming and deleting playlists */

        viewListPlaylists.setCellFactory(view -> {
            ListCell<Playlist> cell = new ListCell<>();

            cell.emptyProperty().addListener(((observable, oldValue, newValue) -> {
                if (!newValue) {
                    MenuItem rename = new MenuItem("Rename");
                    rename.setOnAction(e -> {
                        Image icon = new Image(this.getClass().getResourceAsStream("/images/spaghetti.png")); // Image object containing the icon for the dialog window

                        /* Create, initialize, and display an input dialog for the new playlist name */

                        TextInputDialog dialog = new TextInputDialog();
                        dialog.setTitle("Rename Playlist");
                        dialog.setHeaderText(null);
                        dialog.setGraphic(null);
                        dialog.setContentText("Rename");

                        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(icon);

                        Optional<String> input = dialog.showAndWait();

                        input.ifPresent(name -> {
                            for (Playlist playlist : Main.getPlaylists()) {
                                if (playlist.getName().equals(name)) {

                                    /* Display an error alert if a playlist with the inputted name already exists */

                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setHeaderText(null);
                                    alert.setGraphic(null);
                                    alert.setContentText("Playlist already exists with that name!");
                                    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
                                    alert.showAndWait();

                                    return;
                                }
                            }

                            /* Rename the playlist in the table */

                            cell.itemProperty().get().setName(name);
                            cell.textProperty().bind(cell.itemProperty().asString());
                        });
                    });

                    MenuItem delete = new MenuItem("Delete");
                    delete.setOnAction(e -> {

                        /* Display an empty String instead of "null" in the table */

                        cell.textProperty().unbind();
                        cell.setText("");

                        DB.deleteSQL("DELETE FROM tblPlaylist WHERE fldName = '" + cell.itemProperty().get().getName() + "'");
                        viewListPlaylists.getItems().remove(cell.itemProperty().get());
                    });

                    ContextMenu menu = new ContextMenu();
                    menu.getItems().addAll(rename, delete);

                    cell.setContextMenu(menu);
                    cell.textProperty().bind(cell.itemProperty().asString()); // Bind the name of the playlist in the table to the name field of the associated playlist object
                }
            }));

            return cell;
        });
    }

    /**
     * Plays the {@code tab}'s associated playlist.
     * <p>
     * The current media file that is playing is determined by which row is selected.
     * The next row is automatically selected once the current media is done playing.
     *
     * @param tab {@code Tab} of associated playlist
     */
    private void startPlaylist(Tab tab) {
        TableView<spaget.Media> view = ((TableView) tab.getContent()); // Object of the table of the given tab

        /* Play each media file (in order) after the previous has finished playing */

        view.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            playMedia(newValue.getPath());

            mediaPlayer.setOnEndOfMedia(() -> {
                int currentIndex = view.getSelectionModel().getSelectedIndex() + 1;

                if (currentIndex < view.getItems().size()) {
                    view.getSelectionModel().select(currentIndex);
                }
            });
        }));

        view.getSelectionModel().select(0);
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
        sliderVolume.valueProperty().addListener((observable, oldValue, newValue) -> mediaPlayer.setVolume(newValue.doubleValue() / 100.0)); // Player volume changes when the volume slider changes

        /* The play/pause button's icon changes depending on whether or not a media file is playing */

        mediaPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer.getStatus() == PLAYING) {
                iconBtnPlay.setGlyphName("PAUSE");
            } else {
                iconBtnPlay.setGlyphName("PLAY");
            }
        });

        /* The current time label displays the current time in the media file, and the seek slider moves along with the current time */

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            labelCurrentTime.setText(spaget.Media.formatSeconds((int) Math.round(newValue.toSeconds())));

            if (!sliderSeek.isPressed()) {
                sliderSeek.setValue(newValue.toSeconds());
            }
        });

        mediaPlayer.setOnReady(() -> {
            labelTotalDuration.setText(spaget.Media.formatSeconds((int) Math.round(media.getDuration().toSeconds()))); // The total duration label is set to match the total duration of the current media file
            sliderSeek.setMax(media.getDuration().toSeconds());                                                        // The max value of the seek slider is set to match the total duration of the current media file

            Map<String, Object> metadata = media.getMetadata(); // Map object containing the current media file's metadata

            /* Displays the album cover of the media file if one exists */

            if (metadata.containsKey("image")) {
                imageAlbum.setImage((Image) metadata.get("image"));

                BackgroundImage image = new BackgroundImage(imageAlbum.getImage(), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true));

                regionAlbumBackground.setBackground(new Background(image));
                regionAlbumBackground.setEffect(new GaussianBlur(50));
            }

            mediaPlayer.setVolume(sliderVolume.getValue() / 100.0); // Sets the volume of the media player to be the same as the value of the volume slider
            mediaPlayer.play();                                     // Plays the current media file
        });
    }

    /**
     * Implements an appropriate {@code Listener} to each of the control buttons and sliders.
     */
    private void setControlListeners() {

        /* Plays or pauses the video */

        btnPlay.setOnAction(e -> {
            if (mediaPlayer.getStatus() == PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        });

        btnStepBack.setOnAction(e -> mediaPlayer.seek(javafx.util.Duration.seconds(0)));    // Seeks to the start of the media file

        btnStop.setOnAction(e -> mediaPlayer.stop());                                       // Stops the media file

        btnStepForward.setOnAction(e -> mediaPlayer.seek(media.getDuration()));             // Seeks to the end of the media file
    }
}