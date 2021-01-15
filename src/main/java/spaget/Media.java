package spaget;

/**
 * Models a media file.
 *
 * @author Denis Cokanovic, Morten Kristensen, Niclas Liedke, Rasmus Hansen
 * @version 4.0.0
 * @since 04.01.2021
 */
public class Media {
    private final String path;
    private final String title;
    private final String artist;
    private final String duration;
    private final int length;

    /**
     * Sole constructor.
     *
     * @param path   absolute path to the media file
     * @param title  title of media file
     * @param artist artist of media file
     * @param length length of media file
     */
    public Media(String path, String title, String artist, int length) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.length = length;

        duration = formatSeconds(length);
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

    public String getDuration() {
        return duration;
    }

    public int getLength() {
        return length;
    }

    /**
     * Converts seconds to {@code hours:minutes:seconds}.
     *
     * @param totalSeconds seconds to be converted
     * @return formatted {@code String}
     */
    static String formatSeconds(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return artist + title;
    }
}
