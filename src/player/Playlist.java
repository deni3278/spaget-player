package player;

import java.util.ArrayList;

/**
 * Models a playlist of media files.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 1.0
 * @since 08.01.2021
 */
public class Playlist {
    private String name;
    private ArrayList<Media> mediaList = new ArrayList<>();

    public Playlist(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        DB.updateSQL("UPDATE tblPlaylist SET fldName = '" + name + "' WHERE fldName = '" + this.name + "'");

        this.name = name;
    }

    public ArrayList<Media> getMediaList() {
        return mediaList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }
}
