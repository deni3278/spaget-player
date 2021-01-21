package spaget;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Main class of the {@code JavaFX} application.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 4.0.0
 * @since 04.01.2021
 */
public class App extends Application {
    static final String MEDIA_PATH = Paths.get("media").toAbsolutePath().toString();

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/player.fxml"));

        stage.setScene(new Scene(root));
        stage.setTitle("Spaget Player");
        stage.getIcons().add(new Image(getClass().getResource("/images/spaghetti.png").toExternalForm()));
        stage.show();

        final double DEFAULT_HEIGHT = 608.5;
        stage.setMinHeight(DEFAULT_HEIGHT + (stage.getHeight() - root.prefHeight(0)));

        final double DEFAULT_WIDTH = 608.5;
        stage.setMinWidth(DEFAULT_WIDTH + (stage.getWidth() - root.prefWidth(0)));

        updateMedia();
    }

    /**
     * Updates the database by adding new local media and/or remove non-existent media.
     * <p>
     * A list of database media paths and a list of local media paths are gathered. The lists are compared to see if they're the same. If they're not the same, the database is updated to reflect local
     * changes.
     *
     * @return Up-to-date {@code ArrayList} of local media files.
     * @see Media
     */
    static ArrayList<Media> updateMedia() {
        ArrayList<Media> databaseMedia = getDatabaseMedia();    // Stores all media files from the database
        ArrayList<Media> localMedia = getLocalMedia();          // Stores all media files from the local folder

        /* Delete all media records if the local folder is empty */

        if (localMedia.size() == 0 && databaseMedia.size() != 0) {
            for (Media media : databaseMedia) {
                DB.deleteSQL("DELETE FROM tblMedia WHERE fldPath = '" + media.getPath(true) + "'");
            }

            return localMedia;
        }

        /* Delete records of non-existent media files */

        Iterator<Media> iterator = databaseMedia.iterator(); // An Iterator is used instead of a for-loop to avoid a ConcurrentModificationException

        while (iterator.hasNext()) {
            Media media = iterator.next();

            if (!localMedia.contains(media)) {
                DB.deleteSQL("DELETE FROM tblMedia WHERE fldPath = '" + media.getPath(true) + "'");

                iterator.remove();
            }
        }

        /* New media files are inserted into the database */

        for (Media media : localMedia) {
            if (!databaseMedia.contains(media)) {
                DB.insertSQL("INSERT INTO tblMedia (fldPath, fldTitle, fldArtist, fldLength) VALUES ('" + media.getPath(true) + "', '" + media.getTitle().replaceAll("'","''") + "', '" + media.getArtist().replaceAll("'","''") + "', '" + media.getLength() + "')");
                databaseMedia.add(media);
            }
        }

        return databaseMedia;
    }

    /**
     * Gets playlists from the database and returns it as an {@code ArrayList}.
     *
     * @return Up-to-date {@code ArrayList} of playlists.
     * @see Playlist
     */
    static ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>(); // Stores all playlists and related media files from the database

        DB.selectSQL("SELECT fldName FROM tblPlaylist");

        String field;

        while (!((field = DB.getData()).equals(DB.NOMOREDATA))) {
            playlists.add(new Playlist(field));
        }

        for (Playlist playlist : playlists) {
            DB.selectSQL("SELECT fldPath, fldTitle, fldArtist, fldLength FROM tblMedia WHERE fldPath IN (SELECT fldMediaPath FROM tblPlaylistMedia WHERE fldPlaylistName = '" + playlist.getName() + "')");

            while (!((field = DB.getData()).equals(DB.NOMOREDATA))) {
                String title = DB.getData();
                String artist = ((artist = DB.getData()).equals("null") ? "" : artist);
                int length = Integer.parseInt(DB.getData());

                playlist.getMediaList().add(new Media(field, title, artist, length));
            }
        }

        return playlists;
    }

    /**
     * Retrieves all records in the media table of the database as an {@code ArrayList}.
     * <p>
     * If {@code fldArtist} in the database is empty, artist is displayed as an empty {@code String} instead of {@code null}.
     *
     * @return {@code ArrayList} of database media files.
     * @see Media
     */
    private static ArrayList<Media> getDatabaseMedia() {
        ArrayList<Media> databaseMedia = new ArrayList<>(); // Stores all media files from the database

        DB.selectSQL("SELECT fldPath, fldTitle, fldArtist, fldLength FROM tblMedia");

        String field;

        while (!((field = DB.getData()).equals(DB.NOMOREDATA))) {
            String title = DB.getData();
            String artist = ((artist = DB.getData()).equals("null") ? "" : artist);
            int length = Integer.parseInt(DB.getData());

            databaseMedia.add(new Media(field, title, artist, length));
        }

        return databaseMedia;
    }

    /**
     * Retrieves all media files in the {@link #MEDIA_PATH} folder as an {@code ArrayList}.
     * <p>
     * {@link org.jaudiotagger} is used to retrieve {@code ID3} tags of audio files.
     *
     * @return {@code ArrayList} of local media files.
     * @see Media
     */
    private static ArrayList<Media> getLocalMedia() {
        ArrayList<Media> localMedia = new ArrayList<>(); // Stores all media files from the local directory

        try {
            File mediaDirectory = Files.createDirectories(Paths.get(MEDIA_PATH)).toFile();    // Store local media directory as a File object
            File[] files = mediaDirectory.listFiles(); // Array of File objects representing the files in the local folder

            if (files != null) {
                for (File file : files) {
                    String path = file.getAbsolutePath();                      // Absolute path of current media file
                    String fileType = Files.probeContentType(file.toPath());   // Check whether the file is an audio, video, or other type of file

                    if (fileType != null) {
                        if (fileType.contains("audio")) {

                            /* Get ID3 tags and track length using the library JAudioTagger */

                            AudioFile audioFile = AudioFileIO.read(new File(path));
                            Tag tag = audioFile.getTag();
                            AudioHeader header = audioFile.getAudioHeader();

                            String title = tag.getFirst(FieldKey.TITLE);
                            String artist = tag.getFirst(FieldKey.ARTIST);
                            int length = header.getTrackLength();

                            localMedia.add(new Media(path, title, artist, length));
                        } else if (fileType.contains("video")) {

                            /* Get video length by using a temporary JavaFX MediaPlayer */

                            javafx.scene.media.Media video = new javafx.scene.media.Media(Paths.get(path).toUri().toString());
                            MediaPlayer temp = new MediaPlayer(video);

                            temp.setOnReady(() -> {
                                DB.updateSQL("UPDATE tblMedia SET fldLength = " + (int) Math.round(temp.getTotalDuration().toSeconds()) + " WHERE fldPath = '" + path + "'");

                                temp.dispose();
                            });

                            localMedia.add(new Media(path, file.getName(), "", (int) Math.round(video.getDuration().toSeconds())));
                        }
                    }
                }
            }
        } catch (IOException | CannotReadException | ReadOnlyFileException | TagException | InvalidAudioFrameException e) {
            e.printStackTrace();
        }

        return localMedia;
    }

    public static void main(String[] args) {
        launch(args);
    }
}