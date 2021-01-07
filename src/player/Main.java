package player;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

/**
 * Main class of the {@code JavaFX} application.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 3.0.1
 * @since 04.01.2021
 */
public class Main extends Application {
    static final String MEDIA_PATH = Paths.get("media").toAbsolutePath().toString();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("player.fxml"));
        Parent root = loader.load();

        stage.setScene(new Scene(root));
        stage.setTitle("Spaget Player");
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("../resources/spaghetti.png")));
        stage.show();

        stage.setMinWidth(608.5 + (stage.getWidth() - root.prefWidth(0)));
        stage.setMinHeight(608.5 + (stage.getHeight() - root.prefHeight(0)));

        updateDatabase();
    }

    /**
     * Updates the database by adding new local media and/or remove non-existent media.
     * <p>
     * A list of database media paths and a list of local media paths are gathered.
     * The lists are compared to see if they're the same. If they're not the same, the database is updated
     * to reflect local changes.
     *
     * @return Up-to-date {@code ArrayList} of local media files.
     * @see Media
     */
    static ArrayList<Media> updateDatabase() {
        ArrayList<Media> databaseMedia = getDatabaseMedia();
        ArrayList<Media> localMedia = getLocalMedia();

        if (localMedia.size() == 0) {
            for (Media media : databaseMedia) {
                DB.deleteSQL("DELETE FROM tblMedia WHERE fldPath = '" + media.getPath() + "'");
            }

            return localMedia;
        }

        for (Media media : localMedia) {
            if (!databaseMedia.contains(media)) {
                DB.insertSQL("INSERT INTO tblMedia (fldPath, fldTitle, fldArtist, fldLength) VALUES ('" + media.getPath() + "', '" + media.getTitle() + "', '" + media.getArtist() + "', '" + media.getLength() + "')");
                databaseMedia.add(media);
            }
        }

        for (Media media : databaseMedia) {
            if (!localMedia.contains(media)) {
                DB.deleteSQL("DELETE FROM tblMedia WHERE fldPath = '" + media.getPath() + "'");
                databaseMedia.remove(media);
            }
        }

        return databaseMedia;
    }

    /**
     * Retrieves all records in the media table of the database as an {@code ArrayList}.
     *
     * @return {@code ArrayList} of database media files.
     * @see Media
     */
    private static ArrayList<Media> getDatabaseMedia() {
        ArrayList<Media> databaseMedia = new ArrayList<>();

        DB.selectSQL("SELECT fldPath, fldTitle, fldArtist, fldLength FROM tblMedia");

        String field;

        while (!((field = DB.getData()).equals(DB.NOMOREDATA))) {
            databaseMedia.add(new Media(field, DB.getData(), DB.getData(), Integer.parseInt(DB.getData())));
        }

        return databaseMedia;
    }

    /**
     * Retrieves all media files in the {@link #MEDIA_PATH} folder as an {@code ArrayList}.
     *
     * {@link org.jaudiotagger} is used to retrieve {@code ID3} tags of audio files.
     *
     * @return {@code ArrayList} of local media files.
     * @see Media
     */
    private static ArrayList<Media> getLocalMedia() {
        ArrayList<Media> localMedia = new ArrayList<>();

        try {
            File mediaFolder = new File(MEDIA_PATH);
            File[] mediaList = mediaFolder.listFiles();

            for (File media : mediaList) {
                String path = media.getAbsolutePath();
                String fileType = Files.probeContentType(media.toPath());

                if (fileType.contains("audio")) {
                    AudioFile file = AudioFileIO.read(new File(path));
                    Tag tag = file.getTag();
                    AudioHeader header = file.getAudioHeader();

                    localMedia.add(new Media(path, tag.getFirst(FieldKey.TITLE), tag.getFirst(FieldKey.ARTIST), header.getTrackLength()));
                } else if (fileType.contains("video")) {
                    localMedia.add(new Media(path, media.getName(), "null", 0));
                }
            }
        } catch (IOException | CannotReadException | ReadOnlyFileException | TagException | InvalidAudioFrameException e) {
            e.printStackTrace();
        }

        return localMedia;
    }

    /**
     * Disables {@link org.jaudiotagger} logging.
     */
    private static void disableLogger() {
        java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();

        try {
            manager.readConfiguration(Main.class.getResourceAsStream("../resources/logger.properties"));
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        disableLogger();

        launch(args);
    }
}