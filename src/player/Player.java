package player;

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
 * @version 3.3
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
    ListView<Playlist> viewListPlaylists;

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

        sliderVolume.setValue(50.0); // Default volume

        /* Listeners */

        setControlListeners();

        paneTab.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == 0) {
                updateMediaTable();
            } else {
                updatePlaylistList();
            }
        });

        /* Context Menu for Playlist List */

        MenuItem newPlaylist = new MenuItem("New Playlist");
        newPlaylist.setOnAction(e -> handleNewPlaylist());

        ContextMenu menuPlaylist = new ContextMenu();
        menuPlaylist.getItems().add(newPlaylist);

        viewListPlaylists.setContextMenu(menuPlaylist);

        /* Context Menu for Media List */

        MenuItem refresh = new MenuItem("Refresh");
        refresh.setOnAction(e -> updateMediaTable());

        ContextMenu menuMedia = new ContextMenu();
        menuMedia.getItems().add(refresh);

        viewTableMedia.setContextMenu(menuMedia);
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
     * Plays the selected {@link player.Media} if it's double clicked in the table.
     *
     * @param event event which indicates that a mouse action occurred
     * @see #viewTableMedia
     */
    @FXML
    void handleTableClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            player.Media media = viewTableMedia.getSelectionModel().getSelectedItem();

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
        Image icon = new Image(this.getClass().getResourceAsStream("../resources/spaghetti.png"));

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Playlist");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setContentText("Name");

        ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(icon);

        Optional<String> input = dialog.showAndWait();

        input.ifPresent(name -> {
            for (Playlist playlist : Main.getPlaylists()) {
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
            Playlist playlist = viewListPlaylists.getSelectionModel().getSelectedItem();

            if (playlist != null) {
                for (Tab tab : paneTab.getTabs()) {
                    if (tab.getText().equals(playlist.getName())) {
                        paneTab.getSelectionModel().select(tab);

                        startPlaylist(tab);

                        return;
                    }
                }

                TableColumn<player.Media, String> playlistColumnTitle = new TableColumn<>("Title");
                TableColumn<player.Media, String> playlistColumnArtist = new TableColumn<>("Artist");
                TableColumn<player.Media, String> playlistColumnDuration = new TableColumn<>("");

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

                TableView<player.Media> viewTablePlaylist = new TableView<>();
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
        viewTableMedia.getItems().removeAll(viewTableMedia.getItems());

        columnTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        columnArtist.setCellValueFactory(new PropertyValueFactory<>("artist"));
        columnDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));

        viewTableMedia.getItems().addAll(Main.updateMedia());

        viewTableMedia.setRowFactory(view -> {
            TableRow<player.Media> row = new TableRow<>();

            row.emptyProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    ContextMenu menu = new ContextMenu();
                    Menu addToPlaylist = new Menu("Add To Playlist");

                    for (Playlist playlist : viewListPlaylists.getItems()) {
                        MenuItem item = new MenuItem(playlist.getName());
                        item.setOnAction(e -> {
                            ArrayList<player.Media> mediaList = playlist.getMediaList();

                            for (player.Media media : mediaList) {
                                if (media.getPath().equals(row.getItem().getPath())) {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setHeaderText(null);
                                    alert.setGraphic(null);
                                    alert.setContentText("Media is already in the playlist!");
                                    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(this.getClass().getResourceAsStream("../resources/spaghetti.png")));
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

        viewListPlaylists.setCellFactory(view -> {
            ListCell<Playlist> cell = new ListCell<>();

            cell.emptyProperty().addListener(((observable, oldValue, newValue) -> {
                if (!newValue) {
                    MenuItem rename = new MenuItem("Rename");
                    rename.setOnAction(e -> {
                        Image icon = new Image(this.getClass().getResourceAsStream("../resources/spaghetti.png"));

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
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setHeaderText(null);
                                    alert.setGraphic(null);
                                    alert.setContentText("Playlist already exists with that name!");
                                    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
                                    alert.showAndWait();

                                    return;
                                }
                            }

                            cell.itemProperty().get().setName(name);
                            cell.textProperty().bind(cell.itemProperty().asString());
                        });
                    });

                    MenuItem delete = new MenuItem("Delete");
                    delete.setOnAction(e -> {
                        cell.textProperty().unbind();
                        cell.setText("");

                        DB.deleteSQL("DELETE FROM tblPlaylist WHERE fldName = '" + cell.itemProperty().get().getName() + "'");
                        viewListPlaylists.getItems().remove(cell.itemProperty().get());
                    });

                    ContextMenu menu = new ContextMenu();
                    menu.getItems().addAll(rename, delete);

                    cell.setContextMenu(menu);
                    cell.textProperty().bind(cell.itemProperty().asString());
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
     * @param tab {@code Tab}
     */
    private void startPlaylist(Tab tab) {
        TableView<player.Media> view = ((TableView) tab.getContent());

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
        sliderVolume.valueProperty().addListener((observable, oldValue, newValue) -> mediaPlayer.setVolume(newValue.doubleValue() / 100.0));

        mediaPlayer.statusProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer.getStatus() == PLAYING) {
                iconBtnPlay.setGlyphName("PAUSE");
            } else {
                iconBtnPlay.setGlyphName("PLAY");
            }
        });

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            labelCurrentTime.setText(player.Media.formatSeconds((int) Math.round(newValue.toSeconds())));

            if (!sliderSeek.isPressed()) {
                sliderSeek.setValue(newValue.toSeconds());
            }
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