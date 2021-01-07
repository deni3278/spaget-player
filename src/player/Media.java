package player;

/**
 * Models a media file.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 3.0.1
 * @since 04.01.2021
 */
public class Media {
    private String path, title, artist;
    private int length;

    /**
     * Sole constructor.
     *
     * @param path absolute path to the media file
     * @param title title of media file
     * @param artist artist of media file
     * @param length length of media file
     */
    public Media(String path, String title, String artist, int length) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.length = length;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Media media = (Media) o;

        return path.equals(media.path);
    }
}
